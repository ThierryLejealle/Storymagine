# 2026-06-27 14h41 - Package chapter + log évaluation chapitre

## Évolution demandée
1. Ajouter une ligne vide + header avant l'évaluation chapitre dans les logs (séparation visuelle après la dernière séquence)
2. Déplacer les agents d'évaluation chapitre dans un package `chapter` dédié (séparation logique avec les agents `writer`)

## Ce qui a été touché

### Nouveaux packages créés
- `agent/chapter/textnarrativecritic/` — TextNarrativeCritic + Input + Output + .md
- `agent/chapter/textcoherencecritic/` — TextCoherenceCritic + Input + Output + .md
- `agent/chapter/textdreamcritic/` — TextDreamCritic + Input + Output + .md
- `agent/chapter/textwhatifcritic/` — TextWhatIfCritic + Input + Output + .md
- `agent/chapter/goaltextcritic/` — GoalTextCritic + Input + Output
- `orchestrator/chapter/` — TextNarrativeCriticStep, TextCoherenceCriticStep, TextDreamCriticStep, TextWhatIfCriticStep, GoalTextCriticStep

### Fichiers supprimés
- `agent/writer/text*critic/` — 5 packages supprimés (15 fichiers .java + 4 fichiers .md)
- `orchestrator/write/Text*CriticStep.java` + `GoalTextCriticStep.java` — 5 fichiers supprimés

### Fichiers modifiés
- `orchestrator/write/WriteWorkflow.java` — imports mis à jour + `log.phaseHeader("EVALUER CHAPITRE", chapter.title())` ajouté avant `runChapterCritique`
- `RedacteurModule.java` — imports mis à jour (agents chapter + steps chapter)

## Effet sur les logs
- Avant : `writer/ChapterNarrativeCritic` dans les lignes `[LLM #N]`
- Après : `chapter/ChapterNarrativeCritic`
- Nouvelle ligne séparatrice : `[HH:MM:SS] [EVALUER CHAPITRE] <titre>` (avec ligne vide avant)

## Résultat
Compilation : BUILD SUCCESS (180 sources, 0 erreur)
