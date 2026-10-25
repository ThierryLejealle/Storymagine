# 2026-09-14 - testllm : support llama-server (modele unique)

## Demande

Correction suite a un retour utilisateur : testLLM.bat ne proposait pas le choix Ollama/llama-server
contrairement aux autres .bat du projet (redacteur, chat). Decision initiale ("testllm reste sur
Ollama uniquement") maintenue sur le fond (pas de benchmark multi-modeles avec llama-server, un
seul modele charge par process, pas de catalogue) mais l'utilisateur veut que TOUS les .bat
fonctionnent avec l'un ou l'autre serveur : c'etait justement l'interet de faire renvoyer une liste
vide par listModels() sur llama-server — le coeur (benchModele) sait deja retomber sur le nom de
modele demande quand le catalogue est vide, sans aucune modification necessaire du domaine.

## Ce qui a ete touche

- `commun/infra/OllamaConfig`/`LlamaServerAdapter`/`BenchmarkRunner`/`TestLlmServiceImpl` :
  **aucune modification** — `TestLlmServiceImpl.benchModele()` retombe deja sur
  `new ModelEntry(modele, 0L)` quand `listModels()` ne trouve rien, et `ThinkMode.versions(false)`
  ramene deja "avec/les deux" a un seul run sans thinking quand la detection ne peut rien affirmer.
  Le "coeur qui sait qu'il ne peut pas choisir de modele" (decision du 2026-09-14 sur
  l'adaptateur) fonctionnait deja pour testllm sans y toucher.
- `testllm/infra/TestLlmConfig` : nouvelles methodes `llamaServerConfig()` et
  `llamaServerModelName()`, sur le modele de `ollamaConfig()`.
- `testllm/infra/TestLlmAssembler` : nouvelle methode `assembleLlamaServer(...)` — meme
  `BenchmarkRunner.AdapterFactory` que la variante Ollama, avec les capacites absentes de
  llama-server neutralisees explicitement (detectThinking/isLargeModel -> false, vramSampler -> 0,
  detectContext/configuredCtx -> valeur fixe des proprietes) ; `hardwareSampler()`
  (nvidia-smi + CPU, non specifique a Ollama) reste partage entre les deux variantes.
- `testllm/ui/cli/TestLlmCli` : nouveau menu de tete "1. Ollama / 2. llama-server". Sur
  llama-server : pas de menu GPU (le programme ne gere jamais ce process), pas de prompt de
  contexte (fixe au lancement du process, juste affiche en info), pas de choix
  tout/favoris/choisir (aucun catalogue) — juste un prompt du nom du modele (defaut lu dans
  `llamaserver.model-name`), puis benchmark direct de ce modele avec les passes standard.
  runs et thinkMode restent demandes dans les deux cas.
- `testllm/testllm.properties` : nouvelle section `llamaserver.*` (miroir de la section `ollama.*`
  existante, port 8100 par defaut).

## Resultat

Compilation globale (`mvn clean compile`) et suite de tests (`mvn test`) OK sur l'ensemble du
projet. Non teste en conditions reelles (pas d'execution interactive possible depuis cette
session) — a valider par l'utilisateur au premier lancement, notamment le CSV/resume produits en
mode llama-server (colonnes VRAM/contexte natif forcement vides ou approximatives, voir
`assembleLlamaServer`).
