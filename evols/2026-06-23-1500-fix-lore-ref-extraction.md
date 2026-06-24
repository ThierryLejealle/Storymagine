# 2026-06-23 15h00 — Fix résolution des références lore dans les chapitres

## Description
Le lore déclaré sous forme de tableau YAML (`lore: [TRAIN]`) dans les `defaults` ou
les séquences d'un chapitre n'était pas résolu correctement : au lieu d'injecter
le contenu du lore.md, le prompt recevait la chaîne brute `["TRAIN"]`.

## Cause racine
`LoreItemParser.extractTag` contenait une ligne `t = t.substring(1)` censée normaliser
`["TRAIN"]` en `[TRAIN]`, mais qui enlève le `[` de tête — rendant le check suivant
`t.startsWith("[")` toujours faux. Le token tombait alors dans la branche `LoreInline`
avec le texte `TRAIN]` (ou similaire) au lieu d'être résolu comme `LoreRef`.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/infra/scenario/LoreItemParser.java`
  — suppression de la ligne fautive dans `extractTag` : la méthode traite désormais
  directement `["TAG"]` et `[TAG]` via `replaceAll("\"", "")` sur le contenu entre `[` et `]`.
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/orchestrator/common/ScenarioFormatters.java`
  — ajout d'un guard `if (el == null) yield ""` dans `loreText()` pour éviter une NPE
  si un tag référencé n'existe pas dans le pool.
- `redacteur/src/test/java/storymagine/redacteur/infra/scenario/LoreItemParserTest.java`
  — nouveau test unitaire couvrant les quatre cas : `["TAG"]`, `[TAG]`, texte inline, tag inconnu.

## Résultat
`lore: [TRAIN]` dans un chapitre injecte maintenant le contenu du bloc `[TRAIN]`
de `lore.md` dans la section "Informations de référence" du prompt.
