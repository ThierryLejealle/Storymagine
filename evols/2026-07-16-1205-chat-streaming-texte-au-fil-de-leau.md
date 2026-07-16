# 2026-07-16 12h05 - Streaming du texte au fil de l'eau (token par token)

## 1. Demande

"Et attaque l'evol majeur : renvoyer le texte au fil de l'eau en stream" — après l'affichage
progressif par bulle (une réplique par PNJ dès qu'elle est finie, voir
`evols/2026-07-16-0940-...`), le joueur voit maintenant le TEXTE d'une réplique grandir en temps
réel pendant que le modèle l'écrit, au lieu d'attendre que la réplique entière soit générée avant
de l'afficher d'un bloc.

## 2. Ce qui a été touché

### Module commun — `ModelCallPort` (port partagé avec redacteur/testllm)

Nouvelle méthode par défaut `generate(..., GenerationOptions, Consumer<String> onPartialText)` —
délègue à `generate(..., options)` si non surchargée, donc **aucun impact sur les autres
adaptateurs/modules** qui implémentent ce port (vérifié : `mvn compile` à la racine, aucune
régression). `onPartialText` reçoit toujours le texte VISIBLE complet généré jusqu'ici (pas juste
le dernier fragment) — un appelant remplace, il n'accumule pas ; une reprise interne après erreur
réseau (voir `sendStreaming`, retries existants) redémarre alors proprement sans texte dupliqué.

`OllamaAdapter` : seul adaptateur à surcharger cette méthode. Callback appelé à chaque fragment de
`chunk.message.content` reçu depuis Ollama en streaming (`executeStreaming`), jamais pour la
réflexion (`chunk.message.thinking`) — champ hors scope, voir §4. Mode `ollama.mode=sync` : pas de
callback (dégradation gracieuse vers le comportement précédent, streaming par bulle uniquement).

### Module chat

`RoleplayNarrator.call(input, Consumer<String> onPartialReply)` — nouvelle surcharge, transmet le
callback à `llm.generate(...)`.

Nouveau type `ExchangeProgressListener` (`onPartialReply(npcId, textSoFar)` +
`onTurnReady(ChatTurn)`) — remplace le `Consumer<ChatTurn>` ajouté dans l'évol précédente (plus de
4 paramètres sinon sur `sendMessage`, voir CLAUDE.md §rationalisation). `ChatService.sendMessage`
et `ChatServiceImpl.generateRepliesAndFinish` mis à jour en conséquence.

`ChatWebServer.handleMessage` : nouveau type de ligne NDJSON `PartialReplyEvent
(npcId/textSoFar)`, émis à chaque fragment, avant le `ChatTurn` final de ce PNJ.

`chat.html` : `appendStreamingBubble(npcId)`/`updateStreamingBubble(el, textSoFar)` — bulle
d'aperçu transitoire (pas trackée dans `renderedTurnEls`, jamais comptée par undo/retry/stop),
remplacée par la vraie bulle (`appendTurn`, avec icône ↻ et bloc réflexion) dès que le tour est
fini. Curseur clignotant CSS (`▍`) pendant qu'une bulle est en cours de génération.

## 3. Limites connues

- Seul le texte VISIBLE est streamé, pas la réflexion (`thinking`) — un modèle qui réfléchit
  longtemps avant de répondre laisse donc l'indicateur "typing" générique tourner pendant toute
  cette phase ; le texte ne commence à apparaître qu'une fois la réflexion terminée. Amélioration
  possible plus tard si besoin, pas demandée ici.
- Le marqueur `[NEXT ACT]` (retiré de la réponse finale) peut apparaître brièvement dans l'aperçu
  progressif si le modèle l'écrit en fin de réplique — se corrige de lui-même dès que le tour final
  remplace l'aperçu (fenêtre d'une fraction de seconde). Pas filtré dans l'adaptateur Ollama : ce
  serait de la logique métier chat dans un module générique (commun), contraire à l'architecture
  hexagonale du projet.
- `/retry` (régénération) reste non streamé — limite déjà documentée dans l'évol précédente.

## 4. Tests

`ChatServiceImplTest` : nouveau `StreamingStubModelCallPort` (simule un appel Ollama en streaming),
`sendMessageWithListenerNotifiesPartialReplyTextBeforeTheTurnIsReady` (vérifie l'ORDRE exact :
tour joueur, puis chaque fragment, puis le tour PNJ final). Test existant renommé/adapté au nouveau
type `ExchangeProgressListener`. `mvn -pl chat -am clean test` : 194 tests, tous verts. `mvn
compile` (racine) : aucune régression cross-module (redacteur, testllm compilent toujours contre
`ModelCallPort`).

Non testé en conditions réelles (nécessite Ollama + modèle chargé) — à vérifier par l'utilisateur.
