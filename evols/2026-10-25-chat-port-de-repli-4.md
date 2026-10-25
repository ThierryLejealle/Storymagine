# 2026-10-25 - Chat : port de repli pour lancer plusieurs chats en parallèle

## 1. Évolution demandée

Pouvoir lancer deux chats en parallèle (usage : tests). Le blocage était le conflit de port 8765.
Décision : repli automatique sur le port suivant ; pas de verrou de scénario pour l'instant (l'utilisateur
fera attention à ne pas ouvrir le même scénario deux fois).

## 2. Ce qui a été touché

- `ChatWebServer.start()` : essaie le port préféré puis les suivants (20 essais max, `BindException`
  seulement), et retourne le port réellement lié. Échec explicite si aucun port libre.
- `ChatCli` : utilise le port retourné pour l'URL affichée et ouverte dans le navigateur, et signale
  le repli (« Port 8765 occupé (autre chat ?) »).
- `chat.properties` : commentaire de `chat.web.port` (désormais port préféré).
- Test : `ChatWebServerPortTest` (port préféré occupé → repli sur un port supérieur ; port libre → conservé).
- Docs : `specs/chat.md` (section outils CLI), avec l'avertissement sur le même scénario ouvert deux fois.

## 3. Résultat

`mvn test` vert depuis la racine. Non vérifié à la main : deux `chat.bat` lancés réellement en parallèle.

Limites connues : pas de verrou sur `run/` (même scénario dans deux instances = écritures concurrentes) ;
Ollama et la VRAM sont partagés, donc les générations simultanées se ralentissent ou se sérialisent
selon `OLLAMA_NUM_PARALLEL`. Au lancement simultané, `mvn compile` sur le même `target/` peut aussi
poser problème : lancer le second chat une fois le premier démarré.
