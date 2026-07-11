# 2026-07-11 21h30 — Refonte du résumé de l'histoire : ChapterSummarizer + SummaryCompressor

## 1. Demande

Suite à l'étude du chantier "budget Writer trop timide ?" : aucune troncature du slot `storySoFar`
observée sur les logs réels, le vrai plafond était `StoryCompressor` (800 + 200×chapitre mots). En
creusant `Story.storySoFar()`, découverte d'un bug latent : chaque chapitre stockait son propre
champ `summary` avec le résumé **cumulatif complet** de l'histoire à ce point (pas juste ce
chapitre), et `Story.storySoFar()` concaténait bêtement tous ces résumés cumulatifs entre eux —
duplication massive de l'historique envoyé au Writer.

Nouvelle architecture discutée et validée point par point avec l'utilisateur (et Fable pour les
prompts) :
- Un résumé indépendant par chapitre, écrit une fois et jamais retouché (protège les premiers
  chapitres de l'effacement progressif par les repasses de compression successives).
- Budget par chapitre en ratio (`motsChapitre / D`, `D` démarre à 5) plutôt qu'un forfait fixe.
- Garde-fou : si le résumé global dépasse un SEUIL, on le condense (~50%) et on double `D` pour les
  chapitres suivants — répétable indéfiniment (pas de plafond sur `D`, jugé non prioritaire par
  l'utilisateur, "même pas dans TODO").
- SEUIL mutualisé avec le budget `storySoFar` du Writer (même fraction du contexte, `1/8`), via une
  nouvelle classe partagée `SummaryBudget` (commun) plutôt que dupliquer le calcul.
- Nommage : `ChapterSummarizer` (résume un chapitre — "Summarizer" car vraie tâche de résumé) et
  `SummaryCompressor` (condense un résumé déjà condensé — "Compressor", cohérent avec l'existant).
- Modèle métier simplifié à la demande de l'utilisateur : un seul champ `String summary` sur `Story`
  (pas d'objet `RunningSummary` structuré) — `summary = summary + ChapterSummarizer(nouveauChapitre)`,
  puis `summary = SummaryCompressor(summary)` si le seuil est dépassé.
- Renommage de cohérence : `storySoFar` → `summary` partout (Story, WriterInput, ChapterPlannerInput).

## 2. Ce qui a été touché

### Nouveau — `commun/.../coeur/domaine/text/SummaryBudget.java`
`charBudget(ctx)` = `ctx*4/8` (caractères), `wordBudget(ctx)` = `charBudget(ctx)/6` (mots, 6
caractères/mot en moyenne pour du français). Source unique de la fraction 1/8, utilisée par
`Writer`, `ChapterPlanner` et le seuil de compaction.

### Renommage/scission — `agent/global/storycompressor` → deux nouveaux agents
- **`ChapterSummarizer`** (`agent/global/chaptersummarizer/`) — résume UN chapitre, indépendant,
  jamais retouché. Budget = `motsChapitre / divisor` (`divisor` = `Story.chapterSummaryDivisor`).
  Prompt : INCLURE (événements, état perso, relations, objets/lieux concrets) / EXCLURE (ambiance,
  ressenti ponctuel) — repris de l'ancien `StoryCompressor` mais sans le fold "résumé existant +
  nouveau chapitre".
- **`SummaryCompressor`** (`agent/global/summarycompressor/`) — condense le résumé global entier à
  ~50% quand le seuil est dépassé. Même structure INCLURE/EXCLURE que `ChapterSummarizer`, avec un
  ajout spécifique : arbitrer les faits devenus obsolètes entre chapitres ("si un développement
  plus récent l'a remplacé, ne garde que l'état le plus récent") — la seule vraie différence de
  mandat entre les deux agents.
- Ancien `StoryCompressor`/`StoryCompressorInput`/`StoryCompressorOutput`/`StoryCompressor.md`
  supprimés.

### `Story.java`
- `storySoFar()` (concaténation bugguée des `summary` par chapitre) → `summary()` (simple getter
  d'un champ `String summary`).
- Nouveau champ `chapterSummaryDivisor` (int, démarre à 5).
- `appendChapterSummary(String)` — concatène le résumé du nouveau chapitre.
- `compressSummary(String)` — remplace `summary` par la version condensée et double le diviseur.

### `WrittenChapter.java`
Champ `summary`/`setSummary()`/`summary()` supprimé — obsolète, plus personne ne stocke de résumé
au niveau chapitre (c'est maintenant uniquement sur `Story`).

### Nouveau — `orchestrator/evaluate/ChapterSummaryStep.java` (remplace `StoryCompressorStep`)
Appelle `ChapterSummarizer` sur le chapitre courant, fait `story.appendChapterSummary(...)`, calcule
`SummaryBudget.wordBudget(ctx)` (via `ChapterSummarizer.contextWindow()`, qui expose
`llm.contextWindow()` sans que le Step détienne lui-même un port), compare au nombre de mots du
résumé courant, et si dépassé appelle `SummaryCompressor` puis `story.compressSummary(...)`.
Retourne `boolean` (compaction déclenchée ou non) pour le logging.

### `EvaluateWorkflow.java`
Utilise `ChapterSummaryStep` au lieu de `StoryCompressorStep`. Logue `"ChapterSummarizer"`
systématiquement et `"SummaryCompressor"` seulement si la compaction s'est déclenchée.

### `WriterInput.java` / `Writer.java` / `WriterStep.java`
`storySoFar` → `summary` (champ, section prompt inchangée "Histoire jusqu'ici (résumé)"). Budget
`sHistory` recalculé via `SummaryBudget.charBudget(ctx)` au lieu de `ctx*4/8` en dur (TODO existant
sur le "1/8 peut-être trop conservateur" conservé, juste reformulé).

### `ChapterPlannerInput.java` / `ChapterPlanner.java` / `ChapterPlannerStep.java`
Même renommage `storySoFar` → `summary`, et bascule vers `SummaryBudget.charBudget(ctx)` au lieu du
`ctx*4/8` dupliqué en dur (troncature `t.text()` — tête gardée — inchangée, différente de celle du
Writer qui garde la fin ; pas dans le scope de cette session).

### `RedacteurModule.java`
Wiring mis à jour : `chapterSummarizer`/`summaryCompressor`/`chapterSummaryStep` remplacent
`storyCompressor`/`storyCompressorStep`.

### Tests
`WorkflowLogTest.evaluateWorkflow_alwaysLogsStoryCompressor` → renommé
`evaluateWorkflow_alwaysLogsChapterSummarizer`, assertion sur le nouveau label de step.

### Documentation
`ChapterSummarizer.md`, `SummaryCompressor.md` (nouveaux), `Writer.md`, `ChapterPlanner.md` mis à
jour pour le nouveau nommage.

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**.

**SEUIL calculé pour le scénario 1998** (`context_window: 32768`, identique au défaut projet) :
`SummaryBudget.wordBudget(32768)` = `(32768*4/8)/6` = **2730 mots**. Si le contexte grandit via
l'auto-grow d'`OllamaAdapter` (jusqu'au max configuré 131072), le seuil grandirait proportionnellement
jusqu'à ~10 900 mots.

Non traité, explicitement écarté par l'utilisateur ("même pas dans TODO") : plafond sur la
croissance du diviseur `D` pour les livres très longs — jugé pas un risque réel à ce stade.
