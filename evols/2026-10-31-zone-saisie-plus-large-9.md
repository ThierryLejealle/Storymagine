# 2026-10-31 - Zone de saisie du chat plus large vers la gauche

## Évolution demandée
Avec le conseil de saisie affiché sous la barre, la zone de texte du message paraissait trop petite à
côté des boutons (Annuler, Régénérer, DO, Continue, Répondre, réglages). L'auteur veut l'agrandir vers la gauche.

## Ce qui a été touché
- `chat/src/main/resources/web/chat.html`, style par défaut seulement :
  - `form` passe de 80 % centré à 90 % collé à gauche (marge droite de 10 %) : le bord droit, donc la
    position des boutons, ne bouge pas, et la zone de texte gagne toute la place à gauche ;
  - `.hint` et `.settings-panel` reprennent le même alignement ;
  - `textarea` a une largeur minimale de 12 rem pour ne pas être écrasée sur une fenêtre étroite.
- Les modes d'affichage « immersif » et « script » gardent leur propre mise en page (ils imposent déjà
  leur largeur et leurs marges).
- Aucun code Java, aucun prompt modifié.

## Résultat
La zone de saisie est plus large, d'environ 12 % de la largeur du pied de page, du côté gauche. Réglage
fait sans voir l'écran : à confirmer visuellement dans le navigateur, avec les trois styles d'affichage.
