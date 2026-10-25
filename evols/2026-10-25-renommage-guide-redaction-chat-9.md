# 2026-10-25 - Chat : rules.md devient guide-redaction-chat.md

## 1. Évolution demandée

Renommer `chatscenarios/rules.md` en `chatscenarios/guide-redaction-chat.md` (nom plus parlant : c'est
le guide pour écrire un scénario de chat).

## 2. Ce qui a été touché

- `git mv` du fichier (historique conservé).
- Références mises à jour : skill `chat-scenario-writing`, `chatscenarios/CLAUDE.md`, `specs/chat.md`,
  message affiché par `InitChatCli`, commentaires de `ScenarioScaffolder`.
- Les anciennes fiches `evols/` qui citent `rules.md` sont laissées telles quelles (archives datées).

## 3. Résultat

Plus aucune référence à `rules.md` hors des fiches d'évol. Compilation vérifiée depuis la racine.
