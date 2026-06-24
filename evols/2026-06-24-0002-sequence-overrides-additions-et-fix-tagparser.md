# 2026-06-24 00h02 — Refactoring SequenceOverrides/Additions + fix TagElementParser

## Description de l'évolution

Deux corrections liées à l'utilisation du focus et du lore dans les séquences :

1. **Bug fonctionnel** : le focus et le lore déclarés sur une séquence (`focus: ["WAGON"]`, `lore: ["GARE"]`)
   n'apparaissaient pas dans le log du ChapterPlanner.
   Cause : `TagElementParser.parse()` ne réinitialisait pas `section` à `GLOBAL` lors d'un nouveau `[TAG]`.
   Résultat : le contenu du tag était stocké dans `writerContent` au lieu de `globalContent`,
   et le planner (forWriter=false) ne voyait donc rien.

2. **Refactoring sémantique** : l'ancienne classe `SequenceOverrides` portait deux natures confondues :
   - des éléments **additifs** (focus, lore, characters, checks) — s'ajoutent aux defaults du chapitre
   - des éléments **de surcharge** (stitch, minWords) — remplacent un défaut global
   
   Ces deux natures ont été séparées en deux classes distinctes.

## Ce qui a été touché

| Fichier | Nature du changement |
|---------|----------------------|
| `TagElementParser.java` | Ajout de `section = Section.GLOBAL` à chaque nouveau `[TAG]` |
| `SequenceOverrides.java` | Réécrit : contient désormais `stitch` + `minWords` uniquement |
| `SequenceAdditions.java` | Créé : contient `focus`, `lore`, `characters`, `checks` (ex-SequenceOverrides) |
| `Sequence.java` | Porte les deux objets ; champs directs : `type`, `directive`, `noTransition` |
| `ScenarioFileAdapter.java` | `mapSequence()` construit les deux objets séparément |
| `ScenarioFormatters.java` | `sequenceDescriptions()` utilise `seq.additions()` |
| `WriterStep.java` | Merge via `sequence.additions()` ; stitch/minWords via `sequence.overrides()` |
| `MockModelCallPort.java` | Correction d'une régression pré-existante : signature `generate()` avec `LlmCallContext` |

## Résultat

- Compilation : OK
- Tests : OK (tous passent)
- Le focus et le lore de séquence sont désormais correctement transmis au ChapterPlanner
