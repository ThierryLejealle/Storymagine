# 2026-06-22 - Restructuration : un répertoire par agent

## Evolution demandée
Restructurer les packages d'agents LLM pour donner à chaque agent son propre sous-répertoire
(et donc son propre package Java), au lieu d'un package plat par groupe.

## Ce qui a été touché

### Répertoires renommés / créés (24 sous-répertoires)

**agent/plan/**
- plan/chapterplanner/     (ChapterPlanner + Input/Output + .md)
- plan/sequenceplanner/    (SequencePlanner + Input/Output + .md)
- plan/plannarrativecritic/
- plan/plancoherencecritic/
- plan/goalplanchecker/

**agent/writer/**
- writer/sequencewriter/   (Writer — nommé sequencewriter pour eviter writer/writer/)
- writer/proofreader/
- writer/deusinmachinachecker/
- writer/sequencestylechecker/
- writer/sequencechecker/
- writer/repetitiontracker/
- writer/repetitionfilter/
- writer/stateextractor/
- writer/focusactionfilter/
- writer/textnarrativecritic/
- writer/textcoherencecritic/
- writer/textdreamcritic/
- writer/textwhatifcritic/
- writer/goaltextchecker/

**agent/global/**
- global/storycompressor/
- global/chapterstylechecker/
- global/characterchecker/
- global/narrativearcanalyzer/
- global/causalanalyzer/

**agent/commun/** — inchangé (parsers partages, pas des agents individuels)

### Packages Java mis à jour

Chaque fichier .java a son package mis à jour :
- Avant : storymagine.redacteur.coeur.domaine.agent.<groupe>;
- Apres  : storymagine.redacteur.coeur.domaine.agent.<groupe>.<sousrepertoire>;

Exemple : storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner

## Decision technique

Le sous-repertoire du Writer s'appelle 'sequencewriter' (et non 'writer')
pour eviter la redondance writer/writer/.

## Résultat
24 agents chacun dans son propre package. Les groupes plan/, writer/, global/
n'ont plus de fichiers Java a leur racine.