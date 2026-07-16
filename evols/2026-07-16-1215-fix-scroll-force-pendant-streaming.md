# 2026-07-16 12h15 - Fix : le streaming forçait le défilement en bas, impossible de remonter lire

## 1. Demande

"Quand un personnage est actif coté LLM, tu as mis un carré clignotant. Pas mal. Mais le carré
m'empeche de scroller la fenetre pour lire ce que le précédent vien de lire. Comme si quand il
clignotait, il forçait la fenetre à descendre."

## 2. Diagnostic

`updateStreamingBubble(el, textSoFar)` faisait `turnsEl.scrollTop = turnsEl.scrollHeight` à CHAQUE
fragment de texte reçu (plusieurs fois par seconde pendant qu'une réplique s'écrit, voir
`evols/2026-07-16-1205-...`) — ramenant le défilement tout en bas en continu, rendant impossible
de remonter lire un tour précédent pendant qu'un PNJ répond. Le curseur clignotant lui-même (pure
animation CSS) n'y était pour rien : c'est la fréquence des mises à jour de texte qui posait
problème. Même défaut latent, moins visible, sur `appendTurn`/`appendStreamingBubble`/`showTyping`
(un seul appel chacun, donc jamais gênant en pratique jusqu'ici).

## 3. Ce qui a été touché

`chat.html` : nouveau `isNearBottom()` (tolérance 80px). Les quatre fonctions qui scrollaient
inconditionnellement (`appendTurn`, `appendStreamingBubble`, `updateStreamingBubble`,
`showTyping`) vérifient maintenant la position AVANT de modifier le DOM, et ne forcent le
défilement que si le joueur suivait déjà la conversation en bas — pattern standard de chat (suivre
tant qu'on est en bas, ne jamais arracher la lecture si on a remonté).

## 4. Résultat

Changement purement client (JS), aucun test Java concerné. Syntaxe vérifiée (`node --check` sur le
script extrait). À confirmer par l'utilisateur en conditions réelles.
