# 2026-06-24 14h35 — PlanCoherenceCritic : fiches personnage + sections optionnelles

## Évolution demandée
Passer les fiches personnage du chapitre au PlanCoherenceCritic (somme sans doublon
des personnages des defaults + toutes les séquences). Rendre toutes les sections du
prompt optionnelles (affichées uniquement si non vides).

## Ce qui a été touché

### ScenarioFormatters.java
- Ajout de `planCharactersText(Chapter chapter)` : merge defaults + toutes séquences
  par id (putIfAbsent — defaults prioritaires), formate via `personnages(list, false)`
  (global + planContent)

### PlanCoherenceCriticInput.java
- Ajout du champ `String characters` (entre `sequenceChecks` et `focusText`)

### PlanCoherenceCriticStep.java
- Passage de `ScenarioFormatters.planCharactersText(chapter)` au nouveau champ

### PlanCoherenceCritic.java — delta prompt
- Toutes les sections du user prompt rendues conditionnelles (affichées si non vides) :
  objectif, points à vérifier, contraintes, focus — seules `seqChecks` l'était déjà
- Ajout de la section `### Fiches personnage` (conditionnelle) entre objectif et checks
- Même budget ctx * 4 / 8 que les autres blocs de référence

## Résultat
Compilation OK. Le PlanCoherenceCritic reçoit maintenant les fiches personnage
dédupliquées du chapitre et n'affiche plus les sections vides dans le prompt.
