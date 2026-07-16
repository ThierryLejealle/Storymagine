# 2026-07-16 09h40 - Affichage progressif des réponses PNJ + bouton "régénérer cette bulle"

## 1. Demande

"mouais ce serait plus naturel de les avoir l'un apres les autres sinon on attend beaucoup" — sur
un tour à plusieurs PNJ, le joueur attendait la fin de TOUT le tour avant de voir une seule bulle,
alors que chaque réponse est déjà prête en interne au fur et à mesure. Confirmé ensuite : "deja si
on faisait : afficher chaque pnj quand il a fini ? et le bouton regenerer ?" — deux demandes
groupées, la seconde reprenant le backend `ChatService.retry(fromNpcId)` déjà écrit (voir
`evols/2026-07-15-...`) mais jamais câblé côté interface.

## 2. Ce qui a été touché

### Affichage progressif (uniquement `/message`, pas `/retry` — voir §4)

`ChatService`/`ChatServiceImpl` : nouvelle surcharge `sendMessage(..., Consumer<ChatTurn>
onReplyReady)`. Le callback est appelé de façon synchrone, dans l'ordre, pour le tour joueur puis
pour chaque réplique de PNJ dès qu'elle revient du modèle (donc AVANT que le tour entier soit fini)
— câblé dans `generateRepliesAndFinish` (nouveau paramètre) et sur les éventuels tours NARRATOR
d'un changement d'acte. `sendMessage(...)` sans callback délègue avec un callback vide, aucun
comportement changé pour les appelants existants.

`ChatWebServer.handleMessage` : passe en réponse HTTP chunked NDJSON (`sendResponseHeaders(200,
0)`) au lieu d'un seul JSON en fin de tour. Nouveau `NdjsonWriter` (classe interne) : une ligne
JSON par tour produit (flush immédiat), puis une dernière ligne récapitulative — même forme que
`/history` — avec `newTurns` vide puisque tout a déjà été envoyé au fil de l'eau. Toujours HTTP 200
(le client ne regarde jamais le code de statut) ; succès/arrêt/erreur distingués par les champs
présents sur chaque ligne (`speaker` / `stopped` / `error`), pas par un type explicite — réutilise
telles quelles les formes JSON déjà utilisées ailleurs dans ce fichier. Sur `GenerationCancelledException`,
le nombre de tours déjà envoyés (`streamedCount`) est renvoyé comme `removedTurnCount` du `stopped`
: le service a tout retiré côté session (comportement inchangé), le client doit retirer exactement
ce qu'il avait déjà affiché en streaming. Sur une erreur générique, les bulles déjà reçues restent
affichées (elles correspondent à l'état réel — non persisté — de la session) au lieu de tout
disparaître comme avant.

`chat.html` : nouveau `readNdjsonStream(response, onLine)` (lecture ligne à ligne via
`response.body.getReader()`). `sendMessage()` réécrit pour consommer le flux : chaque tour reçu est
affiché immédiatement (`appendTurn`), l'indicateur "typing" est masqué/réaffiché entre chaque
réplique, le tour joueur optimiste est remplacé par la version serveur dès la première ligne au
lieu d'attendre la fin. La dernière ligne (vue récapitulative) est passée telle quelle à
`applyExchange()`, réutilisée sans changement (sa boucle sur `newTurns` devient un no-op puisque
vide).

### Bouton "régénérer cette bulle" (↻)

`appendTurn()` : chaque bulle PNJ a maintenant une icône ↻ à côté du nom (visible au survol de la
bulle, `.retry-bubble-btn`), qui appelle `retryLast(npcId)`. `retryLast()` généralisé pour accepter
un `npcId` optionnel (`/retry` sans paramètre = tout le tour, `/retry?npcId=X` = à partir de ce
PNJ) — le backend le supportait déjà. Piège corrigé au passage : le bouton global `#retryBtn` était
câblé avec `addEventListener('click', retryLast)`, ce qui aurait passé l'objet évènement du clic
comme `npcId` — corrigé en `() => retryLast()`.

## 3. Tests

`ChatServiceImplTest.sendMessageWithCallbackNotifiesEveryTurnInOrderAsSoonAsItIsAppended` — vérifie
que le callback reçoit exactement le tour joueur puis la réplique, dans cet ordre, même contenu que
le résultat retourné. `mvn -pl chat -am clean test` : 193 tests (+1), tous verts. `mvn compile`
(racine) : aucune régression cross-module.

Non testé en conditions réelles (nécessite Ollama + un modèle chargé, à faire par l'utilisateur) :
le rendu progressif effectif dans le navigateur, et le bouton ↻ sur une vraie session multi-PNJ.

## 4. Limites connues / pas fait

`/retry` (bouton régénérer, global ou par bulle) reste en réponse JSON classique, non streamé —
une régénération concerne généralement 1-2 PNJ, jugé moins prioritaire que `/message` qui peut
enchaîner plusieurs PNJ à chaque tour. Le streamer le nécessiterait de connaître `replacedTurnCount`
AVANT le début de la génération (pour retirer les anciennes bulles avant que les nouvelles
arrivent), ce qui n'est pas trivial avec l'architecture actuelle — à reprendre si le besoin se fait
sentir.
