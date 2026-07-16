# 2026-07-16 12h23 - La réflexion (thinking) s'affiche aussi au fil de l'eau

## 1. Demande

"Ce qui me gène : le bloc Réflexion n'apparait qu'après que le texte de réponse soit affiché. Alors
qu'il devrait apparaître dès que le LLM commence à te rendre la réflexion. Et on doit pouvoir
cliquer dessus et voir la réflexion apparaître." — suite du streaming du texte (voir
`evols/2026-07-16-1205-...`), qui ne couvrait que le texte visible : pendant toute la phase de
réflexion du modèle (souvent la plus longue), rien ne bougeait à l'écran.

## 2. Ce qui a été touché

Nouveau type `PartialGeneration(thinkingSoFar, textSoFar)` (module commun,
`storymagine.commun.coeur.ports`) — remplace le `Consumer<String>` (texte seul) par un
`Consumer<PartialGeneration>` sur `ModelCallPort.generate(...)`. `OllamaAdapter.executeStreaming`
notifie désormais à CHAQUE fragment reçu, que ce soit un fragment de réflexion
(`chunk.message.thinking`) ou de texte visible (`chunk.message.content`).

`RoleplayNarrator.call(input, Consumer<PartialGeneration>)` : transmet le flux, en respectant
`showThinking` — si l'affichage de la réflexion est désactivé pour la session, le callback ne
reçoit jamais qu'une réflexion vide (même règle que pour le résultat final).

`ExchangeProgressListener.onPartialReply(npcId, PartialGeneration)` — signature élargie (au lieu
de `String textSoFar` seul). `ChatWebServer` : `PartialReplyEvent` porte maintenant
`thinkingSoFar` en plus de `textSoFar`.

`chat.html` : `updateStreamingBubble(el, thinkingSoFar, textSoFar)` crée le bloc `<details
class="thinking">` dès le premier fragment de réflexion reçu (structure identique à la bulle
finale : `<summary>🧠 Réflexion</summary>` cliquable) et met à jour son contenu à chaque fragment —
donc cliquable et lisible en direct dès que la réflexion commence, pas seulement une fois le tour
fini, comme demandé.

## 3. Résultat

`mvn -pl chat -am clean test` : 194 tests, tous verts (2 tests de streaming existants adaptés à la
nouvelle signature `PartialGeneration`, `StreamingStubModelCallPort` mis à jour). `mvn compile`
(racine) : aucune régression cross-module (redacteur, testllm compilent toujours contre
`ModelCallPort`). Syntaxe JS vérifiée (`node --check`).

Non testé en conditions réelles (nécessite Ollama + modèle chargé) — à vérifier par l'utilisateur.
