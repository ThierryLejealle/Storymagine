# 2026-07-11 23h56 — Chat : abandon du mode brut, retour à `/api/chat`, fix langue

## 1. Demande

Premier test réel du module chat (fiche `evols/2026-07-11-2327-nouveau-module-chat.md`) : sur le
tout premier tour, le LLM répète le nom du personnage ("Sylka Corvenoire" x3) puis dérive en
répétition d'un seul mot ("scene scene scene..."). Historique brut visible dans
`chatscenarios/temple-noir/history.md`.

Diagnostic mené avec l'utilisateur, confirmé par deux consultations Fable (contexte fourni à la
main, lecture de fichiers interdite à chaque fois — coûts réels : 22 779 puis 23 120 tokens, 0
lecture de fichier) :
- Le mode `RawCompletionPort` (`/api/generate`, `raw:true`) retire l'échafaudage de template de
  chat qu'un modèle instruction-tuned (famille gemma) a appris à l'entraînement — le modèle tourne
  hors distribution. Confirmé par un test manuel de l'utilisateur : chatter directement dans
  Ollama (qui passe par `/api/chat`) avec une simple consigne "tu vas jouer le rôle de..." donne
  un résultat excellent immédiatement, alors que notre prompt à labels (`CHARACTER:`, `Player:`,
  `Character:` en fin de prompt) échoue dès le premier tour.
- Les patches en mode brut (stop sequences, renommage des en-têtes) n'auraient traité que le
  symptôme : le modèle dérive avant même de produire une frontière de tour, donc un stop sur
  `\nPlayer:` ne se serait jamais déclenché.
- Fable a fourni un system prompt concret (repris ci-dessous, avec deltas de l'utilisateur) et un
  conseil pratique : `ollama show <modele> --parameters` peut révéler un `num_ctx` par défaut trop
  petit — déjà couvert, `OllamaAdapter` fixe `num_ctx` explicitement à chaque appel.

Deltas apportés par l'utilisateur à la proposition Fable, après son propre test manuel :
- Retirer "Never speak or act for the player's character." — inutile en pratique (jamais
  nécessaire dans son test), et contradictoire avec `DO:` qui exige justement que le LLM agisse
  parfois à la place d'autres personnages.
- Garder tous les prompts en anglais (pas de traduction française du prompt système, malgré la
  proposition initiale de Claude d'aligner sur la convention `redacteur` où les prompts sont en
  français).
- Ajouter une consigne de langue dynamique ("réponds dans la même langue que le personnage et le
  scénario") plutôt que de coder la langue en dur — le contenu réel (fiche perso, scénario) fait
  foi, pas une décision figée dans le code.

## 2. Ce qui a été touché

### `ChatPromptBuilder.java` — refonte complète
`build()` retourne désormais un record `ChatPrompt(String system, String user)` au lieu d'un seul
bloc de texte brut :
- **system** = intro roleplay (avec la consigne de langue dynamique) + `YOUR CHARACTER:` + fiche
  perso + `SCENARIO:` + scénario + `STORY SO FAR:` (omis si vide) + bloc `FORMATTING` (4
  conventions, un exemple concret pour `*astérisques*`).
- **user** = `Recent exchange:` (transcript `Player:`/`Character:`, omis si aucun tour) + la
  nouvelle ligne du joueur (`DO: ...` si mode narrateur, sinon texte brut sans préfixe).
- Plus de cue final `Character:` ni de séquences d'arrêt : `/api/chat` gère nativement la fin de
  tour.

### `ChatServiceImpl.java`
Dépend de `ModelCallPort` (au lieu de `RawCompletionPort`) ; appelle
`llm.generate(prompt.system(), prompt.user(), TEMPERATURE, ctx)`.

### `ChatModule.java`
`assemble(ModelCallPort llm, ...)` — un seul port au lieu de deux (le même `OllamaAdapter` servait
déjà les deux interfaces, mais le roleplay et `ChatSummarizer` utilisent maintenant explicitement
le même chemin d'appel).

### `ChatCli.java`
`ChatModule.assemble(ollama, ollama)` → `ChatModule.assemble(ollama)`.

### `ChatSummarizer.java`
"Write factual prose in English" → "Write factual prose in the same language as the turns" — même
bug de langue en dur, même fix.

### `RawCompletionPort` / `OllamaGenerateRequest` / `OllamaGenerateResponse` / `OllamaAdapter.complete()`
Conservés tels quels dans `commun` (capacité générale potentiellement utile plus tard), simplement
plus utilisés par `chat`.

### Tests
`ChatPromptBuilderTest` réécrit pour le nouveau `ChatPrompt(system, user)` (8 tests, dont
non-duplication de la ligne du joueur à travers system+user, transcript replié dans `user`
uniquement). `ChatServiceImplTest` : bouchon `RawCompletionPort` remplacé par `ModelCallPort` (4
tests inchangés dans leur intention).

### Documentation
`ChatSummarizer.md` (sections "Prompt shape" et "Why /api/chat, not raw completion" réécrites,
note langue ajoutée), `specs/chat.md` (section "Appel LLM" réécrite).

## 3. Résultat

`mvn compile test` depuis la racine : **BUILD SUCCESS**, 5 modules. Module `chat` : **22 tests, 0
échec** (8 + 5 + 4 + 5, contre 19 avant cette session).

Non encore vérifié en usage réel avec le nouveau prompt (le précédent test manuel de l'utilisateur
était fait directement dans Ollama, pas encore via l'app avec le prompt system+user révisé) — à
confirmer au prochain lancement de `chat.bat`.
