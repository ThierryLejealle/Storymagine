# 2026-07-15 16h33 - L'adaptateur Ollama masque le quirk thinking gemma4, logs corriges

## 1. Demande

Suite au fix precedent (16h02, agents du chat demandent toujours think=true) et a la verification
empirique en direct sur Ollama (le meme modele E2B qui semblait "ne pas penser" renvoie en fait un
`message.thinking` complet des que le champ `think` explicite est omis, au lieu d'etre envoye a
`true` — Ollama rejette `think:true` en HTTP 400 sur ces imports `hf.co/`, mais reflechit nativement
par defaut quand le champ est absent) :

> "Je veux que l'adapteur Ollama MASQUE ce probleme. Donc vu de chat : on demande THINK parce que
> l'adapteur répond toujours OUI si on lui demande si un gemma think. C'est l'adapteur qui se
> débrouille pour que ça marche. Dans les logs je veux voir think : true ! parce que c'est ce qu'on
> a demandé. Tu peux rajouter une ligne : (correctif modele gemma4 sur thinking) si tu dois
> bricoler pour que ça marche. et je VEUX le thinking dans les logs :)"

En cours de route, l'utilisateur a aussi repere une fuite hexagonale : le commentaire ajoute a
16h02 dans `RoleplayNarrator.java` (coeur/domaine) nommait "Ollama" explicitement — corrige avant
de continuer (reformule sans nommer le backend, voir §2).

## 2. Ce qui a ete touche

### Fuite hexagonale corrigee
`RoleplayNarrator.java` : le commentaire sur l'appel `.withThink(true)` ne nomme plus "Ollama" —
reformule en "au LLM (a charge de l'adaptateur de s'en debrouiller, voir ModelCallPort)". Le coeur
ne doit connaitre que l'abstraction (`LlmCallContext`/`ModelCallPort`), jamais le backend concret.

### OllamaAdapter.java — separation "demande" (wantedThink) vs "ce qui part sur le fil" (resolveThink)
- Nouvelle methode privee `wantedThink(Boolean thinkOverride)` : ce que l'appelant a reellement
  demande (jamais `null`, contrairement a `resolveThink()`).
- `resolveThink()` : commentaire enrichi avec le marqueur demande "(correctif modèle gemma4 sur
  thinking)", et la justification empirique du 2026-07-15 (champ absent -> Ollama reflechit quand
  meme nativement pour ces modeles, seule l'activation explicite est rejetee).
- Tous les points d'appel a `logLlmCall(...)` et `log.llmCallOpen(...)` (generate, complete,
  sendSync, sendStreaming/executeStreaming, sendRawSync) utilisent maintenant `wantedThink(...)`
  au lieu de la valeur neutralisee par `resolveThink()` — les logs refletent desormais la demande,
  jamais le bricolage interne. Seul `buildOllamaRequest`/`sendRawSync` (ce qui part reellement sur
  le fil HTTP) continue de passer par `resolveThink(wantedThink(...))`.
- Consequence directe : `Think : n/a` disparait des logs pour les appels qui passent par
  generate()/complete() — remplace par `oui`/`non` refletant fidelement ce qui a ete demande.

### Thinking trace dans les logs (`LogPort` + 2 implementations)
- `LogPort.llmCallClose(...)` : nouveau parametre `String thinking` (signature elargie, javadoc mis
  a jour). Seuls `FileLogAdapter` et `TeeLogAdapter` l'implementaient (verifie par grep sur tout le
  projet) — `ConsoleLogAdapter` et les autres s'appuient sur le defaut no-op, aucun changement.
- `FileLogAdapter.writeTraceFile(...)` : nouveau parametre `thinking`, ajoute une section
  `## RÉFLEXION` dans le fichier `.md` de chaque appel quand le texte est non vide (silencieusement
  omise sinon — pas de section vide qui pollue le fichier).
- `TeeLogAdapter.llmCallClose(...)` : relaie le nouveau parametre a chaque delegue.
- `OllamaAdapter` : passe desormais `result.thinking()` a `llmCallClose` sur le chemin succes
  (chaine vide sur le chemin erreur, rien a montrer).

## 3. Verification

`mvn compile` (racine) propre sur tous les modules. `mvn test` (racine) : 57 (redacteur) + 162
(chat) + tests commun/testllm, tous verts, zero regression — aucun test ne surchargeait
`llmCallClose` (verifie par grep), donc le changement de signature n'a casse aucun test existant.

Verification empirique complementaire faite en amont sur Ollama en direct (curl `/api/chat`) sur
`hf.co/mradermacher/gemma-4-E2B-it-qat-q4_0-unquantized-heretic-GGUF:Q4_K_M` : `think:true` explicite
→ HTTP erreur "does not support thinking" ; `think` omis → `message.thinking` rempli d'un vrai
raisonnement. Confirme que le masquage de l'adaptateur (omettre plutot que forcer `false`) est la
bonne strategie, deja en place depuis le 2026-07-09 — seule la couche logs mentait sur ce qui avait
ete demande, maintenant corrige.

Pas encore reverifie en conditions reelles via `chat.bat` (le fichier `.md` genere lors du prochain
tour RoleplayNarrator sur un favori gemma4 sans capacite declaree devrait maintenant afficher
`Think : oui` et une section `## RÉFLEXION` remplie).
