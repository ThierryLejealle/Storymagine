# CausalAnalyzer

## Rôle
Analyse la cohérence causale entre les chapitres sur l'ensemble des plans du livre.
Purement informatif — exécuté en fin de génération, sans impact sur la rédaction.

## Ce qu'il vérifie
- Chaque événement important a-t-il une cause ?
- Chaque cause a-t-elle une conséquence ?
- Contradictions factuelles entre chapitres
- Conséquences importantes jamais exploitées

## Format de sortie
```
PROBLEME: [description courte d'un problème réel]
SCORE: N  (entier 0-10)
```
Si rien à signaler : `SCORE: 10` sans PROBLEME.

## Contrainte de taille
`plansText` est tronqué à `contextWindow/3` chars.

## Relation avec NarrativeArcAnalyzer
`CausalAnalyzer` + `NarrativeArcAnalyzer` forment ensemble la critique narrative finale du livre,
correspondant aux 2 appels de `NarrativeCritiqueContext.analyze` dans le Redacteur original.

## Source Redacteur
`story.context.NarrativeCritiqueContext.evalCausal`
