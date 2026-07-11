# 2026-07-11 18h30 — Fix : cachedModelInfo jamais alimenté sur l'adaptateur de génération réelle

## 1. Demande

Découvert en direct pendant un test manuel de l'utilisateur sur `hf.co/mradermacher/gemma-4-26B-A4B-it-heretic-i1-GGUF:Q6_K` :
`[Ollama] tentative stream 1/6 échouée (Ollama HTTP 400 : "...does not support thinking"), reprise dans 15s…`
en boucle. Investigation immédiate demandée par l'utilisateur ("testes sur les 26b q6").

## 2. Ce qui a été touché

### Diagnostic
`OllamaAdapter.resolveThink()` est censé neutraliser une demande `think=true` explicite quand le
modèle probé ne déclare pas la capacité "thinking" (cas connu et documenté : certains imports hf.co/
ne déclarent pas la capacité alors que le modèle réfléchit nativement, et rejettent `think=true` en
HTTP 400 — voir `evols/2026-07-09-2319-...`). Cette neutralisation dépend de `cachedModelInfo`, rempli
uniquement par `probe()`.

Or `RedacteurCli.java` :
- construisait le `service` (ligne 147) via `RedacteurModule.assemble(ollama, selectedModel, ...)`,
  qui crée **son propre** `OllamaAdapter` en interne (`ollamaConfig.adapter(modelName, log)`) —
  c'est CET adaptateur qui sert à tous les appels LLM réels ;
- appelait `probe()` (ligne 214) sur un **autre** adaptateur, jetable, créé juste pour l'occasion
  (`ollama.adapter(selectedModel).probe()`), uniquement pour afficher les infos matériel.

Ces deux adaptateurs sont des instances distinctes (`OllamaConfig.adapter(...)` fabrique un nouvel
objet à chaque appel, jamais de cache/mémoïsation). Résultat : `cachedModelInfo` de l'adaptateur
réellement utilisé pour la génération restait **`null` en permanence**, donc `resolveThink()` ne
neutralisait jamais rien (`cachedModelInfo != null` toujours faux).

Tant que `think=false` était le défaut partout, ce trou n'avait aucun effet visible. Depuis le fix
du 2026-07-10/11 (`Agent.thinks()` par défaut `true`), les agents demandent `think=true` par défaut
— ce qui expose le trou : ce modèle précis (import hf.co/ mal déclaré) reçoit `think=true` sans
filtre et Ollama répond 400, à chaque appel, sur les 6 tentatives de retry (échec déterministe, pas
transitoire — le retry ne peut jamais réussir).

### `RedacteurCli.java`
- Construction de l'adaptateur de génération explicite en une seule fois :
  `OllamaAdapter llm = ollama.adapter(selectedModel, log);`, passé à
  `RedacteurModule.assemble(llm, new ScenarioFileAdapter(), log, htmlExport, beatsConfig, correctorConfig)`
  (overload `ModelCallPort` déjà existant, jusqu'ici réservé aux tests).
- `probe()` (ligne ~214, même emplacement/timing qu'avant, après confirmation utilisateur) appelé
  sur **ce même** `llm` au lieu d'un adaptateur jetable — `cachedModelInfo` est donc désormais
  correctement peuplé sur l'adaptateur qui sert à tous les agents avant le premier appel réel.
- Import ajouté : `storymagine.redacteur.infra.scenario.ScenarioFileAdapter`.

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**. Aucune régression.

Pas de changement de comportement UX (le probe se produit toujours au même moment dans le flux CLI,
juste sur la bonne instance).
