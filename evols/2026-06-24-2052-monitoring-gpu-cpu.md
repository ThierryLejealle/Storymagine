# 2026-06-24 20h52 - Monitoring GPU et CPU pendant les benchmarks

## Evolution demandée

Pendant l'exécution des benchmarks (test-llm), mesurer toutes les 10 secondes :
- L'utilisation (%) de chaque GPU (GPU 0 et GPU 1 via nvidia-smi)
- L'utilisation (%) CPU totale (via typeperf Windows)

Afficher la moyenne par modèle à la fin de chaque test, dans la synthèse, et dans le CSV.

## Fichiers touchés

**Domaine (nouveaux)**
- `testllm/coeur/domaine/HardwareUsage.java` — record `(gpu0Pct, gpu1Pct, cpuPct)` + constante `ABSENT`
- `testllm/coeur/domaine/HardwareUsageSampler.java` — thread virtuel, poll toutes les 10s, calcul de moyenne

**Domaine (modifiés)**
- `testllm/coeur/domaine/BenchModeleResult.java` — ajout du champ `HardwareUsage avgHardware`
- `testllm/coeur/domaine/BenchmarkRunner.java` — ajout `hardwareSampler()` dans `AdapterFactory`, démarrage/arrêt de `HardwareUsageSampler`, propagation dans `BenchModeleResult` et `BenchEvent.ModelUnload`

**Infra (nouveau)**
- `testllm/infra/HardwarePoller.java` — `Supplier<HardwareUsage>` via `NvidiaSmiSnapshot` (GPU util%) + `typeperf` (CPU%)

**Infra (modifiés)**
- `testllm/infra/TestLlmAssembler.java` — implémentation de `hardwareSampler()` avec `HardwarePoller.create()`
- `testllm/infra/ConsoleRunLogger.java` — affichage `GPU0=xx% GPU1=xx% CPU=xx%` sur l'événement `ModelUnload`
- `testllm/infra/BenchCsvWriter.java` — 3 nouvelles colonnes : `gpu0_util_pct;gpu1_util_pct;cpu_util_pct`
- `testllm/infra/BenchTextFormatter.java` — 3 nouvelles colonnes dans `formatSynthese` + ligne `hw avg` dans `formatParModele`

## Résultat

Après chaque modèle benchmarké, la console affiche :
```
[bench] |  hw avg  : GPU0= 87%  GPU1= 62%  CPU=  9%
[bench] +- dechargement llama3.2:3b
```

La synthèse texte et le CSV contiennent les colonnes GPU0%, GPU1%, CPU% pour chaque modèle.

## Notes techniques

- nvidia-smi était déjà utilisé et capture `utilization.gpu` — réutilisé sans nouvelle dépendance.
- CPU : méthode primaire `Win32_Processor.LoadPercentage` via CIM/PowerShell (locale-indépendant, retourne un entier direct). Fallback `typeperf` avec chemins FR et EN si CIM échoue.
- Le sampler tourne au niveau modèle (même granularité que `MemorySampler` VRAM), en thread virtuel.
