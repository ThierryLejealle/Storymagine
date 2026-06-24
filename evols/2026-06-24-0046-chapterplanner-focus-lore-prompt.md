# 2026-06-24 00h46 — ChapterPlanner : corrections focus/lore prompt

## Évolution demandée

Deux problèmes dans les prompts du ChapterPlanner :

1. Les textes d'instruction ("Efforce-toi d'intégrer...", "N'hésite pas à piocher...")
   étaient injectés dans le user prompt collés aux données, au lieu du system prompt.

2. Le formatage des séquences dans le user prompt était incorrect :
   - Ligne vide parasite AVANT le focus/lore par séquence (due au `\n` final du YAML `>`)
   - Pas de ligne vide APRÈS le focus/lore (mauvaise séparation avec la séquence suivante)

## Ce qui a été touché

### `ChapterPlanner.java`
- Ajout de la constante `FOCUS_LORE_NOTE` : explication dans le system prompt
  que focus/lore globaux s'appliquent à toutes les séquences, et que les focus/lore
  par séquence s'ajoutent aux globaux (ne les remplacent pas).
- `buildSystem()` : injection de `FOCUS_LORE_NOTE` dans tous les modes (json, free, correction).
- `buildUser()` : suppression des textes d'instruction dans les données ;
  renommage des titres de section :
  - `"Éléments à utiliser (focus)"` → `"Éléments à utiliser (focus) — toutes les séquences"`
  - `"Informations utiles (lore)"` → `"Informations utiles (lore) — toutes les séquences"`
- `appendSequencesBlock()` : `\n` → `\n\n` après chaque séquence pour séparer visuellement.

### `ScenarioFormatters.java`
- `sequenceDescriptions()` : `.trim()` sur la directive pour supprimer le `\n` final
  du YAML (`>`), qui causait une ligne vide parasite avant le focus/lore par séquence.

## Résultat attendu

Format correct dans le user prompt :
```
1. Maya entre dans le compartiment...
Éléments à utiliser (focus) : Un vieux wagon d'autrefois...
Informations utiles (lore) : Une vieille gare de province

2. Maya sort un livre et lit...
```
