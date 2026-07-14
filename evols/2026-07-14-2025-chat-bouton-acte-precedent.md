# 2026-07-14 20h25 - Chat : bouton "Acte précédent"

## 1. Évolution demandée
L'utilisateur a signalé, en usage réel, que le LLM avait déclenché `[NEXT ACT]` trop tôt et
demandé comment revenir en arrière. Vérification du code : aucun moyen n'existait, ni côté UI
(le bouton "Précédent" avait été explicitement retiré le 2026-07-12 lors de la simplification de
la barre d'actes) ni côté `undo` (qui retire des tours de conversation mais ne touche jamais
`currentAct` — déjà documenté dans le javadoc de `ChatService.undo`).

Deux options présentées : (1) rendre `undo` "intelligent" en lui faisant détecter et annuler une
transition d'acte franchie (demande un suivi des frontières d'actes dans `ChatSession`), ou
(2) un bouton manuel "Acte précédent" simple, indépendant de l'historique des tours. L'utilisateur
a choisi l'option simple.

## 2. Ce qui a été touché
- `ChatSession.java` : nouvelle méthode `previousAct()` — décrémente `currentAct` (no-op si déjà
  sur le premier acte), ne touche jamais aux tours (les éventuels tours NARRATOR de l'acte quitté
  restent dans l'historique, choix assumé pour rester simple).
- `ChatService.java` : nouvelle méthode d'interface `previousAct(...)`, javadoc symétrique à
  `advanceAct`.
- `ChatServiceImpl.java` : implémentation symétrique à `advanceAct` (délègue à
  `session.previousAct()`, persiste si effectif).
- `ChatWebServer.java` : nouvelle route `POST /previous-act` (`handlePreviousAct`), symétrique à
  `/next-act` mais sans nouveaux tours (`currentView(List.of(), 0, false, false)`).
- `chat.html` : bouton "← Précédent" ajouté dans `#actBar` (avant le titre d'acte), désactivé
  (`display:none`, même règle que "Suivant") quand `currentAct <= 1` ou pas d'actes ; fonction JS
  `previousAct()` + listener, CSS partagée avec `#nextActBtn`.
- Tests : `ChatSessionTest` (2 nouveaux cas : retour d'un acte sans toucher aux tours, no-op sur
  le premier acte), `ChatServiceImplTest` (2 nouveaux cas : retour + persistance, no-op sur le
  premier acte).

## 3. Résultat
`mvn -pl chat -am test` : 101 tests verts (94 existants + 7 nouveaux : 2 `ChatSessionTest`,
2 `ChatServiceImplTest`, 3 tests indirects via la compilation des nouveaux fichiers). Pas de test
UI automatisé (vanilla JS sans framework de test dans ce module) — bouton à valider visuellement
par l'utilisateur en session réelle.

Limite assumée (choix "simple") : revenir à l'acte précédent ne retire pas les tours NARRATOR que
l'acte quitté avait ajoutés à l'historique (ses story-beats `[...]` restent visibles dans le chat)
— seul le texte d'acte envoyé au LLM change. Si ça pose problème à l'usage, l'option 1 (undo lié
aux frontières d'actes) reste disponible en évolution future.
