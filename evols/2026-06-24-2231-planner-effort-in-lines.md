# 2026-06-24 22h31 - plannerEffortInLines : nombre de lignes par séquence paramétrable

## Évolution demandée
Rendre configurable le nombre minimum de beats (lignes) par séquence produit par le ChapterPlanner.
Valeur par défaut : 25 (au lieu de 6 hardcodé).
Priorité de résolution : chapter > scenario > défaut agent.
Nom du champ : `plannerEffortInLines`.

## Ce qui a été touché

| Fichier | Modification |
|---------|-------------|
| `ScenarioConfigDto.java` | Ajout `Integer planner_effort_in_lines` (lecture YAML) |
| `ChapterDefaultsDto.java` | Ajout `Integer planner_effort_in_lines` (lecture YAML) |
| `ScenarioConfig.java` | Ajout champ `Integer plannerEffortInLines` + accesseur |
| `ChapterDefaults.java` | Ajout champ `Integer plannerEffortInLines` + accesseur ; `EMPTY` passe `null` |
| `ScenarioFileAdapter.java` | Passage de `planner_effort_in_lines` aux deux constructeurs |
| `ChapterPlannerInput.java` | Ajout champ `int plannerEffortInLines` dans le record |
| `ChapterPlanner.java` | Constante publique `DEFAULT_PLANNER_EFFORT_IN_LINES = 25` ; `%d` dans `JSON_PLANNER_SYSTEM` ; `String.format()` dans `buildSystem()` |
| `ChapterPlannerStep.java` | Méthode `resolveEffort()` (chapter → scenario → défaut) + passage à l'input |

## Résultat
- Défaut = 25 beats minimum par séquence (au lieu de 6)
- Surchargeable dans `scenario.md` : `planner_effort_in_lines: 15`
- Surchargeable dans le YAML d'un chapitre (section `defaults`) : `planner_effort_in_lines: 10`
- La valeur chapter gagne sur scenario, qui gagne sur le défaut agent
- Compilation vérifiée : OK
