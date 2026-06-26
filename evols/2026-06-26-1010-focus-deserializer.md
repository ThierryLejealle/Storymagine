# 2026-06-26 10h10 - FocusListDeserializer : robustesse du champ focus

## Evolution demandée
Le champ `focus` d'une séquence YAML peut contenir une chaîne simple, une liste de chaînes, ou un mélange de tags pool et de chaînes libres. Jackson échouait quand la valeur était une chaîne simple au lieu d'une liste.

## Ce qui a été touché

- `redacteur/src/main/java/.../dto/FocusListDeserializer.java` — nouveau deserializer ciblé : accepte chaîne simple ou liste, filtre les blancs
- `SequenceDto.java` — annotation `@JsonDeserialize(using = FocusListDeserializer.class)` sur le champ `focus`
- `ChapterDefaultsDto.java` — même annotation sur `focus`
- `ScenarioFileAdapter.java` — retrait du flag global `ACCEPT_SINGLE_VALUE_AS_ARRAY` (trop large, remplacé par le deserializer ciblé)

## Résultat
Le champ `focus` accepte désormais toutes les formes valides : chaîne seule, liste, mélange tags/texte libre. Les entrées vides ou blanches sont filtrées silencieusement.
