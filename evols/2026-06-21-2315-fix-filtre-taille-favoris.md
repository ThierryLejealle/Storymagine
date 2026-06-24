# 2026-06-21 23h15 - Fix filtre taille ignoré pour les modèles favoris

## Evolution demandée
Le 4ème modèle favori n'était pas testé en mode `benchFavoris`.
Le 3ème modèle (gemma-4-12B, qui supporte le thinking) était testé deux fois (thinking off + on),
ce qui donnait l'impression que le 3ème passait en double alors que le 4ème disparaissait.

## Cause racine
`BenchmarkRunner.run()` appelle `filtrer()` qui écarte les modèles dépassant `bench.maxModelSizeGo`
(14 Go par défaut). Le modèle `gemma-4-26B-A4B` à Q4_0 ≈ 14,5 Go est silencieusement écarté,
même en mode favoris où l'utilisateur a explicitement choisi ce modèle.

## Ce qui a été touché

### BenchmarkRunner.java
- Extraction de la logique principale de `run()` vers une méthode privée `execute()`
- Ajout de `runDirect()` : exécute le benchmark sans filtre de taille (respecte uniquement `maxModels` si > 0)
- `run()` continue d'appeler `filtrer()` (pour le mode `benchTous`)

### TestLlmServiceImpl.java
- `benchFavoris()` appelle désormais `runner.runDirect()` au lieu de `runner.run()`

## Résultat
En mode favoris, tous les modèles configurés dans `testllm.properties` sont testés
quelle que soit leur taille. Le filtre `bench.maxModelSizeGo` ne s'applique qu'au mode `benchTous`.
