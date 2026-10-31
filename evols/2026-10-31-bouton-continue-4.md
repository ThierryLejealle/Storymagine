# 2026-10-31 - Bouton Continue : un tour de PNJ sans message du joueur

## Évolution demandée
Un bouton CONTINUE, plus faible que `DO:`, qui demande simplement aux PNJ d'agir comme d'habitude
« comme si le joueur avait répondu vide » : aucune consigne en plus, aucun texte écrit pour le joueur.
Annuler et Régénérer ne changent pas (ils reviennent au dernier vrai message du joueur).

## Ce qui a été touché
- `ChatService.continueScene` / `ChatServiceImpl.continueScene` : sélection des locuteurs habituelle
  (`SpeakerSelector.select` avec un message vide), sans interjection, aucun tour joueur ajouté ;
  refusé tant que personne n'a parlé. `previousRoundReplies` factorisé en `repliesBefore`.
- `ChatTurnResult.newTurns` : tolère un tour joueur nul.
- `ChatWebServer` : route `POST /continue` (même flux NDJSON que `/message`).
- `chat.html` : bouton « ▶ Continue », `sendMessage(text, continuation)` (pas de bulle optimiste, le
  brouillon du joueur n'est jamais écrasé), désactivé pendant une génération.
- Tests `ChatServiceImplTest` : aucun tour joueur, un seul locuteur sans interjecteur même à chance
  certaine, l'actif parle, refus avant tout échange, Régénérer après un Continue.
- Guide §4.9, `SKILL.md`, `specs/chat.md`.
- Aucun prompt modifié.

## Résultat
Tests du module chat verts. Après un Continue, Régénérer et Annuler refont le tour entier (réponses et
continuation) depuis le dernier message du joueur.
