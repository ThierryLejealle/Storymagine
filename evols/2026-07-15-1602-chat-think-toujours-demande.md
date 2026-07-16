# 2026-07-15 16h02 - think toujours demande dans les agents du chat

## 1. Demande

Suite a l'audit du 2026-07-15 (voir messages precedents) qui a montre que seul RoleplayNarrator
demandait `think` a Ollama, et uniquement si la case "Afficher la reflexion" (showThinking) etait
cochee — ChatSummarizer, NextActReadinessAnalyst et NpcMindStateAnalyst ne le demandaient jamais.
L'utilisateur : "Je veux que tu demandes le think dans les agents du chat. meme si on ne l'affiche
pas !" — meme rationale que le fix redacteur du 2026-07-10 (evols/2026-07-10-2307) : la reflexion
ameliore la reponse elle-meme, pas seulement sa lisibilite pour le joueur.

## 2. Ce qui a ete touche

Scope : les 4 agents utilises par `ChatService` (`ChatModule.assemble`). Les 2 agents du
`ScenarioTesterService` (ScenarioClarityReviewer, ScenarioContinuityReviewer) sont hors scope —
"les agents du chat" ne les couvre pas, pas touches.

- `RoleplayNarrator.java` : `LlmCallContext` construit avec `.withThink(true)` inconditionnellement
  (avant : seulement si `showThinking`). `showThinking` continue de piloter uniquement l'AFFICHAGE
  (`String thinking = showThinking ? result.thinking().strip() : ""`, inchange).
- `ChatSummarizer.java` : `.withThink(true)` ajoute sur l'unique appel LLM.
- `NextActReadinessAnalyst.java` : `.withThink(true)` ajoute — la popup 🔍 continue de n'utiliser
  que `.text()`, la reflexion est demandee mais jamais extraite/affichee.
- `NpcMindStateAnalyst.java` : idem, popup 🧠.

Rappel (deja verifie en amont, cf. discussion) : `OllamaAdapter.resolveThink()` neutralise
silencieusement un `think=true` explicite si le modele sonde ne declare pas la capacite
"thinking" — aucun risque de HTTP 400 sur les favoris gemma4 qui ne la supportent pas (5 sur 6,
voir diagnostic precedent), le `.withThink(true)` est donc sans danger meme demande partout.

## 3. Tests ajoutes

Un test de non-regression par agent, verifiant `ctx.think() == Boolean.TRUE` cote appel LLM capture
(le point precis qui vient d'etre corrige, pour eviter qu'un futur refactor ne re-introduise le
`if (showThinking)` conditionnel sans s'en rendre compte) :
- `RoleplayNarratorTest` (nouveau fichier) : `alwaysRequestsThinkingEvenWhenShowThinkingIsOff` +
  `neverSurfacesThinkingInTheOutputWhenShowThinkingIsOff` (verifie aussi que l'affichage reste
  gate, comportement inchange).
- `ChatSummarizerTest` (nouveau fichier) : `alwaysRequestsThinkingEvenThoughItIsNeverDisplayed`.
- `NextActReadinessAnalystTest` : test ajoute `alwaysRequestsThinkingEvenThoughThisPopupNeverDisplaysIt`.
- `NpcMindStateAnalystTest` : idem.

## 4. Resultat

`mvn -pl chat -am test` : 162 tests verts (4 nouveaux vs avant), zero regression.
