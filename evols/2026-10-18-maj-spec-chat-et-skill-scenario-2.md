# 2026-10-18 - Mise à jour de specs/chat.md et skill d'écriture de scénario

## Évolution demandée
`specs/chat.md` décrivait encore l'ancien module (un seul `character.txt`, pas d'actes, pas de
multi-PNJ, pas de `[NEXT ACT]`). Le mettre à jour, puis créer un mini skill expliquant comment
écrire un scénario mono ou multi-personnages, la fiche personnage, les fichiers du scénario et le
mécanisme `[NEXT ACT]`.

## Ce qui a été touché
- `specs/chat.md` : réécrit (structure du dossier, Cast, scénario multi-fichiers, format des actes,
  fiche personnage, `[NEXT ACT]`, SpeakerSelector, agents, appel LLM Ollama/llama-server,
  endpoints, renvois vers `rules.md`).
- `.claude/skills/chat-scenario-writing/SKILL.md` : nouveau skill de projet.
- Aucun code ni prompt modifié.

## Résultat
La spec reflète le code actuel (vérifié sur `ChatFileStorageAdapter`, `NextActMarker`, `Teaser`,
`ChatPromptBuilder`, `ChatWebServer`). Le skill donne un guide pratique et pointe vers
`chatscenarios/rules.md` pour le détail. Point à noter : `[NEXT ACT]` passe en règle conditionnelle
dès que le texte de l'acte contient le marqueur, pas selon le libellé « Quand écrire ».
