# 2026-06-25 14h30 - ChapterPlanner prompt round 3

## Evolution demandee
Affiner le prompt JSON_PLANNER_SYSTEM du ChapterPlanner sur 5 points.

## Ce qui a ete touche
edacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/chapterplanner/ChapterPlanner.java
- Constante JSON_PLANNER_SYSTEM

## Changements (delta)

1. **Intro** : "si elle est courte, c'est sur ta creativite qu'on compte"
   -> "si elle est courte, developpe largement les evenements deja presents"
   Raison : "creativite" entre en conflit avec la regle "N'ajoute pas d'evenement nouveau".

2. **Nombre de beats** : "Choisis un nombre qui produit le decoupage le plus naturel"
   -> "Choisis le decoupage le plus naturel dans cette plage"
   Raison : reformulation qui ancre le choix dans la contrainte de la plage fournie.

3. **Definition du beat** : "un seul evenement. Il peut s'agir d'une action ; d'une reaction visible ; d'une parole ; d'une observation sensorielle. Un meme beat peut contenir plusieurs actions si elles appartiennent au meme mouvement narratif"
   -> "un seul moment narratif. Il peut contenir plusieurs actions lorsqu'elles appartiennent au meme mouvement"
   Raison : "evenement" est trop atomique ; "moment narratif" est la bonne unite conceptuelle.

4. **Exemple "Le courant passe"** : "Ils se sourient presque en meme temps"
   -> "Leurs regards se croisent ; aucun ne detourne immediatement les yeux"
   Raison : l'original contenait une interpretation implicite (simultaneite).

5. **Dernier bullet** : "Mefie-toi des formulations qui expliquent"
   -> "Evite les formulations qui expliquent"
   Raison : "Mefie-toi" demande une surveillance active (jugement) ; "Evite" est une instruction directe, plus robuste pour les petits modeles.

## Resultat
Prompt coherent, sans contradictions internes, instructions directes.