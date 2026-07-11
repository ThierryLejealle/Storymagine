# 2026-07-12 01h45 — Fix affichage VRAM perime dans RedacteurCli

## 1. Demande

L'utilisateur a remarqué, en cours de génération réelle, que les chiffres "Mo" affichés par
`RedacteurCli` pour les GPU (`nvidia-smi -> GPU 0 ... : 1401/8151 Mo | GPU 1 ... : 0/16311 Mo`)
étaient faux — `nvidia-smi` lancé manuellement à ce moment-là montrait en réalité 7537/8151 MiB
(GPU 0) et 13976/16311 MiB (GPU 1). Hypothèse de l'utilisateur : mesuré au mauvais moment.

## 2. Diagnostic

Confirmé. `RedacteurCli.main()` capture `nvidia-smi` **une seule fois**, tout au début du
programme (`List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture();`, étape 1 — menu
GPU), avant même le choix du modèle, du scénario et le chargement réel du modèle
(`llm.probe()`, étape 7). Ce même snapshot `gpus`, périmé, est ensuite réutilisé tel quel pour
l'affichage post-chargement (`ModelHardwareDisplay.print("[redacteur] ", ps, gpus)`) — alors que
`ps` (résultat `ollama ps`), lui, est bien requêté fraîchement à ce moment-là. D'où l'incohérence
visible : `ollama ps` reflète le modèle réellement chargé (22,5 Go, 83% GPU) pendant que
`nvidia-smi` affiche encore l'état d'avant le chargement.

`testllm` (`ConsoleRunLogger.java`, événement `ProbeOk`) fait ça correctement : il recapture
`NvidiaSmiSnapshot.capture()` juste après le probe, immédiatement avant l'affichage — c'est le
même motif qui a servi de référence pour le correctif.

## 3. Ce qui a été touché

### `redacteur/ui/cli/RedacteurCli.java`
Ajout d'une recapture fraîche (`gpusAfterLoad`) juste avant l'appel à `ModelHardwareDisplay.print`,
après `llm.probe()` — au lieu de réutiliser le `gpus` capturé au tout début (étape 1, menu GPU).
Le `gpus` d'origine reste utilisé tel quel pour son usage légitime : afficher l'état AVANT
lancement dans le menu de sélection GPU (`printGpuOption`, `gpuLabel`).

## 4. Résultat

`mvn compile` : OK. Pas de test unitaire applicable — code CLI pur (`ui/cli`), hors périmètre du
coeur testable ; vérifié par lecture du flux d'exécution et comparaison avec le motif correct
déjà en place dans `testllm/ConsoleRunLogger`.
