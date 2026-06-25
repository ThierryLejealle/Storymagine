# 2026-06-25 18h00 - Affinement prompt ChapterPlanner (beats, exemples)

## Evolution demandee

Affinement du prompt JSON_PLANNER_SYSTEM suite a analyse critique des sorties :
- Regle 3 : preciser "evenements observables" a la place de "comportements"
- Definition du beat : "un seul evenement" + liste ouverte plutot que "uniquement"
- Ajout regle anti-paraphrase
- Verbes-signal : "signal d'alerte + regle d'usage" plutot que "probablement trop abstrait"
- Exemples : remplacement des 8 lignes Bon/Mauvais par 5 paires abstraite → observable

## Ce qui a ete touche

### ChapterPlanner.java — JSON_PLANNER_SYSTEM

Regle 3 :
- Avant : "Traduis-les en comportements, gestes, observations ou dialogues observables."
- Apres : "Traduis-les en evenements observables : gestes, paroles, reactions visibles, details sensoriels."

Definition du beat :
- Avant : "Chaque beat decrit uniquement : une action, une reaction visible, une parole ou une observation sensorielle"
- Apres : "Chaque beat decrit un seul evenement. Il peut s'agir d'une action ; d'une reaction visible ; d'une parole ; d'une observation sensorielle."
  Raison : "uniquement" pouvait etre interprete comme "une seule phrase".

Regle anti-paraphrase (nouvelle) :
- "Chaque beat doit apporter une information nouvelle — ne decoupe pas une meme action en plusieurs beats."

Verbes-signal :
- Avant : "si un beat en contient un, il est probablement trop abstrait."
- Apres : "Ces verbes sont un signal d'alerte — utilise-les uniquement lorsqu'ils decrivent un fait concret, jamais une interpretation de la scene."
  Raison : certains usages sont valides ("Le soleil devient plus bas."), la regle doit cibler l'usage, pas le verbe.

Exemples :
- Avant : 2 Bon + 6 Mauvais en lignes separees
- Apres : 5 paires "abstrait → observable" au format texte (evite le rendu markdown)
  "Le silence s'installe." → "Personne ne parle pendant plusieurs secondes."
  "Une proximite nait entre eux." → "Leurs epaules se rapprochent legerement."
  "La tension monte." → "Maya hesite avant de repondre."
  "Le courant passe." → "Ils se sourient presque en meme temps."
  "Une coexistence silencieuse s'etablit." → "Eddie tourne une page pendant que Maya regarde les champs."

## Resultat

Compilation propre. Prompt plus concis (8 lignes d'exemples → 5), regles plus precises et moins susceptibles de faux positifs (verbes-signal).
