# 2026-06-27 10h00 - Writer : warn sur troncature des slots contexte

## Évolution demandée
Ajouter un log warn quand un champ du prompt Writer est tronqué parce qu'il dépasse son slot de contexte.

## Ce qui a été touché
- `Writer.java` : injection de `LogPort`, méthode `trunc` transformée en méthode d'instance avec paramètre `section`, log warn quand `s.length() > maxChars`, suppression du `FIXME` styleGuide
- `RedacteurModule.java` : passage de `log` au constructeur de `Writer`

## Résultat
Tout débordement de slot produit désormais un warn : `Writer: troncature [<section>] — <taille> chars > slot <max>`

---

# 2026-06-27 10h30 - Writer : consigne anti-IA dans le prompt système

## Évolution demandée
Ajouter en dur dans le prompt système une consigne pour éviter que le texte sonne comme rédigé par une IA.

## Ce qui a été touché
- `Writer.java` : ajout en fin de `buildSystem`, après les règles sur les fiches personnages

## Delta
Ajout :
> Sauf si le guide de style en décide autrement : le texte ne doit pas donner l'impression d'avoir été rédigé par une IA. Privilégie un vocabulaire et des tournures usuels. Ne cherche pas à épater ni à avoir un style trop original — évite les formulations qui sonnent artificiel.

## Résultat
Instruction active par défaut, subordonnée au guide de style explicite si fourni.
