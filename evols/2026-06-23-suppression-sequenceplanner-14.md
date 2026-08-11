# 2026-06-23 - Suppression SequencePlanner

**2026-06-23 — Suppression de l'agent SequencePlanner (hallucination de migration)**

## Evolution demandee
SequencePlanner a ete cree par erreur lors de la migration Redacteur -> Storymagine.
Dans le Redacteur original, SequencePlannerContext est une option avancee (activee par flag planMode)
qui n'est pas utilisee dans le flux nominal.
Dans Storymagine, ChapterPlanner JSON mode produit deja les plans par sequence via sequencePlans().
SequencePlanner etait donc un doublon LLM inutile.

## Ce qui a ete touche

### Fichiers supprimes
- redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/sequenceplanner/SequencePlanner.java
- redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/sequenceplanner/SequencePlannerInput.java
- redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/sequenceplanner/SequencePlannerOutput.java
- redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/plan/sequenceplanner/SequencePlanner.md
- redacteur/src/main/java/storymagine/redacteur/coeur/domaine/orchestrator/write/SequencePlannerStep.java

### Fichiers modifies
- WrittenChapter.java : ajout sequencePlans (List<String>) + getter/setter
- PlanWorkflow.java : stocke le meilleur sequencePlans en parallele du bestPlan
- WriteWorkflow.java : retire SequencePlannerStep du constructeur et de writeSequence ; utilise wc.sequencePlans().get(seqIdx-1)
- RedacteurModule.java : retire SequencePlanner + SequencePlannerStep
- WrittenSequence.java : javadoc mise a jour
- WriterInput.java : javadoc mise a jour
- Writer.md : reference mise a jour
- migration.md : correspondance annotee

## Resultat
Le flux WriteWorkflow utilise maintenant directement les plans par sequence produits par ChapterPlanner
(stockes dans WrittenChapter.sequencePlans), sans appel LLM supplementaire.