# 2026-07-14 00h22 - Chat : simplification de la barre d'actes (nom courant + bouton Suivant)

## Demande

La rangée de boutons numérotés (un bouton par acte, 1 à N) devenait peu praticable avec un
scénario à 40 actes (`temple-noir-actes`), même rendue scrollable. Demande : n'afficher que le nom
de l'acte courant, avec un bouton "Suivant" qui ne révèle jamais le titre de l'acte suivant
(anti-spoiler) ; le "précédent" n'a pas d'intérêt et est abandonné.

## Ce qui a été touché

- `chat.html` : suppression de la rangée `#acts`/fonction `renderActs` (un `<button>` par acte,
  classes `done`/`pending`). Remplacée par `#actBar` : le nom de l'acte courant (`#actTitle`,
  inchangé) + un unique bouton `#nextActBtn` ("Suivant →"), désactivé (et masqué via
  `display:none`) quand il n'y a pas d'acte suivant ou pas d'actes du tout. Le bouton ne porte
  jamais le titre de l'acte suivant — seul son état actif/désactivé change.

## Résultat

`mvn -pl chat test` au vert (94/94, aucun test ne couvrait la rangée de boutons côté JS — UI pure,
vérifiée visuellement par relecture du flux `updateChrome`/`nextAct`). La barre d'actes reste
lisible quel que soit le nombre d'actes du scénario.
