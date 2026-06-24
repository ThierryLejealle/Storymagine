# 2026-06-24 18h00 - Menu GPU + capture GPU dans testllm

## Evolution demandee

Port de la fonctionnalite GPU depuis le projet Redacteur (ou elle avait ete implementee par erreur)
vers le module `testllm` de Storymagine. Objectifs :
- Menu de selection GPU avant le menu mode
- Capacite d'arreter/relancer Ollama sur un GPU precis
- Capture de l'etat GPU apres chaque probe de modele
- Tracer ces infos dans la console, les logs .md et le CSV

Modification config : timeout double, retry reduit a 2 max, multiplicateur gros modeles passe a 2.

## Ce qui a ete touche

### Nouvelles classes crees (infra testllm)

| Fichier | Role |
|---|---|
| `NvidiaSmiSnapshot.java` | Port de Redacteur - capture nvidia-smi, record GpuStat |
| `OllamaLauncher.java` | Port de Redacteur - killAll() / launch() avec CUDA_VISIBLE_DEVICES |
| `OllamaPsInfo.java` | Port de Redacteur - query /api/ps, labels sizeLabel/processorLabel |
| `GpuState.java` | Nouveau record : (gpuMode, OllamaPsInfo, List<GpuStat>) capture apres probe |
| `BenchSession.java` | Nouveau record : (TestLlmService, ConsoleRunLogger) retourne par l'assembleur |

### Classes modifiees

| Fichier | Changement |
|---|---|
| `TestLlmConfig.java` | Defaults : timeoutMs 600000->1200000, retryCount 5->2, largeModelTimeoutMultiplier 3->2 |
| `BenchLogWriter.java` | Signature write() +GpuState ; section ## GPU STATE en tete de chaque .md |
| `BenchCsvWriter.java` | Signature appendResults() +Map<String,GpuState> ; 7 nouvelles colonnes CSV |
| `ConsoleRunLogger.java` | +gpuMode +baseUrl au constructeur ; capture GpuState sur ProbeOk ; affichage console ollama ps + nvidia-smi ; getGpuStates() |
| `TestLlmAssembler.java` | +gpuMode param ; passe baseUrl au logger ; retourne BenchSession au lieu de TestLlmService |
| `TestLlmCli.java` | Menu GPU (1=existant, 2=GPU0, 3=GPU1, 4=0+1, 5=complet) ; Mode 5 = iteration modele par modele, chaque modele teste sur GPU 0 puis GPU 1 puis GPU 0+1 avant de passer au suivant |

## Architecture - points cles

- `BenchSession` : permet a la CLI d'acceder a `logger.getGpuStates()` apres le bench pour ecrire le CSV
- Mode 5 : boucle externe = modeles, boucle interne = configs GPU ; un `resume` par repertoire GPU accumule tous les modeles ; `resume.txt` ecrit a la fin
- Ollama reste sur GPU 0 depuis la phase interactive pour le 1er modele ; kill/launch a chaque changement de GPU ensuite
- `BenchmarkRunner` etant stateful, chaque combinaison (modele x GPU) cree une nouvelle BenchSession
- Le domaine n'est pas touche ; tout est infra

## Resultat

Compilation attendue propre. Rollback dans Redacteur a faire dans une session separee.
