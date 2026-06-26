# 2026-06-26 15h30 - Nom d'agent dans les logs LLM

## Evolution demandée
Ajouter le label "package/NomAgent" sur chaque ligne `[LLM #N]` des logs, pour identifier
immédiatement quel agent est à l'origine de chaque appel LLM.

## Ce qui a été touché

- `Agent.java` — méthode default `agentLabel()` : extrait le package parent (`plan`, `writer`, `global`…)
  depuis le package Java de la classe et retourne `"plan/ChapterPlanner"` etc. Aucune constante
  à ajouter par classe.
- `LlmCallContext.java` — ajout du champ `agentLabel` (le `agentName` court reste pour le nommage
  des fichiers de trace) ; factory `of(String name, String label)`.
- `LogPort.java` — signature `llmCall` enrichie : `llmCall(String agentLabel, long ms, …)`.
- `OllamaAdapter.java` — `agentLabel` threadé depuis `generate()` via `generateInternal()` et
  `sendWithNetworkRetry()` jusqu'à `logLlmCall()`.
- `ConsoleLogAdapter.java`, `FileLogAdapter.java`, `TeeLogAdapter.java` — format mis à jour,
  label affiché sur 28 chars après `[LLM #N]`.
- `CapturingLogPort.java` (test) — signature mise à jour.
- 24 classes agent — `LlmCallContext.of(agentName())` → `LlmCallContext.of(agentName(), agentLabel())`.

## Résultat

Format console :
```
[HH:mm:ss]   [LLM #  1]  plan/ChapterPlanner            28s    4585 ->  4124 tok  ...
```