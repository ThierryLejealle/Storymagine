# 2026-07-08 00h20 00s — Critics livre : fidélité à la consigne + StoryFidelityCritic

## Evolution demandée

Après un test grandeur nature de `StoryNarrativeCritic`/`StoryCausalCritic` (ex-`NarrativeArcAnalyzer`/
`CausalAnalyzer`, voir `evols/2026-07-07-2149-story-plan-workflow.md`) en jouant moi-même le rôle
du LLM sur un vrai scénario (`scenarios/1998`, une histoire autobiographique), il est apparu que
les deux prompts critiquaient systématiquement des choix narratifs explicitement demandés par
l'auteur (disparition de personnages, événement soudain) faute de recevoir la consigne de
l'auteur en entrée — un problème de fond, pas un détail de prompt.

Décision de l'utilisateur : créer un **troisième agent** dédié à la fidélité stricte
plan ↔ consigne + objectif du livre, et reprompter les deux agents existants pour qu'ils ne
critiquent plus que ce que le plan **ajoute** au-delà de la consigne (jamais la consigne
elle-même). Les trois reçoivent désormais la consigne de chaque chapitre **avant** son plan
généré dans le prompt.

## Ce qui a été touché

**Nouvel agent** `StoryFidelityCritic` (`agent/storyplan/storyfidelitycritic/`) : vérifie que
chaque plan de chapitre respecte fidèlement sa consigne (description + objectif) — omission,
inversion de fait, ou événement opposé à celui demandé. Équivalent livre de
`PlanFidelityCritic` (séquence), mais compare plan ↔ consigne au lieu de texte ↔ beats.

**Renommage + reprompt complet** :
- `NarrativeArcAnalyzer` → `StoryNarrativeCritic` (`agent/storyplan/storynarrativecritic/`)
- `CausalAnalyzer` → `StoryCausalCritic` (`agent/storyplan/storycausalcritic/`)
- Ancien format `PROBLEME:`/`SCORE:` (score demandé au LLM) → format à 3 paliers
  `AMELIORATION:`/`DEFAUT_SIGNIFICATIF:`/`DEFAUT_MAJEUR:` avec `CriticOutputParser`
  (score calculé en Java), identique à `PlanNarrativeCritic`/`PlanCoherenceCritic`.
- Prompt construit via `PromptBuilder` (au lieu d'une concaténation manuelle), consigne
  toujours donnée avant le plan.
- Instruction resserrée : ne critiquer que les arcs/faits **ajoutés par le plan au-delà de la
  consigne**, jamais la consigne elle-même.
- `NarrativeArcAnalyzerOutput`/`CausalAnalyzerOutput` : champ `findings`(`int`) →
  `problems`(`double`), conforme à `PlanNarrativeCriticOutput`.
- Fichiers `.md` réécrits avec la nouvelle portée et l'historique de la migration.

**Orchestration** :
- `StoryNarrativeCriticStep`/`StoryCausalCriticStep` (renommés) + nouveau
  `StoryFidelityCriticStep` : prennent désormais `(bookGoal, chaptersBlock)` au lieu de
  `plansText`.
- `StoryPlanWorkflow.java` : construit `chaptersBlock` (par chapitre : titre, consigne, objectif,
  plan généré, dans cet ordre) via `Chapter.description()`/`Chapter.goal()` (déjà disponibles,
  utilisés par `ChapterPlannerStep`) ; appelle les 3 critics ; moyenne/note éliminatoire calculées
  sur 3 scores au lieu de 2.
- `RedacteurModule.java` : câblage des 3 agents/steps.

**Tests** (`StoryPlanWorkflowTest.java`) :
- Constat en écrivant les tests : avec les seuils très permissifs déjà fixés
  (`bookAverageThreshold=1.0`/`bookEliminationThreshold=0.0`, voir
  `evols/2026-07-07-2241-seuils-permissifs-plan-livre.md`), le rejeu est **structurellement
  inatteignable** avec le format à 3 paliers — `CriticOutputParser.calculateScore()` ne descend
  jamais sous 1.0 (plancher volontaire du parser), donc la moyenne ne peut jamais être strictement
  inférieure au seuil. L'ancien test "échec puis rejeu" (basé sur `ProblemScoreParser`, qui pouvait
  descendre à 0) n'a plus de sens avec le nouveau format.
- Remplacé par un test qui prouve directement cette propriété : même une réponse "pire cas"
  (10 `DEFAUT_MAJEUR` sur les 3 critics) ne déclenche aucun retry — valide que les seuils
  fonctionnent comme voulu.
- Gardés : critique qui passe du premier coup (chaque chapitre planifié une fois, les 3 critics
  loggés) ; niveau `SIMPLE` qui saute la critique livre entièrement.

## Résultat

`mvn -pl redacteur -am clean test` : succès, 19 tests, aucune régression. Aucune fausse alerte
possible sur les choix narratifs de l'auteur — validé par relecture manuelle du scénario 1998
avant implémentation (le seul vrai bug détecté dans ce run — une séquence "arrivée" générée à la
place du "départ" demandé par la consigne — est exactement le type de défaut que
`StoryFidelityCritic` est conçu pour attraper).
