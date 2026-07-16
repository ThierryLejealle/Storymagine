# Règle d'écriture des scénarios : toujours à la 3e personne

La Prémisse et les blocs `[...]` (beats de narration) d'un `scenario.txt` sont transmis TELS
QUELS dans le system prompt de CHAQUE personnage (voir `ChatPromptBuilder.build()` et le
javadoc de `Teaser.java`) — pas seulement affichés au joueur. Un PNJ à qui on vient de dire
"YOUR CHARACTER: Sheera" lit ensuite, dans le même prompt, la Prémisse et l'acte en cours.

**Ne jamais écrire ces blocs à la 2e personne** ("Tu es Alex...", "Toi et Sylka...", "vous
venez de..."). Le modèle sait qu'il n'est pas Alex : lui dire "tu es Alex" dans son propre
prompt contredit frontalement son assignation de personnage et peut casser le jeu de rôle.

**Toujours décrire la situation de l'extérieur, comme un narrateur** : "Alex arrive à Ambaya...",
"Alex et Sylka viennent de franchir la porte...". Ça fonctionne aussi bien pour l'affichage au
joueur (narration à la 3e personne, cinématographique) que pour le contexte donné à chaque PNJ.

Les sections "Point de départ", "Ce qui doit se passer" et "Quand écrire [NEXT ACT]" suivent déjà
cette convention (elles parlent de "le joueur" ou du prénom du joueur) — les blocs `[...]` et la
Prémisse doivent faire pareil.

Voir aussi `rules.md` (racine de ce répertoire) pour l'ensemble des règles de scénario, personnage
et mécanismes du jeu.
