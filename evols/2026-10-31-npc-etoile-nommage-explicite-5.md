# 2026-10-31 - Mot-clé `*` dans les commandes npc

## Évolution demandée
`{npc-active *}` (et `present`, `passive`, `absent`) vise tous les PNJ d'un coup, par exemple pour
remettre tout le monde en actif. Cela revient à un nommage **explicite** de chaque PNJ. L'étoile (et non
`all`) évite toute confusion avec un nom de personnage.

## Ce qui a été touché
- `ActivityDirective.isAll()` et la constante `ALL` (`*`).
- `ActivityResolver` : `*` donne à tous les PNJ le niveau en statut explicite (donc une commande sans nom
  ne les touche plus) ; un nom du même titre est plus précis et l'emporte quel que soit l'ordre
  d'écriture ; deux `*` dans un titre ou `*` mêlé à des noms sont refusés avec un message clair.
- `ScenarioValidator` : `*` n'est plus cherché comme un nom de personnage.
- Tests `ActivityResolverTest` et `ActivityDirectiveMarkerTest`, guide §2.11, `SKILL.md`, `specs/chat.md`.
- Aucun prompt modifié.

## Résultat
`{npc-active *}` remet tous les PNJ en actif, y compris ceux qu'un titre parent avait rendus absents.
`{npc-active *}` + `{npc-absent Toto}` donne tous actifs sauf Toto absent.
