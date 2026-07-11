# StoryCausalCritic

## Rôle
Équivalent livre de `PlanCoherenceCritic` : analyse la cohérence causale entre les chapitres sur
l'ensemble des plans du livre. Exécuté par `StoryPlanWorkflow` une fois que tous les chapitres
ont été planifiés, avant toute rédaction.

## Ce qu'il vérifie
Uniquement ce que le plan **ajoute au-delà de la consigne de l'auteur** (description + objectif
de chaque chapitre, transmis en premier dans le prompt). La consigne elle-même n'est jamais un
défaut, même si elle décrit un événement soudain ou non expliqué — ce sont des choix de l'auteur.
- Contradiction factuelle entre deux ajouts de l'IA (faits, causes, conséquences inventés)
  d'un chapitre à l'autre

## Format de sortie
Format à 3 paliers, identique aux critics de plan (`PlanCoherenceCritic` et consorts) :
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

## Relation avec StoryNarrativeCritic et StoryFidelityCritic
Les trois agents tournent ensemble dans `StoryPlanWorkflow` : `StoryFidelityCritic` vérifie que
chaque plan respecte fidèlement sa consigne ; `StoryNarrativeCritic` et `StoryCausalCritic` ne
jugent que les ajouts de l'IA au-delà de cette consigne (arcs de personnages pour l'un,
causalité factuelle pour l'autre).

## Historique
Anciennement `CausalAnalyzer`, mis en sommeil le 2026-06-27 puis réactivé et repromté le
2026-07-07 (voir `evols/`) — l'ancien format PROBLEME:/SCORE: ne recevait pas la consigne de
l'auteur et risquait de critiquer les choix narratifs de l'auteur plutôt que la déclinaison du
LLM.

## Source Redacteur
`story.context.NarrativeCritiqueContext.evalCausal`
