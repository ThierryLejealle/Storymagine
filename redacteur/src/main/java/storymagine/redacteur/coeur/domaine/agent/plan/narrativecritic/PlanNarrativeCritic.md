# PlanNarrativeCritic

## Rôle
Évalue l'arc narratif d'un plan de chapitre. Équivalent plan de `ChapterNarrativeCritic`.
Ne juge pas la cohérence factuelle — uniquement la progression dramatique.

## Format de sortie (3 lignes strictes)
```
AMELIORATION : [texte] ou [RIEN]
DEFAUT_SIGNIFICATIF : [texte] ou [RIEN]
DEFAUT_MAJEUR : [texte] ou [RIEN]
```

## Calcul du score
Calculé par `CriticOutputParser.calculateScore()` à partir des tiers :
- 0 défaut majeur, 0 significatif, 0 amélioration → 10.0
- Chaque défaut majeur soustrait fortement (base 4.0 → 1.5)
- Chaque défaut significatif soustrait modérément (base 7.0 → 4.5)
- Les améliorations comptent peu (−0.5 chacune jusqu'à 3)

## Définitions des tiers
- **AMELIORATION** : faiblesse quasi imperceptible, n'impacte pas l'arc
- **DEFAUT_SIGNIFICATIF** : affaiblit une séquence entière ou brise un lien narratif
- **DEFAUT_MAJEUR** : contredit directement l'objectif du chapitre

## Source Redacteur
`story.context.CriticContext.evalPlanNarrative`
