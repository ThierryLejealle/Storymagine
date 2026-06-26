# 2026-06-26 14h00 - Rename Checker→Critic/Corrector + nouveau agent DeusInMachinaCorrector

## Description de l'évolution

Renommage de 6 agents selon la nouvelle convention de nommage (suffixe Critic pour les agents d'analyse, Corrector pour les agents de correction), et création d'un nouvel agent DeusInMachinaCorrector.

## Ce qui a été touché

### Renames d'agents (package + classes)

| Ancien nom                  | Nouveau nom               | Package                              |
|-----------------------------|---------------------------|--------------------------------------|
| DeusInMachinaChecker        | DeusInMachinaCritic       | deusinmachinacritic                  |
| SequenceStyleChecker        | StyleCritic               | stylecritic                          |
| SequenceChecker             | ElementCritic             | elementcritic                        |
| GoalTextChecker             | GoalTextCritic            | goaltextcritic                       |
| NaturalityFilter            | NaturalityCorrector       | naturalitycorrector                  |
| Proofreader                 | ProofreaderCorrector      | proofreadercorrector                 |

Pour chaque rename : Input, Output et agent renommés. Les anciens répertoires ont été supprimés.

### Modifications fonctionnelles

- DeusInMachinaCriticOutput : ajout de score() calculé depuis leaks.size() (0=10, 1=7, 2=4, 3+=1)
- StyleCritic : suppression du score LLM (ProblemScoreParser.parseScoreInt), score désormais calculé depuis problems.size() (10 - problems.size() * 2, min 1). Section ## Echelle de notation et ligne SCORE: N retirées du prompt.
- ElementCritic : logique de score algorithmique inchangée.

### Nouveau agent : DeusInMachinaCorrector

Créé dans deusinmachinacorrector avec :
- DeusInMachinaCorrectorInput (text, constraints)
- DeusInMachinaCorrectorFinding (wrongPhrase, correctedPhrase)
- DeusInMachinaCorrectorOutput (List<DeusInMachinaCorrectorFinding>)
- DeusInMachinaCorrector : même SYSTEM prompt que DeusInMachinaCritic, FORMAT RÉPONSE FAUX/JUSTE (identique à ProofreaderCorrector)
- DeusInMachinaCorrectorStep

### Fichiers steps renommés (orchestrator/write/)

DeusInMachinaCheckerStep → DeusInMachinaCriticStep
SequenceStyleCheckerStep → StyleCriticStep
SequenceCheckerStep → ElementCriticStep
GoalTextCheckerStep → GoalTextCriticStep
NaturalityFilterStep → NaturalityCorrectorStep
ProofreaderStep → ProofreaderCorrectorStep

### WriteWorkflow.java

Imports, types de champs, paramètres constructeur et corps de méthode mis à jour.
Labels des appels log.critic() mis à jour avec les nouveaux noms.
Noms de variables internes inchangés (sequenceCheckerStep, goalTextCheckerStep, etc.) pour ne pas modifier la signature du constructeur.

### RedacteurModule.java

Imports mis à jour. Instanciations mises à jour. Variable deusInMachinaCorrectorStep ajoutée (pas encore passée au constructeur WriteWorkflow).

## Résultat

6 agents renommés, 1 nouvel agent créé, 0 erreur de compilation (warning attendu sur deusInMachinaCorrectorStep non utilisée).