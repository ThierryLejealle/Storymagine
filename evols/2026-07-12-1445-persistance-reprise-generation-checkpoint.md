# 2026-07-12 14h45 — Persistance et reprise d'une génération interrompue (checkpoint)

## 1. Demande

L'utilisateur a demandé si l'état d'une génération d'histoire pouvait être persisté pour la
reprendre plus tard (utile pour les longues générations FULL interrompues volontairement ou par
un incident). Décision explicite après échange :
- Granularité de reprise au niveau **chapitre** (pas séquence) : `WriteWorkflow` reste inchangé,
  un chapitre interrompu en cours d'écriture est simplement refait depuis son début à la reprise.
- Si l'interruption survient pendant la phase PLAN (avant le premier chapitre écrit), pas de
  reprise possible — il faut relancer depuis zéro. Limite assumée.
- Le profil de génération (BROUILLON/SIMPLE/FULL) est réutilisé automatiquement depuis le
  checkpoint à la reprise, jamais redemandé.
- Le fichier de checkpoint est écrasé (pas accumulé) à chaque chapitre terminé, et supprimé dès
  que la génération se termine avec succès — seules les générations réellement interrompues
  restent listées comme "reprenables".

Aucun prompt LLM touché — uniquement de l'orchestration et de la persistance Java.

## 2. Ce qui a été touché

### Domaine (`coeur/domaine/story`)
Ajout du pattern `snapshot()`/`restore()` (déjà existant sur `WorldState`) à `Story`,
`WrittenChapter` et `RepetitionMemory`, pour obtenir une représentation immuable et
reconstructible de l'état complet d'une génération, sans jamais faire dépendre le domaine d'un
framework de sérialisation :
- `RepetitionMemory.Snapshot` (phrases/thèmes interdits + fenêtres calibrées).
- `WrittenChapter.Snapshot` + `WrittenChapter.restore(...)` (factory statique).
- `Story.Snapshot` (compose les trois précédents) + `Story.restore(...)`.

### `GenerationCheckpoint` (nouveau, `coeur/domaine/orchestrator`)
Record `{ Story.Snapshot story, int nextChapterIndex, GenerationConfig config }` — l'état complet
nécessaire pour reprendre une génération identique à l'originale.

### `CheckpointPort` (nouveau, `coeur/ports`)
`save(checkpoint)` / `clear()` (run actif, via `Supplier<Path>` runDir comme `HtmlExportPort`) et
`load(runDir)` / `findIncomplete(generatedDir)` (accès explicite à un run arbitraire, pour lister
et charger les générations reprenables). `CheckpointPort.NOOP` pour les assemblages qui n'en ont
pas besoin (tests, `testeur`, etc.).

### `JsonCheckpointAdapter` (nouveau, `infra/checkpoint`)
Implémentation Jackson (`jackson-databind`, déjà une dépendance du module) qui sérialise
directement les records `GenerationCheckpoint`/`Story.Snapshot`/... sans DTO intermédiaire — les
records du domaine sont sérialisables tels quels par Jackson (natif depuis 2.12, aucune annotation
requise), donc le domaine reste sans aucune dépendance vers Jackson. Fichier `checkpoint.json`
dans le dossier de run horodaté existant (`generated/<timestamp>/`).

### `StoryOrchestrator`
Boucle d'écriture/évaluation par chapitre extraite dans une méthode privée partagée
(`writeAndEvaluate`), utilisée par :
- `generate(...)` (inchangé dans son comportement : plan puis écriture depuis le chapitre 0) ;
- `resume(scenario, runDir)` (nouveau) : charge le checkpoint, restaure la `Story`, saute la phase
  PLAN, reprend la boucle à `nextChapterIndex`.

Sauvegarde du checkpoint après chaque chapitre terminé (juste après l'export HTML), suppression
(`clear()`) une fois tous les chapitres traités.

### `FileLogAdapter` (module `commun`)
+ méthode `resumeRunDir(Path)` : pointe directement les logs/traces/HTML sur un dossier de run
existant, au lieu de toujours en créer un nouveau horodaté (`setOutputDir` + `initRunDir`).

### `RedacteurService` / `RedacteurServiceImpl`
+ méthode `resume(scenarioRoot, runDir)` (même validation de scénario que `generate`, factorisée
dans `loadValidScenario`).

### `RedacteurModule`
Nouvel overload d'assemblage acceptant un `CheckpointPort` (les overloads existants délégent avec
`CheckpointPort.NOOP`, aucun appelant existant cassé).

### `RedacteurCli`
Après le choix du scénario : scan de `generated/*/checkpoint.json` via
`checkpointPort.findIncomplete(...)`. Si des générations interrompues existent, question
"nouvelle génération / reprendre laquelle" (ENTREE = nouvelle). En cas de reprise : le profil
n'est pas redemandé, `fileLog.resumeRunDir(...)` remplace `fileLog.setOutputDir(...)`, et
`service.resume(...)` remplace `service.generate(...)`.

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec (suite existante
inchangée — aucun test ne construisait `StoryOrchestrator` directement, seul `RedacteurModule` est
impacté par le changement de signature). Non encore testé en conditions réelles (interruption
manuelle d'une génération FULL en cours puis relance du CLI) — à valider par l'utilisateur au
prochain run long.
