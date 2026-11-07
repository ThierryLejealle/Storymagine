# 2026-11-04 - Tableau récapitulatif de toutes les commandes dans le guide

## Évolution demandée
Le guide de rédaction décrivait chaque commande dans sa propre section, sans vue d'ensemble : l'auteur devait
parcourir tout le §2 pour retrouver la liste et la forme des commandes. Demande : un tableau synthétique.

## Ce qui a été touché
- `chatscenarios/guide-redaction-chat.md` : nouvelle section §2.0 « Toutes les commandes en un coup d'œil »
  (famille, commande, rôle, portée, renvoi au paragraphe) ; la ligne « Plan » en tête du guide la mentionne.
- `.claude/skills/chat-scenario-writing/SKILL.md` : version courte du tableau (famille, commande, rôle).
- Aucun code, aucun prompt d'agent modifié.

## Résultat
Le tableau couvre `pick`, `repeat` (fichier, N, casting), `cast`, `npc-delta`, `npc-story`, `npc-active/present/
passive/absent`, `npc-reset`, `npc-mode`, `npc-starts`, `npc-do`, ainsi que les lignes `[...]` et
`Quand écrire [NEXT ACT]`. Les syntaxes ont été relues dans les sections détaillées du guide.
**À maintenir** : toute nouvelle commande doit être ajoutée à ce tableau (§2.0 et le skill) dans la même évolution.
