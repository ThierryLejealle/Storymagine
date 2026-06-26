# 2026-06-26 14h00 - Streaming HTTP Ollama (mode configurable)

## Évolution demandée

Résoudre les timeouts sur les gros modèles à faible débit (≈15 tok/s).
Le timeout fixe de 600 s (≈ 9 000 tokens max) est insuffisant pour une génération longue.
Passer en streaming HTTP pour remplacer le timeout global par deux timeouts ciblés.

## Fichiers touchés

| Fichier | Changement |
|---|---|
| `commun/infra/OllamaMessage` | `@JsonIgnoreProperties(ignoreUnknown=true)` — ignore le champ `thinking` des chunks streaming |
| `commun/infra/OllamaConfig` | +3 champs : `streamMode`, `firstTokenTimeoutMs`, `interTokenTimeoutMs` |
| `commun/infra/OllamaAdapter` | Mode stream : `sendStreaming` + `executeStreaming` + `readLineWithTimeout` ; mode sync inchangé (`sendSync`) |
| `redacteur/resources/redacteur.properties` | Nouvelles clés `ollama.mode`, `ollama.stream.*` |
| `testllm/testllm.properties` | Idem |
| `redacteur/cli/RedacteurCli` | Lecture des nouvelles propriétés dans `buildOllamaConfig()` |
| `testllm/infra/TestLlmConfig` | Idem dans `ollamaConfig()` |

## Ce qui a changé

**Mode stream** (`ollama.mode=stream`, défaut) :
- Requête HTTP sans `.timeout()` — Java ne pose pas de timeout global
- `sendAsync` + `BodyHandlers.ofInputStream()`
- `.get(30 000 ms)` pour les headers HTTP (connexion TCP)
- Premier `readLineWithTimeout(firstTokenTimeoutMs)` — couvre chargement modèle + prefill
- Boucle `readLineWithTimeout(interTokenTimeoutMs)` — entre chaque token
- Sur expiration : fermeture de l'`InputStream` pour débloquer le thread lecteur (daemon)
- Accumule uniquement `message.content` — le champ `thinking` est ignoré

**Mode sync** (`ollama.mode=sync`) :
- Comportement identique à avant — `timeoutMs` + `largeModelTimeoutMultiplier`

**Valeurs par défaut** :
- `firstTokenTimeoutMs` = 300 000 ms (5 min) — charge un 31B depuis le disque
- `interTokenTimeoutMs` = 30 000 ms (30 s) — très généreux à 15 tok/s (1 token/67 ms)

## Résultat

Compilation OK (`mvn compile` sans erreur). Les deux modules (redacteur, testllm) passent en mode stream par défaut via `ollama.mode=stream`.
