# 2026-07-12 10h55 — WorldState : reset + reensemencement à chaque transition de chapitre

## 1. Demande

Suite au fix du bug de parsing `StoryFormatters.applyStateLines` (voir
`evols/2026-07-12-1035-...`), l'utilisateur a relevé que le fix, une fois actif, exposerait un
second problème : `WorldState.entityStates` (`updateEntities()` fait un simple `putAll`) n'a
aucune limite ni purge — chaque entité extraite par `StateExtractor`, séquence après séquence,
s'empile pour toute la durée du livre, sans jamais être nettoyée. Décision explicite de
l'utilisateur : ne pas toucher aux prompts `ChapterSummarizer`/`SummaryCompressor` (ils restent
corrects tels quels) ; l'état des personnages doit vivre dans une zone dédiée et unique
(`WorldState`), jamais fusionné dans le résumé narratif (`Story.summary()`) ; et cette zone doit
représenter un ÉTAT (une vérité actuelle qui écrase l'ancienne), pas un historique cumulatif.

## 2. Ce qui a été touché

Aucun prompt LLM modifié (ni `StateExtractor`, ni `ChapterSummarizer`, ni `SummaryCompressor`) —
uniquement de l'orchestration Java.

### `ChapterSummaryStep`
+ dépendance `StateExtractorStep` (réutilise la même instance que `WriteWorkflow`, pas de
duplication d'agent). Après avoir produit `chapterSummary` :
1. `story.worldState().clearExtractedState()` — vide `entityStates`/`recentEvents` accumulés
   pendant le chapitre qui vient de se terminer (méthode déjà présente dans `WorldState.java`,
   jusqu'ici utilisée seulement pour l'isolation DREAM/WHAT_IF).
2. `stateExtractorStep.run(chapterSummary, story.worldState())` — un seul appel `StateExtractor`
   sur le résumé de chapitre fraîchement produit (déjà synthétique/filtré, pas le texte brut du
   chapitre) pour ré-ensemencer un instantané propre dans le `WorldState` vidé.

Effet : `entityStates` reste borné à "ce qui a été mentionné pendant le chapitre en cours",
rafraîchi à chaque transition, au lieu de grossir sur tout le livre. Le `StateExtractor` par
séquence dans `WriteWorkflow` (Phase 4) reste inchangé — il continue d'accumuler/écraser pendant
UN chapitre (sémantique `Map.put` = dernière valeur gagne par entité), mais cette accumulation
repart désormais à zéro à chaque transition.

### `ChapterSummaryResult`
+ champ `stateReseeded` (true si `StateExtractor` a rapporté au moins un changement lors du
réensemencement) — pour le logging.

### `EvaluateWorkflow`
+ ligne de log dédiée `"SequenceStateExtractor (reseed chapitre)"`, distincte des appels
`SequenceStateExtractor` par séquence dans les logs (même agent/prompt réutilisé aux deux
niveaux, mais l'étiquette de step workflow différencie les deux points d'appel).

### `RedacteurModule`
Câblage du `stateExtractorStep` existant dans `ChapterSummaryStep`.

### Tests
`ChapterSummaryStepTest` (nouveau, aucun test n'existait avant pour cette classe) :
- une entité périmée d'un chapitre précédent est bien remplacée (pas cumulée) après transition ;
- le résumé narratif (`Story.summary()`) ne contient jamais de ligne d'état d'entité ;
- sentinelle `AUCUN` → `entityStates` reste vide après le clear, `stateReseeded = false` ;
- `stateReseeded` reflète fidèlement si `StateExtractor` a rapporté un changement.

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec (4 nouveaux tests
`ChapterSummaryStepTest` + suite existante). Non encore observé en usage réel — prochain run
attendu : `WorldState.entityStates` toujours petit et à jour (jamais plus que "ce qui compte dans
le chapitre en cours"), le résumé narratif inchangé dans sa forme.
