# 2026-06-23 09h10 — Fix désérialisation lore YAML tableau

## Evolution demandée
Crash à la lecture des scénarios : `SequenceDto.lore` déclaré `String`
mais les chapitres utilisent `lore: ["PLAGE"]` (liste YAML).

## Cause
Jackson YAML parse `["PLAGE"]` comme un `JsonToken.START_ARRAY`.
Tentative de désérialisation vers `String` → `MismatchedInputException`.

## Ce qui a été touché

**`dto/LoreStringDeserializer.java`** (nouveau)
Désérialiseur custom : si le token est un tableau, reconstitue la chaîne
`["TAG1"]\n["TAG2"]` attendue par `LoreItemParser`.
Si le token est une chaîne, la retourne telle quelle.

**`dto/SequenceDto.java`**
Annotation `@JsonDeserialize(using = LoreStringDeserializer.class)` sur le champ `lore`.

**`dto/ChapterDefaultsDto.java`**
Même annotation sur le champ `lore`.

## Résultat
Les deux formats YAML sont acceptés sans modifier les fichiers scénario :
- `lore: ["TAG"]` (tableau YAML) → converti en `["TAG"]` pour `LoreItemParser`
- `lore: > ...` (chaîne multi-ligne) → transmis tel quel
