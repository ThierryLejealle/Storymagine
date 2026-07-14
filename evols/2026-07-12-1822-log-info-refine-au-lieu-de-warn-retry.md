# 2026-07-12 18h22 - Log [INFO] orange + renommage RETRY -> REFINE

## Demande

Quand ChapterPlanWorkflow relance un plan uniquement parce qu'il reste au moins une amelioration
(regle stricte premier jet, FULL), le log affichait un [WARN] rouge alors qu'il ne s'agit pas
d'une anomalie mais d'un comportement attendu. L'utilisateur souhaite :
1. Un nouveau niveau [INFO] (orange) pour ce cas, distinct de [WARN] (rouge).
2. Renommer le statut "RETRY" (rouge) affiche par scoresSummary en "REFINE" (orange), l'anglais
   de "affiner".

## Ce qui a ete touche

- `commun/coeur/ports/LogPort.java` : ajout de `default void info(String message) {}`.
- `commun/infra/ConsoleLogAdapter.java` : implementation de `info()` en orange (`[INFO]`) ;
  `scoresSummary()` bascule le statut d'echec de rouge "RETRY" a orange "REFINE".
- `commun/infra/FileLogAdapter.java` : implementation de `info()` (`[INFO] message`) ;
  `scoresSummary()` idem, "RETRY" -> "REFINE" (log fichier, sans couleur).
- `commun/infra/TeeLogAdapter.java` : relai de `info()` vers les delegues.
- `redacteur/.../ChapterPlanWorkflow.java` : le `log.warn(...)` de la regle stricte premier jet
  (FULL) devient `log.info("relance parce qu'il reste au moins une amelioration")`. Le `log.warn`
  du cas note eliminatoire (regle differente) reste inchange.
- Tests : `CapturingLogPort` (nouvelle liste `infos` + implementation `info()`, "RETRY" -> "REFINE"
  dans `scoresSummary`), `ChapterPlanWorkflowTest` (assertions "RETRY" -> "REFINE", verification du
  message deplace de `warnings` vers `infos`).

## Resultat

Compilation et suite de tests `redacteur` (complete) OK. Le statut d'echec de relance passe de
rouge "RETRY" a orange "REFINE" partout ou `scoresSummary` est utilise (plan chapitre, plan livre,
write). Le message explicatif de la regle stricte premier jet passe de [WARN] rouge a [INFO]
orange, avec un texte plus court centre sur la cause ("il reste au moins une amelioration").
