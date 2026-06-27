# 2026-06-27 14h21 — Seuil de re-correction des correctors (mode FULL)

## Evolution demandée
En mode FULL, si un corrector retourne plus de X corrections pour la longueur du texte,
le relancer une fois sur le texte déjà corrigé.
Si après la relance le seuil est encore dépassé, émettre un warning.
Le seuil est exprimé en corrections par mot (float) et configurable dans `redacteur.properties`.

## Ce qui a été touché

- **`CorrectorConfig.java`** (créé) — record `(float repeatThresholdPerWord)` + `defaults()` à 0.016
  (≈ 8 corrections / 500 mots), dans `orchestrator/write/`

- **`QualityLevel.java`** — ajout du champ `runsCorrectorsRepeat` (`true` uniquement pour FULL)
  et de l'accesseur correspondant

- **`WriteWorkflow.java`** — champ + paramètre constructeur `CorrectorConfig` ;
  logique de re-run dans `applyCorrectorsPhase` pour les 4 correctors
  (DeusInMachina, Naturality, Style, Proofreader) ;
  helpers `ratio(int, String)` et `exceedsThreshold(int, String)` ;
  log `(pass 2)` sur la relance, `log.warn` si seuil encore dépassé après relance

- **`RedacteurModule.java`** — nouvelle surcharge `assemble(..., CorrectorConfig)` ;
  les surcharges existantes délèguent avec `CorrectorConfig.defaults()`

- **`RedacteurCli.java`** — méthode `buildCorrectorConfig(Properties)` ;
  lecture de `corrector.repeat.threshold.per.word` ; passage au module

- **`redacteur.properties`** — nouvelle propriété `corrector.repeat.threshold.per.word=0.016`

## Résultat
En mode FULL, chaque corrector qui dépasse le seuil est automatiquement relancé une fois.
Le seuil est ajustable sans recompilation. Les modes BROUILLON et SIMPLE sont inchangés.
