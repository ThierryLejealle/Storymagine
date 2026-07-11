# PlanCoherenceCritic

## Rôle
Vérifie la cohérence factuelle d'un plan de chapitre (points à vérifier, fiches personnages).
Équivalent plan de `ChapterCoherenceCritic`. Ne juge pas la qualité narrative.
Consomme le côté `check()` des `Requirement` du scénario — jamais le côté `constraint()`.

## Ce qu'il vérifie
- Faits établis et continuité factuelle
- Points à vérifier globaux (`checks`, côté check des Requirement)
- Points à vérifier par séquence, injectés dans le plan JSON sous la clé `points_a_verifier`
  (voir `ScenarioFormatters.enrichPlanJson`)
- Fiches personnages : faits et psychologie

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
- **DEFAUT_SIGNIFICATIF** : information qui contredit partiellement un fait établi ou un point à vérifier
- **DEFAUT_MAJEUR** : contradiction directe d'un point à vérifier explicite ou d'un fait fondamental

## Source Redacteur
`story.context.CriticContext.evalPlanCoherence`
