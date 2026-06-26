# 2026-06-26 10h30 - Focus : distinction TAG vs chaîne libre

## Evolution demandée
Les champs focus (et lore) dans les chapter.yaml peuvent contenir soit un TAG ([TOTO] ou ["TOTO"])
à résoudre dans le pool, soit une chaîne libre utilisée directement comme texte inline.
Le code traitait tout comme un TAG → NPE si non trouvé dans le pool.

## Ce qui a été touché

- `ScenarioFileAdapter.resolveFocusItems()` — reécrit : détecte le format `[TAG]`/`["TAG"]`
  via `extractTag()` → crée `FocusRef` si tag, `FocusInline` (sans guillemets parasites) sinon
- `ScenarioFileAdapter.extractTag()` — nouvelle méthode privée statique partagée par la résolution
  et la validation
- `ScenarioFileAdapter.validateRefs()` — ne vérifie focus contre le pool que pour les entrées
  au format TAG ; les chaînes libres sont acceptées sans contrôle

## Résultat
Une séquence avec `focus: "texte libre"` crée un `FocusInline` sans chercher dans le pool.
Un TAG inexistant `[TAG_INCONNU]` est toujours détecté à la validation.
