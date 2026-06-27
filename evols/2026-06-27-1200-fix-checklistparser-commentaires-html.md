# 2026-06-27 12h00 — Fix CheckListParser : les commentaires HTML étaient chargés comme contraintes

## Problème détecté
Le fichier constraints.md contient un bloc de documentation HTML (<!-- ... -->) avec
des exemples de contraintes. Le parser lisait ce bloc ligne par ligne sans l'ignorer,
traitant les exemples comme de vraies contraintes actives.

Conséquence : les contraintes "Le personnage principal ne sourit jamais",
"Interdiction d'écrire il sentit que" et "Les lieux ne sont jamais nommés"
étaient injectées dans le Writer et le ChapterCoherenceCritic alors que les
sections ## PLAN et ## RÉDACTION réelles étaient vides.
Le --> final s'accrochait même à la dernière contrainte.

## Ce qui a été touché
- `redacteur/.../infra/scenario/CheckListParser.java`
  Ajout d'un strip des commentaires HTML avant le parsing :
  `content = content.replaceAll("(?s)<!--.*?-->", "");`

## Résultat
Seules les contraintes définies dans les sections ## PLAN et ## RÉDACTION
sont désormais chargées. Les blocs de documentation sont ignorés.
