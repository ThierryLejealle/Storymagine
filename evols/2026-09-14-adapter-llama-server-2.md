# 2026-09-14 - Adaptateur llama-server, en cohabitation avec Ollama

## Demande

Adapter le projet pour pouvoir utiliser llama-server (llama.cpp) en plus d'Ollama. Decisions
validees avant implementation :
- Cohabitation, pas de remplacement : bascule Ollama / llama-server choisie interactivement au
  demarrage de chaque CLI.
- llama-server ne sert qu'un seul modele deja charge par son process : pas de catalogue
  interrogeable — `listModels()` renvoie toujours une liste vide, le coeur/l'UI en deduisent qu'
  aucun choix de modele n'est possible ici. testllm reste sur Ollama uniquement (benchmark
  multi-modeles hors scope).
- Le programme ne demarre ni n'arrete jamais le process llama-server (pas d'equivalent de
  OllamaLauncher) : `unload()` est un no-op, l'utilisateur lance et gere llama-server lui-meme.
- Port par defaut : 8100 (choisi par l'utilisateur).
- Contexte fixe au lancement du process llama-server (pas d'agrandissement a la volee comme le
  num_ctx d'Ollama) : un depassement remonte en erreur explicite plutot qu'une tentative de retry.
- Infos modele (parametres/quantization) best-effort via `/props` — vide si indisponible.

## Ce qui a ete touche

Nouveau, module `commun` :
- `infra/LlamaServerAdapter` : implemente `ModelCallPort`, `ModelLifecyclePort` et
  `RawCompletionPort`. Chat via `/v1/chat/completions` (format OpenAI-compatible, sync et
  streaming SSE), brut via `/completion` (endpoint natif llama.cpp), sante via `/health`,
  info modele best-effort via `/props`. `reasoning_content` lu tel quel si present (pas de champ
  requete equivalent au think:true/false d'Ollama). `listModels()` -> liste vide, `unload()` ->
  no-op, `detectContext()` -> retourne la fenetre configuree.
- `infra/LlamaServerConfig` : record de configuration + fabrique d'adaptateurs (sur le modele de
  `OllamaConfig`, sans les champs propres a la croissance de contexte/catalogue qui n'ont pas
  d'equivalent ici).
- `infra/LlamaServerMessage`, `LlamaServerChatRequest`, `LlamaServerChatResponse`,
  `LlamaServerCompletionRequest`, `LlamaServerCompletionResponse` : POJO Jackson pour le format
  OpenAI-compatible et le format natif `/completion` (sur le modele des POJO Ollama existants).
- `ui/cli/LlamaServerSetupCli` : verification de connexion (`/health`) + lecture des reglages dans
  les proprietes du module. Pas de menu GPU ni de choix de modele (un seul modele deja charge).
- `ui/cli/LlmSetupCli` : nouveau menu de tete "1. Ollama / 2. llama-server", delegue ensuite a
  `OllamaSetupCli` ou `LlamaServerSetupCli`. Expose un `Selection` unifie
  (`ModelCallPort`/`ModelLifecyclePort`/nom du modele/provider/baseUrl) independant du provider
  choisi.
- `OllamaConfig.withContextWindowSize(int)` : copie immuable avec une autre taille de contexte
  (reutilise par ailleurs par la fonctionnalite testllm de la meme seance).

Modules `redacteur` et `chat` :
- `RedacteurCli`, `ChatCli`, `ScenarioTesterCli` : bascule de `OllamaSetupCli`/`OllamaAdapter` vers
  `LlmSetupCli`/`ModelCallPort` (+ `ModelLifecyclePort` pour `probe()`). L'appel `/api/ps`
  (`OllamaPsInfo`, specifique a Ollama) n'est plus fait que si le provider choisi est "ollama".
- `redacteur.properties`, `chat.properties` : nouvelle section `llamaserver.*` (base-url — defaut
  `http://localhost:8100` —, context-window, top-k/p, repeat-penalty, num-predict, retry,
  timeouts).

Tests :
- `commun/test/infra/LlamaServerAdapterTest` : faux serveur HTTP local (`com.sun.net.httpserver`,
  deja dans le JDK, aucune dependance ajoutee) — jamais de vrai LLM. Couvre generate() sync,
  generate() streaming (concatenation des deltas content/reasoning_content, extraction des
  timings), complete() (`/completion`), mapping d'une erreur HTTP "context exceeded" vers
  `ContextOverflowException`, et `listModels()` toujours vide.

## Resultat

Compilation globale (`mvn compile`) et suite de tests (`mvn test`) OK sur l'ensemble du projet,
5/5 tests verts sur `LlamaServerAdapterTest`. Non teste contre une vraie instance llama-server
(pas d'execution interactive possible depuis cette session) — a valider par l'utilisateur au
premier lancement, notamment le format exact des erreurs de depassement de contexte et la
presence effective du champ `timings` selon la version de llama-server installee.
