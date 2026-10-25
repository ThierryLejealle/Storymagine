# 2026-09-14 - Taille de contexte reglable interactivement dans testllm

## Demande

Pouvoir donner une taille de contexte pour testllm au lieu de la valeur fixe lue dans
testllm.properties. Saisie en "k" (ex. 32 -> 32000 tokens), defaut 32.

## Ce qui a ete touche

- `commun/infra/OllamaConfig` : nouvelle methode `withContextWindowSize(int)` — copie immuable de
  la config avec une autre taille de contexte, le reste inchange.
- `testllm/infra/TestLlmAssembler` : nouvelle surcharge de `assemble(...)` acceptant une
  `OllamaConfig` deja construite (l'ancienne signature delegue a la nouvelle avec
  `config.ollamaConfig()`, comportement inchange pour les appelants existants).
- `testllm/ui/cli/TestLlmCli` : nouveau prompt interactif "Taille de contexte en k [32]" juste
  apres le choix du thinking, applique a tous les modes (tout/favoris/choisir/complet). La valeur
  saisie est multipliee par 1000 puis appliquee via `OllamaConfig.withContextWindowSize(...)` a
  tous les appels `TestLlmAssembler.assemble(...)` du fichier (y compris la boucle GPU 0/1/0+1 du
  mode complet).

## Resultat

Compilation globale (`mvn compile`) et suite de tests (`mvn test`) OK sur l'ensemble du projet.
Non teste en conditions reelles (pas d'execution interactive possible depuis cette session) — a
valider par l'utilisateur au premier lancement.
