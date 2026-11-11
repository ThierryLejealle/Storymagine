# 2026-11-10 - `{style nom}` : plusieurs par titre, et autorisé dans la prémisse

## Évolution demandée
- Supprimer la contrainte « une seule commande `{style}` par titre » (erreur rencontrée : « {style} est ecrit 2 fois sous
  le titre ... »). Cette contrainte venait du modèle de données (un seul nom par titre), pas d'un besoin métier.
- Autoriser `{style nom}` dans la prémisse : le style s'ajoute alors à `style.txt` pour toute l'histoire.

## Ce qui a été touché
- `ScenarioOutlineParser` : un titre garde la liste de ses styles (dans l'ordre écrit, un nom répété compte une fois) ;
  la prémisse extrait ses `{style}` (aussi pour un scénario sans titre) dans `Outline.premiseStyle`.
- `ScenarioStyle.then` (nouveau) : enchaîne deux styles ; utilisé par le parseur, `ChatScenario.styleAt` et l'adaptateur.
- `ChatFileStorageAdapter` : `ChatScenario.style` = `style.txt` puis style de la prémisse. Ordre final d'une réponse :
  `style.txt`, prémisse, scène.
- Inchangé : un `{style}` sous un titre enfant remplace ceux du parent (l'ensemble) ; un `{style}` au milieu d'une
  phrase reste refusé.
- Tests : `SceneStyleTest` (plusieurs par titre, nom répété, prémisse avec et sans titres, `{style}` non seul refusé).
- Documentation : guide §2.10 bis et tableau des commandes, skill `chat-scenario-writing`, `chat/scenario-builder/ref-commands.md`,
  `specs/chat.md`.

## Résultat
Compilation du parent OK, tests du module chat verts. Aucun prompt d'agent modifié. L'avertissement des 1 500 caractères
porte maintenant sur le cumul `style.txt` + prémisse + scène : plusieurs styles s'additionnent vite.
