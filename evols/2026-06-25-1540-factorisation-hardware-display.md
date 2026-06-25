# 2026-06-25 15h40 - Factorisation affichage info matériel RAM/VRAM et GPU

## Evolution demandée

Afficher dans `lancer.bat` les mêmes lignes d'info matériel que `test-llm.bat` :
```
[redacteur] ollama ps  -> 7,9 Go      |  31%/69% CPU/GPU
[redacteur] nvidia-smi -> GPU 0 ... : 7545/8151 Mo  |  GPU 1 ... : 0/16311 Mo
```
Et factoriser le code entre les deux modules.

## Ce qui a été touché

### Nouveau — commun/infra
- `OllamaPsInfo.java` — déplacé de `testllm/infra`, package changé en `storymagine.commun.infra`
- `NvidiaSmiSnapshot.java` — idem
- `ModelHardwareDisplay.java` — nouvelle classe factorisant l'affichage des deux lignes avec un préfixe paramétrable

### Supprimé — testllm/infra
- `OllamaPsInfo.java` (remplacé par la version commun)
- `NvidiaSmiSnapshot.java` (idem)

### Imports mis à jour — testllm
- `GpuState.java` — import commun
- `BenchLogWriter.java` — import commun
- `BenchCsvWriter.java` — import commun
- `ConsoleRunLogger.java` — import commun + refactorisé les deux printf en `ModelHardwareDisplay.print("[bench] |  ", ...)`
- `HardwarePoller.java` — import commun
- `TestLlmCli.java` — import commun

### Modifié — redacteur
- `RedacteurCli.java` — après confirmation, avant génération :
  1. `ollama.adapter(selectedModel).probe()` pour charger le modèle en mémoire
  2. `OllamaPsInfo.query()` + `NvidiaSmiSnapshot.capture()`
  3. `ModelHardwareDisplay.print("[redacteur] ", ...)` pour afficher les deux lignes

## Résultat

- `OllamaPsInfo` et `NvidiaSmiSnapshot` vivent désormais dans `commun/infra` (partagés)
- `ModelHardwareDisplay` centralise le formatage — un seul endroit à modifier si le format change
- `test-llm.bat` et `lancer.bat` affichent le même format, avec préfixe distinct (`[bench] |  ` vs `[redacteur] `)
