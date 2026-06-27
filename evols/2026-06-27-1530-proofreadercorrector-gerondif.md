# 2026-06-27 15h30 - ProofreaderCorrector : règle gérondif mal attribué

## Evolution demandée
Ajouter une règle explicite pour les gérondifs dont le sujet implicite ne correspond pas
au sujet de la principale — faute grammaticale classique non couverte.

Exemple déclencheur : "Marie rentra dans la pièce. En approchant de la chaise de Fred, il se redressa."
→ "En approchant" est grammaticalement rattaché à "il", mais c'est Marie qui approche.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/writer/proofreadercorrector/ProofreaderCorrector.java`
  - Ajout de la règle "gérondif mal attribué" dans la liste des fautes détectées
  - Ajout d'un second exemple illustrant ce cas

## Résultat
Le prompt liste explicitement le cas du gérondif mal attribué avec un exemple concret,
ce qui permet aux petits modèles de reconnaître et corriger ce pattern.
