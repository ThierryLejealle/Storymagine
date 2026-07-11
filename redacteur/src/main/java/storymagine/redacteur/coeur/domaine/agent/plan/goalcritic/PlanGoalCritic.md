# PlanGoalCritic (axe A — fidélité à la consigne)

## Rôle
Vérifie que le plan de chapitre livre, beat par beat, chaque élément explicite de la consigne
de l'auteur : but du chapitre, description, et directive propre à chaque séquence. C'est le seul
des 5 critics de plan qui EST la comparaison à la consigne — il n'a donc pas de clause de
primauté (contrairement à B/C/D/E qui doivent tous accepter qu'un écart voulu par la consigne
n'est jamais un défaut).

## Historique
Remplace/élargit l'ancien `PlanGoalCritic` (qui ne recevait que `chapterDescription`/`chapterGoal`/
`bookGoal`, sans les directives par séquence — jugement holistique "le plan avance-t-il vers
l'objectif ?" plutôt qu'une vérification littérale élément par élément, et un format de sortie
`PROBLEME:`/score-par-comptage différent des autres critics). Refonte à 5 axes orthogonaux
(A fidélité, B faits établis, C cohérence interne, D continuité, E qualité dramaturgique)
remplaçant les 3 anciens critics de plan (`PlanCoherenceCritic` faisait trop de choses,
`PlanNarrativeCritic` retiré — aucun ancrage factuel, jugement narratif trop flou), conçue avec
Fable (2 consultations conceptuelles, sans lecture de code).

## Ce qu'il vérifie
- Chaque élément explicite du but/description du chapitre apparaît-il dans le plan ?
- Chaque directive de séquence est-elle respectée par SA séquence dans le plan (pas juste
  "quelque part dans le chapitre") ?
- DEFAUT_MAJEUR : élément absent ou plan qui dit l'inverse.
- DEFAUT_SIGNIFICATIF : élément présent mais déformé (mauvaise séquence, affaibli au point de
  changer son sens, seulement suggéré).
- AMELIORATION : élément livré mais de façon vague.

## Hors périmètre
Cohérence interne du plan (C), faits déjà établis (B), continuité avec l'histoire (D), qualité
dramaturgique (E). Ce que le plan AJOUTE au-delà de la consigne n'est pas son problème non plus —
il ne suit que ce qui manque ou est trahi par rapport à ce qui est demandé.

## Format de sortie
`AMELIORATION:` / `DEFAUT_SIGNIFICATIF:` / `DEFAUT_MAJEUR:`, une ligne par problème préfixée `- `,
`[RIEN]` si vide. Score calculé par `CriticOutputParser` (jamais demandé au LLM) — remplace
l'ancien format `PROBLEME:`/`ProblemScoreParser`, désormais harmonisé avec les 4 autres critics
de plan.

## Langue
Le prompt système (règles, format) est en anglais — les petits LLM suivent souvent des
consignes anglaises plus fidèlement, y compris pour produire du contenu dans une autre langue.
La langue de sortie est paramétrée dynamiquement (`scenario.config().language()` via
`LanguageNames`), pas figée en dur — TODO : généraliser ce paramétrage aux ~20 autres agents du
projet qui ont encore un "En francais." en dur (voir mémoire projet).

## Source Redacteur
`PlanGoalCriticStep`, appelé depuis `ChapterPlanWorkflow`.
