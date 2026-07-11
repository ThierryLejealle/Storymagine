# PlanDramaCritic (axe E — effort dramaturgique relatif)

## Rôle
Vérifie que le plan de chapitre s'efforce d'être vivant et intéressant — pas plat, pas vide —
à l'intensité que la consigne elle-même appelle. Jugement TOUJOURS relatif à l'intention de
l'auteur, jamais un niveau de tension absolu : un chapitre calme explicitement demandé n'est
jamais "plat" pour son calme.

## Historique
Nouveau agent, suggéré par Fable lors de la consultation conceptuelle sur les 5 axes (aucun des
4 autres axes ne juge si le plan est BON, seulement qu'il ne trahit rien). Voir `PlanGoalCritic.md`
pour le contexte complet de la refonte.

La clause de primauté ici est différente des autres : pas "un écart voulu n'est jamais un
défaut" mais "le niveau jugé est relatif à ce que la consigne demande, pas absolu" — l'intensité
elle-même est fixée par l'auteur, pas par le critic.

## Ce qu'il vérifie
- DEFAUT_MAJEUR : le plan livre très en dessous de l'intensité annoncée par la consigne (la
  description promet une confrontation ou une révélation, les beats restent polis/statiques).
- DEFAUT_SIGNIFICATIF : une séquence est vide d'incident — ses beats ne font que reformuler la
  directive ou décrire un décor sans qu'il ne se passe rien, même tranquillement.
- AMELIORATION : des beats se répètent, ou un détail concret manquant rendrait un moment plus
  vivant dans l'intensité prévue.

## Hors périmètre
Fidélité littérale à la consigne (A), cohérence interne (C), faits déjà établis (B).

## Format de sortie
`AMELIORATION:` / `DEFAUT_SIGNIFICATIF:` / `DEFAUT_MAJEUR:`, `[RIEN]` si vide, score calculé par
`CriticOutputParser`.

## Langue
Prompt système en anglais, sortie paramétrée dynamiquement via `LanguageNames` (voir
`PlanGoalCritic.md` pour le raisonnement complet).

## Source Redacteur
`PlanDramaCriticStep`, appelé depuis `ChapterPlanWorkflow`.
