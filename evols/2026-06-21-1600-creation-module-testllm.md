# 2026-06-21 16h00 - Création du module testllm (benchmark LLM)

## Description de l'évolution demandée

Création du module `/testllm` dans Storymagine. Ce module permet de benchmarker les modèles
Ollama disponibles avec trois types de tests : court (haïku), moyen (plan de séquence),
grand (rédaction de texte littéraire). Trois modes d'utilisation : tous les modèles, les
favoris, ou un modèle choisi interactivement.

## Ce qui a été touché

### Nouveau — structure Maven
- `testllm/pom.xml` — module Maven, dépend de `:commun`
- `pom.xml` — ajout du module `testllm` dans le parent

### Nouveau — `testllm/testllm.properties`
- 4 modèles favoris configurés (variantes gemma-4 E2B/E4B/12B/26B)
- Configuration Ollama complète (URL, contexte, retry, timeouts)
- Paramètres bench (runs, taille max modèle, répertoire de sortie)

### Nouveau — `testllm/coeur/domaine/`
- `PasseBench` — record : nom + systemPrompt + userPrompt
- `Passes` — constantes HAIKU, MOYEN, GRAND + liste TOUTES
- `BenchPasseResult` — résultat d'une passe pour un modèle (ex ModelBenchResult Redacteur)
- `BenchModeleResult` — synthèse globale d'un modèle sur toutes passes (ex ModelSummary)
- `BenchRun` — résultat complet d'une session benchmark
- `MemorySampler` — échantillonnage VRAM via LongSupplier injecté (pas d'HTTP dans le domaine)
- `BenchmarkRunner` — logique de benchmark pure, sans I/O ; interface AdapterFactory et RunLogger

### Nouveau — `testllm/coeur/service/`
- `TestLlmService` — interface exposant benchTous / benchFavoris / benchModele / listerModeles / listerFavoris
- `TestLlmServiceImpl` — implémentation

### Nouveau — `testllm/infra/`
- `TestLlmConfig` — lecture de testllm.properties, fabrique OllamaConfig
- `VramPoller` — LongSupplier qui interroge /api/ps pour la VRAM (package-private)
- `ThinkingDetector` — détecte le support thinking via /api/show sans probe() (package-private)
- `BenchCsvWriter` — export CSV cumulatif (resultats.csv)
- `BenchLogWriter` — écriture des logs .md par modèle/passe
- `BenchTextFormatter` — formatage des tableaux texte (formatPasse, formatSynthese, formatParModele, formatDivergences)
- `ConsoleRunLogger` — implémente RunLogger : console + délègue l'écriture .md à BenchLogWriter
- `TestLlmAssembler` — câble coeur + adaptateurs + config

### Nouveau — `testllm/ui/cli/`
- `TestLlmCli` — point d'entrée, parse le mode (tout/favoris/choisir), appelle le service,
  écrit resume.txt et resultats.csv

### Nouveau — `test-llm.bat`
- Encodage CP1252, délègue à TestLlmCli via `mvn exec:java`
- Usage : `test-llm.bat [tout|favoris|choisir]` (défaut : choisir)

## Résultat

Architecture hexagonale propre :
- Domaine POJO sans aucun I/O ni HTTP
- VRAM et thinking detection en infra (LongSupplier / ThinkingDetector)
- LlmBenchmarkRunner 1100 lignes de Redacteur éclaté en 8 classes à responsabilité unique
- Sorties : bench/bench-YYYYMMDD-HHmm/ avec resume.txt + .md par passe, et bench/resultats.csv cumulatif
