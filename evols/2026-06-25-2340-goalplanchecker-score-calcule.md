# 2026-06-25 23h40 — GoalPlanChecker : score calculé, plus demandé au LLM

## Évolution demandée
Le LLM retournait `PROBLEME: [RIEN]` avec `SCORE: 9` — contradiction : aucun problème mais note inférieure à 10.
Le LLM inventait un score décorrelé des problèmes détectés.

## Solution
- Retirer du prompt toute notion de notation (échelle, étape 3, `SCORE:` dans le format et les exemples).
- Le LLM ne produit plus que des lignes `PROBLEME:`.
- Le score est calculé en Java selon le nombre de problèmes :
  0→10, 1→8, 2→6, 3→5, 4→4, 5→3, 6→2, 7+→1.

## Fichiers touchés
- `redacteur/src/main/java/.../goalplanchecker/GoalPlanChecker.java` — prompt allégé + méthode `scoreFromProblemCount`
- `redacteur/src/main/java/.../goalplanchecker/GoalPlanChecker.md` — doc mise à jour

## Résultat attendu
Score toujours cohérent avec les problèmes remontés. Moins de charge cognitive pour le LLM.
