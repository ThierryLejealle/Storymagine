# 2026-06-25 18h35 - Analyse des prompts des agents

## Évolution demandée

Analyser tous les prompts des 23 agents LLM du module `redacteur` pour identifier
les problèmes vis-à-vis des petits modèles : redites, contradictions, ambiguïtés,
manques d'exemples, incohérences entre agents frères.

## Ce qui a été touché

- Lecture et analyse de 23 agents dans `redacteur/.../domaine/agent/`
- Création de `specs/proposition-ameliorations-prompts.md`

Aucun prompt modifié — uniquement analyse et propositions.

## Résultat

15 problèmes identifiés, classés par priorité (haute / moyenne / faible).

Problèmes hauts :
- `GoalPlanChecker` : risque de fuite de raisonnement dans la sortie (procédure en 4 étapes dont 3 internes)
- `GoalPlanChecker` + `GoalTextChecker` : convention cas-vide incohérente entre agents miroirs
- 4 agents critics : "ces trois lignes" ambigu → "ces trois sections"
- `TextCoherenceCritic` : section focus dans le user non couverte par le system

Agents sans problème notable : RepetitionTracker, StateExtractor, TextDreamCritic.

Voir `specs/proposition-ameliorations-prompts.md` pour le détail agent par agent.
