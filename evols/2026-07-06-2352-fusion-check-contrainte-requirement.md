# 2026-07-06 23h52 - Fusion check/contrainte en concept Requirement

## Évolution demandée

Reprise après incident (session précédente perdue suite à une corruption BOM ayant forcé un
`git revert`). Analyse de l'usage réel de `checks`/`constraints` dans les scénarios (1998,
une-rencontre, as-du-ciel), puis fusion des deux concepts en un seul : `Requirement`, avec une
syntaxe textuelle `"contrainte | check"` :
- une seule valeur (sans `|`) → utilisée à la fois comme contrainte et comme check
- `"contrainte | check"` → formulation différente pour chaque audience
- `"contrainte |"` → contrainte seule (ignorée des Critics)
- `"| check"` → check seul (ignoré du Planner/Writer)

Chaque agent ne consomme plus qu'un seul côté selon son rôle : `Writer`/`Planner` →
`constraint()`, tous les autres (`Critic`/`Corrector`) → `check()`. Les prompts encadrent
désormais explicitement ce côté par un verbe ("Assure-toi que..." / "Vérifie que...") au lieu
de laisser le texte brut porter sa propre formulation (parfois interrogative, parfois avec un
"Vérifie que" déjà intégré au texte — doublon avec le nouveau wrapper).

## Ce qui a été touché

### Domaine (`coeur/domaine/scenario`)
- Suppression de `Check`, `CheckList`, `Constraint`, `ConstraintList`.
- Création de `Requirement` (record `constraint`/`check` + `Requirement.parse(String)`
  implémentant la syntaxe `|`) et `RequirementList` (remplace les deux anciennes listes,
  même structure plan/writer).
- `Scenario`, `ChapterDefaults`, `SequenceAdditions` : un seul champ `requirements`
  (`RequirementList`) à la place de `checks` + `constraints`.

### Infra (`infra/scenario`)
- `CheckListParser` → `RequirementListParser` (une seule méthode `parse`, utilise
  `Requirement::parse` au lieu de `new Check(...)`/`new Constraint(...)`).
- `PlanWriterListDto`/`PlanWriterListDeserializer` → `RequirementListDto`/
  `RequirementListDeserializer` (même logique de déserialisation YAML : texte simple, liste
  plate, ou structuré `common`/`plan`/`writer`).
- `ChapterDefaultsDto`, `SequenceDto` : champ `requirements` à la place de `checks`+`constraints`.
- `ScenarioFileAdapter` : `loadChecks`+`loadConstraints` → `loadRequirements` (lit
  `requirements.md`) ; `mapCheckList`+`mapConstraintList` → `mapRequirementList`.

### Orchestrator (`orchestrator/common/ScenarioFormatters`)
- `planConstraints`/`writerConstraints`/`planChecks`/`writerChecks` (tous niveaux : global,
  scénario+chapitre, scénario+chapitre+séquence) reconstruits sur `RequirementList`, en
  filtrant les côtés vides.
- `enrichPlanJson`/`buildSeqAnnotation` : les clés JSON séparées `"checks"` + `"contraintes"`
  injectées par séquence deviennent une seule clé `"points_a_verifier"`, sourcée du côté
  `check()` (cohérent avec le fait que `PlanCoherenceCritic`, qui lit ce JSON, est un Critic).
- `singleSequenceDescription` : source `seq.additions().requirements()` au lieu de
  `.constraints()`.

### Agents et leurs prompts
- **PlanCoherenceCritic** : Input réduit à `plan, chapterGoal, checks, characters` (suppression
  des champs déjà morts `constraints`/`focusText`, cf. FIXME laissés par la session
  précédente). Ajout du verbe "Vérifie que chacun des points suivants est respecté :". La
  règle de portée du prompt référence désormais `"points_a_verifier"` (cohérent avec
  `enrichPlanJson`).
- **TextCoherenceCritic** : suppression du champ `constraints` (un Critic ne consomme que
  `check()`), ajout du même verbe.
- **TextWhatIfCritic**, **DeusInMachinaCritic**, **DeusInMachinaCorrector** : champ Input
  renommé `constraints` → `checks`, source basculée de `writerConstraints` à `writerChecks`
  (ce sont des Critic/Corrector, jamais Writer/Planner). Verbe ajouté pour TextWhatIfCritic ;
  DeusInMachina garde son cadrage "pour référence" (usage différent : contexte pour éviter les
  faux positifs, pas vérification directe).
- **ChapterPlanner**, **Writer** : ajout du verbe "Assure-toi que chacun des points suivants
  est respecté :" avant le bloc de contraintes — inchangé sur la source (`constraint()`,
  déjà correcte).
- **CheckCritic** : inchangé (avait déjà le bon verbe en fin de prompt).
- `PlanCoherenceCriticStep`, `TextCoherenceCriticStep`, `TextWhatIfCriticStep`,
  `DeusInMachinaCriticStep`, `DeusInMachinaCorrectorStep` : mis à jour pour construire les
  nouveaux Input.

### Scénarios (1998, une-rencontre, as-du-ciel, modele + fixture de test as-du-ciel)
- `checks.md` + `constraints.md` → `requirements.md` dans les 4 scénarios, en pairant
  manuellement chaque contrainte avec son check quand ils exprimaient la même règle (ex.
  as-du-ciel : "huit jours" / "les contraintes temporelles sont-elles respectées ?"), en
  gardant chaque élément seul (`|` en tête ou en fin) quand aucune correspondance n'existait
  (ex. le décalage réel identifié : introduction de Wolf au ch.5 vs. Wolf ne meurt pas — deux
  faits distincts sur le même personnage, pas une reformulation).
- Champs YAML `checks:`/`constraints:` de séquence → `requirements:` dans tous les chapitres
  concernés (as-du-ciel : 8 fichiers, ~20 séquences ; une-rencontre : 1 séquence ; modele : 2
  fichiers). Le cas documenté `chap_2.yaml` (scène Bertrand, as-du-ciel) — seul endroit avec un
  vrai couple plan/writer + checks/constraints distincts — a été fusionné avec pairing 1:1 où
  pertinent et `|` en tête pour le seul check sans contrainte correspondante (durée de scène).
- Idem sur la fixture de test `redacteur/src/test/resources/scenarios/as-du-ciel/` (copie
  figée, contenu identique constaté avant fusion).
- `Rappel-test-Fonctionnalites.md` (scénario + fixture) : exemple de code mis à jour.

### Specs et documentation
- `specs/scenario.md` : sections CONSTRAINTS/CHECKS fusionnées en une section REQUIREMENTS,
  nouvelle section "Syntaxe Requirement (contrainte \| check)", exemples YAML mis à jour.
- `PlanCoherenceCritic.md`, `TextCoherenceCritic.md`, `ChapterPlanner.md` : alignés sur le
  nouveau vocabulaire (points à vérifier / `points_a_verifier`, côté Requirement consommé).
- `redacteur/.../agent/CLAUDE.md` : nouvelle section "Requirement — Contrainte vs Check"
  documentant la règle définitive (Writer/Planner → constraint, tout le reste → check).

### Mémoire / repère de session
- `OUBLI.MD` (racine) créé en amont pour reconstituer le contexte perdu lors de l'incident.

## Résultat

- BOM : vérifié absent sur les 62 fichiers modifiés/créés de la session (aucune exception).
- Compilation : `mvn compile` depuis la racine — OK, 0 erreur.
- Tests : `ScenarioLoadTest` — 7/8 verts, y compris le nouveau `load_globalRequirements`.
  1 échec préexistant (`load_sequenceFocusRefsAreResolved`, résolution `focus:` en
  `FocusInline` au lieu de `FocusRef`) confirmé **non lié** à ce chantier — reproduit à
  l'identique avec `git stash` sur l'état d'avant session. Laissé pour une passe future.

## Points laissés en suspens (passe future)

- `load_sequenceFocusRefsAreResolved` : bug préexistant sur la résolution des tags focus au
  niveau `chapter.defaults()`.
- `PlanCoherenceCritic` ne reçoit pas le focus du chapitre (`focusText` était déjà mort avant
  cette session — signalé à l'utilisateur, à corriger séparément).
- Nettoyage de la formulation interrogative encore présente dans certains textes de check
  globaux (ex. as-du-ciel `requirements.md` section PLAN) — non réécrite dans cette passe,
  seule la structure a été fusionnée ; le wrapper de prompt absorbe la formulation mais une
  réécriture en fait déclaratif nu serait plus propre à terme.
