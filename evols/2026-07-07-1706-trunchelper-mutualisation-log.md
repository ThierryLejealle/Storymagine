# 2026-07-07 17h06 - TruncHelper (mutualisation troncature + log unique)

## Évolution demandée

Factoriser un `TruncHelper` dans `commun` pour remplacer les ~21 copies de la méthode
`trunc()` recopiée à l'identique dans les agents `redacteur`. Trois exigences explicites :
1. Un seul log (pas un par champ) si la troncature a coupé quelque chose.
2. Troncature intelligente : ne pas couper en plein milieu d'une phrase si possible.
3. Troncature de liste : s'arrêter au dernier élément entier qui rentre, jamais en coupant
   un élément en deux.

Avant tout code : présentation d'un plan (concept `TruncHelper`, impact LogPort sur les
constructeurs, portée) et validation explicite de l'utilisateur sur deux points ouverts :
portée de la migration (tout en un lot) et sort des 5 agents désactivés dans `agent/temp/`.
Décision utilisateur : tout migrer en un lot ; ne pas supprimer les agents `temp/` (malgré une
proposition de nettoyage) mais corriger le bug d'encodage préexistant sur les 3 qui en ont un.

## Ce qui a été touché

### `commun/coeur/domaine/text/TruncHelper.java` (nouveau)
Classe agnostique du domaine (comme `PromptBuilder`), instanciée par appel
(`TruncHelper.create()`) :
- `text(s, maxChars, label)` — coupe à la dernière limite de phrase/ligne (`\n`, `.`, `!`, `?`)
  trouvée avant la limite ; coupe brute + `"…"` si aucune limite trouvée.
- `tailText(s, maxChars, label)` — variante qui garde la FIN du texte (reprend le
  comportement historique de `Writer.lastSentences` / `StoryCompressor.lastSentences`,
  utilisé pour `storySoFar` / `existingSummary`).
- `list(s, maxChars, label)` — découpe un texte joint par `"\n"` (une ligne = un élément :
  contraintes, checks, journal d'itérations...), garde uniquement les lignes entières qui
  rentrent.
- `blockList(s, maxChars, label)` — même principe mais pour des blocs multi-lignes séparés
  par une ligne vide (`"\n\n"` : fiches personnages, focus, lore), jamais de coupe en plein
  bloc.
- `logIfTruncated(log, agentName)` — émet UNE ligne de log (`"Agent: troncature — champ1,
  champ2"`) uniquement si au moins un champ a été coupé pendant l'appel.
- 10 tests unitaires (`TruncHelperTest`), couvrant les 4 méthodes, les cas limites (repli
  quand le premier élément seul dépasse le budget) et l'agrégation du log.

### Migration de `trunc()` maison vers `TruncHelper` (21 agents)
`Writer`, `ChapterPlanner`, `PlanNarrativeCritic`, `PlanCoherenceCritic`, `GoalPlanChecker`,
`GoalTextCritic`, `TextCoherenceCritic`, `TextDreamCritic`, `TextNarrativeCritic`,
`TextWhatIfCritic`, `StyleCorrector`, `ProofreaderCorrector`, `NaturalityCorrector`,
`PlanFidelityCritic`, `CheckCritic`, `StateExtractor`, `RepetitionTracker`, `StoryCompressor`,
`NarrativeArcAnalyzer`, `ChapterStyleChecker`, `CausalAnalyzer`.
Classification par champ selon son origine (`ScenarioFormatters`) : prose (`text`/`tailText`)
vs liste à une ligne par élément (`list`, ex. contraintes/checks) vs liste de blocs multi-lignes
(`blockList`, ex. fiches personnages/focus/lore). Suppression des méthodes `trunc()`/
`lastSentences()` devenues mortes dans chaque classe.
Aucun texte de prompt modifié — uniquement la plomberie d'assemblage et l'ajout de logging.

### `LogPort` ajouté au constructeur de 19 agents
Tous les agents migrés sauf `Writer` (l'avait déjà). Nécessaire pour appeler
`TruncHelper.logIfTruncated`.

### `redacteur/RedacteurModule.java`
Mise à jour des 19 lignes d'instanciation (`new XxxAgent(llm)` → `new XxxAgent(llm, log)`)
pour les agents effectivement câblés. Les 3 agents `temp/` migrés (`NarrativeArcAnalyzer`,
`ChapterStyleChecker`, `CausalAnalyzer`) ne sont pas câblés ici (toujours désactivés,
inchangé).

### Bug d'encodage corrigé (agents `temp/`)
`NarrativeArcAnalyzer.java`, `ChapterStyleChecker.java`, `CausalAnalyzer.java` contenaient du
mojibake étendu (UTF-8 réinterprété en Latin-1, ex. `"vÃ©rifies"` au lieu de `"vérifies"`,
`"â€”"` au lieu de `"—"`) sur l'ensemble des chaînes SYSTEM et commentaires, pas seulement le
`"…"` de troncature repéré initialement. Fichiers réécrits intégralement en UTF-8 sans BOM,
texte des prompts identique au sens près (aucun changement sémantique, uniquement les
caractères mal décodés restaurés).

## Résultat

- `mvn -pl commun,redacteur -am clean test` : BUILD SUCCESS, 49 tests (17 `commun` + 32
  `redacteur`), 0 échec.
- Vérification BOM sur les 24 fichiers touchés : aucun.
- Chaque agent émet désormais au plus une ligne de log par appel en cas de troncature,
  au lieu de zéro (20 agents) ou d'un log par champ tronqué (`Writer` seul avant ce lot).
