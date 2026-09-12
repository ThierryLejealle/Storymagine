# 2026-09-12 - Outil testContext : impact de num_ctx sur un modele

## Demande

Un nouvel outil de diagnostic, sur le modele de testLLM/testMTP, mais pour tester UN modele sur
plusieurs tailles de contexte (num_ctx) au lieu de plusieurs modeles ou d'un autre parametre.
Toujours sans thinking. Menu GPU identique aux outils existants (Ollama deja demarre / GPU 0 /
GPU 1 / GPU 0+1). Paliers demandes : 16k, 32k, 64k, 96k, 128k, 192k, 256k — arret du balayage des
qu'une taille depasse le contexte natif du modele (avec ajout de la valeur exacte du contexte natif
si elle ne correspond a aucun palier), ou des qu'un chargement echoue. Verification via `/api/ps`
que le modele a bien ete recharge avec le contexte demande. Mesure tok/s et repartition CPU/GPU
(comme les outils existants) a chaque palier. Reprise des 3 passes actuelles (haiku/moyen/grand).
Nombre de boucles demande interactivement (defaut 3). Resultats sauves dans `bench/`.

## Ce qui a ete touche

- `commun/infra/OllamaPsInfo` : ajout du champ `contextLength` (lu depuis `context_length` dans
  la reponse `/api/ps`) — necessaire pour verifier que le contexte effectivement charge correspond
  a celui demande. Modification additive, seul point de construction du record (`query()`) mis a jour.
- Nouveau `testllm/coeur/domaine/ContextSizes` : calcule la liste des paliers a tester pour un
  modele donne (paliers fixes bornes par le contexte natif, plus ce contexte natif lui-meme si
  absent des paliers).
- Nouveau `testllm/infra/ContextCsvWriter` : export CSV cumulatif des resultats du balayage.
- Nouveau `testllm/ui/cli/TestContextCli` : point d'entree — menu GPU et choix du modele repris
  de TestMtpCli, balayage des paliers avec `OllamaAdapter.probe()` comme verification de charge
  (pas d'appel "Reponds OK" separe — probe() remplit deja ce role), lecture d'`/api/ps` apres
  chaque probe, execution des 3 passes standard (`Passes.TOUTES`) a chaque palier, tableau
  recapitulatif en console, CSV cumulatif (`bench/resultats-context.csv`) et resume.txt par run
  (`bench/context-<horodatage>/`).
- Nouveau `test-context.bat` (memes conventions que `test-mtp.bat`).

## Correction en cours de seance

Le CPU/GPU affiche venait initialement de deux sources melangees : la repartition `/api/ps`
(fiable, propre au processus Ollama) ET un echantillonnage `HardwareUsageSampler`/`HardwarePoller`
(nvidia-smi + `Win32_Processor.LoadPercentage`) — une mesure **systeme globale**, pas scopee au
processus Ollama, qui capte n'importe quelle autre activite de la machine pendant le test (signale
par l'utilisateur). Retire : `HardwareUsageSampler` n'est plus utilise dans `TestContextCli`, et
les colonnes `gpu0_util_pct`/`gpu1_util_pct`/`cpu_util_pct` ont ete retirees de `ContextCsvWriter`.
Seule reste la repartition `/api/ps` (`ollama_processor`) comme mesure CPU/GPU, avec le tok/s.

## Resultat

Compilation globale (`mvn compile`) et suite de tests (`mvn test`) OK sur l'ensemble du projet.
Non teste en conditions reelles (pas d'execution interactive possible depuis cette session) — a
valider par l'utilisateur au premier lancement, notamment la coherence de la valeur `context_length`
remontee par `/api/ps` selon la version d'Ollama installee.
