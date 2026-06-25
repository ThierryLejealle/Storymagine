# 2026-06-25 16h30 - NaturalityFilter, GoalTextChecker, ChapterPlanner

## Evolution demandee

Trois ameliorations distinctes sur les agents redacteur :
1. Nouvel agent **NaturalityFilter** : detecte et remplace les formulations artificielles de LLM dans le texte final
2. Correction **GoalTextChecker** : echelle 1-10 complete, FORMAT STRICT, exemples narratifs
3. Amelioration **ChapterPlanner** : contraintes sur les beats plus precises, renommage `ton_et_rythme` → `intention_de_scene`

## Ce qui a ete touche

### NaturalityFilter (nouvel agent)
- `naturalityfilter/NaturalityFinding.java` — record `(citation, probleme, suggestion)`
- `naturalityfilter/NaturalityFilterInput.java` — record `(text)`
- `naturalityfilter/NaturalityFilterOutput.java` — record `(List<NaturalityFinding>)`
- `naturalityfilter/NaturalityFilter.java` — agent avec 6 criteres de detection, parser bloc EXTRAIT/PROBLEME/SUGGESTION, temperature 0.3
- `naturalityfilter/NaturalityFilter.md` — documentation des 6 criteres, regles de non-detection, comportement substitution
- `orchestrator/write/NaturalityFilterStep.java` — step d'activation
- `orchestrator/write/WriteWorkflow.java` — insertion apres selection du meilleur chapitre, application des substitutions par sequence, conditionne a `runsProofreader()`
- `RedacteurModule.java` — cablage du NaturalityFilter

**Pattern** : substitution directe (comme Proofreader, pas un critic). Split sur `EXTRAIT :`, extraction PROBLEME et SUGGESTION par index.

**Activation** : SIMPLE et FULL (tout sauf BROUILLON) via `config.qualityLevel().runsProofreader()`.

### GoalTextChecker
- Echelle 1-10 complete avec labels descriptifs (sans accents pour petits modeles)
- Section FORMAT STRICT avec `[RIEN]` et deux exemples narratifs
- Cohérent avec les autres agents critics du projet

### ChapterPlanner
- Champ `sensoriels` : contrainte explicite `<sons, textures, odeurs, lumiere, temperatures, mouvements visibles — pas d'emotions ni d'interpretations>`
- Renommage `ton_et_rythme` → `intention_de_scene` : prompt + parser (`extractString`) + `formatForWriter`
- Regle 2 : enrichissement uniquement par comportements observables (jamais interpretations)
- Contraintes beats : test camara, 5 interdictions, regle anti-abstraction, 5 exemples (2 bons, 3 mauvais)
- `ChapterPlanner.md` mis a jour

## Resultat

Compilation propre. NaturalityFilter integre dans le pipeline write. GoalTextChecker corrige. ChapterPlanner avec contraintes renforcees sur les beats.
