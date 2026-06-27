# 2026-06-27 20h30 — Refactoring packages agents : writer → sequence + nettoyage préfixes

## Evolution demandée

Audit de cohérence des packages agents. Deux problèmes :
1. Le package `agent/writer/` nommé d'après le rôle "écrivain", alors que tous ces agents opèrent
   sur des **séquences** — plus cohérent avec le vocabulaire métier.
2. Des sous-packages répétaient inutilement le nom du package parent
   (ex: `plan/plannarrativecritic`, `chapter/textdreamcritic`).

## Ce qui a été touché

### Renommage package principal
- `agent/writer/` → `agent/sequence/`
- `agent/sequence/sequencewriter/` → `agent/sequence/writer/` (préfixe `sequence` redondant dans le sous-package)

### Nettoyage sous-packages `plan/`
- `plannarrativecritic/` → `narrativecritic/`
- `plancoherencecritic/` → `coherencecritic/`
- `goalplanchecker/`     → `goalchecker/`

### Nettoyage sous-packages `chapter/`
- `textnarrativecritic/` → `narrativecritic/`
- `textcoherencecritic/` → `coherencecritic/`
- `textdreamcritic/`     → `dreamcritic/`
- `textwhatifcritic/`    → `whatifcritic/`
- `goaltextcritic/`      → `goalcritic/`

Les noms de **classes** sont inchangés (PlanNarrativeCritic, TextNarrativeCritic…) :
le préfixe `Plan`/`Text` dans le nom de classe a de la valeur sémantique pour distinguer
plan vs chapitre. Seuls les dossiers/packages ont été épurés.

### Fichiers mis à jour
82 fichiers Java mis à jour (déclarations de package + imports) :
agents, Steps orchestrateur, Workflows, RedacteurModule.

## Résultat

Compilation `mvn compile` : OK (0 erreur).
Structure finale :
- `agent/sequence/` — agents de séquence (writer, correcteurs, critiques)
- `agent/plan/narrativecritic/`, `coherencecritic/`, `goalchecker/`, `chapterplanner/`
- `agent/chapter/narrativecritic/`, `coherencecritic/`, `dreamcritic/`, `whatifcritic/`, `goalcritic/`
- `agent/global/` — StoryCompressor
- `agent/temp/` — agents désactivés en attente de réactivation
- `agent/commun/` — utilitaires partagés
