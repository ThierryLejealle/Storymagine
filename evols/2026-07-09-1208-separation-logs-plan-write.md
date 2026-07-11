# 2026-07-09 12h08 - Séparation claire PLAN / CRITIQUE LIVRE / WRITE dans les logs

## Demande
En relisant un master-log.txt réel (génération "Christelle & Thierry 1998"), deux problèmes de
lisibilité relevés par l'utilisateur :

1. Aucune séparation visuelle entre la fin de la planification de tous les chapitres d'une
   tentative "PLAN LIVRE" et le déclenchement de la critique livre (StoryFidelityCritic,
   StoryNarrativeCritic, StoryCausalCritic) qui peut relancer tout le plan. Dans le log, le
   dernier `-> plan retenu : ...` du dernier chapitre est immédiatement suivi, sans en-tête,
   par les appels LLM des 3 critics livre.
2. Le header `[Chapitre X/N]` était utilisé identiquement pendant la planification (dans
   `StoryPlanWorkflow`) et pendant l'écriture (dans `StoryOrchestrator`), ce qui masque le fait
   qu'il s'agit de deux grandes phases globales et séquentielles (on planifie TOUS les
   chapitres, puis on écrit TOUS les chapitres) et non d'un traitement chapitre par chapitre
   entrelacé plan+write.

## Ce qui a été touché
- `StoryOrchestrator.java` : ajout d'un header global `[PLAN]` avant l'appel à
  `storyPlanWorkflow.run(...)` et d'un header global `[WRITE]` avant la boucle d'écriture.
  Le header par chapitre de la boucle d'écriture devient `[WRITE chapitre X/N]` (au lieu de
  `[Chapitre X/N]`).
- `StoryPlanWorkflow.java` : le header par chapitre de `planEveryChapter()` devient
  `[PLAN chapitre X/N]` (au lieu de `[Chapitre X/N]`). Ajout d'un header `[CRITIQUE LIVRE]`
  juste avant le calcul du bloc de chapitres et l'appel aux 3 critics livre, une fois que
  `planEveryChapter()` a terminé — matérialise clairement la transition "tous les plans sont
  prêts" → "on critique et on décide de relancer ou non".
- Aucun changement de logique métier, de seuils, ni d'ordre des étapes : uniquement des labels
  de logs (`log.phaseHeader(...)`).

## Résultat
Nouvelle structure de log attendue pour une génération complète :

```
[SCENARIO] ...
[PLAN]
  [PLAN LIVRE] tentative 1/N
    [PLAN chapitre 1/N] ...
      [PLAN] tentative 1/M   (retry chapitre, inchangé)
    [PLAN chapitre 2/N] ...
    ...
    [CRITIQUE LIVRE]
      ... 3 critics livre ...
    -> moy ... PASS/RETRY
[WRITE]
  [WRITE chapitre 1/N] ...
    [WRITE 1/K] ...          (par séquence, inchangé)
  [POST-PROD]
  [WRITE chapitre 2/N] ...
  ...
```

Compilation (`mvn -pl redacteur -am compile`) et tests (`StoryPlanWorkflowTest`, qui n'assertent
que sur le label `"PLAN LIVRE"`, inchangé) passent sans modification.
