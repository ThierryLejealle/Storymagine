# 2026-07-11 (nuit) — Bug majeur CriticOutputParser + tests manquants comblés

## 1. Demande

Premier vrai run avec les 5 nouveaux critics de plan : le log montrait 5/5 scores à 10.00 "OK"
alors que plusieurs critics avaient de vraies remarques (ex. `PlanFactsCritic` avait un vrai
`AMELIORATION`). L'utilisateur a signalé "UN GROS PROBLEME" et, à raison, a pointé l'absence de
tout test unitaire sur le calcul de note et les conditions de retry — un test avec de "fausses
réponses d'agents" aurait dû empêcher ça.

## 2. Diagnostic

`CriticOutputParser` (partagé par ~15 critics du projet) attendait le contenu **sur la même
ligne que le tag**, répété à chaque problème (`DEFAUT_SIGNIFICATIF: contenu`, une ligne par
défaut). Les 5 nouveaux prompts (conçus avec Fable) utilisaient un format différent : un header
(`AMELIORATION:` seul sur sa ligne) suivi d'une liste à puces `- contenu` en dessous. Un tag seul
sur sa ligne a un contenu vide après le `:` → traité comme `[RIEN]` → toute la section ignorée,
score forcé à 10.0 quoi que le LLM ait vraiment trouvé.

Décision (proposée par l'utilisateur, validée) : plutôt que de réaligner les 5 prompts sur
l'ancien format (moins lisible), rendre `CriticOutputParser` tolérant aux deux conventions.

## 3. Ce qui a été touché

### `agent/commun/CriticOutputParser.java` — réécrit
`parseProblems`/`calculateScore` suivent maintenant un état "tag en attente de puces" :
- Un tag avec contenu inline (même `[RIEN]`) est considéré fermé immédiatement — aucune
  continuation attendue.
- Un tag seul sur sa ligne (contenu vide) passe en attente : les lignes suivantes (avec ou sans
  puce `-`/`*`/`•`) sont comptées comme des problèmes de ce tag, JUSQU'À ce qu'une ligne
  sentinelle (`[RIEN]`, `AUCUN`, etc.) ou un nouveau tag survienne — l'un ou l'autre referme
  l'attente.

**Deux régressions trouvées et corrigées en cours de route** (démontrant très concrètement
pourquoi l'utilisateur avait raison de réclamer des tests) :
1. Premier essai : un tag "fermé" par du contenu inline (y compris `[RIEN]`) ne réinitialisait
   pas l'état d'attente → une ligne parasite en fin de réponse (ex. `SCORE: 10`, présent dans le
   mock `UNIVERSAL_PASS` de test) se faisait compter comme un défaut du dernier tag vu.
2. Deuxième essai : une fois le tag passé en "attente de puces", une ligne sentinelle de
   continuation (`[RIEN]` seul sous un header vide) ne refermait pas non plus l'attente → même
   symptôme, un cran plus profond.
Les deux corrigés en ajoutant la réinitialisation explicite de l'état d'attente à chaque point de
fermeture légitime (contenu inline présent, OU continuation sentinelle).

### Prompts des 5 critics — reprompt inchangé (format Fable conservé)
`PlanGoalCritic.java`/`PlanFactsCritic.java` avaient été temporairement réalignés sur l'ancien
format répété-par-ligne, puis **remis** au format header+puces d'origine une fois le parseur
rendu tolérant — décision explicite de l'utilisateur de garder le format Fable, plus lisible.

### Nouveaux tests

**`agent/commun/CriticOutputParserTest.java`** (13 tests) — couverture directe du parseur :
format répété-par-ligne, format header+puces, régression construite sur la réponse réelle du bug
(`PlanFactsCritic`, run 2026-07-11 19h26), non-fuite d'une ligne `SCORE:` traînante (les deux
scénarios trouvés en cours de fix), variantes de sentinelle (`AUCUN`/`AUCUNE`/`NONE`/`NEANT`),
formule de score (dominance DEFAUT_MAJEUR, etc.), réponse `null`.

**`orchestrator/plan/ChapterPlanWorkflowTest.java`** (3 tests) — mécanique de retry/seuil du pool
à 5 critics, avec de fausses réponses d'agents via `MockModelCallPort` (aucun appel Ollama réel) :
- Les 5 critics passent proprement → 1 seule tentative par chapitre, pas de retry.
- Un critic (`PlanCoherenceCritic`) toujours sous le seuil éliminatoire → chaque chapitre épuise
  ses 2 tentatives (SIMPLE : 1 retry autorisé).
- Un critic (`PlanDramaCritic`) sous le seuil éliminatoire alors que la moyenne des 5 passe
  largement (8.5 ≥ 7.0) → la règle éliminatoire force quand même le retry.

Deux pièges rencontrés en écrivant ces tests (documentés dans les commentaires du fichier) :
`countPhase("PLAN")` matche aussi "PLAN LIVRE"/"PLAN chapitre N/M"/le header top-level de
`StoryOrchestrator" — remplacé par un comptage exact ; et `hasScore("PASS")` remonte aussi les
critiques de la phase WRITE (qui passent proprement dans le mock) — remplacé par un comptage de
"RETRY" (sans ambiguïté ici, la phase WRITE ne retry jamais dans ces scénarios).

## 4. Résultat

`mvn compile` : OK. `mvn test` (tous modules) : **17 (commun) + 5 (testllm) + 35 (redacteur,
dont 16 nouveaux tests) — 0 échec**.
