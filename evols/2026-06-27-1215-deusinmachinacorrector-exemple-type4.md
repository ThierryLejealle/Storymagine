# 2026-06-27 12h15 — DeusInMachinaCorrector : exemple CORRECTIONS pour le type 4

## Evolution demandée
Le SequenceDeusInMachinaCorrector détectait les listes narrativisées (type 4) via le Critic
mais ne produisait aucune correction : le modèle n'avait pas d'exemple montrant comment
formatter un FAUX/JUSTE pour un bloc de plusieurs phrases.

## Ce qui a été touché
- `redacteur/.../agent/writer/deusinmachinacorrector/DeusInMachinaCorrector.java`
  Ajout dans la section FORMAT DE RÉPONSE d'un exemple explicite pour le type 4 :
  FAUX = bloc complet de 4 phrases mécaniques
  JUSTE = version fusionnée en une phrase avec du poids

## Résultat
Le Corrector sait désormais citer le bloc complet comme FAUX et produire
une phrase de remplacement condensée comme JUSTE.
