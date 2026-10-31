# demo-repeat : le ménage du samedi

Montre `{repeat mode nom}` : un gabarit de scène écrit une fois, déroulé une fois par paragraphe de `picks/taches.txt`.

- `repeat random 3 taches` : 3 tâches différentes tirées parmi 5 (essayer `order` pour garder l'ordre du fichier, ou retirer le 3 pour tout jouer).
- `n` : numéro de la tâche (1, 2, 3).
- `item1`, `item2`, `item3` : les trois morceaux de chaque paragraphe, séparés par une ligne `-` (description, mise en situation, condition de fin).

À regarder : 🎲 montre quelles tâches sont sorties, et le scénario joué n'a plus aucune commande `repeat`.
Guide : §2.6 bis.
