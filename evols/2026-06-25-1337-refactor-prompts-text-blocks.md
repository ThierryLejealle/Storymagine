# 2026-06-25 13h37 — Refactoring des prompts agents : text blocks et lisibilité

## Évolution demandée

Appliquer la règle de style : utiliser `"""` (text blocks Java) pour tous les prompts des agents,
et organiser le code de génération du prompt dans l'ordre du prompt final pour faciliter
la lecture et la modification par un humain.

Mise à jour de CLAUDE.md pour clarifier la règle : optimiser pour la lisibilité humaine,
pas pour l'efficacité de la concaténation.

## Ce qui a été touché

### CLAUDE.md
- Reformulation de la règle PROMPT : "optimise pour la lisibilité humaine — le lecteur doit voir
  le contenu du prompt directement dans le code, dans l'ordre."

### Agents refactorés (22 fichiers)

**SYSTEM statiques convertis de `+` en `"""`**
- `CausalAnalyzer` — SYSTEM
- `NarrativeArcAnalyzer` — SYSTEM
- `TextCoherenceCritic` — SYSTEM
- `TextNarrativeCritic` — SYSTEM
- `TextWhatIfCritic` — SYSTEM
- `DeusInMachinaChecker` — SYSTEM + user (variables nommées au lieu de StringBuilder)

**buildSystem dynamiques convertis en blocs `"""` ordonnés**
- `ChapterStyleChecker` — buildSystem (StringBuilder → variables `styleGuideSection`, `qualitySection`, etc.) + buildUser (variables nommées)
- `SequenceStyleChecker` — même structure
- `TextDreamCritic` — extraction d'une méthode `buildSystem(level)` avec `"""` + switch
- `ChapterPlanner` — 5 constantes converties en `"""`, buildSystem simplifié avec `buildForbiddenPhrases()` extrait
- `Writer` — buildSystem restructuré : 4 variables nommées dans l'ordre du prompt (`rewritePrefix`, `lengthConstraint`, `openingRule`, `bannedPhrases`/`bannedThemes`) + suppression de `base` inutilisée

**User prompts en StringBuilder convertis en concaténation lisible**
- `GoalPlanChecker` — helper `section()` retournant String (au lieu de `appendOpt` + StringBuilder)
- `GoalTextChecker` — même structure
- `FocusActionFilter` — 5 variables nommées dans l'ordre du prompt

**Déjà corrects — non modifiés**
- `CharacterChecker`, `Proofreader`, `RepetitionFilter`, `RepetitionTracker`,
  `SequenceChecker`, `StateExtractor`, `StoryCompressor`,
  `GoalPlanChecker` (SYSTEM), `PlanCoherenceCritic`, `PlanNarrativeCritic`

## Résultat

- Compilation : OK (aucune erreur)
- Contenu des prompts : inchangé (reformatage uniquement — whitespace, pas de mots)
- Chaque agent : le code se lit de haut en bas comme le prompt final
- Les parties conditionnelles sont visibles et localisables au bon endroit
