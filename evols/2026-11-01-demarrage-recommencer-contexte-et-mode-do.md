# 2026-11-01 - Démarrage du CLI, « Recommencer » sécurisé, contexte du joueur, mode DO des PNJ

Fiche commune à quatre évolutions petites ou liées (demandé par l'auteur), toutes autour de l'arrivée dans
une partie, de l'erreur de scénario et de l'ouverture d'une scène.

## Évolutions demandées
1. **Bug du démarrage** : un scénario copié avec son `run/` puis adapté s'arrêtait sur une erreur venue du
   `run/` sans jamais demander s'il fallait redémarrer. Demander d'abord « Continuer ? » ; répondre non
   archive le run, et plus rien ne bloque.
2. **« Recommencer » et ⟳ avec un scénario cassé** : « Recommencer » ne marchait pas et l'erreur n'était
   visible qu'au relancement. Valider le scénario avant de faire quoi que ce soit, et afficher l'erreur dans
   une pop-up.
3. **Contexte de départ du joueur** : le documenter (pas de mécanisme) : le donner au minimum dans les
   `[...]` du premier acte, pas dans la prémisse (relue à chaque tour).
4. **Mode DO des PNJ** : un PNJ tient la plume du narrateur pendant un moment et peut forcer le personnage du
   joueur. Décisions prises avec l'auteur :
   - `{npc-mode do on|off nom|*}` : mode permanent, de la même famille que les niveaux d'activité (hérité des
     titres, recalculé depuis les actes, interdit dans la prémisse), étoile ★ sur la vignette. `npc-mode` est
     générique : d'autres modes pourront s'ajouter.
   - `{npc-do}` / `{npc-do Nom}` : ouverture de scène avec la plume (ce que faisait `{npc-starts}`).
   - `{npc-starts}` devient une ouverture **normale** : le PNJ parle sans aucun message du joueur, donc plus de
     bulle « DO: ». Un `{npc-starts Nom}` d'un PNJ en mode DO devient une ouverture avec la plume ; un
     `off` annule seulement cet effet, un `{npc-do}` explicite reste toujours avec la plume.
   - Le joueur continue à taper normalement. Aucun prompt modifié (vérifié : la règle `DO:` existante convient) ;
     le moteur présente au PNJ en mode DO le dernier message du joueur avec le préfixe `DO: `.
   - Avertissement de collision (plusieurs PNJ avec la plume) dans la doc.

## Ce qui a été touché
- `ChatStoragePort` : `hasPlayedTurns`, `resetSession` par nom (versions `ChatScenario` en `default`), `hasRun`.
  `ChatFileStorageAdapter` adapté. `ChatService`/`ChatServiceImpl` : `hasSavedSession(nom)`, `hasRun(nom)`,
  `archiveRun(nom)`, `openScene`, `restartSession` valide d'abord (`ScenarioValidationException`), et
  `turnsSeenBy` (préfixe `DO: ` pour un PNJ en mode DO).
- `ChatCli` : « Continuer ? » avant le chargement ; échec du chargement avec un run existant : propose la
  remise à zéro archivée (`loadOrOfferRestart`).
- `ChatWebServer` : message « Recommencer annulé » ; nouveau `POST /open-scene` ; `streamRound` factorisé avec
  `/continue` ; `NpcView.doMode`, `ChatHistoryView.npcOpeningWithPen`. `chat.html` : `showScenarioError` (pop-up),
  ouverture normale via `/open-scene`, étoile ★ sur la vignette.
- Domaine : `NpcMode` (nouveau), `ActivityDirective` (types `MODE_ON`/`MODE_OFF`), `ActivityDirectiveMarker`,
  `ActivityResolver.modeIds`, `ChatScenario.npcIdsInMode`, `ChatSession.npcIdsInMode` / `pendingOpeningUsesPen`,
  `NpcOpening` (+ `doPen`, `namedDo`, `ANY_DO`), `NpcOpeningMarker` (`{npc-do}`), `ScenarioValidator`,
  `ScenarioOutlineParser` (message de la prémisse).
- Tests : `ChatServiceImplTest` (restart invalide, run par nom, mode DO, `openScene`, ouverture avec la plume),
  `ChatWebServerEndpointsTest` (`/restart` refusé, `/open-scene`), `ActivityDirectiveMarkerTest`,
  `ActivityResolverTest`, `NpcOpeningMarkerTest`, `ScenarioValidatorTest`, `DemoScenariosTest` (+ `demo-npc-do`).
- Démos : `demo-npc-do` (visite guidée), README de `demo-npc-starts` et index mis à jour.
- Documentation : guide §2.4, §2.9, §2.12 (modes, nouveau), §4.5, §4.6, §4.9, §7.1 ; skill
  `chat-scenario-writing` ; `specs/chat.md` ; `specs/todo.md`.
- Aucun prompt d'agent modifié.

## Résultat
508 tests verts. Le démarrage, « Recommencer » et ⟳ ne détruisent plus rien quand le scénario est invalide, et
l'erreur est lisible en entier. Le mode DO et l'ouverture normale ou avec la plume fonctionnent au niveau du
moteur et de l'API.
**Non vérifié à la main** : la question « Continuer ? » avant chargement (code du CLI sans test automatique), la
pop-up et l'étoile ★ dans le navigateur, et surtout le comportement d'un vrai LLM : un PNJ qui tient la plume,
l'ouverture normale sans message du joueur (moins guidée que l'ancien `DO:`), et la collision de plusieurs PNJ.
**Changement de comportement** : un scénario existant avec `{npc-starts}` ouvre désormais en mode normal et sans
bulle « DO: ».

## Incident de cours de route
Des chaînes Java coupées par un vrai retour à la ligne (erreur de mes scripts d'édition, deux fois : tests,
puis une ligne de `ChatWebServer`) n'ont été signalées que par les tests (« Unresolved compilation problem »),
pas par `mvn compile`. Corrigé. Réflexe retenu : lancer les tests, pas seulement la compilation.
Un `git checkout` de trois fichiers de test les avait remis en CRLF (réglage `autocrlf`) : repassés en LF.
