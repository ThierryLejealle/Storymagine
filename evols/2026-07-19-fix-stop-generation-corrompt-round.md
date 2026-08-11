# 2026-07-19 - Fix : Stop pendant une generation corrompait le round et l'affichage

## Demande
Signalement : cliquer sur Stop puis renvoyer un message "casse un peu le programme" — la reponse
au nouveau message ne se relance pas bien ou ne s'affiche pas. Diagnostic demande via un test
bouchonne (pas de vrai LLM), depuis les vrais points d'entree HTTP que le navigateur appelle.

Aucun outil de navigateur pilotable n'etant disponible, le diagnostic a ete fait avec un harnais
Java jetable (`chat/src/test/java/storymagine/chat/manual/StopThenResendHarness.java`, supprime
une fois l'investigation terminee) qui appelle `/message` et `/stop` en HTTP reel contre un
`ChatWebServer` reel, avec un `ModelCallPort` bouchon lent et interruptible (meme contrat que
`OllamaAdapter` face a `Thread.interrupt()`). Deux bugs confirmes par ce harnais.

## Ce qui a ete touche

### Bug 1 — l'accuse de reception "stopped" n'atteignait pas le navigateur
Apres avoir attrape `GenerationCancelledException`, le flag d'interruption du thread restait
positionne. La toute prochaine ecriture (l'accuse `{"stopped":true,...}`) se fait sur un flux
adosse a un canal NIO interruptible cote serveur HTTP du JDK : ecrire alors que le thread est
encore marque interrompu faisait echouer CETTE ecriture aussi, fermant la connexion brutalement
au lieu de transmettre le signal attendu. Cote client, ca tombait dans le handler d'erreur reseau
generique au lieu du handler "stopped" dedie, qui seul retire les bulles obsoletes de l'ecran —
les bulles deja affichees restaient donc visibles alors que le serveur les avait deja effacees.

Fix : `Thread.interrupted()` (purge le flag) juste apres chaque `catch (GenerationCancelledException)`,
avant toute autre operation — `ChatWebServer` (4 endroits : `/message`, `/retry`,
`/analyze-next-act`, `/analyze-mind-state`) et `ChatServiceImpl.generateRepliesAndFinish` (les deux
nouveaux points d'absorption du bug 2, voir ci-dessous).

### Bug 2 — Stop en cours de round a plusieurs PNJ effacait les reponses deja abouties
`ChatServiceImpl.sendMessage` entourait tout le round (chaque PNJ + la compaction en arriere-plan)
d'un seul `catch (GenerationCancelledException)` qui annulait tout depuis le tour joueur — meme
si un PNJ avait deja repondu et que sa reponse etait deja affichee. Confirme par le harnais :
Elena repondait entierement, Marcus demarrait, Stop cliquait — la session se retrouvait a 0 tour,
la reponse d'Elena effacee.

Fix : `generateRepliesAndFinish` attrape desormais `GenerationCancelledException` autour de chaque
appel PNJ et autour de la compaction :
- Si AUCUN PNJ n'a encore repondu ce tour, relance l'exception (comportement inchange :
  `sendMessage`/`retry` annulent tout, le joueur corrige et renvoie).
- Si au moins un PNJ a deja repondu, le round s'arrete la et retourne normalement — tout ce qui a
  ete produit est garde et sauvegarde, rien a retirer cote client.
- Une annulation pendant la compaction (qui ne peut arriver qu'apres que tous les PNJ ont deja
  repondu) est traitee comme un resume vide : compaction sautee ce tour-ci, rien d'autre ne bouge.

Ce fix profite aussi a `retry()` (meme methode partagee) et corrige un troisieme bug lie :
`ChatWebServer.handleRetry` renvoyait toujours `StoppedView(true, 1)` sur annulation, meme quand
plusieurs nouvelles reponses avaient deja ete produites avant le stop — un decompte fige qui
aurait fait retirer la mauvaise bulle cote client. Ce cas ne se produit plus desormais que quand
aucune nouvelle reponse n'a ete produite, ou `1` est effectivement correct.

## Tests
- `ChatServiceImplTest` : 2 nouveaux cas — annulation apres la 1re reponse d'un round a 2 PNJ (la
  reponse survit, persistee), annulation pendant la compaction (round garde, compaction sautee).
- Harnais manuel (jetable, supprime) : confirme sur les vrais endpoints HTTP que l'accuse "stopped"
  arrive proprement (bug 1) et que la reponse d'un PNJ deja abouti survit a l'annulation du
  suivant (bug 2), avant/apres comparaison directe.

## Resultat
Stop pendant un round a plusieurs PNJ ne perd plus les reponses deja produites. L'accuse de
reception "stopped" arrive desormais de maniere fiable au navigateur, evitant le message d'erreur
reseau trompeur et la desynchronisation entre l'ecran et la session serveur qui en decoulait.
Aucun texte de prompt LLM n'a ete modifie.
