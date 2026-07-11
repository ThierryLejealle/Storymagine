# StoryNarrativeCritic

## Rôle
Équivalent livre de `PlanNarrativeCritic` : analyse les arcs narratifs des personnages sur
l'ensemble des plans de chapitres du livre. Exécuté par `StoryPlanWorkflow` une fois que tous
les chapitres ont été planifiés, avant toute rédaction.

## Ce qu'il vérifie
Uniquement ce que le plan **ajoute au-delà de la consigne de l'auteur** (description + objectif
de chaque chapitre, transmis en premier dans le prompt). La consigne elle-même n'est jamais un
défaut, même si elle décrit un départ, une disparition de personnage ou un événement soudain —
ce sont des choix de l'auteur.
- Contradiction entre deux ajouts de l'IA (détails, gestes, traits inventés) sur l'arc d'un
  personnage, d'un chapitre à l'autre
- Ajout redondant d'un chapitre à l'autre

## Format de sortie
Format à 3 paliers, identique à `PlanNarrativeCritic`/`PlanCoherenceCritic` :
```
AMELIORATION : ...
DEFAUT_SIGNIFICATIF : ...
DEFAUT_MAJEUR : ...
```
Parsé par `CriticOutputParser` (`parseProblems` + `calculateScore` — score calculé en Java,
jamais demandé au LLM).

## Contrainte de taille
`chaptersBlock` (consigne + plan par chapitre, concaténés) est tronqué à `contextWindow*4/3`
chars.

## Relation avec StoryCausalCritic et StoryFidelityCritic
Les trois agents tournent ensemble dans `StoryPlanWorkflow` : `StoryFidelityCritic` vérifie que
chaque plan respecte fidèlement sa consigne ; `StoryNarrativeCritic` et `StoryCausalCritic` ne
jugent que les ajouts de l'IA au-delà de cette consigne (arcs de personnages pour l'un,
causalité factuelle pour l'autre).

## Historique
Anciennement `NarrativeArcAnalyzer`, mis en sommeil le 2026-06-27 puis réactivé et repromté le
2026-07-07 (voir `evols/`) — l'ancien format PROBLEME:/SCORE: ne recevait pas la consigne de
l'auteur et risquait de critiquer les choix narratifs de l'auteur plutôt que la déclinaison du
LLM.

## Source Redacteur
`story.context.NarrativeCritiqueContext.evalArcs`
