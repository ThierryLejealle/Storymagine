# 2026-06-26 16h30 — Refactoring workflow WRITE : correcteurs + critique globale

## Évolution demandée

Refonte complète du workflow de rédaction par séquence :
- L'ancienne architecture avait 3 boucles de retry indépendantes (DeusInMachina, StyleChecker, SequenceChecker),
  chacune relançant WriterStep seule, pouvant défaire le travail des autres.
- Objectif : correcteurs directs (sans retry Writer), puis critique globale avec vote collectif.

## Ce qui a été touché

### Renames d'agents (package + classe + step)

| Ancien | Nouveau | Changement |
|--------|---------|------------|
| `DeusInMachinaChecker` | `DeusInMachinaCritic` | + `score()` calculé (0 fuite=10, 1=7, 2=4, 3+=1) |
| `SequenceStyleChecker` | `StyleCritic` | Score LLM supprimé → algo depuis `problems.size()` |
| `SequenceChecker` | `ElementCritic` | Score algo inchangé |
| `GoalTextChecker` | `GoalTextCritic` | Rename seul |
| `NaturalityFilter` | `NaturalityCorrector` | Déplacé au niveau séquence (était chapitre) |
| `Proofreader` | `ProofreaderCorrector` | Rename seul |

### Nouvel agent

- `DeusInMachinaCorrector` — même 5 types de fuites que `DeusInMachinaCritic`, mais retourne des
  paires FAUX/JUSTE (appliquées via String.replace) plutôt qu'une liste de détections.

### WriteWorkflow — nouvelle architecture par séquence

```
Phase 1 — WRITE
  Writer

Phase 2 — CORRECTORS  (skip si BROUILLON)
  DeusInMachinaCorrector → NaturalityCorrector → ProofreaderCorrector
  Patches inline, jamais de retry Writer.

Phase 3 — CRITIQUE GLOBALE  (skip si BROUILLON)
  DeusInMachinaCritic + StyleCritic + ElementCritic (si checks présents)
  → score moyen global
  → si score < 7 ET budget retry > 0 :
       Writer (TOUS les problèmes) → Phase 2 → Critics à nouveau
  → garder meilleure version (best score)

Phase 4 — POST-TRAITEMENT  (toujours)
  StateExtractor → RepetitionTracker → RepetitionFilter
```

### Fichiers de règles créés

- `agent/CLAUDE.md` — convention de nommage (Critic, Corrector, Writer, Planner, Extractor, Tracker, Filter)
  + règle Critic (score algo) + règle Corrector (paires FAUX/JUSTE)
- `orchestrator/CLAUDE.md` — règle de doc des workflows + schéma WriteWorkflow de référence

### Fichiers modifiés

- `WriteWorkflow.java` — refactoring complet
- `RedacteurModule.java` — imports + instanciations + nouveau constructeur WriteWorkflow
- 18 fichiers Java nettoyés du BOM UTF-8 (introduit par le tool Write du subagent)

## Résultat

`mvn compile` : OK (0 erreur).
