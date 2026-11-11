# 2026-11-11 - Numéros de fichiers de scénario décimaux

## Évolution demandée
- Pouvoir couper un fichier de scénario ou en insérer un entre deux autres sans renommer les suivants :
  `scenario-1.1.txt` entre 1 et 2, `scenario-3.1.2.txt` entre 3.1.1 et 3.1.3, `1` et `1.1` côte à côte,
  `1.1.1` entre 1.1 et 2.
- Recommander aux rédacteurs cette numérotation hiérarchique, pour garder des fichiers petits.

## Ce qui a été touché
- `ScenarioFiles` (infra, nouveau) : liste `scenario.txt` puis tous les `scenario-NUMÉRO.txt` du dossier,
  rangés par numéro décimal (comparaison de `ActNumber` : un numéro avant ses enfants, segments comparés
  comme des nombres, donc `1.2` avant `1.10`). Trous permis. Deux fichiers de même numéro (`1` et `01`) :
  erreur explicite.
- `ChatFileStorageAdapter` : `scenarioTextFiles` et `appendContinuationFiles` passent par `ScenarioFiles`
  (chargement, validation et copie des fichiers résolus d'une partie, qui gardent les noms des sources).
  La validation et le listage des scénarios rapportent un doublon de numéro au lieu de planter.
- `LegacyLayout` : un fichier `scenario-1.1.txt` n'est pas pris pour une fiche de personnage de l'ancien format.
- Comportement supprimé : le chargement s'arrêtait au premier numéro manquant et ignorait silencieusement les
  fichiers suivants. Les scénarios existants (`scenario-1.txt`, `scenario-2.txt`…) se lisent dans le même ordre.
- Tests (`ChatFileStorageAdapterTest`) : un trou ne perd plus de fichier, ordre décimal complet
  (1 < 1.1 < 1.1.1 < 1.2 < 1.10 < 2 < 3.1.1 < 3.1.2 < 4) quel que soit l'ordre d'écriture, doublon refusé et
  signalé par la validation.
- Documentation d'auteur, mise à jour ensemble : guide (§1, §2.3, §7.3), skill `chat-scenario-writing`
  (règle 7), `chat/scenario-builder/` (`ref-format.md`, `phase-4-steps.md`, `README.md`), `specs/chat.md`.

## Résultat
Compilation et 658 tests du module chat verts. Les numéros d'actes restent calculés à partir des titres : le
numéro d'un fichier ne donne que l'ordre de lecture. On coupe toujours au niveau `#` (un fichier de suite
commence par un titre de premier niveau). Aucun prompt modifié.
