# 2026-07-19 - Icone "revenir à ce message" sur les bulles du joueur

## Demande
Il existait deja une icone ↻ pour regenerer une reponse PNJ, et un bouton "Annuler" global (avec
un nombre de tours), mais rien pour revenir directement a un message du joueur plus haut dans la
conversation et le corriger.

## Ce qui a ete touche
`chat/src/main/resources/web/chat.html` uniquement (aucun changement service/domaine, aucun
prompt LLM touche) :
- Nouvelle icone ↺ sur chaque bulle joueur (symetrique de l'icone ↻ des bulles PNJ), ajoutee dans
  `appendTurn()`.
- Nouvelle fonction `rewindToHere(playerTurnEl, text)` : calcule combien de tours joueur separent
  la bulle cliquee de la fin de la conversation affichee (en comptant dans `renderedTurnEls`), puis
  appelle le meme endpoint que le bouton "Annuler" (`POST /undo?steps=N`) avec ce nombre exact —
  aucun nouvel endpoint serveur necessaire, `ChatService.undo(steps)` retirait deja depuis le Nieme
  tour joueur en partant de la fin. Confirmation demandee avant l'action (memes conventions que
  "Recommencer"). Le texte du message retire est remis dans la zone de saisie pour etre corrige
  avant renvoi.
- CSS : `.turn.player .speaker-row{justify-content:flex-end}` pour aligner l'icone a droite,
  cote bulles joueur (les bulles PNJ ont deja leur icone a gauche via le label du personnage).

## Verification
Pas d'outil de navigateur pilotable disponible dans cette session. Verifie a la place :
- `node -e "new Function(script)"` sur le bloc `<script>` extrait de chat.html : aucune erreur de
  syntaxe.
- Harnais Java jetable (supprime apres usage) qui demarre un vrai `ChatWebServer` (LLM bouchonne)
  et verifie en HTTP reel : la page servie contient bien le nouveau JS/libelle (pas de version en
  cache), 3 echanges produisent 6 tours, et `POST /undo?steps=3` retire exactement les 6 tours en
  remontant jusqu'au tout premier message — confirmant que le calcul de `steps` cote client
  correspond bien a la logique de `ChatService.undo` cote serveur.
- Suite de tests `chat` complete (deja verte, aucun test de service/domaine a ajouter puisque
  `/undo` existait deja et est deja couvert).

## Resultat
Chaque message du joueur affiche a l'ecran a desormais sa propre icone ↺ pour y revenir
directement : la conversation est retiree jusqu'a ce message inclus, et son texte est remis dans
la saisie pour etre modifie avant d'etre renvoye.
