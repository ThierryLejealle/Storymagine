# 2026-07-12 00h15 — Fix : timeout de connexion stream Ollama en dur à 30s

## 1. Demande

L'utilisateur a rencontré des `Timeout connexion Ollama (30s)` répétés dans le chat, notamment
quand Ollama est occupé par un autre usage en parallèle ("j'utilise le modèle pour autre chose...
oups") — dans ce cas Ollama peut mettre facilement 300s à répondre. Question posée : le 30s
est-il une attente fixe avant tentative, ou un délai maximum laissé à Ollama pour répondre ?
Réponse : un délai maximum (`cf.get(30_000, MILLISECONDS)` sur le `CompletableFuture` de la
requête HTTP asynchrone — la requête part immédiatement, seule l'attente de la réponse est
bornée). Jugé trop court.

## 2. Ce qui a été touché

### `commun/infra/OllamaAdapter.java` — `executeStreaming()`
Le `30_000` (30s) était une constante en dur pour l'attente des en-têtes HTTP, avec un commentaire
supposant à tort que c'est "quasi-instantané sur Ollama local". Faux dès qu'Ollama est occupé par
un autre appel en cours ou doit charger un modèle — exactement le scénario déjà couvert, plus loin
dans le même flux, par `firstTokenTimeoutMs` (configurable, `ollama.stream.first-token-timeout-ms`,
défaut 300000ms).

Remplacé `cf.get(30_000, TimeUnit.MILLISECONDS)` par `cf.get(firstTokenTimeoutMs, TimeUnit.MILLISECONDS)`
— même budget réutilisé plutôt qu'une constante séparée plus courte. Message d'erreur mis à jour
pour refléter la valeur réellement configurée (`"Timeout connexion Ollama (" + (firstTokenTimeoutMs/1000) + "s)"`)
au lieu du texte "30s" figé. Commentaire réécrit pour expliquer pourquoi ce délai peut être long.

Fichier partagé (`commun`) : impacte `redacteur` et `chat` (les deux utilisent `OllamaAdapter` en
mode stream par défaut).

## 3. Résultat

`mvn compile test` depuis la racine : **BUILD SUCCESS**, 5 modules, tous les tests existants
toujours verts (aucun test ne verrouillait la valeur 30s en dur).

Non touché, non demandé : le nombre de tentatives (`ollama.retry-count=5`) et les délais entre
tentatives (`retry-delay1/2/3` = 15/30/60s) restent inchangés.
