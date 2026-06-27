# 2026-06-27 20h15 — Désactivation agents EVAL → POST-PROD

## Evolution demandée

Le bloc `[EVAL]` exécutait 5 agents après chaque chapitre mais 4 d'entre eux n'affichaient
aucun résultat en console et n'entraînaient aucune conséquence (pas de score, pas de feedback loop).
Diagnostic :
- NarrativeArcAnalyzer et CausalAnalyzer analysent le PLAN, pas le texte rédigé → trop tard, inutile.
- ChapterStyleChecker et CharacterChecker sont des critics intéressants mais désactivés faute d'intégration.
- Seul StoryCompressor a une utilité réelle (met à jour le résumé cumulé de l'histoire).

## Ce qui a été touché

- `EvaluateWorkflow.java` : suppression des 4 steps inutiles, renommage du label `[EVAL]` → `[POST-PROD]`
- `RedacteurModule.java` : suppression des imports, instanciations et injection des 4 agents + steps
- `agent/global/` : déplacement des 4 packages vers `temp/agents/`
  - causalanalyzer, chapterstylechecker, characterchecker, narrativearcanalyzer
- `orchestrator/evaluate/` : déplacement des 4 Step vers `temp/steps/`
  - CausalAnalyzerStep, ChapterStyleCheckerStep, CharacterCheckerStep, NarrativeArcAnalyzerStep
- `specs/todo.md` : ajout section "Agents en attente" avec roadmap de réactivation

## Résultat

Le bloc `[POST-PROD]` ne lance plus qu'un seul appel LLM (StoryCompressor).
Les 4 agents sont archivés dans `temp/` pour réactivation future selon la roadmap dans specs/todo.md.

## Idée architecturale notée

Planifier TOUS les chapitres avant de commencer la rédaction permettrait à NarrativeArcAnalyzer
et CausalAnalyzer d'opérer sur l'ensemble des plans avec une vraie valeur ajoutée.
