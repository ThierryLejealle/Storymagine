# 2026-07-13 18h13 - Chat : 40 sous-actes pour temple-noir-actes + phrase d'orientation manquante en ouverture

## Demande

1. Etoffer le scenario de demonstration `temple-noir-actes` avec une quarantaine de sous-actes
   interessants, en s'appuyant sur des propositions de beats de Fable (l'utilisateur a autorise
   explicitement une consultation Fable pour proposer les beats, la redaction en prose complete
   restant faite par Claude).
2. Bug remonte apres coup : "il manque quelques phrases introductives : on ne sait pas qui on est,
   qui est avec nous surtout et ce qu'on doit faire !!" — la toute premiere narration montree au
   joueur ne donnait aucune orientation d'identite/objectif.

## Ce qui a ete touche

- `chatscenarios/temple-noir-actes/scenario.txt` : reecriture complete en 8 actes de haut niveau
  x 40 sous-actes (numerotation 1.1 a 8.8), fil rouge sur le passe de Sylka Corvenoire et un motif
  recurrent de dette (theme du dieu dechu Vaskorreth). Une erreur de redaction (fusion accidentelle
  de deux actes "Le seuil"/"Le premier piege", 2 sous-actes manquants) a ete detectee via un test
  temporaire de verification (charge-compte les actes/beats, execute puis supprime) et corrigee.
- `chatscenarios/temple-noir/character.txt` et `chatscenarios/temple-noir-actes/character.txt` :
  ajout de la ligne `# Sylka Corvenoire` en tete.
- Fix orientation manquante : ajout d'une ligne `[...]` supplementaire en tete du premier acte
  ("# Le prêtre de Bragheval"), avant la ligne existante, rappelant explicitement qui est le
  joueur, qui est Sylka, et l'objectif (rapporter la Larme de Vaskorreth pour un commanditaire
  anonyme). Cette ligne devient le tout premier tour NARRATOR affiche au lancement de la session
  (voir ScenarioOutlineParser/Teaser : les lignes `[...]` du ou des ancetres nouvellement entres
  deviennent les beats d'ouverture de la premiere feuille).

## Resultat

`mvn test` (module chat) au vert. Le scenario compte bien 8 actes de haut niveau apres correction.
L'ouverture de session mentionne desormais explicitement l'identite du joueur, sa partenaire et
l'objectif du scenario.
