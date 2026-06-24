# 2026-06-23 20h00 — Interface Agent commune + nom centralisé

## Évolution demandée
Chaque agent portait son nom en dur dans le call `LlmCallContext.of("NomAgent")`.
Introduire une interface `Agent` commune à tous les agents afin que :
- le type "agent" soit explicite dans le domaine
- le nom de l'agent soit déclaré une seule fois (constante `AGENT_NAME`) et réutilisé
  partout via `agentName()`, sans risque de désynchronisation

## Ce qui a été touché

### Nouveau : `redacteur/coeur/domaine/agent/Agent.java`
Interface marqueur avec une unique méthode `agentName() → String`.

### Modifié : 24 agents
Pour chacun :
- `public class X implements Agent`
- Constante `private static final String AGENT_NAME = "X";`
- `@Override public String agentName() { return AGENT_NAME; }`
- `LlmCallContext.of("X")` → `LlmCallContext.of(agentName())`

Agents concernés (plan) : ChapterPlanner, SequencePlanner, PlanNarrativeCritic,
PlanCoherenceCritic, GoalPlanChecker.
Agents concernés (writer) : Writer, DeusInMachinaChecker, FocusActionFilter,
GoalTextChecker, Proofreader, RepetitionFilter, RepetitionTracker,
SequenceChecker, SequenceStyleChecker, StateExtractor, TextCoherenceCritic,
TextDreamCritic, TextNarrativeCritic, TextWhatIfCritic.
Agents concernés (global) : StoryCompressor, ChapterStyleChecker, CharacterChecker,
NarrativeArcAnalyzer, CausalAnalyzer.

## Résultat
- Compilation OK, BUILD SUCCESS sur les 4 modules.
- Le nom de chaque agent est déclaré une seule fois dans sa classe.
- L'interface `Agent` permet de typer n'importe quelle collection ou paramètre
  acceptant un agent, sans avoir à inspecter la classe concrète.
