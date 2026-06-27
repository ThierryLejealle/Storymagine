# 2026-06-27 14h43 - Fix OOM ChapterPlanner.extractBeats

## Evolution demandée
Corriger un OutOfMemoryError (Java heap space) survenant dans `ChapterPlanner.extractBeats`
lors du parsing JSON d'un plan produit par le LLM.

## Cause
Dans la boucle while de `extractBeats`, si le JSON du LLM est malformé (beat sans guillemet
fermante), la variable `i` n'était jamais mise à jour : la boucle externe relançait la boucle
interne sur le même `q1` indéfiniment, ajoutant le même beat à la liste sans fin.

Stack trace observé :
```
java.lang.OutOfMemoryError: Java heap space
    at java.util.ArrayList.add (ArrayList.java:497)
    at storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlanner.extractBeats (ChapterPlanner.java:294)
```

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/chapterplanner/ChapterPlanner.java`
  - Méthode `extractBeats` : ajout d'un flag `closed` dans la boucle interne
  - Si la guillemet fermante est absente, `break` sur la boucle externe
  - Garde supplémentaire : `beats.size() < 50` dans la condition du while

## Résultat
La boucle s'arrête proprement sur un JSON malformé. Plus de boucle infinie, plus d'OOM.
Les beats normalement formés continuent d'être parsés correctement.
