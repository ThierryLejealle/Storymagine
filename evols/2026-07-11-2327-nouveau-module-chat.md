# 2026-07-11 23h27 — Nouveau module `chat` : console de roleplay avec un LLM

## 1. Demande

Créer un nouveau module, entièrement en anglais (rupture volontaire avec la convention française
`redacteur`/`commun`/`testllm`), pour dialoguer en roleplay avec un LLM local (Ollama) : fiche
personnage du LLM + prémisse de scénario + résumé compacté régulièrement, pour tenir une
conversation longue sans mourir de contexte. Contraintes posées par l'utilisateur au fil de la
session :
- 4 conventions de formatage dans le chat : `*action*` (description physique, jamais du dialogue
  parlé), pensées privées entre `*astérisques*` jamais connues de l'autre personnage (no
  metagaming), `OOC: ...` (consigne hors-jeu, parfois utilisée pour négocier/planifier une future
  action avec le personnage du LLM), `DO: ...` (le joueur prend la main narrateur pour une
  réponse).
- Fichiers de scénario simples (pas de plan/chapitres comme redacteur) : juste fiche personnage +
  prémisse en texte brut.
- Historique conservé dans le répertoire du chatscenario, choix continuer/remettre à zéro au
  lancement.
- Interface web légère (pas de gros framework) façon `story.html`, avec champ de saisie + bouton.
- `chat.bat` à la racine, même style que `lancer.bat`/`test-llm.bat` (choix GPU + modèle).

Investigation Ollama en cours de session : le Modelfile du modèle custom de l'utilisateur
(`gemma4:e2b`, celui qui "marche nickel" en RP direct) a `TEMPLATE {{ .Prompt }}` et aucun
`SYSTEM` — donc aucun rôle system/user, juste du texte brut en continuation. L'`OllamaAdapter`
existant appelle toujours `/api/chat` avec des rôles, ce qui casserait ce rendu. Décision validée
(4 questions posées, toutes acceptées avec l'option recommandée) : nouveau `RawCompletionPort`
(`/api/generate`, `raw:true`, sans rôles) pour le tour de roleplay ; menu GPU/modèle extrait de
`RedacteurCli` vers `commun/ui/cli` (partagé) ; serveur HTTP embarqué JDK pur (pas de framework) ;
`DO:` = mode narrateur ponctuel (une seule réponse, avance la scène/les événements).

Prompt du `ChatSummarizer` (compaction périodique) conçu avec l'aide de Fable (consultation
validée par l'utilisateur, coût réel 23 623 tokens, 0 lecture de fichier) : le brouillon initial ne
protégeait pas les plans négociés en `OOC:` avant compaction — corrigé (voir delta présenté et
validé avant écriture, section "Prompt design" de `ChatSummarizer.md`).

## 2. Ce qui a été touché

### `commun` — extraction et nouvelle capacité (impacte aussi `redacteur`)
- **Nouveau** `commun/coeur/ports/RawCompletionPort.java` — capacité additionnelle de
  l'adaptateur Ollama (règle projet : port explicite pour toute capacité au-delà d'un port
  existant), `complete(prompt, temperature, ctx)` + `contextWindow()`.
- **Nouveau** `commun/infra/OllamaGenerateRequest.java` / `OllamaGenerateResponse.java` — DTOs
  pour `/api/generate` (`raw:true`, réponse dans `response` et non `message.content`).
- `OllamaAdapter.java` — implémente désormais aussi `RawCompletionPort` (`complete`/
  `completeInternal`/`sendRawSync`, même logique de retry/context-overflow que `sendSync`, mais
  toujours synchrone — pas de variante streaming pour le mode brut).
- **Nouveau** `commun/ui/cli/OllamaSetupCli.java` — extrait de `RedacteurCli` : menu GPU + choix
  du modèle (favoris en tête), `buildOllamaConfig(Properties)`, `loadFavoris(Properties)`.
  `RedacteurCli.java` mis à jour pour l'utiliser (suppression des méthodes dupliquées :
  `buildOllamaConfig`, `loadFavoris`, `printGpuOption`, `gpuLabel`, `formatSize`).

### Nouveau module `chat`
- `chat/pom.xml`, ajouté aux `<modules>` du pom racine.
- `chat/src/main/resources/chat.properties` — mêmes réglages Ollama que redacteur +
  `chatscenarios.root.default=C:\dev\llm\story\chat` + `chat.web.port=8765`.
- **Domaine** (`coeur/domaine/`) : `ChatScenario` (record), `ChatTurn` (record, enum
  `Speaker{PLAYER,LLM}`), `PlayerMessage` (record, parse du préfixe `DO:`), `ChatSession` (POJO
  mutable : tours + résumé courant), `ChatPromptBuilder` (assemble règles + fiche perso + scénario
  + résumé + tours récents + ligne du joueur, dans l'ordre — pas de rôles, prompt brut).
- **Agent** `coeur/domaine/agent/chatsummarizer/` : `ChatSummarizer` + Input/Output + `.md` (prompt
  validé avec Fable, voir §1).
- **Port** `coeur/ports/ChatStoragePort.java` — un seul port pour tout le répertoire
  chatScenariosRoot/{name}/ (scénario statique + session persistée), volontairement pas scindé en
  plusieurs ports.
- **Service** `coeur/service/` : `ChatService` (interface), `ChatServiceImpl` (construit le prompt,
  appelle `RawCompletionPort`, ajoute les 2 tours, déclenche la compaction si
  `SummaryBudget.wordBudget(contextWindow)` dépassé ET plus de `KEEP_RECENT_TURNS`(4) tours),
  `ChatTurnResult`.
- **Infra** `infra/ChatFileStorageAdapter.java` — implémente `ChatStoragePort` :
  `character.txt`/`scenario.txt` (statiques) + `history.md`/`summary.md` (persistés, format
  `### PLAYER`/`### LLM` + texte multi-ligne).
- **Assemblage** `ChatModule.java` — même instance `OllamaAdapter` injectée comme
  `RawCompletionPort` (tour de roleplay) et `ModelCallPort` (appel `ChatSummarizer`).
- **UI CLI** `ui/cli/ChatCli.java` — menu GPU/modèle partagé, choix du chatscenario, continuer/
  remettre à zéro, probe + info matériel, démarrage du serveur web + ouverture navigateur.
- **UI Web** `ui/web/ChatWebServer.java` (JDK `HttpServer`, exécuteur mono-thread — `ChatSession`
  n'est pas thread-safe) + `ChatHistoryView.java` + `chat.html` (page statique vanilla JS/CSS,
  style proche de `story.html`, bulles joueur/personnage, rendu `*actions*` en italique, tags
  `OOC:`/`DO:`).
- `chat.bat` à la racine (écrit en CP1252 via l'API .NET, comme `lancer.bat`).
- **Chatscenario d'exemple** `chatscenarios/temple-noir/` (`character.txt` : Sylka Corvenoire,
  voleuse ; `scenario.txt` : infiltration du temple du dieu déchu Vaskorreth pour voler la Larme
  de Vaskorreth, monde fantastique médiéval).

### Tests (`chat/src/test/java`, aucun vrai appel LLM — bouchons `RawCompletionPort`/`ModelCallPort`)
- `PlayerMessageTest` (5) — parsing `DO:`, insensibilité à la casse, `OOC:` reste `DIALOGUE`.
- `ChatPromptBuilderTest` (5) — pas de duplication de la ligne du joueur dans le prompt, tag
  `DO:` vs `Player:`, résumé vide omis / non-vide inclus, format du transcript.
- `ChatFileStorageAdapterTest` (5) — filtrage des scénarios incomplets, aller-retour
  sauvegarde/relecture avec tours multi-lignes (lignes vides internes), reset qui préserve les
  fichiers statiques.
- `ChatServiceImplTest` (4) — persistance après envoi, préfixe `DO:` conservé, pas de compaction
  tant que `turns.size() <= KEEP_RECENT_TURNS` même à budget minuscule, compaction effective une
  fois les deux conditions réunies (résumé rempli par le bouchon, 4 tours conservés).

## 3. Résultat

`mvn compile` depuis la racine : **BUILD SUCCESS**, 5 modules (commun, testllm, redacteur, chat +
parent). `mvn test -pl chat -am` : **19 tests, 0 échec**.

Non traité, explicitement laissé en l'état par l'utilisateur/Fable : fuite de metagaming possible
si les pensées privées du joueur (`*...*`) sont foldées dans le résumé puis réinjectées dans le
prompt du personnage LLM (note dans `ChatSummarizer.md`, à traiter si ça pose problème en usage
réel).
