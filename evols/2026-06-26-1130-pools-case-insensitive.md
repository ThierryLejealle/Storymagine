# 2026-06-26 - Matching insensible à la casse dans les pools

## Evolution demandée
Corriger les erreurs de validation du scénario 1998 dues à des différences de casse
entre les références dans les YAML de chapitres et les identifiants dans les pools.

## Ce qui a été touché

### Bug 1 - Casse des personnages / focus / lore (16 erreurs)
- PersonnagePool.find() : .equals() → .equalsIgnoreCase()
- LorePool.find() : .equals() → .equalsIgnoreCase()
- FocusPool.find() : .equals() → .equalsIgnoreCase()

Cause : fichiers nommés en minuscules (christelle.md → id christelle)
mais YAML référençant "Christelle" (majuscule).

### Bug 2 - Lore multi-tags sur une ligne (1 erreur chap_3)
- LoreItemParser : extractTag() → extractTags() (retourne une liste)
  - Supporte désormais ["UCPA", "COLIOURE"] sur une seule ligne
  - Split sur la virgule à l'intérieur du [...]

Cause : ["UCPA", "COLIOURE"] était interprété comme un tag unique UCPA, COLIOURE.

## Résultat
Compilation OK. Les 17 erreurs de validation du scénario 1998 doivent être résolues.