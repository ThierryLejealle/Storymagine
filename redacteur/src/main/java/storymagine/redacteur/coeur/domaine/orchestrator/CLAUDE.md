# Orchestrator Rules — Storymagine

## Workflow Documentation

Every workflow class MUST include a text diagram at the top of the class (Javadoc or
block comment) documenting the full step sequence.

The diagram must show:
- Phase names and their order
- Steps within each phase, in execution order
- Decision points (score threshold, retry conditions)
- Skip conditions per quality level (e.g. skip if BROUILLON)
- Which steps can trigger a Writer retry and under what condition

Keep the diagram in sync with the code. If a step is added, removed, or reordered,
update the diagram in the same commit.

## Write Workflow — Schéma

```
╔══════════════════════════════════════════════════════════════════╗
║              WORKFLOW WRITE — PAR SÉQUENCE                      ║
╚══════════════════════════════════════════════════════════════════╝

                    ┌──────────────┐
                    │    Writer    │ ← directive, contexte, états monde,
                    │              │   répétitions, [feedback chapitre]
                    └──────┬───────┘
                           │
                           ▼
          ┌────────────────────────────────────┐
          │   CORRECTEURS  (skip si BROUILLON) │
          │                                    │
          │  1. DeusInMachinaCorrector         │
          │  2. NaturalityCorrector            │
          │  3. StyleCorrector                 │
          │  4. ProofreaderCorrector           │
          │                                    │
          │  → patches inline (String.replace) │
          └──────────────┬─────────────────────┘
                         │
         ┌───────────────▼──────────────────────────────────────┐
         │   CRITIQUE GLOBALE  (skip si BROUILLON)              │
         │                                                       │
         │   Lancés ENSEMBLE :                                   │
         │   • DeusInMachinaCritic     → score + problèmes      │
         │   • StyleCritic             → score + problèmes      │
         │   • PlanFidelityCritic      → score + problèmes      │
         │     (si beats présents dans le plan de séquence)     │
         │   • CheckCritic             → score (échecs NON      │
         │     remontés au Writer — TODO dual representation)   │
         │     (si checks déclarés dans le scénario)            │
         │                   ↓                                   │
         │         SCORE MOYEN GLOBAL                           │
         │                   ↓                                   │
         │   score ≥ 7 ?  ──── OUI ──────────────────── STOP   │
         │        │                                              │
         │       NON + budget retry > 0                         │
         │        │                                              │
         │        ▼                                              │
         │   Writer (TOUS les problèmes collectés)              │
         │        ↓                                              │
         │   1. DeusInMachinaCorrector                          │
         │   2. NaturalityCorrector                             │
         │   3. StyleCorrector                                  │
         │   4. ProofreaderCorrector                            │
         │        ↓                                              │
         │   → passe suivante (mêmes critiques)                 │
         │                                                       │
         │   → garder meilleure version (best score moyen)      │
         └──────────────────────────┬────────────────────────────┘
                                    │
                    ┌───────────────▼──────────────┐
                    │   POST-TRAITEMENT (toujours)  │
                    │                               │
                    │   1. StateExtractor           │
                    │   2. RepetitionTracker        │
                    │   3. RepetitionFilter         │
                    └───────────────────────────────┘


╔══════════════════════════════════════════════════════════════════╗
║         NIVEAU CHAPITRE (après TOUTES les séquences)            ║
╚══════════════════════════════════════════════════════════════════╝

         ┌──────────────────────────────────────────────────────┐
         │   CRITIQUE CHAPITRE (si qualityLevel active)         │
         │                                                       │
         │   Lancés ENSEMBLE selon type de chapitre :           │
         │   • TextNarrativeCritic   [IMPERATIVE]               │
         │   • TextCoherenceCritic   [IMPERATIVE, WHAT_IF]      │
         │   • TextDreamCritic       [DREAM]                    │
         │   • TextWhatIfCritic      [WHAT_IF]                  │
         │   • GoalTextCritic        [tous types]               │
         │                   ↓                                   │
         │         SCORE MOYEN GLOBAL                           │
         │                   ↓                                   │
         │   score ≥ seuil OU dernière tentative ?              │
         │     OUI → garder best, terminer                      │
         │     NON → REBOOT toutes séquences                    │
         │            (retour Writer avec feedback chapitre)    │
         └──────────────────────────────────────────────────────┘
```

TODO: filtrer les critiques de Phase 3 selon le type de séquence (DREAM/WHAT_IF),
      comme c'est fait au niveau chapitre.
