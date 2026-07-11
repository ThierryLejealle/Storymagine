# ChapterCoherenceCritic

## Rôle
Vérifie la cohérence factuelle d'un texte de chapitre (points à vérifier, fiches personnages).
Ne juge pas la qualité narrative, littéraire ou stylistique.
Consomme le côté `check()` des `Requirement` du scénario — jamais le côté `constraint()`.

## Ce qu'il vérifie
- Faits établis et continuité factuelle dans le texte
- Points à vérifier explicites (`checks`, côté check des Requirement)
- Fiches personnages : faits physiques, psychologie, comportement

Le focus (`# CHECK` de `focus.md`) n'est volontairement PAS vérifié ici : ce sont des questions
de qualité d'exécution ("est-ce montré avec du détail concret ?"), pas de cohérence factuelle —
hors du périmètre de cet agent (voir `agent/CLAUDE.md` sur le mélange des rôles). Le côté check
du focus (`ScenarioFormatters.focusChecks()`) n'est aujourd'hui branché sur aucun agent actif.

## Format de sortie (3 lignes strictes)
```
AMELIORATION : [texte] ou [RIEN]
DEFAUT_SIGNIFICATIF : [texte] ou [RIEN]
DEFAUT_MAJEUR : [texte] ou [RIEN]
```

## Définitions des tiers
- **AMELIORATION** : détail factuel qui pourrait être plus précis (type de matériel, rang, toponyme)
- **DEFAUT_SIGNIFICATIF** : information qui contredit partiellement un fait établi ou le comportement attendu
- **DEFAUT_MAJEUR** : contradiction directe d'un check explicite ou d'un fait fondamental

## Slots
- text : 55% du contexte
- checks : 1/10

## Source Redacteur
`story.context.CriticContext.evalCoherence`
