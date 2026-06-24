# 2026-06-24 12h38 — Dualité checks/constraints à tous les niveaux

## Évolution demandée
Étendre la dualité checks/constraints (jusqu'ici uniquement au niveau scénario global)
aux niveaux chapitre et séquence, en conservant la logique additive.
Les checks et contraintes restent deux blocs séparés — pas de wrapper commun.

## Ce qui a été touché

### Domaine
- `ChapterDefaults.java` : `List<Check> checks` → `CheckList checks` (gain du split plan/writer) + ajout `ConstraintList constraints`
- `SequenceAdditions.java` : même changement

### Infra — DTOs
- `PlanWriterListDto.java` (nouveau) : DTO partagé checks/constraints avec champs `global`, `plan`, `writer`
- `PlanWriterListDeserializer.java` (nouveau) : désérialiseur Jackson gérant deux formes YAML :
  - liste plate `- "item"` → rétrocompatible, items dans `global` (plan ET writer)
  - map structurée `plan: [...] / writer: [...]` → split explicite
- `ChapterDefaultsDto.java` : `List<String> checks` → `PlanWriterListDto checks` + ajout `PlanWriterListDto constraints`
- `SequenceDto.java` : même changement

### Infra — Adapteur
- `ScenarioFileAdapter.java` : `mapChecks(List<String>)` remplacé par `mapCheckList(PlanWriterListDto)` + `mapConstraintList(PlanWriterListDto)` + helper `merge(global, specific)`

### Scénario as-du-ciel (illustration)
- `chap_1.yaml`, séquence 2 (Bertrand) : checks migrés en forme structurée plan/writer + bloc `constraints` ajouté en miroir
- Séquence 1 : liste plate conservée en l'état — illustre la rétrocompatibilité

## Résultat
- Les scénarios existants avec `checks: [liste plate]` continuent à fonctionner sans modification
- Il est désormais possible de définir checks ET constraints à granularité chapitre ou séquence
- La compilation ne produit aucune nouvelle erreur (les erreurs pré-existantes sont sans rapport)

## Prochaine étape
Les Steps (PlanCoherenceCriticStep, SequenceCheckerStep, etc.) ne collectent pas encore
les checks/constraints des niveaux chapitre et séquence — c'est une évolution séparée.
