# 2026-07-12 00h05 — Chat : streaming à l'écran + affichage du thinking (idée, pas implémenté)

## 1. Demande

Discussion avec l'utilisateur, pas encore une demande d'implémentation ("dis-moi juste si c'est
coûteux et compliqué") — à reprendre "demain" si ça vaut le coup.

Deux questions posées :
1. Le chat est-il en mode stream ou en mode "tout d'un coup" ?
2. Peut-on restituer le stream à l'écran caractère par caractère (comme ChatGPT/Ollama CLI) ? Et
   pareil pour le thinking ?

## 2. Constat actuel

`ollama.mode=stream` dans `chat.properties` : l'échange `OllamaAdapter` ↔ Ollama est bien en
streaming NDJSON. Mais `OllamaAdapter` (`executeStreaming`) accumule tous les chunks en interne
(`StringBuilder content`) et ne renvoie le texte complet qu'une fois `done=true` — donc le
navigateur reçoit toujours la réponse entière d'un coup via `ChatWebServer`/`chat.html`. **Pas de
streaming à l'écran aujourd'hui**, malgré le mode stream côté transport Ollama.

Le champ `thinking` de chaque chunk streamé est actuellement ignoré partout (`OllamaMessage.java`,
`@JsonIgnoreProperties`) — jamais capturé, jamais exposé sur `LlmResult`.

## 3. Ce qu'il faudrait toucher (si on le fait)

### Streaming à l'écran
1. **`ModelCallPort`/`OllamaAdapter`** — nouvelle variante qui pousse chaque chunk de texte au fur
   et à mesure (callback/consumer) au lieu de tout accumuler avant de renvoyer un `LlmResult`
   unique. Garder la méthode `generate()` actuelle intacte (additif, ne touche pas `redacteur`).
2. **`ChatWebServer`** — garder la réponse HTTP `/message` ouverte et streamer au navigateur
   (Server-Sent Events — faisable avec `com.sun.net.httpserver.HttpServer` sans dépendance
   externe, mais nouveau code, pas juste un ajustement de `writeJson`).
3. **`chat.html`** — remplacer le `fetch` + `res.json()` actuel par un rendu incrémental
   (`EventSource` ou lecture manuelle du flux) qui complète la bulle en cours caractère par
   caractère.
4. **Cas limites** — la compaction et la sauvegarde (`storage.saveSession`) doivent quand même
   attendre le texte complet avant de s'exécuter ; gérer une erreur survenant en cours de stream
   (bulle partielle affichée, puis échec).

### Thinking
Une fois la plomberie streaming en place, capturer le thinking coûte peu en plus côté serveur
(même flux NDJSON, il suffit de lire le champ `thinking` au lieu de l'ignorer). Ajoute en revanche
une zone d'affichage "réflexion" séparée côté front, qui se remplit en direct — complexité front
supplémentaire, pas juste un ajout de champ.

## 4. Statut

**Pas implémenté.** Idée notée pour une session dédiée future — l'utilisateur juge que ce n'est ni
trivial ni un gros chantier, mais pas une petite retouche non plus (nécessite de toucher les 3
couches : port/adaptateur, serveur web, page HTML).
