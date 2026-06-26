# 2026-06-26 10h20 - Validation complète du scénario avant génération

## Evolution demandée
La validation ne détectait pas les références non résolues (focus, lore, personnages),
ni les erreurs de format YAML dans les chapitres. Un tag absent plantait en NPE durant la génération.

## Ce qui a été touché

- `ScenarioFileAdapter.java` — `validate()` étendue :
  - Charge les pools (focus.md, lore.md, characters/) et signale les erreurs de parsing
  - Nouveau `validateChapters()` : pour chaque .yaml, vérifie le parsing YAML, le titre, la présence de séquences
  - Nouveau `validateRefs()` : vérifie que chaque tag focus, id personnage et tag lore référencé existe dans son pool
  - Erreurs remontées via `ScenarioError.unresolvedRef()` et `ScenarioError.invalidFormat()`

## Résultat
Toute référence non résolue (focus, lore, personnage) est désormais détectée à la validation
et affichée à l'utilisateur avant le lancement de la génération.
