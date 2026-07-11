# StoryFidelityCritic

## Rôle
Nouvel agent (2026-07-07). Équivalent livre de `PlanFidelityCritic` (qui vérifie que le texte
rédigé exploite tous les beats d'un plan de séquence) — mais compare le **plan généré** de
chaque chapitre à la **consigne de l'auteur** (`description` + `goal` du chapitre), sur
l'ensemble du livre. Exécuté par `StoryPlanWorkflow` une fois que tous les chapitres ont été
planifiés, avant toute rédaction.

## Ce qu'il vérifie
Uniquement la fidélité du plan à sa consigne — jamais la pertinence ou la qualité de la consigne
elle-même, qui fait toujours foi :
- Omission d'un élément important explicitement demandé par la consigne
- Inversion d'un fait précis de la consigne (qui fait quoi, dans quel sens)
- Événement du plan opposé à celui demandé par la consigne

## Pourquoi cet agent existe
`StoryNarrativeCritic`/`StoryCausalCritic` ne critiquent que ce que le plan **ajoute** au-delà
de la consigne (voir leur documentation). Mais un plan peut aussi **trahir** la consigne sans
rien ajouter d'incohérent en soi — par exemple décrire une arrivée alors que la consigne demande
un départ. Ce cas de figure n'est ni un ajout d'arc ni une contradiction causale entre chapitres :
c'est un défaut de fidélité, le rôle propre de cet agent.

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

## Origine
Créé le 2026-07-07 après qu'une relecture manuelle d'un vrai scénario (voir
`evols/`) a montré que `StoryNarrativeCritic`/`StoryCausalCritic`, seuls, ne pouvaient pas
distinguer une trahison de consigne (bug réel) d'un choix narratif de l'auteur — d'où la
séparation en un agent dédié, strictement borné à la fidélité consigne ↔ plan.
