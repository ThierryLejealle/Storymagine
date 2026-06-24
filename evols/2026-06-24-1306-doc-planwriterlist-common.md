# 2026-06-24 13h06 — Documentation PlanWriterList et section ## COMMON

## Évolution demandée
Documenter la convention PlanWriterList (checks/constraints en YAML) avec ses trois formes,
la dualité checks/constraints, et la section ## COMMON dans les fichiers .md.

## Ce qui a été touché

### specs/scenario.md
- Section `## CONSTRAINTS` et `## CHECKS` : ajout de la mention `## COMMON` optionnelle
- Section `# Structure COMMUN/PLAN/WRITER` : refonte complète
  - Fichiers .md : `# COMMON` optionnel, utilisable n'importe où, ne perturbe pas les autres sections
  - checks.md / constraints.md : `## COMMON` documenté
  - Champs YAML : les trois formes PlanWriterList documentées avec exemples

### specs/chapter.md
- Suppression de la note obsolète "Pas de contraintes par chapitre ni par séquence" dans defaults
- Suppression de la note obsolète "Pas de constraints par séquence" dans sequences
- `defaults.checks` : type mis à jour (PlanWriterList), agents mis à jour
- `defaults.constraints` : nouvelle section documentée
- `sequences[].checks` : type mis à jour, mention des planChecks par séquence au PlanCoherenceCritic
- `sequences[].constraints` : nouvelle section documentée
- Nouvelle section **Format PlanWriterList** avec les 3 formes et la règle de dualité

## Reste à implémenter (non fait dans cette évol)
- `## COMMON` dans les parsers .md (TagElementParser, CheckListParser, PersonnageParser)
- Clé `common:` dans PlanWriterListDeserializer (actuellement seul `plan:` et `writer:` sont reconnus)
