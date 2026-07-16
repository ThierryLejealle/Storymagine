# 2026-07-15 21h43 - Fix : STOP n'arretait pas reellement la generation cote Ollama

## 1. Demande

"STOP does not work to stop the character working ?" — le bouton Stop semblait ne pas arreter le
personnage en train de generer.

## 2. Diagnostic

Ce bug etait deja repere et documente (memoire `project_ollama_stream_interrupt_leak_queued`,
"pas bloquant" a l'epoque) : dans `OllamaAdapter.readLineWithTimeout(...)`, la branche
`TimeoutException` fermait bien le flux (`raw.close()`), coupant la connexion vers Ollama et lui
signalant d'arreter la generation — mais la branche `InterruptedException`, exactement le chemin
emprunte par le bouton Stop (`ChatWebServer.handleStop` → `Thread.interrupt()`), ne le faisait PAS.
Consequence : le clic sur Stop faisait bien abandonner la LECTURE cote client (l'appel Java
retournait), mais la connexion HTTP vers Ollama restait ouverte — le modele continuait donc a
generer server-side, invisible pour l'utilisateur mais consommant du calcul, et pouvant retarder
la requete suivante (nouveau message, ou NPC suivant d'un tour multi-PNJ) mise en file derriere
cette generation "fantome".

## 3. Ce qui a ete touche

`OllamaAdapter.readLineWithTimeout(...)` : la branche `InterruptedException` ferme desormais
`raw` (comme la branche Timeout) — meme mecanisme, meme effet (coupe reellement la connexion vers
Ollama). `future.cancel(true)` ajoute dans les deux branches par coherence, meme si le vrai
declencheur du deblocage du thread de fond est la fermeture du flux (une lecture bloquante
classique n'honore pas l'interruption Java).

## 4. Verification

- `mvn test` (racine) : 163 (chat) + reste, tous verts.
- Verification empirique en conditions reelles (pas seulement relecture de code) : appel
  `OllamaAdapter.generate(...)` reel sur un thread separe, avec une consigne volontairement longue
  ("2000-word fantasy story"), interrompu 2s apres le demarrage — retour de l'appel en ~2004ms
  (immediat), `GenerationCancelledException` levee proprement. Avant le fix, ce meme scenario aurait
  laisse le thread de fond bloque sur la lecture socket, connexion jamais fermee.

## 5. Portee

Fix uniquement dans `commun` (OllamaAdapter, partage par tous les modules) — profite donc aussi a
redacteur/testllm si un Stop equivalent y est un jour cable, pas seulement au chat.
