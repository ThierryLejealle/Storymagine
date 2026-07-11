# 2026-07-07 16h00 - Note éliminatoire par critique (plan, séquence, chapitre)

## Demande
Jusqu'ici, une plannification ou une écriture de chapitre n'était rejouée que si la
**moyenne** des scores des critiques passait sous le seuil configuré (7 pour la séquence,
`chapitreThreshold` pour le chapitre, 10 — perfection — pour le plan). Un critique très mauvais
pouvait donc être « noyé » dans une moyenne acceptable si les autres critiques étaient bons.

Demande utilisateur : ajouter une note éliminatoire (ex. 4.5, paramétrable) — si **au moins un**
critique est en dessous de ce seuil sur une passe, on rejoue, même si la moyenne globale est
suffisante. Choix explicite de l'utilisateur : le seuil est paramétrable **par niveau de qualité**
(`QualityLevel`), sur le même principe que `chapitreThreshold` existant, plutôt qu'une valeur
unique globale.

## Ce qui a été touché

- `QualityLevel.java` : nouveau champ `eliminationThreshold` (double), une colonne par niveau
  (`PLAN_ONLY`, `BROUILLON`, `SIMPLE`, `FULL`), valeur initiale 4.5 pour les quatre niveaux
  (ajustable individuellement plus tard), nouvel accesseur `eliminationThreshold()`.
- `PlanWorkflow.java` (ligne ~90) : la décision `passed` combine désormais la moyenne (`avg >= 10.0`)
  ET l'absence d'élimination (aucun des 3 scores narrative/coherence/goal sous le seuil).
  Un `log.warn` signale le cas (rare, car le seuil de moyenne à 10.0 rend ce cas déjà quasi
  redondant en pratique) où l'élimination est la seule cause du rejet.
- `WriteWorkflow.java` :
  - `ChapterCritiqueResult` : ajout du champ `List<Double> scores` (jusqu'ici seule la moyenne
    et le texte des problèmes étaient conservés) pour permettre le test d'élimination au niveau
    chapitre.
  - `runChapterCritique()` : renvoie désormais aussi la liste des scores individuels.
  - `run()` (niveau chapitre, ligne ~178) : `passed` combine `avg > chapitreThreshold` ET
    absence d'élimination. `log.warn` si l'élimination est la seule cause du rejet.
  - `runSequenceCritique()` (ligne ~400) : `passed` combine `avg >= SEQUENCE_CRITIC_THRESHOLD`
    ET absence d'élimination. `log.warn` si l'élimination est la seule cause du rejet.
  - Javadoc de classe (schéma texte des phases) mis à jour pour documenter la nouvelle règle.
- `orchestrator/CLAUDE.md` : schémas ASCII (séquence + chapitre) mis à jour avec la condition
  d'élimination, et note explicative ajoutée en bas du document.

Non touché : pas de nouvelle propriété dans `redacteur.properties` — le seuil suit le même
mode de configuration que `chapitreThreshold` (constante par niveau dans l'enum `QualityLevel`),
pas une valeur runtime externalisée.

## Résultat
Un critique individuel sous le seuil éliminatoire du niveau de qualité actif force désormais
une relance (plan, séquence, ou chapitre), même si la moyenne globale des critiques passait le
seuil habituel. Le budget de relance et la logique de conservation de la meilleure passe
restent inchangés — seule la condition de succès (`passed`) est durcie. Compilation vérifiée
(`mvn -pl redacteur -am compile`) : OK.
