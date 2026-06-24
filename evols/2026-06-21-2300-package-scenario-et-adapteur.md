# 2026-06-21 23h00 - Package Scenario et adapteur de lecture

## Evolution demandee
Migrer la partie "Scenario" de ../Redacteur dans un modele objet propre dans Storymagine.
Creer un module redacteur avec :
- Tous les beans du domaine scenario (POJO purs, sans framework)
- Un port ScenarioReaderPort
- Un adapteur ScenarioFileAdapter qui lit et valide les fichiers d'un scenario
Reprendre les scenarios as-du-ciel, 1998 et une-rencontre comme exemples.

## Ce qui a ete touche

### Nouveau module : redacteur/
- pom.xml (dependances : commun, jackson-databind, jackson-dataformat-yaml, junit)
- Module ajoute au pom.xml parent

### /coeur/domaine/scenario/
- TagElement.java (abstract) - parent commun avec tag/globalContent/planContent/writerContent
- NarrativeType.java (enum) - IMPERATIVE / DREAM / WHAT_IF
- ScenarioConfig.java - parametres de scenario.md
- Check.java (record) / CheckList.java - verifications PLAN + WRITER
- Constraint.java (record) / ConstraintList.java - contraintes PLAN + WRITER
- ChapterDefaults.java - valeurs heritees par les sequences
- SequenceOverrides.java - additifs declares sur une sequence
- Sequence.java - sequence narrative
- Chapter.java - chapitre avec setting, comment, type, sequences
- Scenario.java - racine d'agregat

### /coeur/domaine/scenario/focus/
- FocusElement.java (extends TagElement)
- FocusItem.java (sealed interface)
- FocusRef.java / FocusInline.java
- FocusPool.java

### /coeur/domaine/scenario/lore/
- LoreElement.java (extends TagElement)
- LoreItem.java (sealed interface)
- LoreRef.java / LoreInline.java
- LorePool.java (avec EMPTY statique)

### /coeur/domaine/scenario/personnage/
- Personnage.java (extends TagElement)
- PersonnagePool.java

### /coeur/ports/
- ScenarioError.java (record avec factory methods)
- ScenarioReaderPort.java (interface : validate + load)

### /infra/scenario/
- TagElementParser.java - parse [TAG]/#PLAN/#WRITER en blocs
- CheckListParser.java - parse checks.md et constraints.md
- LoreItemParser.java - parse les directives lore mixtes (ref + inline)
- PersonnageParser.java - parse les fiches personnage (sections GENERAL/INTRIGUE/DESCRIPTION)
- dto/ : ScenarioConfigDto, ChapterDto, ChapterDefaultsDto, SequenceDto (Jackson)
- ScenarioFileAdapter.java - implements ScenarioReaderPort

### scenarios/ (a la racine du projet)
- as-du-ciel/ (copie depuis Redacteur/story/as-du-ciel/)
- 1998/ (copie depuis Redacteur/story/1998/)
- une-rencontre/ (copie depuis Redacteur/story/une-rencontre/)

### Tests
- src/test/resources/scenarios/as-du-ciel/ (copie stable pour les TU)
- ScenarioLoadTest.java (7 tests d'integration sur as-du-ciel)

### specs/migration.md
- Mis a jour : phase 1 terminee, phases 2-5 documentees

## Decisions de conception notables
- FocusGroup exclu du modele (uniquement pour loop/foreach, exclus du projet)
- TagElement abstract : FocusElement, LoreElement, Personnage partagent la meme structure
- LorePool.EMPTY et CheckList.EMPTY : valeurs sentinelles pour les fichiers optionnels
- Sealed interfaces FocusItem / LoreItem : exhaustivite garantie par le compilateur
- DTOs Jackson en infra uniquement : le coeur reste pur Java
- Scenarios de test dans src/test/resources : independants des scenarios de production

## Resultat
Module redacteur operationnel avec le package scenario complet.
Les 3 scenarios sont charges sans erreur.
A recharger dans l'IDE apres ajout du module au pom.xml parent (Java: Reload Projects).
