# 2026-07-07 15h57 - Fix logs retry manquants (TeeLogAdapter) + affichage seuil moyenne

## Demande

Utilisateur a remarqué un run où une séquence relance (`RETRY SEQUENCE 3/3`) avec une moyenne
(8.00) supérieure au seuil de retry (7.0), sans qu'aucune explication n'apparaisse dans les
logs — ni le seuil moyenne, ni la raison du retry (note éliminatoire d'un critique individuel,
`SequencePlanFidelityCritic` à 4.00). Demande de vérifier le même problème côté plan et writer.

## Ce qui a été touché

Root cause identifiée : `TeeLogAdapter` (utilisé par `RedacteurCli` pour dupliquer les logs
console + fichier) implémente `LogPort` directement et ne redéfinissait pas les méthodes à
implémentation par défaut *no-op* de l'interface (`warn`, `sequenceRetained`,
`chapterRetained`). Résultat : les appels `log.warn("... note eliminatoire franchie ...")`
dans `WriteWorkflow` (séquence et chapitre) et les lignes `-> sequence retenue : ...` /
`-> chapitre retenu : ...` ne s'affichaient jamais, ni en console ni dans le fichier —
alors que le code métier (seuil éliminatoire, ajouté dans la fiche du 2026-07-07-1600)
était correct.

- `LogPort.java` : `scoresSummary` prend désormais un paramètre `avgThreshold` (seuil moyenne
  utilisé pour la décision pass/retry), affiché à côté de la moyenne.
- `TeeLogAdapter.java` : ajout des 3 méthodes manquantes (`warn`, `sequenceRetained`,
  `chapterRetained`) qui forwardent vers tous les délégués, comme `planRetained` le faisait
  déjà. Signature de `scoresSummary` mise à jour.
- `ConsoleLogAdapter.java` / `FileLogAdapter.java` : `scoresSummary` affiche désormais
  `-> moy X.XX (seuil Y.Y)  PASS|RETRY ...`. `FileLogAdapter` n'implémentait pas non plus
  `warn()` (no-op hérité) — ajouté (écrit `[WARN] ...` dans master-log.txt).
- `PlanWorkflow.java` : appel `scoresSummary` avec seuil 10.0 (perfection requise).
- `WriteWorkflow.java` : appel `scoresSummary` avec `chapitreThreshold` (niveau chapitre) et
  `SEQUENCE_CRITIC_THRESHOLD` (niveau séquence).
- `CapturingLogPort.java` (test) et `TruncHelperTest.java` : signature mise à jour.

Non touché : la logique de décision retry elle-même (seuil moyenne + seuil éliminatoire) était
déjà correcte, seul l'affichage était en cause.

## Résultat

Les logs séquence/chapitre affichent maintenant explicitement le seuil moyenne dans la ligne
de résumé, et le message `[WARN] ... note eliminatoire franchie ...` s'affiche réellement
(console + fichier) quand un critique individuel force un retry malgré une moyenne suffisante.
Compilation vérifiée (`mvn -pl commun,redacteur -am compile` et `test-compile`) : OK.
Tests `WorkflowLogTest` et `TruncHelperTest` : OK.
