# 2026-06-23 19h06 - Fix parsing GoalPlanChecker / ProblemScoreParser

## Evolution demandée
Le LLM renvoyait un résultat mal formé (`PROBLEME: Aucune ligne PROBLEME:`) au lieu de `PROBLEME : [RIEN]`.
Le prompt a été corrigé pour imposer `PROBLEME : [RIEN]` quand il n'y a aucun problème.
Le code Java de parsing devait suivre.

## Ce qui a été touché
- `ProblemScoreParser.parseTagged` (méthode partagée par tous les agents PROBLEME/SCORE)

## Corrections apportées
1. Reconnaissance de `TAG :` (espace avant le deux-points) en plus de `TAG:` — les exemples du prompt utilisent la forme avec espace.
2. Filtrage de la valeur `[RIEN]` (case-insensitive) : une ligne `PROBLEME : [RIEN]` produit désormais une liste vide, pas un problème factice.
3. Suppression des guillemets autour des valeurs (les exemples du prompt encadrent les problèmes de `"`).

## Résultat
`parseProblems` retourne une liste vide quand le LLM répond `PROBLEME : [RIEN]`, et parse correctement les deux formes (`TAG:` et `TAG :`).
