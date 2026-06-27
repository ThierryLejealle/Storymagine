# 2026-06-27 11h15 — StyleCorrector : répétitions lexicales directes

## Evolution demandée
Élargir la règle de répétition du StyleCorrector pour couvrir les répétitions
lexicales directes (même mot ou groupe significatif dans des phrases proches),
pas seulement les répétitions de structure ou de tournure syntaxique.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/writer/stylecorrector/StyleCorrector.java`
  Modification de la ligne "Repetitions de structure ou de tournure dans le meme passage"
  → "Repetitions de structure, de tournure, ou d'un meme mot ou groupe significatif dans des phrases proches"
  avec ajout d'un exemple illustrant la répétition lexicale.

## Contexte
Le cas manqué : "Il effaça la rature avec précipitation. Il prit sa gomme et effaça la rature."
Le groupe "effaça la rature" répété en deux phrases consécutives n'était pas couvert
par l'ancienne formulation orientée vers les répétitions syntaxiques.

## Résultat
Le StyleCorrector détecte désormais les répétitions de mots ou groupes significatifs
entre phrases proches, en plus des répétitions de structure.
