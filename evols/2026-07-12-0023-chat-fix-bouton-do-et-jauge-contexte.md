# 2026-07-12 00h23 — Chat : fix bouton DO + jauge de contexte

## 1. Demande

- Le bouton "DO" (ajouté en `evols/2026-07-12-0002-...`) ne marchait pas comme attendu : il
  insérait `DO: ` dans le champ de saisie au lieu d'envoyer directement le message.
- Nouvelle demande : un repère visuel approximatif à droite de l'écran, une jauge de % de
  contexte utilisé.

## 2. Ce qui a été touché

### Fix bouton DO — `chat.html` uniquement
Le bouton était un toggle qui préfixait/retirait `DO: ` dans le textarea sans jamais envoyer.
Refondu en bouton d'envoi direct : au clic, prend le texte actuellement tapé, le préfixe de
`DO: ` s'il ne l'a pas déjà (insensible à la casse), vide le champ et envoie immédiatement — un
seul clic au lieu de deux. `formEl` (submit normal) et `doBtnEl` (clic DO) partagent maintenant la
même fonction `sendMessage(text)` (affichage optimiste, gestion d'erreur, rollback compris).

### Jauge de contexte
Donnée utilisée : `promptTokens` — taille réelle du dernier prompt, mesurée par Ollama (déjà
disponible sur `LlmResult`, jusqu'ici jetée après `.text()`) — rapportée à `contextWindow`
(capacité totale du modèle). Volontairement approximatif par rapport au seuil de compaction
(`SummaryBudget.wordBudget`, qui ne compte que les tours non compactés en mots estimés) — deux
repères différents, pas mélangés sur la même jauge.

- `ChatTurnResult` : + champ `promptTokens`.
- `ChatService`/`ChatServiceImpl` : + méthode `contextWindow()` (délègue à `llm.contextWindow()`).
- `ChatServiceImpl.sendMessage()` : garde le `LlmResult` complet au lieu de ne garder que
  `.text()`, pour récupérer `.promptTokens()`.
- `ChatHistoryView` : + champs `promptTokens`, `contextWindow`.
- `ChatWebServer` : + `lastPromptTokens` (volatile, mis à jour après chaque `/message`) et
  `contextWindow` (final, récupéré une fois via `service.contextWindow()`) ; les deux exposés par
  `/history` et `/message`.
- `chat.html` : barre verticale fixe collée au bord droit (`#gauge`/`#gaugeFill`), remplissage
  proportionnel avec dégradé de couleur (vert < 70%, orange < 90%, rouge au-delà), label % au-dessus
  et détail en tokens au survol (`title`). `main`/`footer` ont un padding-right supplémentaire pour
  ne pas passer sous la jauge.

### Tests
`ChatServiceImplTest` : bouchon `ModelCallPort` étendu pour renvoyer un `promptTokens`
configurable ; 2 nouveaux tests (`contextWindowDelegatesToTheRoleplayPort`,
`turnResultCarriesTheActualPromptTokensMeasuredByOllama`). 24 tests au total (contre 22 avant).

## 3. Résultat

`mvn compile test` depuis la racine : **BUILD SUCCESS**, 5 modules, **24 tests, 0 échec**. Non
encore re-testé dans le navigateur à la rédaction de cette fiche.
