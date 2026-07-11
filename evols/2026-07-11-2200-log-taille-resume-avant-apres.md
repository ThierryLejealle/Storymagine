# 2026-07-11 22h00 — Logs taille du résumé (avant/après/delta) et alerte de compaction

## 1. Demande

Suite à la refonte `ChapterSummarizer`/`SummaryCompressor` (voir
evols/2026-07-11-2130-refonte-resume-histoire-chaptersummarizer.md) : ajouter dans les logs la
taille du résumé avant/après (avec delta) à chaque chapitre, et une alerte explicite quand la
compaction se déclenche.

## 2. Ce qui a été touché

### Nouveau — `orchestrator/evaluate/ChapterSummaryResult.java`
Record retourné par `ChapterSummaryStep.run()` (remplace le simple `boolean` précédent) :
`wordsBefore`, `wordsAfterAppend`, `threshold` (SEUIL utilisé pour le test), `compressed`,
`wordsFinal` (== `wordsAfterAppend` si pas de compaction).

### `ChapterSummaryStep.java`
Calcule et retourne ces compteurs de mots à chaque étape (avant l'ajout, après l'ajout, après
compaction éventuelle) au lieu de se contenter d'un booléen.

### `EvaluateWorkflow.java`
- `log.step("ChapterSummarizer", ms, "{avant} -> {après} mots (+{delta})")` — toujours loggé.
- Si compaction déclenchée : `log.warn("Résumé : seuil dépassé ({après} > {seuil} mots) —
  compaction déclenchée")` puis `log.step("SummaryCompressor", ms, "{avant} -> {après} mots
  ({delta négatif})")`.

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**.
