# 2026-10-25 - chat.bat : ne plus proposer de « reprendre » quand il n'y a rien à reprendre

## 1. Évolution demandée

Au lancement, `ChatCli` demandait « Continuer la conversation ? [O/n] » même pour un scénario jamais
joué, puis affichait « Reprise (1 tour(s) en mémoire) ». Ce « tour » était en réalité l'introduction
du narrateur (les lignes `[...]` du premier acte) d'une partie neuve, et non une reprise. Demande :
s'il n'y a pas d'historique, ne pas proposer de reprendre.

## 2. Ce qui a été touché

- `ChatStoragePort.hasPlayedTurns` (+ implémentation dans `ChatFileStorageAdapter`) : vrai si le run
  persisté contient au moins un tour dit par le joueur ou un PNJ. Les lignes seules du narrateur
  (ce qu'écrivent un démarrage ou un « Recommencer ») ne comptent pas : il n'y a pas encore de partie.
- `ChatService.hasSavedSession` (+ `ChatServiceImpl`) : délègue au port.
- `ChatCli` : la question « Continuer ? » n'est posée que si une partie existe. Sinon la partie démarre
  directement et affiche « Nouvelle partie (introduction : N tour(s)). ». Avec une partie existante,
  le comportement est inchangé (« Reprise (N tour(s) en mémoire) » ou « Historique remis à zéro »).
- Tests : 4 dans `ChatFileStorageAdapterTest` (jamais joué, intro seule, tour joueur, après reset),
  1 dans `ChatServiceImplTest` (faux avant le premier échange, vrai après, faux après « Recommencer »).

## 3. Résultat

`mvn test` vert depuis la racine (310 tests pour le module chat). Le comportement de `ChatCli`
lui-même (dialogue console) n'a pas été rejoué, seules les méthodes qu'il appelle sont testées.
