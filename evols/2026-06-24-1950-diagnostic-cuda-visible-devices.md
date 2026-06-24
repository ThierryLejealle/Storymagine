# 2026-06-24 19h50 - Diagnostic CUDA_VISIBLE_DEVICES + capture logs Ollama

## Evolution demandee

Lors des tests Mode 5 (iteration modele x config GPU), CUDA_VISIBLE_DEVICES=1 est ignore :
Ollama charge les modeles sur GPU 0 (5070, 8 Go) au lieu de GPU 1 (5060 Ti, 16 Go).
Symptome observe : 26-A4B met 7 Go sur 5070, rien sur 5060, 74% CPU (normal si GPU 0 seul,
attendu si CUDA_VISIBLE_DEVICES etait honore le modele tiendrait entier en GPU 1).

Deux mesures prises :

### 1. Capture des logs de demarrage Ollama

Au lieu de jeter tout le stdout d'Ollama, on capture et affiche les 40 premieres lignes
avec le prefixe [ollama]. OLLAMA_DEBUG=1 active les logs verbeux de selection GPU.
Objectif : voir explicitement quel GPU Ollama annonce utiliser au demarrage.

### 2. Wrapper cmd.exe

Remplacement de `new ProcessBuilder("ollama", "serve")` par
`new ProcessBuilder("cmd", "/c", "ollama serve")`.
Hypothese : passer par le shell Windows permet que la variable d'environnement
CUDA_VISIBLE_DEVICES soit dans la session cmd avant qu'Ollama soit lance,
ce qui peut aider si Ollama lit les variables autrement qu'en heritant du process parent.

## Ce qui a ete touche

| Fichier | Changement |
|---|---|
| `testllm/infra/OllamaLauncher.java` | `launch()` : ProcessBuilder -> cmd wrapper ; OLLAMA_DEBUG=1 ; drainer -> BufferedReader 40 lignes |

## Note diagnostic

Si les logs [ollama] montrent "GPU 0" malgre CUDA_VISIBLE_DEVICES=1, cela confirme
qu'Ollama override la variable en interne lors du spawn de llama-server.exe.
Dans ce cas il faudra une approche differente (OLLAMA_GPU_OVERHEAD, ou accepter
que Ollama gere lui-meme la selection GPU).

## Resultat

**Cause racine confirmee** : Vulkan (OLLAMA_VULKAN=true par defaut) ignorait CUDA_VISIBLE_DEVICES
et causait un "device count mismatch" (3 llama-server vs 5 Vulkan). En consequence, Ollama
classait la 5060 Ti comme "integrated GPU" et la droppait, chargeant tout sur la 5070.

**Fix confirme** : OLLAMA_VULKAN=false resout le probleme.
Avec Vulkan desactive, Ollama choisit entre cuda_v12 et cuda_v13, garde cuda_v13 (Blackwell
compute=12.0), et identifie correctement la 5060 Ti via CUDA_VISIBLE_DEVICES=1.
Log final : "Ollama pret - GPU 1 - NVIDIA GeForce RTX 5060 Ti".

Note : Ollama injecte CUDA_VISIBLE_DEVICES=0 + GGML_CUDA_INIT=1 quand il spawne llama-server
(remap interne de son GPU selectionne en "device 0"). Cela peut provoquer un split sur les
deux cartes dans certains cas, ce qui s'est revele plus rapide.
