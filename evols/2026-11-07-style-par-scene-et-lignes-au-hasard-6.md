# 2026-11-07 - Style par scène (`{style nom}`) et lignes au hasard dans le scénario (`{N%}`)

## Évolution demandée
- Un style qui change selon la scène (auberge, forêt, donjon), fixé pour la scène et ses sous-scènes, qui s'ajoute
  au style global `style.txt` ; le nom de fichier accepte les tirets (`foret-sombre`).
- Des mini contenus au hasard dans une scène : statiques ou accompagnés d'un `pick`, conditionnés par un pourcentage ;
  résolus une fois à la construction de la partie (comme un `pick`) et partagés par tous les PNJ. Les `{N%}` des
  fichiers de style restent tirés à chaque réponse, avec le conseil d'y mettre des descriptions, pas des actions.

## Ce qui a été touché
- Nouveaux : `SceneStyleMarker` (la ligne `{style nom}`), `ChanceLines` (tirage des lignes `{N%}` d'un texte).
- `ScenarioAct.sceneStyle` ; `ScenarioOutlineParser` (le style du titre le plus proche, refus dans la prémisse, une
  commande par titre) ; `ChatFileStorageAdapter.readSceneStyle` (`styles/nom.txt`, lu à chaque chargement) ;
  `ChatScenario.styleAt(acte)` utilisé par `ChatServiceImpl` (tirage par réponse et vue 🐞).
- `PickResolver` : `ChanceLines` d'abord, avant `repeat` et `pick` (une commande sur une ligne supprimée ne tire rien) ;
  `containsTag` voit les marqueurs, donc le tirage est figé avec les fichiers `resolved/`.
- `ScenarioValidator` : commande `style`, contrôle des marqueurs `{N%}` des fichiers du scénario, avertissement de
  taille pour style global + style de scène, `checkStyleText(texte, où)`.
- Démo `chatscenarios/demos/demo-scene-style` ; tests : `SceneStyleTest`, `DemoScenariosTest`, `ScenarioBuilderFormatTest`.
- Documentation : guide (§2.10 bis, tableau des commandes, structure du dossier, pièges), `specs/chat.md`, skill
  `chat-scenario-writing`, `chat/scenario-builder/` (`ref-commands`, `ref-format`, `ref-writing-tips`, `skeleton`,
  `phase-5-finish`).

## Résultat
Tests verts. Aucun prompt d'agent modifié (le texte `STYLE ADVICE:` reçoit simplement le style combiné).
Limites documentées : un contenu `{N%}` de scène reste dans le prompt à chaque tour (à écrire comme un état) et tous
les PNJ le lisent ; dans un bloc `{repeat}` il est tiré une fois pour le bloc ; pas de `{style}` dans la prémisse ;
pas de bibliothèque `/lib` pour les styles. Le tableau 🎲 ne liste pas les lignes `{N%}`.
