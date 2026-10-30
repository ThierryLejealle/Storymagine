# 2026-10-27 - Chat : pensées du joueur entre parenthèses, retirées avant envoi aux LLM

## 1. Évolution demandée

Les PNJ réagissaient aux pensées internes que le joueur glissait dans ses actions : le prompt ne
connaissait aucune convention de pensée (seulement `*action*`, parole, `OOC:`, `DO:`). Décision :
convention `(texte)` = pensée, supprimée côté code avant que le LLM ne la voie, plutôt que demandée
au modèle de l'ignorer. Aucun prompt modifié.

## 2. Ce qui a été touché

- Nouveau `PlayerThoughts` (session) : `strip()` retire les paires `(...)`, y compris imbriquées,
  et normalise les espaces.
- `ChatPromptBuilder.transcript()` : applique `strip()` aux tours PLAYER ; un tour réduit à des
  pensées est omis. `transcript()` étant partagé, le résumeur, l'analyse de disponibilité NEXT ACT
  et l'état d'esprit des PNJ ne voient pas non plus les pensées.
- Tests : `PlayerThoughtsTest`, `ChatPromptBuilderTest.transcriptHidesPlayerThoughtsButNotNpcParentheses`.
- Docs : `guide-redaction-chat.md` §4.9, `RoleplayNarrator.md`.

## 3. Résultat

`mvn test` sur le module chat (et ses dépendances) : tous les tests passent. Limites connues : une parenthèse
ouverte sans fermeture n'est pas retirée ; les `(...)` d'un message `DO:` sont aussi retirés ;
l'historique affiché au joueur garde les pensées.
