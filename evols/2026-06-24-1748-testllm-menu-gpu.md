# 2026-06-24 17h48 - Menu GPU + capture GPU dans testllm

## Objectif

Enrichir `testllm` avec :
1. Un menu de sélection GPU au démarrage (avant le menu mode)
2. La technique d'arrêt/relance d'Ollama sur un GPU précis
3. La capture GPU après chargement de chaque modèle
4. Ces infos tracées dans la console, les logs `.md` et le CSV

## Ce qu'on ajoute

### Fichiers à créer dans `testllm/src/main/java/storymagine/testllm/infra/`

#### `NvidiaSmiSnapshot.java`

Appelle `nvidia-smi --query-gpu=index,name,memory.used,memory.free,memory.total,utilization.gpu --format=csv,noheader,nounits`.

Record `GpuStat(int index, String name, int usedMb, int freeMb, int totalMb, int utilPct)`.

Méthodes :
- `capture()` → `List<GpuStat>` (retourne liste vide si nvidia-smi absent/erreur)
- `formatShort(List<GpuStat>)` → `"GPU 0 RTX5070: 4209/8151 Mo  |  GPU 1 RTX5060Ti: 3566/16311 Mo"`

#### `OllamaLauncher.java`

Gère le cycle de vie du processus Ollama.

Méthodes :
- `isReachable(String baseUrl)` → `boolean` — GET `/api/version`, timeout 3s
- `killAll()` — tue dans l'ordre : `"ollama app.exe"`, `"llama-server.exe"`, `"ollama.exe"` via `taskkill /f /im` ; attend que le port 11434 soit libéré (max 10 s) ; lève une exception si Ollama tient encore le port
- `launch(String cudaDevices, String baseUrl)` — `ollama serve` avec `CUDA_VISIBLE_DEVICES=cudaDevices` ; draine stdout en thread daemon ; attend que `/api/version` réponde (max 60 s)

**Ordre de kill obligatoire :** `ollama app.exe` en premier — sinon il relance `ollama.exe` automatiquement.

---

### Fichier modifié : `TestLlmCli.java`

#### Menu GPU (à insérer entre `assemble()` et le menu mode)

```
Configuration GPU Ollama :
  1. Utiliser Ollama deja demarre
  2. Relancer sur GPU 0 - RTX 5070 Laptop GPU    8 Go    (si nvidia-smi disponible)
  3. Relancer sur GPU 1 - RTX 5060 Ti             16 Go   (si nvidia-smi disponible)
  4. Relancer sur GPU 0+1 - split auto
  5. Complet : tester sur GPU 0, GPU 1 et GPU 0+1
Choix GPU [1-5] :
```

Si `nvidia-smi` n'est pas disponible (liste vide), les options 2 et 3 s'affichent sans nom ni taille :
```
  2. Relancer sur GPU 0 (CUDA_VISIBLE_DEVICES=0)
  3. Relancer sur GPU 1 (CUDA_VISIBLE_DEVICES=1)
```

Comportement par option :
- **Option 1** — vérifier `isReachable` ; si non joignable : message d'erreur clair + exit (pas de stack trace)
- **Options 2/3** — `killAll()` + `launch("0"|"1", url)` ; afficher confirmation `[testllm] Ollama pret - Mode GPU : GPU 0 - RTX...`
- **Option 4** — `killAll()` + `launch("0,1", url)`
- **Option 5** — relancer sur GPU 0 pour la sélection de modèle, puis boucler sur GPU 0 / GPU 1 / GPU 0+1 (voir section "Mode 5" ci-dessous)

Le label GPU actif (ex. `"GPU 0 - RTX 5070 Laptop GPU"`, `"GPU 0+1 split"`, `"?"` pour option 1) est transmis à `TestLlmService` pour être tracé.

#### Mode 5 — structure de sortie

Trois passes successives, chacune avec son propre sous-répertoire dans `runDir` :
```
bench/bench-TIMESTAMP/
  gpu-0/       (résultats GPU 0)
  gpu-1/       (résultats GPU 1)
  gpu-0-1/     (résultats GPU 0+1)
resume.txt     (synthèse des 3 passes)
```
CSV cumulatif : toutes les lignes dans `bench/resultats.csv`, distinguées par la colonne `gpu_mode`.

---

### Nouvelles infos tracées

#### Console — après le probe de chaque modèle

```
[testllm] │  ollama ps  → 7.7 Go  |  25%/75% CPU/GPU
[testllm] │  nvidia-smi → GPU 0 RTX 5070 Laptop GPU : 4209/8151 Mo  |  GPU 1 RTX 5060 Ti : 3566/16311 Mo
```

L'info `ollama ps` nécessite un appel GET `/api/ps` après probe (comme dans Redacteur `OllamaPsInfo`).

#### Logs `.md` par modèle/passe — section `## GPU STATE`

À ajouter en tête de chaque fichier de log (après le titre, avant `## PROMPT SYSTEME`) :

```markdown
## GPU STATE
- Mode GPU   : GPU 0 - RTX 5070 Laptop GPU
- ollama ps  : 7.7 Go  |  25%/75% CPU/GPU
- GPU 0      : RTX 5070 Laptop GPU            4209 / 8151 Mo
- GPU 1      : RTX 5060 Ti                    3566 / 16311 Mo
```

#### CSV — 7 nouvelles colonnes (après la colonne `divergence`)

```
gpu_mode;ollama_size_go;ollama_processor;gpu0_used_mo;gpu0_total_mo;gpu1_used_mo;gpu1_total_mo
```

- `gpu_mode` : label GPU actif (ex. `"GPU 0 - RTX 5070 Laptop GPU"`)
- `ollama_size_go` : taille du modèle en VRAM selon `/api/ps` (ex. `"7.7"`)
- `ollama_processor` : processeur selon `/api/ps` (ex. `"25%/75% CPU/GPU"`)
- `gpu0_used_mo`, `gpu0_total_mo`, `gpu1_used_mo`, `gpu1_total_mo` : valeurs nvidia-smi au moment du probe

---

### Infra `/api/ps` — `OllamaPsInfo` ou équivalent

Interroge GET `/api/ps` après probe pour récupérer :
- `sizeLabel()` → `"7.7 Go"`
- `processorLabel()` → `"25%/75% CPU/GPU"` (ratio CPU/GPU calculé depuis les champs `size` et `size_vram`)

Peut être une classe dédiée ou une méthode utilitaire dans `OllamaLauncher`. À décider à l'implémentation.

---

## Fichiers à créer/modifier

| Fichier | Action |
|---|---|
| `testllm/src/main/java/storymagine/testllm/infra/NvidiaSmiSnapshot.java` | Créer |
| `testllm/src/main/java/storymagine/testllm/infra/OllamaLauncher.java` | Créer |
| `testllm/src/main/java/storymagine/testllm/infra/OllamaPsInfo.java` | Créer |
| `testllm/src/main/java/storymagine/testllm/ui/cli/TestLlmCli.java` | Modifier |
| `testllm/src/main/java/storymagine/testllm/coeur/service/TestLlmService.java` | Modifier (champ `gpuMode`, expose l'info GPU dans les logs) |
| `testllm/src/main/java/storymagine/testllm/infra/BenchCsvWriter.java` | Modifier (7 nouvelles colonnes) |

## Reste à traiter

- Rollback des mêmes modifications dans `Redacteur` (projet séparé — à faire dans une session Redacteur)

## Note technique

Le projet Storymagine n'a pas de git. Les classes `NvidiaSmiSnapshot` et `OllamaLauncher` existent déjà dans `Redacteur/src/main/java/story/bench/` — les porter en changeant le package vers `storymagine.testllm.infra`.
