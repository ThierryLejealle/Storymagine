# 2026-06-23 14h30 - Structure unifiée des fiches personnage

## Description

Les fiches personnage utilisaient un format propriétaire (`## GÉNÉRAL / ## INTRIGUE / ## DESCRIPTION`)
incompatible avec le format des autres fichiers de tag (`[TAG] / #PLAN / #WRITER`).

Migration vers une structure unifiée pour focus.md, lore.md et les fiches personnage :
- **focus.md / lore.md** : `[TAG]` + contenu commun + `#PLAN` + `#WRITER`
- **characters/X.md** : contenu commun + `#PLAN` + `#WRITER` (pas de `[TAG]`, un fichier = un personnage)
- Les sous-sections dans `#PLAN` et `#WRITER` utilisent `##` (transmis comme markdown au LLM)

## Règles de validation (à implémenter)

- `#PLAN` et `#WRITER` (exact, casse stricte) : seuls marqueurs de section autorisés avec `#` simple
- `##` ou plus : sous-titres de contenu libres dans n'importe quelle section
- Tout autre `#` (seul) → erreur de validation `INVALID_FORMAT`

## Ce qui a été touché

### Code
- `PersonnageParser` — réécriture complète : abandonne `## GÉNÉRAL/INTRIGUE/DESCRIPTION`,
  utilise `#PLAN`/`#WRITER` comme `TagElementParser`

### Fiches personnage converties (14 fichiers × 2 emplacements = test fixture + production)
- `as-du-ciel` : pierre_moreau, henri_leclair, jules_meca, commandant_bertrand,
  oberleutnant_wolf, pilote_fw190_a, pilote_fw190_b, pilote_me109
- `une-rencontre` : eddie, maya
- `1998` : thierry, catherine, christelle, corinne

### Nettoyage focus.md
- `as-du-ciel/focus.md` (test fixture + production) : suppression de `# Éléments de focus`
  (ligne 1) et des groupes de focus scorie (`## TIR_MITRAILLEUSE`, etc., lignes 29-62)

### Mapping de conversion
| Ancien | Nouveau |
|---|---|
| `## GÉNÉRAL` | contenu global (avant `#PLAN`) |
| `## INTRIGUE` | `#PLAN` |
| `# privé` | `## Privé` (sous `#PLAN`) |
| `# inconscient` | `## Inconscient` (sous `#PLAN`) |
| `## DESCRIPTION` | `#WRITER` |

## Résultat

BUILD SUCCESS — 12/12 tests passent.
`ScenarioFormatters.personnages()` non modifié : il utilise `planContent()`/`writerContent()`
qui sont remplis identiquement par le nouveau parseur.
