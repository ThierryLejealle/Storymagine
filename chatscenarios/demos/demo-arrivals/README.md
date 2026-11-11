# demo-arrivals : arrivées et départs de personnages

Montre comment un personnage n'entre dans l'histoire qu'à un acte précis, et comment créer des figurants à partir d'un modèle de fiche.

- En-tête : `{npc-only Elena}`. Au départ, seule Elena est dans l'histoire. Orlo n'existe pas encore (ni vignette, ni fiche chez les autres).
- Acte « Le marché » : `{npc-arrives Orlo}` le fait entrer, `{npc-present Orlo}` règle son rôle.
- Acte « L'embuscade » : `{npc-leaves Orlo}` le retire ; `{npc-arrives gobelin Grik, Snak}` crée deux gobelins à partir de `characters/gobelin.txt`
  (un fichier utilisé avec un nom est un modèle : il n'apparaît pas lui-même). `%NPC%` dans le modèle devient le nom donné.
  L'hostilité vient de `{npc-delta}`, pas du modèle.
- Acte « Fin de route » : `{npc-leaves Grik, Snak}`.

À regarder : les vignettes changent d'un acte à l'autre. Un personnage parti reste dans l'historique : les autres s'en souviennent.
Limite : un personnage qui arrive connaît tout l'historique, y compris ce qui s'est dit avant son arrivée.
Guide : §2.13.
