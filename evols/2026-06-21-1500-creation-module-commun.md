# 2026-06-21 15h00 - Création du module commun (adaptateur Ollama)

## Description de l'évolution demandée

Création du module `/commun` dans Storymagine à partir du code existant du projet Redacteur.
Objectif : poser une base hexagonale propre avec deux ports distincts pour l'accès au LLM,
et l'adaptateur Ollama comme première implémentation.

## Ce qui a été touché

### Nouveau — structure Maven
- `pom.xml` — POM parent multi-modules (Java 21, Jackson 2.17.2)
- `commun/pom.xml` — module commun avec dépendance Jackson

### Nouveau — `commun/coeur/ports/`
- `ModelCallPort` — port minimal pour les agents : `generate()` uniquement
- `LlmResult` — record résultat d'appel LLM (texte + métriques tokens)
- `ContextOverflowException` — exception levée quand le prompt dépasse la fenêtre
- `ModelLifecyclePort` — port riche : probe, unload, info modèle, listModels, detectContext
- `ModelEntry` — record entrée de liste de modèles (name + sizeBytes)

### Nouveau — `commun/infra/`
- `OllamaAdapter` — implémente `ModelCallPort` + `ModelLifecyclePort` ; absorbe l'ancienne classe `OllamaModelLister` (plus de méthodes statiques)
- `OllamaConfig` — record de configuration + fabrique d'adapters avec logique large-modèle
- `OllamaModelInfo` — cache interne info modèle (deux passes : /api/show + /api/ps)
- `RetryPolicy` — politique de reprise réseau (record)
- `OllamaRequest`, `OllamaResponse`, `OllamaMessage` — DTOs JSON Jackson

## Résultat

Module `/commun` fonctionnel avec architecture hexagonale claire :
- Les agents n'ont accès qu'à `ModelCallPort` (contrat minimal)
- L'orchestration utilise `ModelLifecyclePort` (gestion modèles)
- L'implémentation Ollama est entièrement encapsulée dans `/infra`
- Aucun code copié dans Redacteur — source inchangée
