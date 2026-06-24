# 2026-06-23 18h00 — Séquences overrides additifs (focus, lore, personnages)

## Évolution demandée
Les éléments déclarés sur une séquence (SequenceOverrides) doivent s'additionner
aux défauts du chapitre (ChapterDefaults), pas les remplacer.
- Le Plan (ChapterPlanner) doit voir les focus/lore/personnages globaux du chapitre
  ET les éléments propres à chaque séquence.
- Le Writer d'une séquence doit recevoir la somme chapitre + séquence courante.

## Ce qui a été touché

### ScenarioFormatters.sequenceDescriptions()
Avant : retournait uniquement `seq.directive()` pour chaque séquence.
Après : si la séquence a des overrides non vides, ils sont appendés après la directive
avec des labels `[Focus : ...]`, `[Lore : ...]`, `[Personnages : ...]`.
Le focus/lore/personnages chapitre (déjà dans les champs dédiés de ChapterPlannerInput)
ne sont pas répétés ici — seuls les items propres à la séquence s'ajoutent.

### WriterStep
Avant : passait `chapter.defaults().focus()`, `.lore()`, `.characters()` seuls.
Après : passe la liste mergée `chapter.defaults().X() + sequence.overrides().X()`
pour les 3 dimensions, via un helper privé générique `merge(base, extra)`.

## Résultat
- Compilation OK, tests OK.
- Les overrides séquence, jusqu'ici données mortes, sont désormais effectivement
  transmis aux agents concernés.
- FocusActionFilter (mise en sommeil à venir) était de toute façon un no-op
  sur les actionCategories (toujours vide).
