# 2026-07-07 16h33 - Affichage du score minimum face au seuil éliminatoire dans les logs

## Description de l'évolution demandée

La ligne de résumé (`-> moy X (seuil Y) PASS/RETRY`) ne montrait que la moyenne face à son
seuil, sans jamais indiquer si le score le plus bas passait le seuil éliminatoire. La seule
trace de ce contrôle était un `log.warn` ponctuel, déclenché uniquement quand il devenait la
seule cause de rejet — invisible dans le cas nominal.

Demande : afficher systématiquement le score minimum et le seuil éliminatoire sur la ligne
de résumé, pour Plan ET Write (séquence + chapitre).

## Ce qui a été touché

- `LogPort.java` : signature de `scoresSummary` étendue avec `minScore` et
  `eliminationThreshold` ; NOOP mis à jour.
- `ConsoleLogAdapter.java` / `FileLogAdapter.java` : nouveau format
  `-> moy X (seuil Y)  min Z (elim W)  PASS/RETRY`.
- `TeeLogAdapter.java` : transmission des deux nouveaux paramètres aux délégués.
- `PlanWorkflow.java` : calcul explicite de `minScore` (min des 3 critiques de plan),
  transmis à `scoresSummary`.
- `WriteWorkflow.java` : calcul de `minScore` aux deux points d'appel existants (critique
  séquence et critique chapitre), à partir des listes de scores déjà collectées.
- Tests : `CapturingLogPort.java` et `TruncHelperTest.java` (implémentations de `LogPort`)
  mis à jour pour la nouvelle signature.

## Résultat

Compilation complète du projet validée (`mvn compile test-compile` à la racine). Aucune
valeur de seuil n'a changé — seule la visibilité du contrôle éliminatoire dans les logs a
été ajoutée.
