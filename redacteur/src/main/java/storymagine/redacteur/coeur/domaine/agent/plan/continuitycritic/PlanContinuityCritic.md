# PlanContinuityCritic (axe D — continuité avec l'historique du livre)

## Rôle
Vérifie que le plan de chapitre s'enchaîne de façon cohérente avec l'histoire déjà écrite :
arcs en cours, ton général, fils narratifs que le lecteur s'attend à revoir. Recentré sur la
continuité **narrative** (pas factuelle — c'est le rôle de `PlanFactsCritic`, axe B, qui aurait
sinon fait doublon en comparant à peu près la même chose à deux granularités différentes,
conseil de Fable). Clause de primauté : une rupture d'arc/ton voulue par la consigne n'est
jamais un défaut.

## Historique
Nouveau agent. Avant cette refonte, rien ne vérifiait jamais qu'un chapitre cohère avec
l'histoire déjà écrite en dessous du niveau de qualité FULL (seuls les critics livre
`StoryNarrativeCritic`/`StoryCausalCritic` le faisaient, et seulement une fois tous les
chapitres planifiés). Voir `PlanGoalCritic.md` pour le contexte complet de la refonte à 5 axes.

## Cas du premier chapitre
Si `Story.summary()` est vide (aucun chapitre écrit avant celui-ci), l'agent renvoie directement
un résultat vide/parfait sans appeler le LLM — rien à comparer. Le `writingExample` du scénario
(exemple de style pour le Writer) n'est jamais traité comme un "chapitre 0" : il est explicitement
documenté comme un exemple de ton générique, sans lien avec l'intrigue réelle.

## Ce qu'il vérifie
- DEFAUT_MAJEUR : le plan inverse ou abandonne l'arc central en cours, sans que la consigne ne
  le demande.
- DEFAUT_SIGNIFICATIF : le plan ignore un fil narratif que le résumé présente comme pressant, ou
  brise le ton de l'histoire sans rien pour le motiver.
- AMELIORATION : le plan manque un callback naturel à un chapitre antérieur qui renforcerait la
  continuité.

## Hors périmètre
Faits ponctuels (blessures, objets, dates — rôle de B), fidélité littérale à la consigne (A),
cohérence interne du plan (C), qualité dramaturgique (E).

## Budget de contexte
`Story.summary()` est tronqué au même budget que le slot `summary` du Writer
(`SummaryBudget.charBudget`, fraction 1/8 partagée) — garde la fin du résumé (`tailText`), la
partie la plus récente et donc la plus pertinente pour juger la continuité immédiate.

## Format de sortie
`AMELIORATION:` / `DEFAUT_SIGNIFICATIF:` / `DEFAUT_MAJEUR:`, `[RIEN]` si vide, score calculé par
`CriticOutputParser`.

## Langue
Prompt système en anglais, sortie paramétrée dynamiquement via `LanguageNames` (voir
`PlanGoalCritic.md` pour le raisonnement complet).

## Source Redacteur
`PlanContinuityCriticStep`, appelé depuis `ChapterPlanWorkflow`.
