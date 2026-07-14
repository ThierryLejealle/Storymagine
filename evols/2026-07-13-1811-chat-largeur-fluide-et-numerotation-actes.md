# 2026-07-13 18h11 - Chat : largeur de fenetre fluide + numerotation d'actes en sous-chapitres

## Demande

1. La fenetre de chat avait une largeur fixe (`max-width:780px`), trop etroite sur grand ecran.
   L'utilisateur voulait une largeur fluide, "80% par exemple".
2. Possibilite de decouper un acte en sous-actes/sous-chapitres avec une numerotation a plusieurs
   etages (1, 1.1, 1.1.1).

## Ce qui a ete touche

- `chat.html` : `main`, `form`, `.hint`, `.acts`, `#actTitle` passent de `max-width:780px` a
  `width:80%` (fluide, centre).
- `chat/coeur/domaine/scenario/ActNumber.java` (nouveau) : numerotation d'acte en pointilles
  (1, 1.1, 1.1.1...), avec une methode `child(n)` pour deriver le numero d'un sous-acte.
- `ScenarioAct.java` : le champ `int` de numero d'acte devient un `ActNumber`.

## Resultat

Fenetre fluide confirmee ("Oui c'est top" implicite, largeur validee visuellement). La
numerotation a etages a servi de base a la refonte du scenario en fichier unique structure (voir
fiche suivante), qui est devenue la forme finale retenue pour les sous-actes.
