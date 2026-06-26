# 2026-06-26 14h30 - StyleCritic transformé en StyleCorrector

## Évolution demandée

Transformer StyleCritic (Phase 3 — critique avec score) en StyleCorrector (Phase 2 — correction
inline via paires FAUX/JUSTE), sans modifier le prompt d'identification des défauts.

## Ce qui a été touché

### Nouveaux fichiers

- `stylecorrector/StyleCorrectorFinding.java` — record `(wrongPhrase, correctedPhrase)`
- `stylecorrector/StyleCorrectorInput.java` — mêmes champs que StyleCriticInput
- `stylecorrector/StyleCorrectorOutput.java` — `List<StyleCorrectorFinding>`
- `stylecorrector/StyleCorrector.java` — prompt identique à StyleCritic sauf :
  - step 2 : ajout d'une ligne "Pour chaque point, ecris uniquement la correction (pas d'explication)."
  - FORMAT STRICT : remplacé par CORRECTIONS: / FAUX: / JUSTE: / PAS DE CORRECTION
  - buildUser : "Évalue ce texte. Liste tes PROBLEME:." → "Corrige ce texte. Donne les corrections FAUX/JUSTE."
- `orchestrator/write/StyleCorrectorStep.java`

### Supprimés

- `stylecritic/StyleCritic.java`
- `stylecritic/StyleCriticInput.java`
- `stylecritic/StyleCriticOutput.java`
- `orchestrator/write/StyleCriticStep.java`

### Mis à jour

- `WriteWorkflow.java` : StyleCorrector ajouté en Phase 2 (après NaturalityCorrector, avant ProofreaderCorrector) ; bloc StyleCritic retiré de Phase 3 ; méthode `applyStyleCorrections` ajoutée avec log.warn() sur replace miss
- `RedacteurModule.java` : câblage StyleCorrector/StyleCorrectorStep, suppression StyleCritic
- `orchestrator/CLAUDE.md` : diagramme mis à jour (Phase 2 et Phase 3)

## Résultat

Compilation OK. StyleCorrector en position 3 de Phase 2 :
DeusInMachinaCorrector → NaturalityCorrector → StyleCorrector → ProofreaderCorrector
