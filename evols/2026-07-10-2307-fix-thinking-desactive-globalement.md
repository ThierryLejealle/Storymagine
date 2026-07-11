# 2026-07-10 23h07 — Fix majeur : la réflexion (thinking) était désactivée sur tout le pipeline

## 1. Demande

En travaillant sur le Writer, l'utilisateur a remarqué que les générations récentes tournaient
plus vite mais semblaient dégradées ("tout a l'air dégradé... mais ça marche plus vite"), et a
suspecté qu'on ait "cassé le thinking" sur gemma4-26B. Investigation en remontant tout le code :

- `RedacteurModule.java` construit tous les adaptateurs via `ollamaConfig.adapter(modelName, log)`,
  qui appelle `adapter(model, false, log)` — **`think=false` en dur, depuis le tout premier commit**
  (`git log -p` vérifié). Ce n'est PAS une régression du jour.
- Mais `OllamaAdapter.buildOllamaRequest()` calcule
  `req.think = resolveThink(thinkOverride != null ? thinkOverride : (think ? TRUE : FALSE))`.
  Avant le 09/07, un bug faisait `req.think = think ? TRUE : null` — le cas "désactivé" envoyait
  un champ **absent**, et un champ absent fait qu'Ollama applique son propre défaut : "réflexion
  activée" pour un modèle qui la supporte (gemma4 compris). **Donc avant le 09/07, gemma4
  réfléchissait par défaut, par accident.**
- La correction du 09/07 (evols/2026-07-09-2319, §8) a corrigé ce bug latent en envoyant
  désormais `false` explicitement quand aucun agent ne demande autre chose. Correction bien
  intentionnée (permettre à un agent de vraiment désactiver la réflexion), mais comme
  `RedacteurModule` construit tout avec `think=false` et qu'aucun agent n'appelait
  `withThink(TRUE)`, cette correction a **silencieusement coupé la réflexion sur tout le
  pipeline** — alors qu'elle tournait par accident auparavant. Régression réelle du 09/07,
  découverte seulement aujourd'hui.

Décision de l'utilisateur : la réflexion doit être **activée par défaut pour tous les agents**,
au niveau de chaque agent (pas au niveau de l'adaptateur Ollama), via une méthode explicite sur
l'interface `Agent`, avec un défaut à `true`, à surcharger uniquement pour les agents où on a
déjà délibérément choisi `false` (les deux correcteurs mécaniques).

## 2. Ce qui a été touché

### Nouveau concept métier : `Agent.thinks()`
`Agent.java` — nouvelle méthode par défaut :
```java
default boolean thinks() { return true; }
```
Chaque appel LLM doit passer par cette méthode (`LlmCallContext.of(...).withThink(thinks())`),
jamais coder un booléen en dur à l'appel.

### 28 agents mis à jour
Tous les appels `LlmCallContext.of(agentName(), agentLabel())` (26 agents) sont passés à
`.withThink(thinks())`. Les deux qui avaient déjà `.withThink(Boolean.FALSE)` en dur
(`GrammarCorrector`, `PhrasingCorrector`) sont passés au même appel `.withThink(thinks())`, avec
`thinks()` surchargé à `false` dans ces deux classes (tâches mécaniques/fermées, cf. diagnostic
du 09/07 §6.4 — réflexion n'apporte rien).

Liste complète des 28 : `Writer`, `PlanGoalCritic`, `PlanCoherenceCritic`, `PlanNarrativeCritic`,
`ChapterPlanner`, `DeusInMachinaCritic`, `DeusInMachinaCorrector`, `GrammarCorrector`,
`PhrasingCorrector`, `StyleCorrector`, `NaturalityCorrector`, `ChapterGoalCritic`,
`ChapterWhatIfCritic`, `ChapterDreamCritic`, `ChapterNarrativeCritic`, `ChapterCoherenceCritic`,
`ChapterStyleCritic` (temp), `CharacterCritic` (temp), `StateExtractor`, `StoryFidelityCritic`,
`StoryCausalCritic`, `StoryNarrativeCritic`, `RepetitionTracker`, `PlanFidelityCritic`,
`CheckCritic`, `StoryCompressor`, `RepetitionFilter`, `FocusActionFilter` (temp).

`RedacteurModule.java` reste inchangé (`think=false` au niveau adaptateur, sert seulement de
filet — chaque agent porte désormais sa propre préférence explicite via `thinks()`).

### Traçabilité ajoutée (LogPort + 3 implémentations)
Absence totale de traçabilité constatée avant ce fix : ni les fichiers `llm_calls/*.md` ni la
console n'indiquaient si un appel avait effectivement demandé la réflexion. Ajouté :

- `LogPort.llmCall(...)` et `llmCallOpen(...)` : nouveau paramètre `Boolean think`.
- `ConsoleLogAdapter`/`FileLogAdapter` : affichent `[think]` (console/master-log) quand
  `think == true` ; `FileLogAdapter` ajoute une ligne `- Think : oui/non/n/a` dans l'en-tête de
  chaque fichier `llm_calls/*.md`.
- `TeeLogAdapter` : relaie le nouveau paramètre.
- `OllamaAdapter` : calcule la valeur de `think` réellement résolue (après `resolveThink()`, donc
  après neutralisation éventuelle pour un modèle qui ne supporte pas l'activation explicite) et la
  transmet à la fois à `llmCallOpen` (trace fichier) et à `logLlmCall`/`llmCall` (résumé
  console/master-log) — en mode sync ET en mode stream (`executeStreaming` reçoit désormais
  `think` en paramètre).
- `CapturingLogPort` (test double `redacteur`) mis à jour en conséquence.

## 3. Résultat

`mvn -q test-compile` propre sur tous les modules. `mvn test` (redacteur + commun) : 19 tests,
1 seul échec — **`WorkflowLogTest.planOnly_doesNotLogWriteOrEval`, le même échec pré-existant déjà
documenté dans evols/2026-07-09-2319 (§13.5), sans rapport avec ce travail** (chantier PLAN_ONLY
en cours ailleurs, ni `StoryOrchestrator.java` ni `QualityLevel.java` touchés ici).

Coût accepté en connaissance de cause : le diagnostic du 09/07 mesure un facteur ×7 à ×17 en
latence/tokens avec la réflexion active. Appliqué à 26 agents (dont plusieurs tournent "ensemble"
par lot), chaque génération de livre sera sensiblement plus lente qu'avant ce fix — mais on
revient au comportement de référence (avant le bug du 09/07), pas à une dégradation nouvelle.

## 4. Reporté à demain (explicitement, décision de l'utilisateur)

- Les points A-K restants de la revue du Writer (positionnement des interdictions de répétition,
  "aucune décision narrative" vs enrichissement autorisé, triple synonyme anticipation, bloc final
  surchargé, branche "nouvelle séquence" invérifiable, doublon système/user sur le raccord, branche
  stitch, consigne de style couplée à un seuil numérique, liste de priorité incomplète, conventions
  de formatage, "ces directives" ambigu en fin de prompt).
- Le chantier Deus/Writer sur la fuite de raisonnement LLM (3 deltas déjà prêts et validés en
  discussion — voir mémoire `project_deus_writer_leak_fix_queued`), à traiter avec les 2 agents
  correcteurs mentionnés par l'utilisateur.
