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

## Story Plan Workflow — Schéma

```
╔══════════════════════════════════════════════════════════════════╗
║   WORKFLOW PLAN LIVRE (StoryPlanWorkflow) — AVANT TOUTE ÉCRITURE  ║
╚══════════════════════════════════════════════════════════════════╝

         ┌──────────────────────────────────────────────────────┐
         │   Pour CHAQUE chapitre du scénario, dans l'ordre :   │
         │     ChapterPlanWorkflow.run(...)  (inchangé,          │
         │     voir schéma "un chapitre" plus bas)              │
         └──────────────────────┬───────────────────────────────┘
                                │
                    runsBookCritics (QualityLevel) ?
                    NON → STOP (chapitres planifiés, pas de critique livre)
                                │ OUI
                                ▼
         ┌──────────────────────────────────────────────────────┐
         │   CRITIQUE LIVRE — pour chaque chapitre : consigne    │
         │   de l'auteur (description+objectif) PUIS plan        │
         │   généré, lancés ENSEMBLE :                           │
         │   • StoryFidelityCritic    → le plan respecte-t-il   │
         │     fidèlement sa consigne ? (jamais la consigne      │
         │     elle-même)                                        │
         │   • StoryNarrativeCritic   → arcs de personnages      │
         │     AJOUTÉS par le plan au-delà de la consigne        │
         │   • StoryCausalCritic      → causalité factuelle      │
         │     AJOUTÉE par le plan au-delà de la consigne        │
         │                   ↓                                   │
         │         SCORE MOYEN LIVRE                             │
         │                   ↓                                   │
         │   avg ≥ bookAverageThreshold  ET  aucun critique       │
         │   < bookEliminationThreshold (QualityLevel) ?         │
         │     OUI ou dernière tentative → garder meilleur jeu   │
         │            de plans (best score), terminer            │
         │     NON  → feedback fusionné injecté dans TOUS les    │
         │            WrittenChapter.coherence(), puis REBOOT :  │
         │            tous les chapitres sont replanifiés        │
         │            (jamais une sélection ciblée)              │
         └──────────────────────────────────────────────────────┘
```

Le rejeu livre replanifie systématiquement l'intégralité des chapitres (même logique que le
reboot chapitre du Write : jamais de sélection ciblée). Chaque chapitre replanifié entre dans
`ChapterPlanWorkflow` avec un plan déjà existant (`WrittenChapter.plan() != null`), donc
`ChapterPlannerStep` le traite automatiquement comme une réécriture (`isRewrite`), guidée par le
feedback livre déposé dans `coherence()`.

La consigne de l'auteur (description + objectif du chapitre) est toujours présentée EN PREMIER
dans le prompt des 3 critics, avant le plan généré — elle fait foi et n'est jamais elle-même un
défaut. `StoryNarrativeCritic`/`StoryCausalCritic` ne jugent que ce que le plan ajoute au-delà de
cette consigne ; `StoryFidelityCritic` vérifie que le plan ne la trahit pas (omission, inversion,
événement opposé à celui demandé). Cette séparation évite qu'un critic ne signale comme "défaut"
un choix narratif voulu par l'auteur (départ d'un personnage, événement soudain, etc.).

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
          │  1. PhrasingCorrector (retry ONCE) │
          │  2. DeusInMachinaCorrector         │
          │  3. NaturalityCorrector            │
          │  4. StyleCorrector                 │
          │  5. GrammarCorrector               │
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
         │   score ≥ 7  ET  aucun critique < eliminationThreshold│
         │   (QualityLevel)  ?  ──── OUI ─────────────── STOP   │
         │        │                                              │
         │       NON + budget retry > 0                         │
         │        │                                              │
         │        ▼                                              │
         │   Writer (TOUS les problèmes collectés)              │
         │        ↓                                              │
         │   1. PhrasingCorrector                               │
         │   2. DeusInMachinaCorrector                          │
         │   3. NaturalityCorrector                             │
         │   4. StyleCorrector                                  │
         │   5. GrammarCorrector                                │
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
         │   • ChapterNarrativeCritic   [IMPERATIVE]               │
         │   • ChapterCoherenceCritic   [IMPERATIVE, WHAT_IF]      │
         │   • ChapterDreamCritic       [DREAM]                    │
         │   • ChapterWhatIfCritic      [WHAT_IF]                  │
         │   • ChapterGoalCritic        [tous types]               │
         │                   ↓                                   │
         │         SCORE MOYEN GLOBAL                           │
         │                   ↓                                   │
         │   (score > chapitreThreshold  ET  aucun critique     │
         │   < eliminationThreshold) OU dernière tentative ?    │
         │     OUI → garder best, terminer                      │
         │     NON → REBOOT toutes séquences                    │
         │            (retour Writer avec feedback chapitre)    │
         └──────────────────────────────────────────────────────┘
```

Note — seuils par phase : le Plan livre, le Plan chapitre et le Write (séquence + chapitre) ont
chacun leur propre paire de seuils dans `QualityLevel`, pour ne pas confondre les logiques :
- Write : `chapitreThreshold` (moyenne) / `eliminationThreshold` (éliminatoire) — s'applique
  à la fois à la critique séquence et à la critique chapitre.
- Plan chapitre : `planAverageThreshold` (moyenne) / `planEliminationThreshold` (éliminatoire).
- Plan livre : `bookAverageThreshold` (moyenne) / `bookEliminationThreshold` (éliminatoire),
  actif seulement si `runsBookCritics()` est vrai (niveau FULL uniquement à ce jour).

La note éliminatoire (`eliminationThreshold`, `planEliminationThreshold` ou
`bookEliminationThreshold` selon la phase) force une relance si au moins un critique individuel
tombe sous ce seuil, même si la moyenne globale passe le seuil de moyenne habituel. Loggé
séparément (`log.warn`) quand c'est la seule cause du rejet.

Valeurs actuelles par niveau (moyenne / éliminatoire) :

| Niveau     | Write     | Plan chapitre | Plan livre        |
|------------|-----------|----------------|--------------------|
| PLAN_ONLY  | 7.0 / 3.0 | 7.0 / 3.0      | désactivé          |
| BROUILLON  | 8.0 / 1.0 | 8.0 / 1.0      | désactivé          |
| SIMPLE     | 7.0 / 3.0 | 7.0 / 3.0      | désactivé          |
| FULL       | 7.0 / 5.0 | 8.0 / 5.5      | 1.0 / 0.0 (2 retry)|

Plan livre volontairement très permissif (1.0 / 0.0) tant que StoryFidelityCritic,
StoryNarrativeCritic et StoryCausalCritic ne sont pas éprouvés en usage réel — objectif : qu'ils
ne bloquent quasiment jamais une génération pour l'instant. À resserrer une fois leur
comportement observé. Note : avec ces seuils, le rejeu est aujourd'hui structurellement
inatteignable — `CriticOutputParser.calculateScore()` ne descend jamais sous 1.0, donc
`avg ≥ 1.0` est toujours vrai. Volontaire, pas un bug (voir `StoryPlanWorkflowTest`).

TODO: filtrer les critiques de Phase 3 selon le type de séquence (DREAM/WHAT_IF),
      comme c'est fait au niveau chapitre.
