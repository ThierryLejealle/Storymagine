# 2026-08-05 - Fix transition abrupte scénario "route forestière -> camp du premier soir"

## Évolution demandée
L'utilisateur a signalé une transition de scène incohérente en session de chat : après une scène de
tension (Sheera repère des traces de sang et des griffures dans la jungle), la narration passait
directement, sans aucun raccord, à une note "Le jour décline vite sous la canopée. L'équipe cherche
un endroit dégagé..." — comme si les deux scènes n'avaient aucun lien.

Diagnostic mené en trois temps :
1. Vérification du code (`ChatSession.advanceAct`, `Teaser`, `RoleplayNarrator`) : le déclenchement
   de `[NEXT ACT]` était correct (condition de l'acte remplie), et le bloc `[...]` du nouvel acte est
   injecté tel quel comme tour NARRATOR, sans passer par un LLM — comportement voulu, pas un bug.
2. Vérification du contenu du scénario réel sur disque (`chatscenarios/scenario.txt`) : le lien
   logique existe bien dans le texte ("encore marquée par l'alerte du sentier"), donc pas une
   incohérence de fond.
3. Relecture complète des ~50 actes du scénario : un seul bloc `[...]` ne comble jamais l'écart de
   temps/tension entre les deux scènes qu'il enchaîne — "La route forestière" (Jour 1, signe
   inquiétant repéré) -> "Le camp du premier soir". Tous les autres enchaînements du scénario
   référencent déjà explicitement l'état précédent (combat, fièvre, attaque de nuit...).

## Ce qui a été touché
`chatscenarios/scenario.txt`, section "Jour 1 — la route forestière et le premier camp" / acte
"Le camp du premier soir" : le bloc `[...]` a été complété pour couvrir explicitement les heures de
marche tendue entre la découverte du signe inquiétant et l'installation du camp, au lieu de sauter
directement à la tombée du jour.

## Résultat
Le bloc `[...]` de "Le camp du premier soir" raconte maintenant la marche tendue qui suit la
découverte des traces, avant la tombée du jour — la transition ne se lit plus comme un saut de
scène sans rapport.

Aucun autre bloc du scénario ne présentait ce défaut après relecture complète du fichier.
