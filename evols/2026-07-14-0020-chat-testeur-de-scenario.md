# 2026-07-14 00h20 - Chat : nouveau testeur de scénario (QA statique acte par acte)

## Demande

"Tu peux me faire une fonction dans 'chat' qui teste une histoire : tu construit un agent dédié à
qui tu vas dérouler le scénario, acte par acte. Et tu lui demandes de relever des incohérences
dans l'histoire. Et un autre agent qui vérifie si c'est utilisable, clair et s'il comprend les
conditions de sorties (NEXT ACT). Demandes lui de synthétiser. Si possible ce serait bien que
chaque agent propose des améliorations (sans chercher à les noter, les caractériser) que tu me
restitueras dans un log et une page HTML (au meme format que la story de readacteur) : act par
act." Cadrage complémentaire validé via questions : synthèse = agrégation mécanique (pas de 3e
appel LLM) ; l'agent cohérence reçoit l'historique complet cumulatif (pas de résumé) ;
déclenchement via un nouveau `.bat` dédié, séparé du chat interactif. Plan présenté et validé
avant codage (`EnterPlanMode`/`ExitPlanMode`), avec consultation Fable sur les deux prompts avant
écriture finale (confirmation explicite du coût ~20-30K tokens avant l'appel).

## Ce qui a été touché

- **Nouveau suffixe d'agent `Reviewer`** (chat uniquement) : analyse un texte, renvoie des
  constats + suggestions en texte libre, jamais de score — distinct du suffixe `Critic` de
  `redacteur` qui calcule toujours un score Java.
- `chat/coeur/domaine/agent/commun/ReviewOutputParser.java` (nouveau) : parseur partagé du format
  `ISSUES:`/`SUGGESTIONS:`/sentinelle `[RIEN]` (reprend la convention déjà utilisée dans
  `redacteur`).
- `chat/coeur/domaine/agent/continuityreviewer/ScenarioContinuityReviewer.java` (+Input/Output/.md)
  — lit les actes dans l'ordre, garde tout l'historique déjà lu en texte intégral (cumulatif),
  relève incohérences narratives + suggestions.
- `chat/coeur/domaine/agent/clarityreviewer/ScenarioClarityReviewer.java` (+Input/Output/.md) —
  lit UN SEUL acte isolé, exactement ce que verrait le vrai `RoleplayNarrator` (fiche perso +
  prémisse + acte, sans historique), juge l'utilisabilité et si la condition `[NEXT ACT]` est
  compréhensible.
- Prompts des deux agents revus après une consultation Fable (contexte auto-suffisant, lecture de
  fichiers interdite, ~36K tokens réels) : ajout d'un 2e exemple systématique montrant le cas
  `[RIEN]`/`[RIEN]` (un seul exemple biaisait un petit modèle vers toujours trouver un problème),
  langue de sortie explicitement alignée sur la langue du scénario (au lieu d'être non spécifiée),
  garde-fous contre deux faux positifs prévisibles (élément nouveau introduit comme nouveau ≠
  incohérence ; mystère volontaire non résolu ≠ incohérence), citation obligatoire de l'acte
  contredit, et un "test de paraphrase" pour juger la condition `[NEXT ACT]` (la reformuler en un
  événement concret et observable, sinon la signaler) plutôt qu'un jugement de vague direct.
- `chat/coeur/domaine/scenariotester/{ActTestResult,ScenarioTestReport}.java` (nouveaux records).
- `chat/coeur/service/{ScenarioTesterService,ScenarioTesterServiceImpl}.java` (nouveaux) —
  orchestre les deux agents acte par acte, fusionne leurs suggestions par acte (préfixées
  `[Continuité]`/`[Clarté]`, jamais notées), logue une ligne par acte via `LogPort`.
- `ChatModule.java` : `assembleTester(llm, log)`.
- `chat/coeur/ports/ScenarioTestExportPort.java` (nouveau port) +
  `chat/infra/ScenarioTestHtmlExporter.java` (pur, CSS et structure repris de
  `redacteur/infra/HtmlExporter.java` — TOC + une section par acte avec 3 blocs Incohérences/
  Clarté/Suggestions) + `chat/infra/ScenarioTestFileExportAdapter.java` (écrit `report.html`).
- `chat/ui/cli/ScenarioTesterCli.java` (nouveau) — reprend le flux GPU/modèle/choix de scénario de
  `ChatCli`, écrit logs + `report.html` dans `chatscenarios/<nom>/tests/<horodatage>/` (réutilise
  le mécanisme de sous-dossier horodaté existant de `FileLogAdapter`), ouvre le rapport dans le
  navigateur.
- `test-scenario.bat` (racine, écrit en CP1252 via l'API .NET PowerShell) — `mvn exec:java` avec
  `-Dexec.mainClass` surchargé, pas de modification du `pom.xml`.
- Tests : `ReviewOutputParserTest`, `ScenarioContinuityReviewerTest`, `ScenarioClarityReviewerTest`
  (dont la vérification que l'agent clarté ne reçoit jamais d'historique),
  `ScenarioTesterServiceImplTest` (ordre des actes, historique cumulatif croissant côté
  cohérence), `ScenarioTestHtmlExporterTest` (contenu + échappement HTML).

## Résultat

`mvn clean test` (4 modules) au vert, 94 tests côté chat (+20 pour cette fonctionnalité). Un
scénario avec actes peut désormais être testé statiquement via `test-scenario.bat`, sans lancer de
vraie session de chat, avec un rapport HTML au format proche de celui de `redacteur`.
