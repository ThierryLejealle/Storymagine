# 2026-06-21 17h00 - Refactor RunLogger → sealed BenchEvent

## Description de l'évolution demandée

Remplacer l'interface `RunLogger` à 10 méthodes par un pattern event typé :
`sealed interface BenchEvent` + `RunLogger.onEvent(BenchEvent)`.
Objectif : rendre l'ajout d'un nouvel événement non cassant pour les implémentations
existantes et réduire la surface de l'interface de 10 méthodes à 1.

## Ce qui a été touché

### `testllm/coeur/domaine/BenchmarkRunner.java`
- Suppression des 10 méthodes de `RunLogger`
- Ajout de `sealed interface BenchEvent` avec 10 records imbriqués :
  `ModelStart`, `ProbeOk`, `ProbeFail`, `RunOk`, `RunFail`, `Divergence`,
  `PasseComplete`, `IterationSummary`, `PasseDone`, `ModelUnload`
- `RunLogger` réduit à `@FunctionalInterface void onEvent(BenchEvent)`
- Tous les appels `log.onXxx()` dans `run()` et `runPasse()` remplacés par
  `log.onEvent(new BenchEvent.Xxx(...))`

### `testllm/infra/ConsoleRunLogger.java`
- Les 10 `@Override` remplacés par un seul `onEvent(BenchEvent event)` avec
  `switch(event)` exhaustif sur les 10 types (Java 21 pattern matching)

## Résultat

- Interface `RunLogger` stable à 1 méthode quelle que soit l'évolution du domaine
- Le compilateur signale tout oubli grâce au switch exhaustif sur sealed
- Les events sont des records immuables auto-documentés
- `ConsoleRunLogger` peut maintenant être remplacé par une lambda pour les tests
