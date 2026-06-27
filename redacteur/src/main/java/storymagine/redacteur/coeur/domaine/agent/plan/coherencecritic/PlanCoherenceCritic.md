# PlanCoherenceCritic

## Rôle
Vérifie la cohérence factuelle d'un plan de chapitre (checks, fiches personnages, contraintes, focus).
Équivalent plan de `TextCoherenceCritic`. Ne juge pas la qualité narrative.

## Ce qu'il vérifie
- Faits établis et continuité factuelle
- Contraintes explicites (checks)
- Fiches personnages : faits et psychologie
- Éléments de focus requis

## Ce qu'il ignore
- Progression narrative, qualité littéraire, grammaire, style

## Format de sortie (3 lignes strictes)
```
AMELIORATION : [texte] ou [RIEN]
DEFAUT_SIGNIFICATIF : [texte] ou [RIEN]
DEFAUT_MAJEUR : [texte] ou [RIEN]
```

## Définitions des tiers
- **AMELIORATION** : détail factuel qui pourrait être plus précis ou conforme à la fiche
- **DEFAUT_SIGNIFICATIF** : information qui contredit partiellement un fait établi ou un check
- **DEFAUT_MAJEUR** : contradiction directe d'un check explicite ou d'un fait fondamental

## Source Redacteur
`story.context.CriticContext.evalPlanCoherence`
