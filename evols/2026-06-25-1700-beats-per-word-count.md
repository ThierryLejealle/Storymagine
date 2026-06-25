# 2026-06-25 17h00 - Beats par nombre de mots (planificateur)

## Evolution demandee

Remplacer le minimum de beats global (`plannerEffortInLines`) par un nombre de beats cible par sequence, calcule depuis le nombre de mots cible. Le planner recoit desormais une fourchette `[Nombre de beats : X a Y]` par sequence, derivee du ratio mots/beats et d'une marge de tolerance.

Cascade de resolution du nombre de mots par sequence :
sequence override → defaut chapitre → defaut scenario → valeur proprietes (300 mots)

## Ce qui a ete touche

### Nouvelles proprietes (`redacteur.properties`)
- `sequence.default.words=300`
- `beats.per.words.ratio=80` — un beat tous les 80 mots par defaut
- `beats.tolerance.pct=20` — marge de +/-20% sur le nombre de beats cible

### Nouveau fichier
- `orchestrator/plan/BeatsConfig.java` — record `(wordsPerBeat, tolerancePct)` avec `defaults()`

### Domain
- `ChapterDefaults.java` — ajout champ `sequenceMinWords: Integer` (optionnel) + mise a jour constructeur et `EMPTY`
- `ChapterDefaultsDto.java` — ajout `sequence_min_words: Integer`
- `ScenarioFileAdapter.java` — mapping `dto.sequence_min_words` dans `mapDefaults()`
- `Scenario.java` — ajout `resolveSequenceWords(Sequence, Chapter)` : cascade sequence → chapitre → scenario
- `ScenarioFormatters.java` — extraction `singleSequenceDescription(Sequence)`, refactorisation de `sequenceDescriptions()`

### Writer
- `WriterStep.java` — utilise `scenario.resolveSequenceWords(seq, chapter)` a la place de `seq.overrides().minWords()` direct

### Planner
- `ChapterPlannerInput.java` — suppression du champ `plannerEffortInLines` (remplace par annotations dans les descriptions)
- `ChapterPlanner.java` — remplacement de la ligne `- Au moins %d par sequence` par une section "Nombre de beats par sequence" avec la guidance sur le choix naturel ; suppression `String.format`
- `ChapterPlannerStep.java` — ajout `BeatsConfig` en constructeur ; calcul de la fourchette beats par sequence (avec arrondi) ; injection `[Nombre de beats : X a Y]` dans chaque description de sequence (mode JSON uniquement) ; suppression `resolveEffort()`

### Cablage
- `RedacteurModule.java` — tous les overloads courts delegent au nouvel overload avec `BeatsConfig` ; `ChapterPlannerStep` recoit `beatsConfig`
- `RedacteurCli.java` — lecture des proprietes beats, construction `BeatsConfig`, passage a `assemble()`

## Formule de calcul

```
targetBeats = round(targetWords / wordsPerBeat)       ex. round(300/80) = 4
minBeats    = max(1, round(targetBeats * (100-tol)/100))  ex. max(1, round(3.2)) = 3
maxBeats    = round(targetBeats * (100+tol)/100)          ex. round(4.8) = 5
→ annotation : [Nombre de beats : 3 a 5]
```

## Resultat

Compilation propre (`mvn compile -pl redacteur -am`). Le planner recoit desormais un cadrage par sequence au lieu d'un minimum global, favorisant un decoupage naturel plutot qu'artificiel.
