# 2026-06-23 08h30 — Service, Assembler et CLI complet

## Evolution demandée
Câbler le module `redacteur` bout-en-bout pour que `lancer.bat` fonctionne :
charger le scénario, lancer la génération, afficher les logs, écrire les fichiers résultat.

## Ce qui a été touché

### Nouveaux fichiers

**`coeur/service/RedacteurService.java`**
Interface publique du module : `validate()`, `generate()`, `listScenarios()`.

**`coeur/service/RedacteurServiceImpl.java`**
Implémentation : charge et valide le scénario via `ScenarioReaderPort`, appelle `StoryOrchestrator`, renvoie la Story.

**`RedacteurModule.java`** (racine du module)
Assembleur hexagonal unique : câble ~20 agents (`ModelCallPort` → agents → Steps → Workflows → `StoryOrchestrator` → `RedacteurServiceImpl`).
Méthode principale : `assemble(OllamaConfig, modelName)`.

**`infra/StoryExporter.java`**
Exporte la Story générée sur disque : un fichier par chapitre (`chapitre-01.txt`…) + `histoire-complete.txt` dans le dossier `generated/` du scénario.

**`ui/cli/RedacteurCli.java`** (remplace le stub)
CLI interactive complète :
- Connexion Ollama, liste des modèles avec favoris en tête (lus depuis `redacteur.properties`)
- Sélection scénario, profil (BROUILLON / STANDARD / FULL), confirmation
- Génération avec affichage de progression et durée
- Export dans `<scenario>/generated/`

**`src/main/resources/redacteur.properties`**
Configuration Ollama (URL, contexte, timeouts, retries) + liste de modèles favoris calquée sur `testllm.properties`.

### Corrections orthogonales (bugs bloquants)

**BOM UTF-8 sur 72 fichiers agents**
Les agents créés dans la session précédente avaient un BOM `﻿` (outil `Write` sur Windows).
Suppression en bulk via PowerShell `[System.IO.File]::ReadAllBytes` / `WriteAllBytes`.

**`LlmResult` vs `String` — 24 agents**
`ModelCallPort.generate()` retourne `LlmResult` mais tous les agents assignaient directement à `String`.
Fix bulk via regex PowerShell : ajout de `.text()` après chaque appel `llm.generate(...)`.
3 cas atypiques (`user.toString()`, `sb.toString()` dans les args) corrigés manuellement :
`SequencePlanner`, `DeusInMachinaChecker`, `FocusActionFilter`.

**Chemin de sortie `output/` → `generated/`**
La convention du projet utilise `generated/` comme répertoire de sortie des scénarios.

## Résultat
- 152 fichiers compilent sans erreur (BUILD SUCCESS).
- `lancer.bat` → `RedacteurCli` → `RedacteurModule.assemble()` → `StoryOrchestrator` → `StoryExporter` : pipeline complet opérationnel.
- Sortie dans `<scenario>/generated/chapitre-XX.txt` + `histoire-complete.txt`.
