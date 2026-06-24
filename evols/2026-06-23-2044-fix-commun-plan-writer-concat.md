# 2026-06-23 20h44 - Fix concaténation COMMUN+PLAN/WRITER et corrections annexes

## Evolution demandée
Corriger la logique de composition des données de scénario passées aux agents :
1. La règle COMMUN+PLAN (pour le Planner) et COMMUN+WRITER (pour le Writer) n'était pas respectée — le code faisait un OR au lieu d'une concaténation.
2. Le label "Contexte et lieux" dans ChapterPlanner ne reflétait pas le contenu (focus).
3. Coquilles dans scenario.md sur les marqueurs # PLAN / # WRITER.
4. Documenter dans TextCoherenceCritic.md que le focus séquence n'est pas transmis (choix de conception).

## Ce qui a été touché

### `ScenarioFormatters.java`
- Ajout de la méthode helper privée `joinContent(String global, String specific)`
- `focusText()` : `planContent OR globalContent` → `joinContent(global, planContent)`
- `loreText()` : idem
- `personnages()` : idem

### `ChapterPlanner.java`
- Label du slot focus : `"Contexte et lieux"` → `"Informations utiles"`

### `specs/scenario.md`
- Ligne 18 : `WRITE :` → `WRITER :`
- Lignes 70-71 : `#PLAN` → `# PLAN`, `#WRITER` → `# WRITER`

### `TextCoherenceCritic.md`
- Ajout d'une section "Périmètre du focus" expliquant que seul le focus chapitre est transmis, pas les overrides de séquence (choix délibéré : vérification post-hoc suffisante au niveau chapitre).

## Résultat
Les agents Planner et Writer reçoivent désormais la somme COMMUN + partie spécifique pour focus, lore et personnages, conformément à la spec scenario.md.
