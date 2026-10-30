# 2026-10-28 - Chat : validation du scénario à chaque chargement et à la demande

## 1. Évolution demandée

Une fonction de validation du scénario : exécutée à chaque chargement, qui liste toutes les erreurs
(plutôt que la première) et bloque le chargement ; avertissements affichés sans bloquer ; utilisable aussi
à la demande.

## 2. Ce qui a été touché

- Domaine `validation` : `ScenarioValidator` (`checkSources` : erreurs ligne par ligne ; `checkScenario` :
  avertissements), `ScenarioIssue`, `ScenarioReport`, `ScenarioSource`, `ScenarioValidationException`.
- `ChatFileStorageAdapter` : valide les textes lus depuis le répertoire source ; `validateScenario` à la
  demande, sans effet de bord (tirage en mémoire, copie temporaire supprimée, aucun run créé).
  Port : `ChatStoragePort.validateScenario`.
- Service : `ChatService.scenarioWarnings`. La CLI de chat affiche les avertissements au lancement et
  attrape aussi `IllegalArgumentException` ; le bouton 🐞 les affiche.
- `ValidateScenarioCli` + `validation-scenarios.bat`.
- `ScenarioScaffolder` : la note sur `npc-starts` n'utilise plus d'accolades (une mention entre accolades
  est maintenant validée comme une vraie commande).
- Tests : `ScenarioValidatorTest`, tests de chargement adaptés (`ScenarioValidationException`),
  `ChatFileStoragePicksTest` (validation à la demande sans rien écrire).

## 3. Résultat

`mvn clean test` : tous les tests passent. Sur les 8 scénarios du dépôt : aucune erreur, un avertissement
(un PNJ non cité dans le premier acte de `carnet-bleu`).
