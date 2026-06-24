# 2026-06-23 19h15 - PlanWorkflow : pas de retry si moyenne = 10

## Evolution demandée
Si la moyenne des agents critiques du workflow plan est égale à 10, il ne doit pas y avoir de retry.
La décision doit se baser **uniquement sur la note** (pas sur la liste de problèmes).

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/orchestrator/plan/PlanWorkflow.java`
  - Ligne 99 : ajout de la condition `|| avg >= 10.0` dans la condition de break

## Changement
```java
// Avant
if (!anyProblems || isLastAttempt) break;

// Après
if (!anyProblems || isLastAttempt || avg >= 10.0) break;
```

## Résultat
Quand les trois agents critiques (PlanNarrativeCritic, PlanCoherenceCritic, GoalPlanChecker) donnent tous 10,
la moyenne atteint 10.0 et le retry est stoppé immédiatement, indépendamment de ce que les listes de problèmes contiennent.
