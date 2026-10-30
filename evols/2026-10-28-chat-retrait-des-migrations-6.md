# 2026-10-28 - Chat : retrait des programmes de migration (toutes les migrations ont été faites)

## 1. Évolution demandée

Les migrations (structure, langue, syntaxe des commandes) ont toutes été utilisées. Supprimer les `.bat`
et les programmes de migration, et retirer toute mention dans le code, les messages, le guide et les specs.

## 2. Ce qui a été touché

- Supprimés : `migration.bat`, `migration-langue.bat`, `migration-syntaxe.bat` ; `ScenarioLayoutMigrator`,
  `ScenarioLanguageMigrator`, `ScenarioSyntaxMigrator` ; `MigrateScenarioLayoutCli`,
  `MigrateScenarioLanguageCli`, `MigrateScenarioSyntaxCli` ; leurs trois classes de test. Les migrateurs de
  langue et de syntaxe n'avaient jamais été commités (non récupérables dans git) ; ceux de structure le
  sont (historique git). `ScenarioHeader.withLanguage`, sans plus d'appelant, est retiré.
- Conservé : le refus d'un scénario à l'ancienne structure (`LegacyLayout`) et d'une ancienne syntaxe de
  commande (`LegacyCommandSyntax`), qui protègent contre un affichage involontaire au joueur ; leurs
  messages ne parlent plus de migration (« format non pris en charge », renvoi vers le guide).
- Docs : `guide-redaction-chat.md` (§2.1 langue, §2.9, §6.10 réécrit en « Ancien format »),
  `specs/chat.md` (section « Ancienne structure », liste des CLI, langue, ancienne syntaxe), `specs/todo.md`.
  Les anciennes fiches `evols/` restent telles quelles (historique).
- Tests adaptés : `LegacyCommandSyntaxTest`, `ChatFileStorageAdapterTest`.

## 3. Résultat

`mvn clean test` : tous les tests passent.
