---
date: 2026-06-24 20h45
sujet: Fix affichage problemes critic - condition score >= 10
---

## Évolution demandée

BUG : les problèmes des critics (ex. PlanNarrativeCritic) étaient affichés
dans le log même quand la note était inférieure à 10.
La règle correcte : n'afficher les lignes ! problème que si score == 10 ET problems non vide
(cas anomalie : le LLM donne un score parfait mais retourne quand même des remarques).

## Ce qui a été touché

- commun/src/main/java/storymagine/commun/infra/ConsoleLogAdapter.java
  méthode critic() : ajout d'une condition score >= 10.0 avant l'impression des lignes ! p

## Résultat

Quand score < 10 avec problems non vides : seule la ligne de score est affichée (pas de ! lines).
Quand score == 10 avec problems non vides : les ! lines s'affichent (anomalie LLM signalée).
Quand score == 10 sans problems : tag OK affiché comme avant.