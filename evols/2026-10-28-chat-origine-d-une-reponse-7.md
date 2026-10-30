# 2026-10-28 - Chat : bouton ❓, pistes sur l'origine de la dernière réponse (ReplyOriginAnalyst)

## 1. Évolution demandée

Un nouvel agent de debug, à côté du 🧠 : on lui donne le prompt reçu par le personnage et la réponse qu'il a
faite, et il met en lumière les consignes et informations du prompt qui peuvent expliquer la réponse.
Analyse froide et algorithmique, sans jugement ni critique. Il agit sur la dernière réponse du dernier
PNJ, ne modifie rien, n'est pas archivé. Un dépassement du budget de contexte est accepté. Prompt laissé à
mon appréciation.

## 2. Ce qui a été touché

- Agent `ReplyOriginAnalyst` (entrée, sortie, `ReplyOriginPromptBuilder`, fiche `.md`) : traçabilité, pas
  introspection (un modèle ne peut pas dire pourquoi il a écrit ce qu'il a écrit). Sections `TRACED`
  (élément -> bloc -> citation, `[direct]` ou `[indirect]`), `UNTRACED` (éléments sans passage
  correspondant) et `READING`. Température plus basse que les agents de jeu (0,3).
- Prompt exact : `RoleplayNarratorOutput.prompt` le porte, `ChatServiceImpl` le garde avec la réponse en
  mémoire (`ReplyTrace`, `ChatSession.rememberReplyTrace` / `lastReplyTrace`), jamais persisté, invalidé
  après undo, rollback, redémarrage ou chargement.
- Service `analyzeLastReplyOrigin` (lecture seule), `POST /analyze-reply-origin`, bouton ❓, pop-up
  réutilisée. `ChatModule` branche l'agent ; `ChatServiceImpl` garde ses anciens constructeurs (agent
  absent = bouton qui l'indique).
- Tests : `ReplyOriginAnalystTest` (sections, sentinelle, séparateurs, réflexion absente), et trois tests
  de service (prompt réellement envoyé, rien à analyser après un undo, agent non branché).
- Docs : `ReplyOriginAnalyst.md`, guide §4.3 bis, `specs/chat.md`, raccourci du skill.

## 3. Résultat

`mvn clean test` : tous les tests passent (faux LLM uniquement). Non essayé avec un vrai modèle : à voir si
un petit modèle découpe bien la réponse en éléments et cite fidèlement le prompt ; le prompt est à ajuster
à l'usage (nouvelle fiche).
