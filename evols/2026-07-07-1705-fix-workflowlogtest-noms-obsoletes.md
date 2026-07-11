# 2026-07-07 17h05 - Fix WorkflowLogTest (noms de step/phase obsolètes) + suppression ScenarioLoadTest foireux

## Évolution demandée

Suite au travail sur `PromptBuilder`, `mvn test` a révélé 2 échecs sur `WorkflowLogTest` en plus
de l'échec déjà connu de `ScenarioLoadTest`. L'utilisateur, pensant la fusion Check/Constraint →
Requirement et l'ajout du focus check propres, a demandé une vérification soigneuse avant de
corriger.

Vérification faite en comparant `WriteWorkflow.java`/`EvaluateWorkflow.java` au dernier commit
(`git show HEAD:...`) : les noms de step/phase en cause (`"SequenceWriter"`,
`"SequenceProofreaderCorrector"`, `"SequenceStateExtractor"`, `"SequenceRepetitionTracker"`,
`"SequenceRepetitionFilter"`, phase `"POST-PROD"`) sont **déjà dans le dernier commit**, avant
tout chantier Requirement/Focus en cours (non commité). Confirmé sans rapport avec ce chantier
ni avec `PromptBuilder` — bug de dérive de nommage ancien, jamais rattrapé par le test.

## Ce qui a été touché

### `WorkflowLogTest.java`
- `writeWorkflow_draft_logsAllWriterSteps` : les 5 assertions `hasStep(...)` utilisaient
  d'anciens noms (`"WriterStep"`, `"ProofreaderStep"`, `"StateExtractorStep"`,
  `"RepetitionTrackerStep"`, `"RepetitionFilterStep"`) qui ne correspondent à aucun nom
  réellement loggé (`log.step(...)` dans `WriteWorkflow.java` utilise le préfixe `Sequence`
  depuis le renommage `evols/2026-06-27-2030-refactor-agent-packages-writer-to-sequence.md`).
  Alignés sur les noms actuels.
- Cette même méthode asserte aussi que le correcteur `SequenceProofreaderCorrector` est loggé
  sur un run `GenerationConfig.brouillon()` — or `QualityLevel.BROUILLON.runsProofreader()` vaut
  `false` (doc de la classe : "minimum agents (StateExtractor + repetition memory only)"), donc
  aucun correcteur ne tourne en BROUILLON. Assertion inversée en `assertFalse` : vérifie
  désormais que le skip fonctionne bien, plutôt que d'attendre un comportement contraire à la
  conception documentée. Stub de mock `.when("fautes de langue", "PAS D'ERREUR")` devenu mort
  (proofreader jamais appelé dans ce test) — supprimé.
- `planOnly_doesNotLogWriteOrEval` : `hasPhase("EVAL")` → `hasPhase("POST-PROD")`
  (`EvaluateWorkflow.java` loggue `"POST-PROD"`, jamais `"EVAL"`, déjà au dernier commit).
  `steps.contains("WriterStep")` → `steps.contains("SequenceWriter")`.

### `ScenarioLoadTest.java` — suppression de `load_sequenceFocusRefsAreResolved`

Investigation du dernier échec restant (`Expected all chapter default focus to be FocusRef` —
obtenu `FocusInline`). Mécanisme retrouvé :
- `focus: ["CIEL", "MACHINE", "CAMARADERIE"]` dans `chap_1.yaml` est une liste YAML — une fois
  parsée, chaque élément est le mot nu `CIEL` (les crochets sont la syntaxe de liste YAML, pas
  des caractères de la chaîne).
- `ScenarioFileAdapter.extractTag()` n'identifie une référence de tag que si la **chaîne
  elle-même** commence par `[` littéral (ex. `"[CIEL]"`) — conforme à la conception documentée
  dans `evols/2026-06-26-1010-focus-deserializer.md` : le champ `focus` accepte volontairement
  un **mélange de tags pool et de texte libre dans la même liste**, désambiguïsé par cette
  syntaxe crochets explicite.
- Le fixture n'a jamais écrit `focus:` avec cette syntaxe crochets — donc `CIEL`/`MACHINE`/
  `CAMARADERIE` sont, par conception, du texte libre. `FocusInline` est le résultat correct.

Conclusion, validée avec l'utilisateur : le test partait d'une hypothèse fausse (un mot qui
ressemble à un nom de tag ne devient pas automatiquement une référence sans la syntaxe crochets
explicite) — et son nom (`load_sequenceFocusRefsAreResolved`) ne correspond même pas à ce qu'il
vérifie (`chap1.defaults()`, niveau chapitre, pas séquence). **Aucun code de production touché**
— méthode de test supprimée, sur décision explicite de l'utilisateur.

## Résultat

- `mvn -pl commun,redacteur -am test` : suite complète 100% verte (`WorkflowLogTest` 4/4,
  `ScenarioLoadTest` 7/7 restants), 0 échec.
