# 2026-07-12 01h00 — Fix PlanDramaCritic : ajout des directives par séquence

## 1. Demande

Run réel (scénario 1998, chapitre "Le retour", appel 026) : `PlanDramaCritic` a signalé en
`DEFAUT_SIGNIFICATIF` la séquence 2 ("les beats se contentent de restituer la directive... rendant
la séquence purement descriptive et statique"). Or la directive de cette séquence demande
explicitement une scène calme de convalescence solitaire (Christelle seule chez elle, béquilles) —
exactement le cas que la propre RULE OF PRIMACY de l'agent est censée exempter ("a calm chapter
the author asked to be calm is never flat").

## 2. Diagnostic

`PlanDramaCriticStep` ne transmettait que `chapter.description()` et `chapter.goal()` (le
chapitre entier), jamais les directives par séquence — contrairement à `PlanGoalCritic`. Le
chapitre entier est cadré à haute intensité (Thierry quitte tout pour rejoindre Christelle) ; sans
jamais voir la directive propre de la séquence 2, l'agent la juge contre l'intensité globale du
chapitre et la trouve statique. La clause de primauté existait dans le prompt mais ne pouvait pas
s'appliquer au bon niveau de granularité, faute de donnée d'entrée.

Confirmé et validé par l'utilisateur, avec demande explicite d'insister dans le prompt sur le fait
que les directives priment toujours.

## 3. Ce qui a été touché

### `agent/plan/dramacritic/PlanDramaCriticInput.java`
Ajout du champ `sequenceDirectives`.

### `orchestrator/plan/PlanDramaCriticStep.java`
Transmet `ScenarioFormatters.sequenceDirectivesBlock(chapter.sequences())` (même helper que
`PlanGoalCriticStep`).

### `agent/plan/dramacritic/PlanDramaCritic.java`
- Slot utilisateur `"Sequence directives"` ajouté (entre description et plan).
- Prompt (`buildSystem`) : delta présenté et validé avant écriture —
  - RULE OF PRIMACY : `"The intended intensity is set by the goal and description, never by
    you."` → `"The intended intensity is set by the goal, the description and each sequence's
    own directive — they always prevail, never you."` + précision "Judge each sequence against
    the intensity ITS OWN directive calls for, not against the chapter's overall level."
  - INPUT : ajout de la ligne `"Sequence directives: what each sequence is meant to deliver —
    may set a calmer or more intense moment than the chapter as a whole."`
  - DEFAUT_SIGNIFICATIF : ajout de la condition `"and that sequence's own directive does not
    call for a calm or transitional moment."`
  - Nouveau COUNTER-EXAMPLE (scène calme correctement non signalée), demandé par la préférence
    du projet pour les exemples avec les petits modèles.
  - Exemple existant traduit en anglais au passage (resté en français par oubli lors de la
    refonte à 5 axes, incohérent avec le reste du prompt).

### `agent/plan/dramacritic/PlanDramaCritic.md`
Section "Historique" complétée d'un paragraphe correctif daté ; section "Ce qu'il vérifie" mise à
jour (DEFAUT_SIGNIFICATIF conditionné à l'absence d'appel au calme dans la directive propre).

## 4. Résultat

`mvn compile` : OK. `mvn test` (tous modules) : **17 (commun) + 5 (testllm) + 35 (redacteur) —
0 échec**.
