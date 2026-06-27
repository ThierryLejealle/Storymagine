# 2026-06-27 12h30 — SequencePlanFidelityCritic : intention narrative vs mots exacts

## Problème détecté
Le SequencePlanFidelityCritic signalait MANQUANT des beats présents dans le texte
mais formulés différemment. Le modèle comparait mot à mot au lieu de chercher
si l'intention du beat était couverte.

Exemple observé : beat "Leurs regards se croisent un instant tandis que Thierry cherche
sa place sur la ligne" → présent dans le texte comme "Leurs regards se croisèrent un
bref instant. Il cherche ses mots..." → signalé MANQUANT.

## Ce qui a été touché
- `redacteur/.../agent/writer/planfidelitycritic/PlanFidelityCritic.java`
  Modification de la règle principale :
  AVANT : "Un beat est exploite s'il est developpe dans le texte — une allusion fugace ne compte pas."
  APRÈS : "Un beat est exploite si son intention narrative est developpee dans le texte —
           cherche l'essence du beat, pas ses mots exacts. Une allusion fugace ne compte pas."

## Résultat
Le Critic évalue désormais si l'intention narrative du beat est présente,
indépendamment de la formulation exacte.
