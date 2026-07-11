# 2026-07-07 16h20 - Seuils Plan / Write distincts par niveau de qualité

## Description de l'évolution demandée

`PlanWorkflow` utilisait un seuil de moyenne codé en dur (`10.0`, jamais atteignable en
pratique sauf note parfaite) et réutilisait le `eliminationThreshold` du Write pour sa
propre note éliminatoire. Bug détecté en observant un log où le seuil affiché coïncidait
avec les scores (tous à 10.00 dans ce run), ce qui masquait le problème.

Demande : séparer complètement les seuils de la phase Plan de ceux de la phase Write
(séquence + chapitre), avec une valeur propre par niveau de qualité (`QualityLevel`).

Valeurs validées (moyenne / éliminatoire) :

| Niveau     | Write     | Plan      |
|------------|-----------|-----------|
| PLAN_ONLY  | 7.0 / 3.0 | 7.0 / 3.0 |
| BROUILLON  | 8.0 / 1.0 | 8.0 / 1.0 |
| SIMPLE     | 7.0 / 3.0 | 7.0 / 3.0 |
| FULL       | 7.0 / 5.0 | 8.0 / 5.5 |

## Ce qui a été touché

- `QualityLevel.java` : ajout de deux nouveaux paramètres `planAverageThreshold` et
  `planEliminationThreshold`, avec valeurs par constante d'enum (table ci-dessus).
  Les champs existants `chapitreThreshold` / `eliminationThreshold` sont conservés tels
  quels, désormais explicitement scope Write.
- `PlanWorkflow.java` : remplacement du seuil de moyenne codé en dur (`10.0`) par
  `config.qualityLevel().planAverageThreshold()`, et de l'appel partagé
  `eliminationThreshold()` par `planEliminationThreshold()`. Mise à jour du javadoc de
  classe en conséquence.
- `orchestrator/CLAUDE.md` : documentation des deux paires de seuils désormais
  distinctes par phase, avec la table de valeurs par niveau.

## Résultat

Compilation du module `redacteur` (et dépendances) validée (`mvn -pl redacteur -am
compile`). Le Plan a maintenant son propre couple de seuils, réglable indépendamment du
Write, pour chaque niveau de qualité.
