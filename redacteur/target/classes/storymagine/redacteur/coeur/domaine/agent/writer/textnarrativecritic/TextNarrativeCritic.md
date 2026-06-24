# TextNarrativeCritic

## Rôle
Évalue l'arc narratif d'un texte de chapitre terminé.
Ne juge pas la cohérence factuelle (c'est le rôle de `TextCoherenceCritic`).

## Format de sortie (3 lignes strictes)
```
AMELIORATION : [texte] ou [RIEN]
DEFAUT_SIGNIFICATIF : [texte] ou [RIEN]
DEFAUT_MAJEUR : [texte] ou [RIEN]
```

## Calcul du score
Calculé par `CriticOutputParser.calculateScore()` depuis les tiers.

## Définitions des tiers
- **AMELIORATION** : faiblesse quasi imperceptible, n'impacte pas l'arc
- **DEFAUT_SIGNIFICATIF** : le lien de cause à effet entre moments clés est absent,
  ou un ton contraire à l'objectif traverse une scène entière
- **DEFAUT_MAJEUR** : le texte contredit directement l'objectif du chapitre

## Ce qu'il ne signale pas
- Ellipses temporelles (si l'arc reste lisible)
- Choix de technique narrative (résumer vs montrer) si l'objectif est atteint

## Slots
- text : 55% du contexte
- bookGoal : 1/8

## Source Redacteur
`story.context.CriticContext.evalNarrative`
