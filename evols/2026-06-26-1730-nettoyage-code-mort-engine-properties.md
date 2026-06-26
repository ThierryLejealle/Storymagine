# 2026-06-26 17h30 — Nettoyage code mort : engine.properties

## Évolution demandée

Supprimer le fichier `redacteur/engine.properties` (code mort) et corriger la valeur
`planMaxRetry` dans `QualityLevel.FULL` pour la cohérence avec l'intention initiale.

## Diagnostic

`engine.properties` n'était jamais lu : la CLI charge `storymagine.properties`, pas `engine.properties`.
`EngineConfigLoader.java` avait déjà été supprimé lors du refactoring `QualityLevel` (2026-06-25).
Le fichier décrivait aussi l'ancienne architecture (3 boucles indépendantes par checker),
remplacée par la critique globale lors du refactoring du 2026-06-26-1630.

`QualityLevel.FULL` avait `planMaxRetry = 4` (5 tentatives) alors que l'intention
documentée dans `engine.properties` était `critique.plan.max_retry=3` (4 tentatives).

## Fichiers touchés

| Fichier | Changement |
|---|---|
| `redacteur/engine.properties` | Supprimé |
| `redacteur/coeur/domaine/orchestrator/QualityLevel.java` | `FULL.planMaxRetry` : 4 → 3 (4 tentatives au total) |
| `specs/retry-rules.md` | Réécrit pour refléter l'architecture actuelle (QualityLevel enum, critique globale) |

## Résultat

Plus de fichier de configuration fantôme. `QualityLevel.FULL` : 4 tentatives plan au total
(1 initiale + 3 retries), cohérent avec l'intention. `specs/retry-rules.md` à jour.
