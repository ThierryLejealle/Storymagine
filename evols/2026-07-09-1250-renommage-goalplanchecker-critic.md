# 2026-07-09 12h50 - Renommage GoalPlanChecker → GoalPlanCritic

## Demande

En repassant sur le master-log, l'utilisateur a repéré une incohérence de nommage entre
agents : `plan/PlanGoalChecker` porte le suffixe `Checker`, alors que `agent/CLAUDE.md`
déclare ce suffixe aboli au profit de `Critic`. Confirmation : l'agent produit une liste
`PROBLEME:` notée ensuite en Java par `ProblemScoreParser` — exactement le patron
"Critic — Score Rule" documenté dans `agent/CLAUDE.md`. Son jumeau côté chapitre
(`GoalTextChecker`) avait déjà été migré vers `GoalTextCritic` ; seul le niveau plan avait
été oublié. La doc `agent/CLAUDE.md` ligne 52 citait d'ailleurs encore `GoalTextChecker`
comme référence, alors que cette classe n'existe plus sous ce nom.

Portée validée avec l'utilisateur : renommer uniquement cet agent (classe, `AGENT_NAME`,
Input/Output, Step, références directes) — le dossier de package `goalchecker/` n'est
volontairement pas renommé, pour limiter le diff.

## Ce qui a été touché

- `agent/plan/goalchecker/GoalPlanChecker.java` → `GoalPlanCritic.java`
  (classe + `AGENT_NAME`: `"PlanGoalChecker"` → `"PlanGoalCritic"`)
- `agent/plan/goalchecker/GoalPlanCheckerInput.java` → `GoalPlanCriticInput.java`
- `agent/plan/goalchecker/GoalPlanCheckerOutput.java` → `GoalPlanCriticOutput.java`
- `agent/plan/goalchecker/GoalPlanChecker.md` → `GoalPlanCritic.md` (contenu adapté,
  correction au passage d'une référence erronée : `GoalPlanChecker.scoreFromProblemCount`
  → `ProblemScoreParser.scoreFromProblemCount`, qui est la vraie méthode appelée)
- `orchestrator/plan/GoalPlanCheckerStep.java` → `GoalPlanCriticStep.java`
- `RedacteurModule.java` : imports + variables locales (`goalPlanChecker` →
  `goalPlanCritic`, `goalPlanCheckerStep` → `goalPlanCriticStep`)
- `orchestrator/plan/ChapterPlanWorkflow.java` : import, champ, paramètre constructeur,
  label de log (`log.critic("PlanGoalChecker", …)` → `log.critic("PlanGoalCritic", …)`),
  message de warn (`"GoalPlanChecker score 10…"` → `"GoalPlanCritic score 10…"`)
- `redacteur/src/test/java/.../WorkflowLogTest.java` : assertion mise à jour
  (`hasCritic("PlanGoalChecker")` → `hasCritic("PlanGoalCritic")`)
- `agent/CLAUDE.md` ligne 52 : référence corrigée (`GoalPlanChecker`, `GoalTextChecker`
  → `GoalPlanCritic`, `GoalTextCritic`)

Non touché (hors scope demandé) : dossier de package `goalchecker/`, fiches `/evols`
existantes (historique), `OUBLI.MD`, `specs/*.md`.

## Résultat

Le nom de log affiché pour cet agent passe de `PlanGoalChecker` à `PlanGoalCritic` —
cohérent avec ses deux voisins du package `plan` (`PlanNarrativeCritic`,
`PlanCoherenceCritic`) et avec son jumeau chapitre (`ChapterGoalCritic`). Plus aucune
occurrence de `GoalPlanChecker`/`PlanGoalChecker` dans le code source.

Compilation vérifiée (`mvn -pl redacteur -am compile` et `test-compile`) : OK.
