# 2026-09-10 - Affichage des variables Ollama de gestion du contexte au demarrage

## Demande

Afficher au demarrage de tous nos programmes (chat, redaction, tests, ...) la valeur des deux
variables d'environnement Ollama qui pilotent la gestion du contexte et sa compression :
`OLLAMA_FLASH_ATTENTION` et `OLLAMA_KV_CACHE_TYPE`.

## Ce qui a ete touche

- Nouvelle classe `commun/infra/OllamaEnvDisplay` : lit `System.getenv(...)` pour les deux
  variables et les affiche alignees (ou "(non definie)" si absentes).
- Appel ajoute juste apres la banniere d'en-tete, dans les 5 points d'entree qui parlent
  reellement a Ollama :
  - `ChatCli`, `ScenarioTesterCli` (module chat)
  - `RedacteurCli` (module redacteur)
  - `TestLlmCli`, `TestMtpCli` (module testllm)
- `InitStoryCli` exclu volontairement : ce CLI ne fait aucun appel Ollama.

## Resultat

Compilation globale (`mvn compile` depuis la racine) OK sur les 3 modules impactes.

Limite connue et assumee : `System.getenv(...)` lit l'environnement du processus Java qui lance
le CLI, pas necessairement celui du serveur Ollama s'il tourne comme service separe avec ses
propres variables — l'affichage peut alors dire "(non definie)" meme si Ollama a bien la
variable positionnee cote serveur.
