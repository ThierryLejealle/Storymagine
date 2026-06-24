# 2026-06-24-1403 - Portée des infos per-séquence dans les prompts

## Évolution demandée
Préciser dans tous les prompts qui reçoivent le détail des séquences que les informations
contenues dans une séquence (checks, contraintes, focus, lore) ne s'appliquent qu'à cette séquence.
Exemple observé : une contrainte de S1 « aucun échange entre les personnages » était appliquée
par le LLM à toutes les séquences du chapitre.

## Ce qui a été touché

### PlanCoherenceCritic.java
Ajout d'une REGLE DE PORTEE explicite avant PROCEDURE OBLIGATOIRE :
- précise que les champs "checks", "contraintes", "focus", "lore" dans un objet séquence du JSON enrichi
  ne s'appliquent qu'à cette séquence
- sections globales du prompt ("Points a verifier", "Contraintes", "Éléments à utiliser") restent globales
- la ligne 1 de PROCEDURE OBLIGATOIRE précise désormais d'appliquer uniquement les checks/contraintes propres
  à chaque séquence lors de l'évaluation

### ChapterPlanner.java — FOCUS_LORE_NOTE
Extension de la note existante qui couvrait déjà focus et lore per-séquence :
- ajout des "contraintes" dans la liste des éléments à portée séquence uniquement
- reformulé : "ceux-ci s'appliquent uniquement à cette séquence, pas aux autres"

### Writer.java — buildSystem
Ajout d'une phrase dans le prompt système :
"La trame est découpée en séquences numérotées : checks, contraintes, focus ou lore propres
à une séquence ne s'appliquent qu'à cette séquence."
Préventif : le plan brut passé au Writer ne contient pas encore de checks/contraintes injectés,
mais la règle assure la cohérence avec les autres agents.

## Résultat
Le LLM ne devrait plus propager une contrainte de S1 aux séquences suivantes.