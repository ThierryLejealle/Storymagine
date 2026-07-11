# PlanCoherenceCritic (axe C — cohérence interne)

## Rôle
Vérifie que le plan de chapitre n'introduit pas, de lui-même, une contradiction ou une rupture
bizarre entre ses propres séquences (logique, causalité). Mandat volontairement étroit — pas de
fiches, pas d'état des entités, pas de consigne globale : le plan seul, plus les directives par
séquence (uniquement pour la clause de primauté). Clause de primauté : une rupture voulue par
une directive (ex. un revirement émotionnel explicitement demandé) n'est jamais un défaut.

## Historique
Ancien mandat bien plus large (fiches perso, points à vérifier, continuité factuelle avec l'état
des entités — voir git history) — jugé "mal nommé et fait trop" par l'utilisateur. Ces
responsabilités sont maintenant portées par `PlanFactsCritic` (axe B). Ce recentrage rend enfin
le nom "Coherence" fidèle au mandat réel de l'agent. Voir `PlanGoalCritic.md` pour le contexte
complet de la refonte à 5 axes.

Exemple qui a motivé la clause de primauté : deux personnages qui se détestent puis s'embrassent
brutalement — une rupture qui semble incohérente en soi peut être exactement ce que la directive
de la séquence demande.

## Ce qu'il vérifie
- DEFAUT_MAJEUR : une séquence est impossible compte tenu d'une précédente (personnage agit
  après sa mort, effet avant sa cause, lieu détruit intact à nouveau).
- DEFAUT_SIGNIFICATIF : une séquence contredit un état posé par une précédente (objet, blessure,
  connaissance qu'un personnage ne peut pas encore avoir).
- AMELIORATION : une transition entre séquences est abrupte ou inexpliquée, sans être
  contradictoire.

## Hors périmètre
Fidélité à la consigne globale (A), fiches/faits établis (B — c'est désormais son rôle exclusif),
continuité avec l'historique du livre (D), qualité dramaturgique (E).

## Format de sortie
`AMELIORATION:` / `DEFAUT_SIGNIFICATIF:` / `DEFAUT_MAJEUR:`, `[RIEN]` si vide, score calculé par
`CriticOutputParser`.

## Langue
Prompt système en anglais, sortie paramétrée dynamiquement via `LanguageNames` (voir
`PlanGoalCritic.md` pour le raisonnement complet).

## Source Redacteur
`PlanCoherenceCriticStep`, appelé depuis `ChapterPlanWorkflow`.
