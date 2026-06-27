# 2026-06-27 11h30 — Logs : précision du type de retry

## Evolution demandée
Les logs affichaient "RETRY 2/3" sans préciser de quel niveau de retry il s'agissait
(plan, séquence ou chapitre), rendant le suivi difficile.

## Ce qui a été touché
- `redacteur/.../orchestrator/plan/PlanWorkflow.java` → hint "PLAN N/M"
- `redacteur/.../orchestrator/write/WriteWorkflow.java` → hint "SEQUENCE N/M" (retry séquence)
- `redacteur/.../orchestrator/write/WriteWorkflow.java` → hint "CHAPITRE N/M" (retry chapitre)

## Résultat
Les logs affichent désormais :
  -> moy 6.00  RETRY SEQUENCE 2/3
  -> moy 6.47  RETRY CHAPITRE 2/3
  -> moy 5.00  RETRY PLAN 2/3
