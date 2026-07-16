# 2026-07-15 22h00 - Bouton "Recommencer" le scenario

## 1. Demande

"Et un bouton restart pour le scenario ?" — jusqu'ici, remettre une session a zero necessitait de
redemarrer `chat.bat` et repondre "n" au prompt "Continuer la conversation ?". Demande d'un bouton
equivalent directement dans le chat. Placement discute en direct : au-dessus de "Sauvegarder" puis
"en dessous très bien aussi" — garde au-dessus, avec pop-up de confirmation (destructif, meme regle
que "charger une sauvegarde").

## 2. Ce qui a ete touche

- `ChatService.restartSession(...)` (interface) + `ChatServiceImpl` : reutilise
  `storage.resetSession(...)` (meme mecanisme que le reset CLI — archive history/summary/act/present,
  ne supprime rien) puis `ChatSession.fresh(scenario)`, applique en place sur la session live via
  `session.restore(...)` (meme pattern que `loadSavePoint`) — l'objet `ChatSession` reste le meme,
  seul son etat change. Presence remise a "tout le monde present" (repart de zero, aucun mute ne
  survit).
- `ChatWebServer` : nouvel endpoint `POST /restart`, meme forme de reponse que `/load-save` (repaint
  complet cote client).
- `chat.html` : bouton "🔄 Recommencer" au-dessus de "💾 Sauvegarder" dans le panneau sauvegardes,
  style rouge/warning (meme famille que le bouton supprimer une sauvegarde). JS `restartSession()` :
  `confirm()` explicite avant tout appel reseau (meme garde-fou que `loadSave`, voir memoire
  `feedback_explicit_confirmation_over_silent_safety_net`), puis `render()` complet.

## 3. Tests

`restartSessionWipesTheConversationAndResetsPresenceButKeepsTheScenario` (ChatServiceImplTest) :
verifie tours vides, presence reinitialisee (un PNJ mute avant le restart redevient present), le
scenario lui-meme inchange, et persistance sur le slot de session live (relecture depuis disque).

## 4. Resultat

`mvn -pl chat -am test` : 164 tests (+1), tous verts.
