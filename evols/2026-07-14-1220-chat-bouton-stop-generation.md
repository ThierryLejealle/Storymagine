# 2026-07-14 12h20 - Chat : bouton STOP pour interrompre une génération en cours

## Demande

"Ajoute un bouton STOP pour que je puisse corriger mon message aussi" — pouvoir interrompre une
génération en cours (plutôt que d'attendre la fin puis Annuler) et récupérer directement son
message dans la zone de saisie pour le corriger et le renvoyer.

## Design (validé en plan avant codage)

Plutôt que de faire descendre un objet d'annulation à travers `ModelCallPort`/`GenerationOptions`/
`LlmCallContext` (impact sur tous les modules), réutilisation du mécanisme d'interruption de
thread natif de Java : le thread qui exécute `/message`/`/retry` s'enregistre dans un champ
`volatile` de `ChatWebServer`, et `/stop` (sur un thread séparé) l'interrompt.

## Ce qui a été touché

- `commun/coeur/ports/GenerationCancelledException.java` (nouveau, unchecked) — signale un arrêt
  volontaire, jamais retenté.
- `commun/infra/OllamaAdapter.java` : dans la boucle de retry de `sendStreaming` (chemin utilisé
  par le chat, `ollama.mode=stream`), ajout d'un test `Thread.currentThread().isInterrupted()`
  dans le `catch` générique — sans ça, une interruption pendant la lecture du flux (convertie en
  `RuntimeException` par `readLineWithTimeout`) déclenchait une NOUVELLE tentative au lieu
  d'abandonner. Le chemin sync (`/api/chat` non-stream) n'a nécessité aucun changement — il gérait
  déjà correctement `InterruptedException` en abandonnant immédiatement.
- `chat/ui/web/ChatWebServer.java` : exécuteur passé de 1 à 2 threads (`/stop` doit pouvoir
  tourner pendant qu'une génération bloque sur l'autre thread) ; nouveau champ
  `volatile Thread currentGenerationThread`, enregistré en entrée de `handleMessage`/`handleRetry`
  (avec purge d'un flag d'interruption résiduel), nettoyé en `finally`. Nouvel endpoint
  `POST /stop` qui interrompt ce thread s'il existe. Nouveau `catch (GenerationCancelledException
  e)` dans les deux handlers, répondant `200` avec un nouveau `StoppedView(stopped,
  removedTurnCount)` au lieu d'une erreur 500 — `removedTurnCount=1` pour `/retry` (l'ancienne
  réponse avait déjà été retirée avant l'appel LLM, la bulle correspondante est devenue obsolète
  côté client), `0` pour `/message`.
- `chat/coeur/service/ChatServiceImpl.java` : `sendMessage` retire le tour joueur qu'il venait
  d'ajouter si la génération est annulée, pour que le joueur puisse corriger et renvoyer sans tour
  orphelin dans l'historique. `retry()` n'a nécessité aucun changement (l'état laissé par le
  `truncateFrom` déjà existant est déjà correct en cas d'annulation).
- `chat.html` : nouveau bouton "⏹ Stop" (masqué sauf pendant une génération, style d'alerte),
  `POST /stop` fire-and-forget au clic — pas d'`AbortController` côté client : c'est le fetch
  `/message`/`/retry` déjà en attente qui se débloque côté serveur et répond `stopped:true`.
  `sendMessage()` recharge la zone de texte avec le message original en cas d'arrêt ;
  `retryLast()` retire la bulle de réponse devenue obsolète.
- Tests : `ChatServiceImplTest` — nouveau cas vérifiant que `sendMessage` retire bien le tour
  joueur orphelin quand le `ModelCallPort` lève `GenerationCancelledException`. Pas de test dédié
  pour la boucle de retry d'`OllamaAdapter` (aucune infra de test HTTP mock existante pour cette
  classe, le coût de la construire pour ce seul cas dépassait la valeur du test) ni pour
  `ChatWebServer` (aucun test n'existe sur cette classe aujourd'hui).

## Résultat

`mvn clean test` (4 modules) au vert, 97 tests côté chat (+1). Test end-to-end réel (cliquer Stop
pendant une vraie génération) laissé à l'utilisateur — nécessite un LLM réel en cours de
génération pour être observé.
