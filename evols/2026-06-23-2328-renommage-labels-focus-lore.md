# 2026-06-23 23h28 - Renommage des labels de sections focus et lore dans les prompts

## Evolution demandée

Harmoniser les labels des sections "focus" et "lore" dans tous les prompts d'agents,
pour que le LLM reçoive un signal sémantique clair et cohérent partout.

- "Informations utiles" était trop passif pour désigner du focus (éléments à appliquer activement)
- "Informations de référence" ne signalait pas qu'il s'agit de lore
- "Elements de focus demandes" (dans les critiques) n'était pas aligné avec les autres agents

## Ce qui a été touché

| Fichier | Avant | Après |
|---|---|---|
| `ChapterPlanner.java:114` | `"Informations utiles"` | `"Éléments à utiliser (focus)"` |
| `ChapterPlanner.java:115` | `"Informations de référence"` | `"Informations utiles (lore)"` |
| `Writer.java:109` | `"Éléments de focus"` | `"Éléments à utiliser (focus)"` |
| `Writer.java:110` | `"Informations de référence"` | `"Informations utiles (lore)"` |
| `PlanCoherenceCritic.java:65` | `"### Elements de focus demandes"` | `"### Éléments à utiliser (focus)"` |
| `TextCoherenceCritic.java:52` | `"### Elements de focus demandes"` | `"### Éléments à utiliser (focus)"` |

## Résultat

Labels unifiés dans les 4 agents : ChapterPlanner, Writer, PlanCoherenceCritic, TextCoherenceCritic.
Le mot "focus" indique explicitement que ces éléments doivent être utilisés (pas juste lus).
Le mot "lore" indique explicitement la nature référentielle de ces informations.
