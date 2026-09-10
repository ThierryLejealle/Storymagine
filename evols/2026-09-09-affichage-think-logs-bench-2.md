# 2026-09-09 - Affichage think manquant sur les lignes live du bench

## Demande
En observant un run `test-llm.bat` sur un modèle qui supporte le thinking (`Qwen3.8-27B-...-MTP`,
famille qwen35), l'utilisateur a demandé confirmation que le run était sans thinking. Réponse
donnée par déduction (longueur/vitesse de sortie), mais l'utilisateur a fait remarquer à juste
titre que rien dans les logs ne l'indique explicitement pendant l'exécution.

## Touché
- `BenchmarkRunner.BenchEvent.RunOk` et `RunFail` (module testllm, `coeur/domaine`) : ajout du
  champ `boolean think`, renseigné depuis `runPasse()`.
- `ConsoleRunLogger` : les lignes live `[bench] |  haiku  run X/Y -> OK ...` affichent désormais
  `[think:O]`/`[think:N]` juste après le nom de la passe.

## Cause
`BenchmarkRunner.execute()` teste automatiquement un modèle "thinking-capable" deux fois :
`thinkVars = List.of(false, true)`. Seul l'événement `IterationSummary`, imprimé en `[bench] +-
resume ... [think:N]` **après** les 3 passes (haïku/moyen/grand) complètes d'une itération,
portait cette info — les lignes `RunOk`/`RunFail` imprimées en direct pendant chaque appel ne la
portaient pas du tout. L'utilisateur observait donc un run en cours sans aucun moyen de savoir,
avant la fin de toute l'itération, s'il regardait la passe think:false ou think:true.

## Résultat
Compilation (`mvn -q -pl testllm -am compile`) et tests unitaires (`mvn -q -pl testllm -am test`,
`BenchmarkRunnerTest` inclus) vérifiés verts. Aucun changement au Service ni aux Ports.
