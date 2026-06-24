# 2026-06-23 22h05 - Rationalisation COMMUN/PLAN/WRITER et chargement des fichiers directifs

## Evolution demandée

Rationaliser la structure COMMUN/PLAN/WRITER du scénario : mutualiser le code dupliqué,
corriger un bug de parsing, charger les fichiers directifs manquants (goal, quality, style, keep_phrases),
et supprimer les fichiers non implémentés (context_events, actions, character_pools).

## Ce qui a été touché

### Suppression de fichiers inutilisés
- `scenarios/*/context_events.md` — supprimé de as-du-ciel et modele
- `scenarios/*/actions/` — supprimé de as-du-ciel et modele
- `scenarios/*/character_pools.yaml` — supprimé de as-du-ciel et une-rencontre
- Idem dans `redacteur/src/test/resources/scenarios/as-du-ciel/`
- Section `context_cards` retirée de `Rappel-test-Fonctionnalites.md` (et sa copie en test/resources)

### Rationalisation du parsing

**`TagElementParser`** — ajout de `parseSingleBlock(String content)` : parse un fichier
entier (sans `[TAG]`) en GLOBAL/PLAN/WRITER. Évite la duplication de logique.

**`PersonnageParser`** — réduit à 3 lignes : délègue à `TagElementParser.parseSingleBlock()`.
Suppression de ~30 lignes de logique dupliquée.

**`CheckListParser`** — ajout d'une section GLOBAL (bullets avant `## PLAN`/`## RÉDACTION`).
Les items globaux sont injectés dans planChecks ET writerChecks.
Corrige la perte silencieuse des checks de `une-rencontre/checks.md` (sans section explicite).

### Nouveaux champs dans Scenario

`Scenario` reçoit 4 nouveaux champs : `bookGoal`, `quality`, `writingStyle`, `keepPhrases`.
Chargés par `ScenarioFileAdapter` via `readOptionalDirective()` (strip des blocs HTML `<!-- -->`).

Fichiers sources : `goal.md`, `quality.md`, `style.md`, `keep_phrases.md`.

### ScenarioFormatters

`bookGoal(ScenarioConfig)` → `bookGoal(Scenario)` : utilise `goal.md`, fallback sur le titre.
Ajout de `keepPhrases(Scenario)`.

### Steps mis à jour

`bookGoal(scenario.config())` → `bookGoal(scenario)` dans :
- `GoalPlanCheckerStep`, `PlanNarrativeCriticStep`, `ChapterPlannerStep`

Signature `ScenarioConfig` → `Scenario` dans :
- `GoalTextCheckerStep`, `TextNarrativeCriticStep`, `TextDreamCriticStep`

### Bug corrigé — WriteWorkflow

`RepetitionFilterStep` passait `loreText(chapter.defaults().lore(), true)` comme contenu
de `keep_phrases.md` — le lore était utilisé à la place des leitmotivs autorisés.
Corrigé : `ScenarioFormatters.keepPhrases(scenario)`.

### Specs mises à jour

`specs/scenario.md` — réécrit avec arborescence complète, 4 catégories de fichiers,
règles d'accumulation checks/constraints, structure COMMUN/PLAN/WRITER par catégorie.
Nom de fichier corrigé : `keep_phrases.md` (underscore, conformément aux fichiers réels).

## Résultat

- Compilation : OK (0 erreur)
- ScenarioLoadTest : 8/8 verts
- `quality` et `writingStyle` chargés mais pas encore câblés à des agents (pas de modification de prompt)
- `WorkflowLogTest` : 4 erreurs pré-existantes non liées (`MockModelCallPort` signature obsolète)
