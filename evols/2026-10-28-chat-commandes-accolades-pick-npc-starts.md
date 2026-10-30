# 2026-10-28 - Chat : commandes de scénario en accolades, pick random/unique/all/N, npc-starts

## 1. Évolution demandée

Séparer les commandes de l'auteur du texte partagé : `[PICK: x]`, `[PICK-UNIQUE: x]` et `[NPC STARTS]`
passent en accolades. Grammaire `{commande sous-type argument}`, sous-type obligatoire pour `pick`
(`random`, `unique`, `all`, un nombre N). `npc-starts` : `{npc-starts}` ou `{npc-starts Nom}`. Les
narrations `[...]` et `[NEXT ACT]` ne changent pas. Tableau des tirages : une ligne par commande
(Fichier, Type, Valeur).

## 2. Ce qui a été touché

- Domaine `pick` : `PickMode` (random / unique / all / N), `PickDraw` (mode, `PickTag`, paragraphes),
  `PickResolver` réécrit (commande mal formée = erreur, jamais laissée dans le texte ; `all` et `N`
  comptent comme « déjà tirés » pour un `unique` ultérieur ; N supérieur à la taille = erreur).
- `NpcOpeningMarker` : `{npc-starts}` / `{npc-starts Nom}`, seul sur sa ligne, casse ignorée.
- `LegacyCommandSyntax` : l'ancienne écriture en crochets est refusée au chargement avec un message
  qui renvoie à `migration-syntaxe.bat` (sinon elle serait affichée au joueur comme narration).
- `PickDrawsFile` : format `@@ mode fichier` + paragraphes séparés par `@@ --`.
- Web : le tableau 🎲 a les colonnes Fichier, Type, Valeur, une ligne par commande.
- Tests : `PickResolverTest`, `PickDrawsFileTest`, `NpcOpeningMarkerTest`, `LegacyCommandSyntaxTest`,
  tests de chargement migrés vers la nouvelle syntaxe.
- Aucun prompt modifié. Les scénarios du dépôt (`chatscenarios/`) utilisent encore l'ancienne syntaxe
  tant que la migration n'a pas été lancée (étape suivante).

## 3. Résultat

`mvn clean test` à la racine : tous les tests passent.
