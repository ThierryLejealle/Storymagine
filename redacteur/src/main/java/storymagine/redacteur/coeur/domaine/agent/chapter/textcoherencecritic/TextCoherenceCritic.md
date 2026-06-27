# TextCoherenceCritic

## Rôle
Vérifie la cohérence factuelle d'un texte de chapitre (checks, fiches personnages, contraintes, focus).
Ne juge pas la qualité narrative, littéraire ou stylistique.

## Ce qu'il vérifie
- Faits établis et continuité factuelle dans le texte
- Contraintes explicites (checks)
- Fiches personnages : faits physiques, psychologie, comportement
- Éléments de focus requis

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
- checks / constraints / focusText : 1/10 chacun

## Périmètre du focus
Le critic reçoit uniquement le focus de niveau chapitre (`chapter.defaults`).
Les focus overrides de séquence ne sont pas transmis : le critic vérifie que les éléments de focus globaux du chapitre sont présents dans le texte, sans descendre dans le détail propre à chaque séquence.

## Source Redacteur
`story.context.CriticContext.evalCoherence`
