# 2026-11-07 - Avertissement : fiche de personnage ignorée faute de `# Nom`

## Évolution demandée
Avertir quand une fiche n'a pas de ligne `# Nom`. Constat à l'analyse : une telle fiche n'est pas un PNJ sans nom,
elle n'est tout simplement pas chargée (`loadSheets` ne garde que les fichiers dont la première ligne est `# `).
Le piège réel est donc la disparition silencieuse du personnage.

## Ce qui a été touché
- `ChatFileStorageAdapter.validateScenario` : un avertissement par fichier `.txt` de `characters/` ignoré
  (`ignoredSheetWarnings`).
- Test `ActivityScenarioLoadingTest.aFileOfCharactersWithoutNameHeadingIsReportedAsIgnored`.
- Guide (§ structure des fichiers), `chat/scenario-builder/ref-format.md` et `phase-5-finish.md`.

## Résultat
Tests verts. Le skill `chat-scenario-writing` et `specs/chat.md` ne décrivent pas ce détail et sont inchangés.
