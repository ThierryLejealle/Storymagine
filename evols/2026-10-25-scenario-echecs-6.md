# 2026-10-25 - Création du scénario "echecs"

## 1. Évolution demandée

Un scénario de test simple : Alex (joueur) et son ami Franck (PNJ) jouent aux échecs. Franck a les
blancs et commence (`[NPC STARTS: Franck]`). Objectif : voir si le LLM sait suivre une partie d'échecs.
Nom du scénario : `echecs`.

## 2. Ce qui a été touché

- `chatscenarios/echecs/scenario.txt` : pas d'actes, joueur Alex, prémisse, ton, règles de la partie
  (notation algébrique, un coup légal par tour, position gardée en mémoire), marqueur
  `[NPC STARTS: Franck]` et un bloc d'ouverture à la 3e personne.
- `chatscenarios/echecs/characters/franck.txt` : fiche publique (ami depuis le lycée, joueur offensif,
  pince-sans-rire) et secret (a perdu les trois dernières parties, a révisé ses ouvertures).
- Aucun code, aucun prompt d'agent touché.

## 3. Résultat

Le scénario se charge (1 PNJ, 0 acte, ouverture `Franck` en attente, marqueur retiré du texte, bloc
d'ouverture affiché). Non joué avec un vrai LLM. À surveiller : la dérive de la position après 10 à
15 coups (la compaction résume les coups anciens), et la notation en français (C, F, T, D, R).
