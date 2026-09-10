# 2026-09-09 - Scénarios de prompt substantiels pour testMTP

## Demande
Le haïku utilisé jusque-là dans le sweep MTP (~20-25 tokens) donnait des résultats "pourris" —
trop court pour que `draft_num_predict` ait un effet mesurable de façon stable (bruit de mesure
dominant). Remplacer par deux scénarios plus substantiels (~500 mots), choisis au menu comme pour
le thinking : une histoire courte sur un aviateur de la Seconde Guerre mondiale, et une fonction
Java de taille comparable.

Modification de prompt validée au préalable (delta/raison/objectif présentés, cf. tour de
conversation) : la taille de la fonction Java a été calibrée en la faisant réellement écrire par
Claude et en mesurant le résultat (LruCache minimal : 87 lignes / 226 mots / 2041 caractères, jugé
trop court) puis étoffée (remove/containsKey/toString/javadoc par méthode → 139 lignes / 427 mots /
3533 caractères, comparable à 500 mots de prose ~2900-3300 caractères).

## Touché
- `TestMtpCli` (module testllm, `ui/cli`) :
  - Nouvel enum privé `Scenario` (HISTOIRE, FONCTION) portant chacun son propre system/user prompt,
    remplace les constantes `SYSTEM_PROMPT`/`USER_PROMPT` (haïku) supprimées.
  - Nouveau menu `demanderScenarios()` (1. Histoire / 2. Fonction Java / 3. Les deux — défaut),
    même style que `demanderThinkMode()`.
  - La boucle principale imbrique désormais `for (Scenario scenario : scenarios) { for (boolean
    think : thinkVars) { ...sweep... } }` — jusqu'à 4 sweeps complets en "les deux / les deux".
    En-têtes, tableaux et lignes live incluent le libellé du scénario.
  - `testerValeur()`/`appel()` prennent désormais un `Scenario` et utilisent ses prompts.
- `MtpCsvWriter` : colonne `scenario` ajoutée à l'en-tête et à chaque ligne (nom de l'enum).
- **Prompts repassés en anglais** (delta validé séparément, cf. tour de conversation) : un run
  réel sur `Qwen3.8-27B-...-Aggressive-MTP` a montré le modèle mélanger français/anglais en cours
  de génération (consigne de langue absente des prompts d'origine, rédigés en français). Plutôt que
  d'ajouter "en français" (fix initialement proposé), l'utilisateur a demandé de repasser les deux
  prompts intégralement en anglais avec consigne de langue explicite (histoire en anglais ; noms de
  variables et commentaires du code en anglais) — langue plus stable pour la plupart des modèles
  testés. Seuls les prompts envoyés au modèle ont changé ; les libellés de menu CLI restent en
  français.

## Résultat
Compilation et tests unitaires du module testllm verts (`mvn -q -pl testllm -am test`), compilation
complète du projet vérifiée (`mvn -q compile`). Pas de smoke-test live cette fois — l'utilisateur a
signalé qu'un test tournait déjà en parallèle sur sa machine (source des "crashs" Ollama observés
plus tôt dans la session, pure contention GPU, pas un bug) ; éviter de solliciter Ollama en
concurrence. Aucun changement au cœur testLLM (service/domaine/ports) : scénarios scopés à testMTP
uniquement, comme demandé.
