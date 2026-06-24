# 2026-06-23 10h17 - Règles de retry et EngineConfig

## Description

Implémentation des règles de retry correctes inspirées de `../Redacteur`,
et introduction d'un fichier de configuration `engine.properties` par module.

## Ce qui a été touché

### Nouveau
- `redacteur/engine.properties` — paramètres moteur (plan_max_retry, chapitre_threshold, chapitre_max_retry, sequence_max_retry)
- `redacteur/coeur/domaine/orchestrator/EngineConfig.java` — POJO portant les valeurs de config (domaine pur, pas d'accès fichier)
- `redacteur/infra/EngineConfigLoader.java` — lit engine.properties et crée un EngineConfig (infra)
- `specs/retry-rules.md` — spécification lisible des règles de retry

### Modifié
- `GenerationConfig` — suppression du champ `maxRetries` (les retry counts viennent désormais de EngineConfig)
- `WrittenChapter` — ajout de `clearSequences()` pour la réécriture complète de chapitre
- `PlanWorkflow` — condition de retry corrigée : `anyHasProblems` au lieu de `score >= 0.6` ; best plan retenu
- `WriteWorkflow` — restructuration majeure :
  - Vérificateurs par séquence (DeusInMachina, StyleChecker, SequenceChecker) avec boucles de retry indépendantes
  - Critique de chapitre sur le texte complet après toutes les séquences, retry si avg <= threshold
  - Meilleure version de chapitre retenue
- `RedacteurModule` — câblage d'EngineConfig (chargé par EngineConfigLoader en prod, defaults() en test)
- `RedacteurCli` — suppression de l'affichage de maxRetries
- Tests `WorkflowLogTest` — ajout de `UNIVERSAL_PASS` dans le mock, mise à jour des GenerationConfig

### Bugs corrigés
- `PlanWorkflow.PASS_THRESHOLD = 0.6` contre des scores 1-10 : les critiques ne pouvaient jamais déclencher de retry

## Résultat

BUILD SUCCESS — 12/12 tests passent.
Les règles de retry sont alignées sur le comportement de `../Redacteur`.
