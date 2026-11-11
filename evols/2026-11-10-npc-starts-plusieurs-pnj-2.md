# 2026-11-10 - `{npc-starts}` et `{npc-do}` : plusieurs PNJ à l'ouverture

## Évolution demandée
- Faire démarrer plusieurs PNJ : `{npc-starts Elena, Marcus}`. Le premier ouvre, les suivants enchaînent dans la même ronde,
  dans l'ordre écrit. Avec `{npc-do ...}`, tous ont la plume du narrateur.

## Ce qui a été touché
- `NpcOpening` : `npcName` devient `namesText` (le texte des noms tel qu'écrit, un ou plusieurs).
- `ActivityResolver.openingNpcs` (nouveau) : liste les PNJ d'une ouverture avec `resolveNames` (virgules ou espaces, nom
  de plusieurs mots reconnu) ; un même PNJ peut être nommé plusieurs fois (`A, B, A`).
- `SpeakerSelector.openingSpeakers` (ex-`openingSpeaker`) : les nommés présents dans l'ordre ; sinon un PNJ présent au hasard.
- `ChatServiceImpl.resolveSpeakers` : une ronde d'ouverture à plusieurs locuteurs, sans interjection.
- `ChatSession.pendingOpeningUsesPen` : le premier nom décide pour `{npc-starts}` (mode DO), `{npc-do}` vaut pour toute la ronde.
- `ChatScenario` (nom inconnu, PNJ pas encore dans l'histoire) et `ScenarioValidator.checkNpcStarts`.
- Tests : `NpcOpeningMarkerTest`, `NpcOpeningScenarioLoadingTest`, `ChatServiceImplTest` (ronde Marcus puis Elena).
- Documentation : guide (§4.5 et tableau des commandes), skill `chat-scenario-writing`, `chat/scenario-builder/ref-commands.md`, `specs/chat.md`.

## Résultat
Compilation du parent et 638 tests du module chat verts. Aucun prompt d'agent modifié. Le marqueur `NpcOpeningMarker` est
inchangé (il capturait déjà tout le texte après le nom de la commande).
