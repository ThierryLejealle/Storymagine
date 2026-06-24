# 2026-06-23 19h00 — Traçabilité des appels LLM (llm_calls/)

## Évolution demandée
Reproduire à 100% le mécanisme de logging des appels LLM de ../Redacteur :
un fichier par appel dans `llm_calls/`, avec le prompt système, le prompt utilisateur
et la réponse du modèle. Les fichiers doivent inclure le nom de l'agent appelant.

## Ce qui a été touché

### Nouveau : `commun/coeur/ports/LlmCallContext.java`
Record portant le nom de l'agent (`agentName`) et une note libre (`note`).
Transmis à `ModelCallPort.generate()` par chaque agent au moment de l'appel.

### Modifié : `commun/coeur/ports/ModelCallPort.java`
- Nouvelle signature principale : `generate(sys, usr, temp, LlmCallContext ctx)`
- Surcharge `default` rétrocompat sans contexte (→ `LlmCallContext.of("Unknown")`)

### Modifié : `commun/coeur/ports/LogPort.java`
Deux méthodes `default` ajoutées :
- `llmCallOpen(agentName, localNum, sys, usr) → String` — ouvre un fichier trace, retourne un handle
- `llmCallClose(handle, response, elapsedMs, tokIn, tokOut)` — finalise le fichier

### Modifié : `commun/infra/FileLogAdapter.java`
Implémente `llmCallOpen`/`llmCallClose` :
- Compteur `traceCounter` par instance (indépendant du compteur de métriques)
- Compteur par agent dans `OllamaAdapter` (→ `localNum` par agent)
- Fichiers écrits dans `{runDir}/llm_calls/{NNN}_{agentName}_{local}.md`
- Format identique à ../Redacteur : EN-TÊTE + PROMPT SYSTÈME + PROMPT UTILISATEUR + RÉPONSE
- Écriture immédiate à l'ouverture (statut ⏳) puis réécriture complète à la fermeture (statut ✅)

### Modifié : `commun/infra/TeeLogAdapter.java`
Délègue `llmCallOpen` (retourne le premier handle non vide) et `llmCallClose` à tous les delegates.

### Modifié : `commun/infra/OllamaAdapter.java`
- Implémente la nouvelle signature `generate(..., LlmCallContext ctx)`
- `Map<String, AtomicInteger> agentCallCounts` — compteur local par agent
- Appelle `log.llmCallOpen()` avant le HTTP, `log.llmCallClose()` après (ou sur erreur)
- Extrait la boucle context-overflow dans `generateInternal()` (private)

### Modifié : 24 agents (plan + writer + global)
Chacun passe `LlmCallContext.of("NomAgent")` à son appel `llm.generate()`.
Import `LlmCallContext` ajouté dans chaque fichier.

## Résultat
- Compilation OK, BUILD SUCCESS sur les 4 modules.
- À chaque génération, `{scenario}/generated/{ts}/llm_calls/` contient un fichier
  par appel LLM, nommé `{NNN}_{AgentName}_{local}.md`.
- Format identique à ../Redacteur (EN-TÊTE, PROMPT SYSTÈME, PROMPT UTILISATEUR, RÉPONSE).
- Aucun changement dans `RedacteurModule` ni dans la CLI.
