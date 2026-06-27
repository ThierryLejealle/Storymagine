# 2026-06-27 15h00 — Affinement seuils de retry des Correctors

## Evolution demandée
Rendre les re-passes des Correctors plus sensibles pour les petits LLMs :
- Baisser le seuil ratio de `0.016` à `0.010` corrections/mot
- Ajouter un seuil absolu : retry si `>= 7 corrections` indépendamment du ratio
- Porter le nombre de re-passes de 1 à 2 (paramétrable)

## Ce qui a été touché
- `CorrectorConfig.java` — ajout de `minCorrectionsForRetry` (7) et `maxRetryPasses` (2) ; valeur par défaut du ratio passée à `0.010f`
- `RedacteurCli.java` — lecture des nouvelles propriétés `corrector.repeat.min.corrections` et `corrector.repeat.max.passes`
- `WriteWorkflow.java` — `exceedsThreshold` renommé `needsRetry` avec condition `ratio > threshold OR count >= min` ; `applyCorrectorsPhase` restructuré en boucles `for` paramétrées par `maxRetryPasses`

## Résultat
Condition de retry : `ratio(corrections/mots) > 0.010 OU count >= 7`
Jusqu'à 2 re-passes par correcteur (au lieu de 1).
Les messages de warn indiquent désormais les deux seuils.
