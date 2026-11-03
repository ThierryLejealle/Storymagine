# demo-cast : l'atelier de cuisine

Montre les variables de casting : `{cast NOM …}` choisit un personnage, `%NOM%` le désigne ensuite dans le texte et
dans les commandes. À chaque partie le tirage est différent.

- `{repeat order cast CHEF}` fait une copie du bloc par personnage (trois copies ici). Dans chaque copie, `%CHEF%` est le
  personnage de la copie.
- `{cast COMMIS}` choisit un autre personnage, **jamais le chef de la copie** : par défaut un `cast` ne reprend pas un
  personnage que tient une autre variable. Pour le permettre, écrire `reuse-possible`.
- `{npc-starts %CHEF%}` : la variable marche aussi dans les commandes.
- `{cast PATISSIER (mia, john)}` limite le tirage à une liste (un seul mot par personnage). Sans liste, tout le casting.
- Mot clé `unique` (non utilisé ici) : la variable ne reprend jamais un personnage qu'elle a déjà eu, par exemple pour
  avoir un personnage différent à chaque copie d'un `{repeat 3}`.

À regarder : le tableau des tirages (🎲) de la partie, et que le commis n'est jamais le chef.
Guide : §2.6 ter.
