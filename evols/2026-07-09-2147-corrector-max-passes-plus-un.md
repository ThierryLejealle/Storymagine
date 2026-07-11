2026-07-09 21h47 - Augmentation du nombre de relances des correcteurs + properties explicites

# Demande
Augmenter de 1 le nombre de relances (`maxRetryPasses`) accordées à chaque correcteur
(DeusInMachina, Naturality, Style, Proofreader) quand il a encore des findings au-delà du
seuil, en mode qualité activant `runsCorrectorsRepeat()` (FULL).

Au passage : `corrector.repeat.min.corrections` et `corrector.repeat.max.passes` étaient lues
depuis les properties dans `RedacteurCli.java` mais absentes de `redacteur.properties` — seul
`corrector.repeat.threshold.per.word` y était déclaré. Les deux autres tournaient donc
silencieusement sur leur fallback code, invisibles pour qui relit le fichier de config.

# Modifié
- `redacteur/src/main/resources/redacteur.properties` — ajout explicite de
  `corrector.repeat.min.corrections=7` (valeur inchangée, rendue visible) et
  `corrector.repeat.max.passes=3` (2 → 3, la demande initiale).
- `RedacteurCli.buildCorrectorConfig` — fallback `corrector.repeat.max.passes` "2" → "3"
  (cohérence si la propriété venait à disparaître du fichier).
- `CorrectorConfig.defaults()` — `maxRetryPasses` 2 → 3 (cohérence des assemblages hors CLI,
  ex. tests / `RedacteurModule.assemble` sans CorrectorConfig explicite).

# Résultat
Chaque correcteur peut désormais faire jusqu'à 3 passes de relance (au lieu de 2) tant qu'il
a encore des findings dépassant `corrector.repeat.threshold.per.word` ou
`corrector.repeat.min.corrections`, en mode FULL. Les trois propriétés du mécanisme de
re-correction sont maintenant toutes explicites et groupées dans `redacteur.properties`.
