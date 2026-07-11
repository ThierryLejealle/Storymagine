# 2026-07-11 10h20 — Correction du test en échec WorkflowLogTest (PLAN_ONLY)

## 1. Demande

`WorkflowLogTest.planOnly_doesNotLogWriteOrEval` échouait depuis au moins deux sessions
précédentes (2026-07-10 et 2026-07-11), à chaque fois documenté comme "pré-existant, sans rapport"
sans être corrigé. L'utilisateur a explicitement demandé de corriger l'échec immédiatement et a
posé une règle permanente : ne jamais laisser un test en échec, même qualifié de pré-existant.

## 2. Ce qui a été touché

### Diagnostic
`StoryOrchestrator.generate()` loguait `log.phaseHeader("WRITE", null)` et
`log.phaseHeader("WRITE chapitre N/M", ...)` **sans condition**, alors que l'appel réel à
`writeWorkflow.run(...)` était déjà correctement gardé par `config.qualityLevel().runsWriting()`.
Résultat : en mode `PLAN_ONLY` (`runsWriting=false`), les en-têtes de phase "WRITE" s'affichaient
quand même dans les logs, vides de tout contenu — pas juste un problème de test, un vrai bug de
logging (une phase annoncée qui ne se produit pas induit en erreur quiconque lit les logs).

### `StoryOrchestrator.java`
`runsWriting` extrait en variable locale, et les deux appels `log.phaseHeader("WRITE"...)` (global
et par chapitre) déplacés sous la même condition `if (runsWriting)` que `writeWorkflow.run(...)`.
`story.activateChapter(...)` reste inconditionnel (nécessaire pour `evaluateWorkflow`, qui tourne
que l'écriture ait eu lieu ou non).

## 3. Résultat

`mvn test -pl redacteur,commun` : **19/19 tests passent, 0 échec** (vs 18/19 avant ce fix).
Aucune régression sur les 3 autres tests de `WorkflowLogTest` ni ailleurs.

Règle permanente ajoutée en mémoire (feedback) : ne jamais laisser un test en échec dans une
session, même documenté comme pré-existant/sans rapport — le corriger avant de clore.
