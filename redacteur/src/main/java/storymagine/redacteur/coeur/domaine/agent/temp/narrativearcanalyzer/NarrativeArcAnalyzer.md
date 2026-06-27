# NarrativeArcAnalyzer

## Rôle
Analyse les arcs narratifs des personnages sur l'ensemble des plans de chapitres du livre.
Purement informatif — exécuté en fin de génération, sans impact sur la rédaction.

## Ce qu'il vérifie
- Arcs ouverts sans résolution
- Personnages qui disparaissent sans explication
- Arcs redondants entre personnages
- Tournants promis et jamais tenus

## Format de sortie
```
PROBLEME: [description courte d'un problème réel]
SCORE: N  (entier 0-10)
```
Si rien à signaler : `SCORE: 10` sans PROBLEME.

## Contrainte de taille
`plansText` est tronqué à `contextWindow/3` chars (plans de tous les chapitres concaténés).

## Relation avec CausalAnalyzer
`NarrativeArcAnalyzer` + `CausalAnalyzer` forment ensemble la critique narrative finale du livre,
correspondant aux 2 appels de `NarrativeCritiqueContext.analyze` dans le Redacteur original.

## Source Redacteur
`story.context.NarrativeCritiqueContext.evalArcs`
