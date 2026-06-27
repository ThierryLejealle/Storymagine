# 2026-06-27 11h00 — NaturalityCorrector : règle nom propre auto-référentiel

## Evolution demandée
Ajouter une règle et un exemple dans le prompt du NaturalityCorrector pour détecter
l'utilisation du nom propre d'un personnage-focalisateur à la place d'un pronom,
dans sa propre perspective — artefact typique des petits modèles LLM.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/writer/naturalitycorrector/NaturalityCorrector.java`
  Ajout d'un item dans la section "Cherche en priorite" du prompt système :
  "nom propre auto-referentiel"

## Résultat
Le NaturalityCorrector détecte désormais les cas où le LLM-rédacteur désigne
un personnage par son propre prénom alors qu'un pronom suffirait.

Exemple détecté :
  FAUX : "Elle le voyait étudier attentivement les blessures de la main de Lexy."
         (Lexy est le point de vue de la scène — cf. phrase précédente)
  JUSTE : "Elle le voyait étudier attentivement les blessures de sa main."
