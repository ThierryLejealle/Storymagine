# 2026-10-28 - Chat : migration de la syntaxe des commandes, documentation et guide

## 1. Évolution demandée

Un `.bat` de migration des scénarios existants vers les commandes en accolades, et la mise à jour de la
documentation et du guide de rédaction. Les parties en cours sont jetées (pas de migration de `run/`).

## 2. Ce qui a été touché

- `ScenarioSyntaxMigrator` (plan puis exécution, copie `.bak`, fins de ligne conservées, idempotent),
  `MigrateScenarioSyntaxCli` (`--dry-run`, confirmation) et `migration-syntaxe.bat` (CP1252). Une mention
  d'un ancien `[NPC STARTS]` au milieu d'une phrase devient le mot `npc-starts`.
- Migration appliquée aux scénarios du dépôt : `echecs` (1 commande) et `test-picks` (4). Le dossier de
  scénarios externe n'a pas été touché : à migrer avec `migration-syntaxe.bat`.
- Documentation : `chatscenarios/guide-redaction-chat.md` (§2.6 `pick` et ses types, §2.8 `delta-pnj`,
  §2.9 validation, §4.5, §5, arborescence du run, §7.4 pièges), `specs/chat.md` (`pick`, `npc-starts`,
  `delta-pnj`, validation), raccourci `.claude/skills/chat-scenario-writing/SKILL.md`,
  `specs/todo.md`.
- Test : `ScenarioSyntaxMigratorTest`.

## 3. Résultat

`mvn clean test` : tous les tests passent. Les scénarios du dépôt se chargent et se valident
(`ValidateScenarioCli` : 8 scénarios, 0 erreur).
