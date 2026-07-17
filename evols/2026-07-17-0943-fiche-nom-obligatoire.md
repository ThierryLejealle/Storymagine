# 2026-07-17 09h43 - "# Nom" obligatoire pour qu'un .txt soit chargé comme PNJ

## 1. Demande

En examinant `chatscenarios/temple-noir-actes/character.txt` : "Bon déjà ça charge n'importe quel
fichier .txt comme un perso. Ça craint." Confirmé : "on va mettre la règle # Nom."

## 2. Diagnostic

`ChatFileStorageAdapter.loadCast` chargeait tout fichier `.txt` d'un dossier de scénario SAUF 4
noms réservés (`scenario.txt`, `act.txt`, `present.txt`, `interject.txt`) — sans aucune autre
validation. Un fichier `.txt` égaré (note, brouillon, sauvegarde manuelle, fichier mal nommé)
devenait donc silencieusement un PNJ fantôme, avec son id de fichier comme nom d'affichage, sans
avertissement ni moyen simple de s'en rendre compte.

## 3. Ce qui a été touché

`ChatFileStorageAdapter.loadCast` : nouveau filtre `hasNameHeading` — un `.txt` n'est chargé comme
PNJ que si sa première ligne (non vide) commence par `"# "`. Sinon, ignoré silencieusement (pas
d'erreur : un dossier de scénario contenant des notes de travail reste utilisable). Extraction
commune `firstLine(content)` partagée avec `extractCharacterName` (déduplication).

`chatscenarios/rules.md` (§3) : "# Nom" passe d'optionnel à obligatoire dans la documentation.

Vérifié par grep : aucune fiche existante dans le projet n'est concernée (toutes ont déjà une
ligne `# Nom`), donc aucune migration de contenu nécessaire.

## 4. Tests

Nouveau `ChatFileStorageAdapterTest.loadScenarioIgnoresATxtFileWithNoNameHeadingInsteadOfLoadingItAsANamelessNpc`.

Fixtures de test mises à jour (plusieurs `character.txt`/`sheet` de test n'avaient pas de `# Nom`,
utilisées par convention de brièveté) : `ChatServiceImplTest.writeScenarioFiles` et
`ChatFileStorageAdapterTest.writeScenarioFiles` préfixent désormais leur contenu avec `"# Character\n"`.
Une assertion de contenu exact (`loadScenarioReadsBothFilesVerbatim`) ajustée en conséquence.

`mvn -pl chat -am clean test` : 207 tests (+1), tous verts. `mvn compile` (racine) : aucune
régression cross-module.

## 5. Diagnostic du scénario externe qui ne progresse pas (en parallèle, non résolu)

Contenu du scénario cassé fourni par l'utilisateur testé via un dossier temporaire jetable dans
`chatscenarios/` (supprimé immédiatement après, jamais committé — voir rappel utilisateur explicite
sur ce point). Résultat : ce contenu exact charge et progresse CORRECTEMENT (3 actes détectés, un
seul beat affiché à l'ouverture). Le bug rapporté ("toutes les sections d'un coup, aucun appel LLM")
ne vient donc pas de la structure `#`/`##` en tant que telle — cause encore à déterminer (état de
session existant avec un `act.txt` obsolète ? différence entre le contenu collé et le fichier réel
sur disque ?). En attente d'éléments supplémentaires de l'utilisateur.
