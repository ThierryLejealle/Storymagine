# 2026-07-12 16h00 — Chat : scénarios progressifs (actes) + fixes de build

## 1. Demande

Implémentation de l'idée notée en mémoire (`project_chat_optional_sequences_idea.md`) : découper
un `ChatScenario` en actes numérotés optionnels (1, 2, 3...), activés dans l'ordre, en plus de la
fiche scénario de base toujours envoyée. But explicite de l'utilisateur : prompts plus petits et
anti-spoiler plus robuste (contenu futur physiquement absent du contexte, pas juste une consigne
de ne pas l'utiliser). Deux mécanismes de transition : bouton manuel côté joueur, et marqueur
`[NEXT ACT]` que le personnage (LLM) peut écrire lui-même quand il juge la scène terminée —
chaque acte porte sa propre consigne de "quand" l'utiliser.

Contrainte explicite : le scénario de base, la fiche personnage et l'acte courant ne sont **jamais**
résumés (contrairement à l'historique de tours, qui lui passe par `ChatSummarizer`).

Demande complémentaire : doubler le contexte Ollama du chat (32768 → 65536), ajouter un modèle
aux favoris (`gemma-4-26B-A4B-it-heretic-i1`), et créer un scénario de démonstration
(`temple-noir-actes`, variante à 8 actes de `temple-noir`) pour vérifier le mécanisme en usage
réel.

Noms de concepts validés avant codage : `ScenarioAct`, `NARRATOR` (finalement pas utilisé comme
`Speaker`, voir §2), marqueur `[NEXT ACT]`.

## 2. Ce qui a été touché

### Domaine (`chat/coeur/domaine`)
- `ScenarioAct` (nouveau) — `record(int number, String text)`.
- `ChatScenario` — + champ `List<ScenarioAct> acts()` (liste vide = comportement inchangé,
  rétrocompatible avec `temple-noir`).
- `ChatSession` — + `int currentAct` (0 = pas d'actes en jeu, sinon index 1-based de l'acte
  actif) + `advanceAct()` (avance d'un cran, no-op si déjà au dernier acte ou si acts vide).
  `fresh()` démarre à l'acte 1 si des actes existent, 0 sinon.
- `ChatPromptBuilder.build()` — signature `+ int currentAct`. Si un acte est actif, ajoute une
  section `CURRENT ACT (N of TOTAL)` juste après `SCENARIO`, avec un renvoi court à la même règle
  anti-spoiler (pas de duplication du paragraphe complet) plutôt qu'une nouvelle liste d'actes
  complète. Si ce n'est pas le dernier acte, ajoute la règle `[NEXT ACT]` juste après le texte de
  l'acte (pas dans le bloc FORMATTING, pour rester contextuellement liée à l'acte).
- **Décision de design** : pas de nouveau `ChatTurn.Speaker.NARRATOR` — la note de transition
  ("— Acte N commence —") suit le même pattern déjà en place pour la compaction (`compacted`,
  flag éphémère côté vue, jamais persisté comme un vrai tour) plutôt que d'étendre le format de
  persistance `history.md`. Plus simple, cohérent avec l'existant.

### Infra (`chat/infra/ChatFileStorageAdapter`)
- Lit `act-1.txt`, `act-2.txt`, ... dans l'ordre, arrêt au premier numéro manquant.
- Persiste `currentAct` dans un nouveau fichier `act.txt` (juste l'entier) ; `resetSession`
  le supprime aussi.

### Service (`ChatService`/`ChatServiceImpl`/`ChatTurnResult`)
- `ChatService` + `boolean advanceAct(Path, ChatSession)` (bouton manuel).
- `ChatServiceImpl.sendMessage()` — détecte `[NEXT ACT]` (regex insensible à la casse) dans la
  réponse LLM, le retire du texte stocké/affiché, avance l'acte si trouvé.
- `ChatTurnResult` — + `boolean actAdvanced`.

### Web (`ChatWebServer`/`ChatHistoryView`/`chat.html`)
- Nouvel endpoint `POST /next-act` (bouton manuel, pas d'appel LLM).
- `ChatHistoryView` — + `currentAct`, `totalActs`, `actAdvanced`.
- `chat.html` — rangée de boutons numérotés (actes passés grisés "done", le suivant seul
  cliquable, les futurs grisés "pending" — jamais de saut d'acte), note visuelle "— Acte N
  commence —" (même style que la note de compaction) affichée juste après la transition.

### Configuration
- `chat.properties` : `ollama.context-window` 32768 → 65536.
- `storymagine.properties` : `favoris.6=hf.co/mradermacher/gemma-4-26B-A4B-it-heretic-i1-GGUF:Q6_K`.

### Scénario de démonstration
`chatscenarios/temple-noir-actes/` — variante à 8 actes de `temple-noir` (même personnage Sylka
Corvenoire, même univers) : 1) repérage du prêtre suspect en ville, 2) fouille de sa maison,
3) découverte de la carte, 4) préparatifs le lendemain, 5) traversée de la jungle, 6) seuil du
temple, 7) premier piège (imaginé, cohérent avec le lore existant), 8) la Larme de Vaskorreth /
climax (imaginé, dernier acte — consigne explicite de ne jamais écrire `[NEXT ACT]` dedans).

### Bug pré-existant trouvé et corrigé (sans rapport avec le chat)
En lançant un `mvn clean` complet (jamais fait depuis le début de la session — les vérifications
précédentes tournaient en incrémental et masquaient le problème), découverte que
`TruncHelperTest.java` (module `commun`) ne compilait plus : une implémentation anonyme de
`LogPort` n'avait pas été mise à jour avec le paramètre `Boolean think` ajouté à `llmCall()` lors
d'une session antérieure. Corrigé (signature alignée). Rappel pour la suite : préférer `mvn clean
test`/`mvn clean compile` de temps en temps plutôt que du seul incrémental, qui peut masquer des
régressions.

### Tests
- `ChatPromptBuilderTest` — signatures mises à jour (+`currentAct`, +`List.of()` sur
  `ChatScenario`) + 4 nouveaux tests (section CURRENT ACT présente/absente, contenu limité au
  seul acte courant, règle `[NEXT ACT]` présente sauf sur le dernier acte).
- `ChatFileStorageAdapterTest` — signatures mises à jour + 4 nouveaux tests (lecture des
  `act-N.txt` avec arrêt au premier trou, défaut acte 1 si scénario avec actes et rien de
  persisté, round-trip `currentAct`, suppression de `act.txt` au reset).
- `ChatServiceImplTest` — 5 nouveaux tests (marqueur détecté et retiré du texte stocké + acte
  avancé, marqueur ignoré sur le dernier acte, pas de marqueur = pas d'avancement, bouton manuel
  avance et persiste, bouton manuel no-op après le dernier acte).

## 3. Résultat

`mvn clean test` depuis la racine (build complet, tous modules) : **BUILD SUCCESS**, 110 tests,
0 échec (17 commun + 5 testllm + 53 redacteur + 35 chat). Non encore testé en usage réel dans le
navigateur (pas d'accès à un LLM local depuis cette session) — à valider par l'utilisateur avec
`chat.bat` sur le scénario `temple-noir-actes`.
