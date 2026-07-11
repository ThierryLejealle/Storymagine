# PlanFactsCritic (axe B — faits déjà établis)

## Rôle
Vérifie que le plan de chapitre n'invente pas, de sa propre initiative, une contradiction avec
un fait déjà établi : fiches personnages, `checks` déclarés par l'auteur, état actuel des
entités. Clause de primauté : un changement voulu par la consigne (but + description du
chapitre) n'est JAMAIS un défaut, même s'il contredit une fiche ou un fait établi ailleurs —
seul ce que le plan ajoute AU-DELÀ de la consigne peut l'être.

## Historique
Nouveau agent, issu de la scission de l'ancien `PlanCoherenceCritic` (qui mélangeait fiches
perso + contraintes + continuité factuelle en un seul mandat). Voir `PlanGoalCritic.md` pour le
contexte complet de la refonte à 5 axes.

Exemple réel qui a motivé la clause de primauté : une opération médicale d'un personnage,
explicitement demandée par la consigne, avait été signalée à tort comme une "incohérence" par
l'ancien `PlanCoherenceCritic` — ce risque existe identiquement ici sans la clause.

## Ce qu'il vérifie
- DEFAUT_MAJEUR : contradiction avec un `check` ou un fait établi central (mort, membre perdu,
  identité révélée).
- DEFAUT_SIGNIFICATIF : contradiction avec un fait établi secondaire (compétence, lieu,
  relation).
- AMELIORATION : un beat cohabite maladroitement avec un fait établi sans le contredire.

## Hors périmètre
Fidélité littérale à la consigne (A), cohérence interne (C), continuité narrative avec
l'historique (D), qualité dramaturgique (E).

## Entrée
Le plan est enrichi (`ScenarioFormatters.enrichPlanJson`) — les `checks` par séquence sont déjà
injectés dans le JSON du plan lui-même (`points_a_verifier`), en plus des `checks` globaux
chapitre/scénario passés à part.

## Format de sortie
`AMELIORATION:` / `DEFAUT_SIGNIFICATIF:` / `DEFAUT_MAJEUR:`, `[RIEN]` si vide, score calculé par
`CriticOutputParser`.

## Langue
Prompt système en anglais, sortie paramétrée dynamiquement via `LanguageNames` (voir
`PlanGoalCritic.md` pour le raisonnement complet).

## Source Redacteur
`PlanFactsCriticStep`, appelé depuis `ChapterPlanWorkflow`.
