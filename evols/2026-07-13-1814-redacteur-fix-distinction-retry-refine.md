# 2026-07-13 18h14 - Redacteur : correction de la distinction RETRY (note basse) vs REFINE (remarques seules)

## Demande

La fiche `2026-07-12-1822-log-info-refine-au-lieu-de-warn-retry.md` avait renomme TOUT statut
d'echec de `scoresSummary` de "RETRY" (rouge) vers "REFINE" (orange), sans distinguer la cause.
L'utilisateur a signale l'erreur (verbatim) : "tu as mal compris ma derniere demande. Quand on
retry un plan a cause d'une note moyenne basse ou note eliminatoire c'est bien un WARN rouge et un
RETRY rouge. COMME AVANT ! Fallait pas toucher ce cas la ! Si tu refais juste parce qu'il reste
des remarques : c'est un INFO orange (tu l'a mis rouge) et un REFINE orange (tu l'a mis rouge)".
Regle a appliquer : retry sur mauvaise note = WARN + RETRY rouge ; retry uniquement a cause de
remarques (notes bonnes) = INFO + REFINE orange.

## Ce qui a ete touche

- `commun/coeur/ports/LogPort.java` : `scoresSummary` prend un parametre supplementaire
  `boolean forcedRetry` ; javadoc precisant la semantique PASS/REFINE/RETRY.
- `commun/infra/ConsoleLogAdapter.java` et `FileLogAdapter.java` : nouvelle logique
  `statusLabel(passed, forcedRetry)` -> PASS (vert) si reussite normale, REFINE (orange) si notes
  bonnes mais relance forcee par des remarques, RETRY (rouge) si notes insuffisantes.
- `redacteur/.../ChapterPlanWorkflow.java` : `passed` renomme `scorePassed` (jamais ecrase),
  introduction de `forcedRetry` (regle stricte premier jet FULL, attempt 0, remarques presentes,
  notes bonnes), `willRetry = !scorePassed || forcedRetry`. Condition de sortie de boucle
  `if (!willRetry || isLastAttempt) break;`.
- `StoryPlanWorkflow.java` et `WriteWorkflow.java` : appels `scoresSummary` mis a jour avec
  `forcedRetry=false` (mecanisme non utilise a cet endroit).
- `CapturingLogPort.java` (tests) : `scores.add(passed && !forcedRetry ? "PASS" : passed ? "REFINE" : "RETRY")`.
- `ChapterPlanWorkflowTest.java` : `retryCount()` filtre desormais "RETRY" (et non plus "REFINE") ;
  assertions mises a jour sur les cas de note eliminatoire/moyenne basse (RETRY) et de remarques
  seules (REFINE).
- `redacteur/.../orchestrator/CLAUDE.md` : documentation corrigee (elle affirmait a tort que les
  deux cas utilisaient `log.warn`).

## Resultat

`mvn test` (module redacteur, complet) au vert. La distinction rouge/orange correspond de nouveau
exactement a la regle de l'utilisateur : RETRY rouge = probleme de note, REFINE orange = relance
de confort sur remarques uniquement.
