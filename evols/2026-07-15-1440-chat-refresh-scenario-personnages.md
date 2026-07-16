# 2026-07-15 14h40 - Bouton refresh scenario/personnages

## Demande
"tu peux mettre un bouton refresh pour recharger le scenario et personnages ?"

Objectif : pouvoir editer scenario.txt ou un fichier de personnage (.txt) pendant qu'une session
de chat tourne, et recharger le contenu depuis le disque sans perdre la conversation en cours ni
redemarrer le serveur.

## Ce qui a ete touche

- `ChatSession` : champ `scenario` rendu non-final. Nouvelle methode `reloadScenario(ChatScenario)` :
  remplace le scenario, ne touche ni aux tours ni au resume ni a l'acte courant. Reconcilie
  `presentNpcIds` avec le nouveau Cast : les id disparus sont retires, les id nouvellement apparus
  demarrent presents (meme defaut qu'une session fraiche), les id deja presents/mutes gardent leur
  etat. Si la reconciliation ne laisserait plus personne de present (ex. tout le cast present a ete
  renomme), repli sur "tout le nouveau cast present".
- `ChatService` / `ChatServiceImpl` : nouvelle methode `reloadScenario(Path, ChatSession)` — relit
  le scenario via `storage.loadScenario` (port deja existant, aucun nouveau port necessaire), appelle
  `session.reloadScenario(...)`, persiste (sauvegarde `present.txt` reconcilie).
- `ChatWebServer` : nouvel endpoint `POST /reload-scenario`, meme forme de reponse que `/history`
  (repaint complet cote client, comme `/load-save`).
- `chat.html` : bouton "⟳" a cote du titre "Personnages" dans le panneau de gauche (`#castPanel`).
  JS `reloadScenario()` -> `fetch('/reload-scenario')` -> `render(data)`.
- Tests ajoutes : `ChatSessionTest` (3 tests sur la reconciliation de presence : edit simple, ajout/
  suppression de PNJ, repli sur "tout present"), `ChatServiceImplTest`
  (`reloadScenarioPicksUpFileEditsAndAddsNewNpcsPresentWithoutTouchingTurns` — edite un fichier .txt
  sur disque entre l'ouverture de session et l'appel, verifie que le contenu est repris, que les
  tours ne bougent pas, et que la reconciliation est bien persistee).

## Resultat
`mvn -pl chat -am test` vert (tous les tests existants + les nouveaux). Compilation multi-module OK.
Pas encore teste a l'oeil dans le navigateur (pas de `chat.bat` relance dans cette seance).
