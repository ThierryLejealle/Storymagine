# 2026-10-25 - Chat : mot-clé [NPC STARTS], un PNJ ouvre l'histoire ou l'acte

## 1. Évolution demandée

Un mot-clé dans le scénario, comme `[NEXT ACT]`, qui fait commencer un PNJ plutôt que le joueur :
comportement identique à un clic du joueur sur DO (sans texte). Un seul PNJ parle ; au hasard s'il y
en a plusieurs, ou le PNJ nommé (`[NPC STARTS: Elena]`). Valable pour l'ouverture de l'histoire et
pour le début d'un acte. À documenter et à expliquer dans la graine de `init-chat`.

## 2. Ce qui a été touché

- Domaine : `NpcOpening` (aucune / au hasard / nommé) et `NpcOpeningMarker` (nouveaux) ; la ligne du
  marqueur doit être **seule**, elle est retirée du texte (le LLM ne la lit pas, `Teaser` non plus).
  `ScenarioAct.opening` et `ChatScenario.storyOpening` ; `ChatScenario.openingOfAct`, et refus au
  chargement d'un nom inconnu du casting (`Cast.findByName` : id, nom complet ou un mot du nom).
- `ChatSession.npcOpeningAt` / `pendingNpcOpening` : dérivé des tours, rien de stocké ni de persisté
  (donc rien à synchroniser avec rollback, sauvegardes, restart). `SpeakerSelector.openingSpeaker`,
  `PlayerMessage.isEmptyHandoff`.
- `ChatServiceImpl` : un `DO:` vide à un début d'histoire ou d'acte avec marqueur donne la parole à
  un seul PNJ, sans interjection ; le retry retrouve le même PNJ. Aucun prompt modifié.
- `ScenarioOutlineParser` (extraction de l'en-tête et des corps de titres, ouverture des nœuds
  nouvellement entrés), `ChatFileStorageAdapter`.
- Web : `ChatHistoryView.npcOpeningPending`, et `chat.html` envoie un `DO:` vide après chargement,
  redémarrage, bouton « Suivant » ou changement d'acte automatique (jamais après undo, rollback,
  sauvegarde chargée, arrêt ou erreur).
- `ScenarioScaffolder` : consigne entre parenthèses dans la graine d'`init-chat`.
- Docs : `chatscenarios/rules.md` (§4.5), `specs/chat.md`, skill `chat-scenario-writing`.
- Tests : `NpcOpeningMarkerTest`, `NpcOpeningScenarioLoadingTest`, 6 tests dans
  `ChatServiceImplTest`, 1 dans `ScenarioScaffolderTest`.

## 3. Résultat

`mvn test` vert depuis la racine. Non vérifié à la main : le déclenchement automatique dans le
navigateur (JavaScript) et un vrai LLM ; seule la logique serveur est couverte par des tests.

Points d'attention : la bulle `DO:` du joueur s'affiche avant la réplique du PNJ (comportement voulu :
« comme un clic sur DO »). Arrêter la génération pendant l'ouverture retire le `DO:` et l'ouverture
se relancera au prochain chargement de la page. Si l'ouverture se révèle trop faible avec un `DO:`
vide, une modification de prompt serait à proposer (delta à valider).
