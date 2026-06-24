# 2026-06-23 17h00 - Logs console + fichier

## Description

Mise en place d'un systeme de logs horodates, envoyes simultanement vers la console
et vers des fichiers dans le repertoire de sortie du scenario.

Inspire de `../Redacteur` (ConsoleProgressLogger + SessionLogger).

## Ce qui a ete touche

### commun/coeur/ports
- `LogPort` : 4 nouvelles methodes (`llmCall`, `chapterPlan`, `sequenceText`, `sessionEnd`)
  + champ statique `NOOP` pour les tests et contextes sans log.

### commun/infra (modifies)
- `ConsoleLogAdapter` : horodatage `[HH:mm:ss]` sur toutes les lignes,
  `llmCall()` avec cumul running tokens, `sessionEnd()` avec boite de stats.
- `OllamaAdapter` : injection `LogPort` (NOOP par defaut) ; `logLlmCall()`
  route desormais via `log.llmCall()` au lieu de `System.out.printf`.
- `OllamaConfig` : nouveaux overloads `adapter(model, LogPort)` et
  `adapter(model, think, LogPort)`.

### commun/infra (nouveaux)
- `TeeLogAdapter` : fan-out vers N delegates.
- `FileLogAdapter` : init paresseuse ; ecrit
  `{scenario}/generated/{timestamp}/master-log.txt`,
  `chapters/{slug}/plan.md` et `chapters/{slug}/seq_N.md`.

### redacteur
- `RedacteurModule` : nouvel overload `assemble(OllamaConfig, model, LogPort)`.
- `RedacteurCli` : cree `TeeLogAdapter(ConsoleLogAdapter, FileLogAdapter)` avant
  l'assemblage ; appelle `fileLog.setOutputDir(...)` apres selection du scenario ;
  affiche le chemin du dossier de logs dans le recap final.
- `RedacteurServiceImpl` : suppression des 3 `System.out.println` de chargement.
- `StoryOrchestrator` : `log.phaseHeader("SCENARIO", ...)` en tete de generation,
  `log.sessionEnd()` en fin.
- `PlanWorkflow` : `log.chapterPlan()` apres restauration du meilleur plan.
- `WriteWorkflow` : `log.sequenceText()` en fin de `writeSequence()`.

### tests
- `CapturingLogPort` : implementation des 4 nouvelles methodes (NOOP).
- `BenchmarkRunnerTest` : 2 lambdas `ModelCallPort` converties en classes anonymes
  (ModelCallPort n'est plus une interface fonctionnelle depuis l'ajout de
  `contextWindow()` dans une precedente session).

## TODO - llm_calls/ (reporte)

Pour sauvegarder les prompts complets (system + user + reponse) dans
`generated/{ts}/llm_calls/{N}_{agent}_{pass}.md`, il faudrait soit :
- Un decorateur `LoggingModelCallPort` qui intercepte `generate()` et
  transmet les prompts au FileLogAdapter.
- Ou une modification des agents pour exposer le contexte.
Evalue et implemente dans une prochaine session.

## Resultat

BUILD SUCCESS -- 12/12 tests passent.
Format de sortie console :
  [12:34:56] [SCENARIO] as-du-ciel - 5 chapitre(s)
  [12:34:56] [Chapitre 1/5] Arrivee a Londres
  [12:34:56] [PLAN] tentative 1/4
  [12:34:58]   ChapterPlanner                  2,345ms
  [12:34:58]   [LLM #  1]    2,343ms   12345 ->   456 tok  2.1 tok/s  [sum in:12.3k out:0.5k]
  ...
  [boite statistiques finales]