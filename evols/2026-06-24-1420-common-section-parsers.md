# 2026-06-24 14h20 — Support section ## COMMON et clé common: dans les parsers

## Évolution demandée
Implémenter la convention `## COMMON` / `# COMMON` dans les parsers de fichiers .md
et la clé `common:` dans le désérialiseur YAML PlanWriterList.

## Ce qui a été touché

### PlanWriterListDeserializer.java
- Ajout de `case "common" -> dto.global = items;` dans la branche `START_OBJECT`
- La clé `common:` route maintenant vers `dto.global` (plan ET writer), comme une liste plate

### TagElementParser.java (parseSingleBlock + parse)
- Ajout de la reconnaissance de `#COMMON` et `# COMMON` → reset section à GLOBAL
- Applicable aux fichiers lore.md, focus.md, et fiches personnage (PersonnageParser délègue ici)

### CheckListParser.java
- Ajout de la reconnaissance de `## COMMON` (insensible à la casse via upper) → reset section à GLOBAL
- Placé avant `## PLAN` et `## R` pour éviter l'ambiguïté
- Applicable aux fichiers checks.md et constraints.md

## Résultat
Les trois formes PlanWriterList fonctionnent complètement :
- Texte simple `checks: "texte"` → global
- Liste plate `checks: [...]` → global
- Map `checks: {common: [...], plan: [...], writer: [...]}` → routage correct

Les fichiers .md peuvent utiliser `# COMMON` / `## COMMON` pour revenir en section globale
depuis n'importe quelle position dans le fichier, sans perturber les autres sections.

Compilation OK — aucune erreur.
