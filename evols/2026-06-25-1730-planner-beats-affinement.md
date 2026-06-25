# 2026-06-25 17h30 - Affinement prompt ChapterPlanner et formule beats

## Evolution demandee

Suite a une analyse des sorties reelles du planner, 7 ameliorations validees :

A. Regle 2 : "au-dela de la consigne" → "en developpant les evenements deja presents", "majeur" → "significatif"
B. Nouvelle regle 3 : interdiction de recopier les abstractions des instructions comme beats
C. Definition du beat : "reaction visible" + "observation sensorielle" (supprimer ambiguite)
D. Test camera : "sans narration explicative" (plus precis)
E. Verbes-signal d'abstraction : liste de 9 verbes typiques LLM
F. 3 exemples mauvais supplementaires (personnification abstraite d'etat)
G. Formule beats : `base + round(mots/ratio)` au lieu de `round(mots/ratio)` seul

## Ce qui a ete touche

### ChapterPlanner.java — JSON_PLANNER_SYSTEM
- Regle 2 : "Enrichis en developpant les evenements deja presents [...] N'ajoute pas de nouvel evenement significatif."
- Regle 3 (nouvelle) : "Les elements abstraits fournis dans les instructions [...] ne les recopie jamais en beat."
- Beat : "reaction visible", "observation sensorielle"
- Test camera : "une camera doit pouvoir montrer le beat sans narration explicative"
- Ajout section verbes-signal : devenir, symboliser, representer, exprimer, illustrer, traduire, incarner, installer, etablir
- 3 mauvais exemples ajoutes : "Le silence s'installe...", "Une coexistence silencieuse...", "Le compartiment retrouve son calme."

### BeatsConfig.java
- Ajout champ `beatsBase` (offset constant ajoute au nombre cible)
- `defaults()` → `BeatsConfig(2, 75, 20)` (base=2, ratio=75, tolerance=20%)

### redacteur.properties
- `beats.base=2`
- `beats.per.words.ratio=75` (etait 80)

### ChapterPlannerStep.java
- Formule : `targetBeats = beatsConfig.beatsBase() + max(1, round(mots / ratio))`

### RedacteurCli.java
- Lecture `beats.base` depuis proprietes

## Resultat

Pour 300 mots : [5-7 beats] (etait [3-5]).
Pour 500 mots : [7-11 beats].
Compilation propre.
