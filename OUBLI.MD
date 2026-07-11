# OUBLI.MD — Récapitulatif de session interrompue

Document de reprise après un incident (BOM en masse ayant cassé la compilation,
forçant un `git revert`/reset et une perte de session). Sert à ne pas repartir de zéro.

État au moment de la rédaction : 2026-07-06. Dernier commit : `448b6e2 probleme BOM`.

**MISE À JOUR 2026-07-06 23h52** : le chantier check/contrainte décrit en section 3 est
terminé — voir `evols/2026-07-06-2352-fusion-check-contrainte-requirement.md` pour le détail
complet (concept `Requirement`, syntaxe `contrainte | check`, migration des 4 scénarios,
compilation + tests OK). Ce fichier reste comme trace de l'incident et du raisonnement qui a
mené à la décision — la section 3 ci-dessous est désormais un historique, pas un TODO.

---

## 0. Incident BOM — pourquoi on a dû revenir en arrière

Une session de modification de prompts a introduit des BOM (`EF BB BF`) dans des fichiers
`.java`, ce qui casse `javac` (`illegal character`). Plus rien ne compilait, la session a été
perdue (tokens épuisés en plein milieu). L'utilisateur a fait un `git revert`/reset pour
retrouver un état sain.

**Règle absolue retenue (en mémoire persistante) : JAMAIS DE BOM.**
Vérification systématique après chaque `Write`/`Edit` sur un fichier texte du projet, pas
seulement en bulk. Voir mémoire `feedback_bom_write_tool`.

L'état actuel de l'arbre de travail (6 fichiers modifiés non commités, cf. section 1) a été
revérifié : aucun BOM, tout est propre.

---

## 1. État actuel de l'arbre de travail (non commité, non fiché)

Aucune fiche d'évolution dans `/evols` ne documente ce lot (dernière fiche :
`2026-06-27-2030-refactor-agent-packages-writer-to-sequence.md`). Rien n'a été committé.

### `redacteur/.../agent/plan/chapterplanner/ChapterPlanner.java`
- Reformulation du prompt `JSON_PLANNER_SYSTEM` : les 4 règles beats sont devenues 6 règles
  (ajout d'une règle "chaque beat doit montrer un fait observable"), suppression de la mention
  "en respectant intégralement les instructions fournies" et du test caméra "sans qu'un
  narrateur ait besoin de l'expliquer".
- `INNER_STATE_NOTE` / `FOCUS_LORE_NOTE` : ajout d'une ligne vide en tête (cosmétique).
- Pas de changement structurel (Input/Step intacts).

### `redacteur/.../agent/plan/coherencecritic/PlanCoherenceCritic.java`
- SYSTEM : fusion des concepts `checks` + `contraintes` + `focus` + `lore` en un seul concept
  **"contraintes additionnelles"**.
- `call()` : `input.constraints()` et `input.focusText()` ne sont **plus lus** — mais ces
  champs existent toujours dans `PlanCoherenceCriticInput` → **champs morts**.
- 2 `//FIXME` laissés dans le code :
  - *"Il faudrait passer les checks sous forme de 'contraintes propre à la séquence' dans la
    planification passée au prompt"*
  - *"il faut définitivement clarifier la dualité check/contrainte"*
- **C'est le point de reprise demandé par l'utilisateur (section 3).**

### `redacteur/.../agent/plan/narrativecritic/PlanNarrativeCritic.java`
- Clarification de la définition AMELIORATION : *"le plan atteint déjà correctement l'objectif
  du chapitre. Signale uniquement une faiblesse réelle mais très mineure... Ne propose jamais
  une autre manière de raconter la même histoire."* + 2 nouveaux exemples contextualisés.
- Pas de changement structurel (`PlanNarrativeCriticInput` reste sain : `plan, bookGoal,
  chapterGoal`, 3 champs).

### `redacteur/.../agent/CLAUDE.md`
Deux règles ajoutées :
- **VITAL** : cohérence stricte nom-cité/nom-réel dans un prompt (si le prompt dit "Fiches de
  personnages", la section correspondante ne doit pas s'appeler "Acteurs :").
- **RAPPEL** : cibler des petits LLM → prompts précis, simples, sans boucle, sans redite.

### `claude.md` (racine)
Ajout de la règle "Toujours enregistrer les fichiers texte en UTF-8 sans BOM" (doublon avec
la même règle dans le CLAUDE.md projet — conséquence directe de l'incident BOM).

### `scenarios/1998/checks.md`
Ajout : *"ne critique pas la vraisemblance de l'histoire ou des transitions de séquences un peu
rapides : c'est une histoire vraie."*

---

## 2. La capacité mutualisée d'enrichissement de prompt (idée en suspens)

L'utilisateur se souvenait avoir voulu une **capacité mutualisée pour les agents, leur
permettant d'enrichir le prompt avec à peu près n'importe quelle partie du scénario ou de
l'histoire en cours de rédaction**, sans dupliquer la mécanique à chaque agent.

### Preuve du besoin dans le code actuel

- `ChapterPlannerInput.java` : **17 champs positionnels** (`chapterTitle, chapterDescription,
  chapterSetting, sequenceDescriptions, bookGoal, storySoFar, entityState, characters,
  constraints, focusText, loreText, coherence, forbiddenPhrases, jsonMode, isRewrite,
  previousPlan, problemsToFix`) — très au-dessus du seuil "5 attributs → chercher un concept"
  du CLAUDE.md projet.
- Dans `PlanCoherenceCritic.call()`, `PlanNarrativeCritic.call()`, `ChapterPlanner` : le même
  motif de concaténation est dupliqué partout :
  ```java
  (checks.isBlank() ? "" : "\n\n### Contraintes additionelles globales\n" + checks)
  ```
- `ScenarioFormatters.java` (28 méthodes statiques) mutualise déjà le **formatage**
  (Scenario/Chapter/Sequence → String) mais pas **l'assemblage du prompt** : chaque `XxxStep`
  pioche à la main les bonnes méthodes et les pousse dans un record `Input` à champs fixes.
- `specs/todo.md:26` : *"passer les directives de séquences... aux 3 agents critiques de plan
  (PlanNarrativeCritic, PlanCoherenceCritic, GoalPlanChecker)"* — backlog non fait, même famille
  de besoin.
- `specs/proposition-ameliorations-prompts.md` (analyse du 2026-06-25) : incohérences déjà
  répertoriées entre agents-miroirs Plan/Text (ex. focus présent dans le user de
  `TextCoherenceCritic` mais absent du system) — symptôme du manque de mutualisation.

### Piste envisagée (PAS validée, PAS codée)

Concept candidat : un objet POJO du Domaine, style `PromptSection` (label + contenu) porté par
une liste ordonnée (ex. `EnrichmentPack` / `ContexteEnrichi` — noms à discuter), rempli
librement par le `Step` à partir de `Scenario`/`Chapter`/`Sequence`/`Story`, consommé
génériquement par l'agent pour construire le bloc `user` (au lieu de champs positionnels fixes).

Questions restées ouvertes :
1. Nom du concept métier — à valider.
2. Portée : seulement les 3 agents plan (ChapterPlanner, PlanCoherenceCritic,
   PlanNarrativeCritic) ou préparer le terrain pour tous les agents (Text-level, Writer...) ?
3. Est-ce qu'on fusionne définitivement `checks` et `constraints` partout, ou on les garde
   distincts ? **→ C'est la question qu'on rouvre maintenant, voir section 3.**
4. Nettoyer les champs morts `constraints`/`focusText` de `PlanCoherenceCriticInput` — à faire
   dans tous les cas, indépendamment de la décision ci-dessus.

**Décision de l'utilisateur (2026-07-06) : on ne repart PAS directement sur la capacité
mutualisée. On repart d'abord sur les bases avec la question check/contrainte (section 3),
qui doit être tranchée en premier — la capacité mutualisée en dépend (point 3 ci-dessus).**

---

## 3. Le point de reprise : la dualité check / contrainte

### Ce que dit la spec actuelle (`specs/scenario.md`)

Deux fichiers/concepts globaux distincts, tous deux à "accumulation" (global → chapitre →
séquence) :

- **CHECKS** (`checks.md`) : *"Éléments à vérifier à chaque séquence."* Sections `## PLAN` /
  `## RÉDACTION` / `## COMMON`. Champ YAML `checks:` dans les séquences.
- **CONSTRAINTS** (`constraints.md`) : *"Contraintes à respecter pendant le plan et la
  rédaction."* Même structure (`## PLAN` / `## RÉDACTION` / `## COMMON`). Champ YAML
  `constraints:` dans les séquences.

Sur le papier la distinction entre les deux fichiers n'est pas évidente (les deux sont des
listes de tirets à portée PLAN/RÉDACTION accumulables) — d'où le FIXME laissé dans le code :
*"il faut définitivement clarifier la dualité check/contrainte"*.

### Ce qui existe en code autour de cette distinction

- `Check` / `CheckList` et `Constraint` / `ConstraintList` : deux hiérarchies de classes
  séparées dans `domaine/scenario/`.
- `ScenarioFormatters` : méthodes séparées `planChecks`/`writerChecks` (retournent
  `List<String>`) vs `planConstraints`/`writerConstraints` (retournent `String` joint par
  `\n`) — **déjà une incohérence de forme** (liste vs texte joint) entre les deux concepts.
- `ScenarioFormatters.enrichPlanJson()` injecte par séquence : `"checks"` (array JSON) et
  `"contraintes"` (string JSON) — deux clés différentes, deux formats différents.
- `PlanCoherenceCritic` (diff en cours) a déjà commencé à fusionner les deux dans son prompt
  sous un seul terme "contraintes additionnelles", **sans toucher au modèle de données**
  sous-jacent (`Check`/`Constraint` restent séparés) → source des champs morts et du FIXME.

### À trancher avant tout code (conforme à la règle "plan avant d'implémenter")

- Est-ce que `checks` et `constraints` doivent rester deux concepts métier distincts (avec une
  différence de sens claire à formuler), ou fusionner en un seul concept ?
- Si fusion : impact sur `specs/scenario.md`, les fichiers `.md` des scénarios existants
  (`checks.md` / `constraints.md` dans `scenarios/1998`, `scenarios/as-du-ciel`,
  `scenarios/modele`, etc.), les classes `Check`/`Constraint`, `CheckList`/`ConstraintList`,
  les parseurs, `ScenarioFormatters`, et tous les prompts d'agents qui les mentionnent
  (PlanCoherenceCritic, TextCoherenceCritic, ChapterPlanner, Writer...).
- Si distinction conservée : redéfinir clairement la frontière sémantique entre les deux (et
  revenir sur la fusion faite dans `PlanCoherenceCritic` pour la rendre cohérente avec cette
  frontière, au lieu de laisser les deux champs morts).

---

## 4. Backlog associé (rappel `specs/todo.md`, non traité par ce lot)

- Passer les directives de séquences aux 3 agents critiques de plan (PlanNarrativeCritic,
  PlanCoherenceCritic, GoalPlanChecker) avec label "Directives :" + note "le plan peut enrichir
  librement ces directives, ne signaler que les violations de CONTRAINTE ou LIMITE explicites".
- Nombre de lignes fixe (6) par séquence dans le plan — jugé trop limitant.
- Numérotation des séquences S1..Sn à harmoniser avec les retours d'erreur.
- Corriger Français → Anglais dans les specs et objets (règle projet : le code est en anglais).
- `NarrativeArcAnalyzer` + `CausalAnalyzer` désactivés — nécessitent une replanification
  architecturale (tous les plans de chapitres d'abord, puis rédaction).

---

## 5. Prochaine étape (accord explicite du 2026-07-06)

Repartir des bases sur **check/contrainte** (section 3) avant de toucher à quoi que ce soit
d'autre. Rien ne doit être codé tant que :
1. la décision fusion/distinction n'est pas prise ;
2. le périmètre d'impact (specs + scénarios existants + classes + prompts) n'est pas listé
   précisément.
