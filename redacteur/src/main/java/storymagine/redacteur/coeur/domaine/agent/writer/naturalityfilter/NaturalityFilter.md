# NaturalityFilter

## Rôle
Détecte les phrases et tournures typiques de prose générée par un LLM dans un texte de chapitre,
et propose une réécriture plus naturelle pour chacune.

Appliqué en post-traitement par substitution directe (comme `Proofreader`), sur le texte final
du chapitre après la boucle de retry des critics.

## Ce qu'il détecte (par ordre de priorité)
- **Sur-interprétation narrative** : le texte attribue une signification, une intention ou
  une fonction à un geste alors que le geste seul suffirait
- **Phrases de synthèse** : phrases qui résument ou théorisent la situation au lieu de montrer
- **Conclusions narratorielles** : phrases qui expliquent au lecteur ce qu'il doit comprendre
  au lieu de le laisser l'inférer
- **Langage conceptuel** : concepts abstraits là où un geste ou une perception concrète suffirait
- **Groupes nominaux artificiels** : nominalisation excessive ("le déploiement réfléchi du foulard")
- **Vocabulaire de planification/optimisation** dans une narration ordinaire

## Ce qu'il ne signale pas
- Formulations abstraites appartenant clairement au point de vue d'un personnage
- Phrases littéraires, imagées ou évocatrices — seulement les formulations surconstruites

## Format de sortie
Un bloc répété par finding :
```
EXTRAIT :
[citation exacte]

PROBLEME :
[description du défaut]

SUGGESTION :
[réécriture naturelle]
```
Ou `[RIEN]` si aucun passage artificiel.

## Intégration dans le workflow
Runs quand `qualityLevel.runsProofreader()` est vrai (SIMPLE et FULL, pas BROUILLON).
Chaque `citation` est remplacée par sa `suggestion` via `String.replace()` dans chaque sequence.

## Slots
- text : 60% du contexte
