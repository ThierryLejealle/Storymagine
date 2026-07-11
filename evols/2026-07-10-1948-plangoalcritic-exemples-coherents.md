# 2026-07-10 19h48 — PlanGoalCritic : exemples du format corrigés

## 1. Demande

L'utilisateur a repéré que les deux exemples du bloc FORMAT STRICT de `PlanGoalCritic` étaient
incohérents avec le mandat réel de l'agent : *"L'ours n'a pas de chemise"* / *"Le lapin est vert"*
illustrent des défauts de cohérence factuelle générique, alors que cet agent évalue exclusivement
si le plan progresse vers la consigne et l'objectif narratif du chapitre — un mandat entièrement
différent (et déjà couvert, pour la cohérence factuelle, par `PlanCoherenceCritic`).

Delta présenté et validé avant écriture (règle projet).

## 2. Ce qui a été touché

### `PlanGoalCritic.java`
Exemples du FORMAT STRICT remplacés :

AVANT :
```
Exemple 1 - deux problèmes :
PROBLEME: "L'ours n'a pas de chemise"
PROBLEME: "Le lapin est vert"
```

APRÈS :
```
Exemple 1 - deux problèmes (objectif du chapitre : "installer la méfiance de Pierre envers son capitaine") :
PROBLEME: "Le plan ne fait jamais interagir Pierre et le capitaine — la méfiance ne peut pas s'installer si rien ne la déclenche"
PROBLEME: "La S3 montre au contraire Pierre complimentant chaleureusement le capitaine, ce qui va à l'encontre de l'objectif"
```

Le reste du prompt (procédure, format PROBLEME/[RIEN], règle "consigne jamais fautive") est
inchangé.

## 3. Résultat

Compilation vérifiée depuis la racine du projet (`mvn -q compile`, aucune erreur). Aucun autre
fichier touché.
