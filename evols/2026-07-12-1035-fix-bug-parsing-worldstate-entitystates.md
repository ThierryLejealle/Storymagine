# 2026-07-12 10h35 — Fix bug : WorldState.entityStates jamais alimenté (parsing ETAT:)

## 1. Demande

En analysant `252_SequenceWriter_15.md` (scénario 1998), l'utilisateur s'est inquiété du bloc
`**État des personnages :**` fourni au Writer dans "Histoire jusqu'ici (résumé)" : totalement
obsolète (décrit l'état après l'archerie — entraînement de Christelle terminé, badge d'accueil —
alors que la séquence 15 se déroule des jours plus tard, après Collioure). Ce bloc vient de
`ChapterSummarizer`, qui écrit un `État des personnages` indépendant par chapitre, jamais
fusionné ni remplacé (concaténation pure dans `Story.appendChapterSummary`) — accumulation de
snapshots obsolètes au fil des chapitres, sans étiquette de chapitre/date pour les distinguer.

## 2. Ce qui a été touché

### Diagnostic
En creusant pourquoi cette accumulation obsolète est le SEUL état de personnage vu par le
Writer, découverte du vrai bug : le second mécanisme censé fournir un état toujours à jour —
`WorldState.entityStates`, alimenté après chaque séquence par `StateExtractor` et affiché au
Writer sous `"État actuel des entités"` — n'a **jamais fonctionné**, sur aucune séquence, dans
aucun run. Confirmé par le log réel `250_SequenceStateExtractor_12.md` (réponse LLM juste avant
l'appel Writer 15) et par l'absence totale de la section `"État actuel des entités"` dans le
prompt Writer 15 lui-même (section vide → `PromptBuilder.section()` l'omet entièrement).

**Cause** : `StateExtractor` produit `ETAT: [entité] → [état actuel]` (flèche, format documenté
dans son propre Javadoc et cohérent avec `WorldState.applyPlotDirectives` qui utilise la même
syntaxe flèche ailleurs). Mais `StoryFormatters.applyStateLines()` cherchait un **second `:`**
pour séparer entité et état — jamais trouvé puisque le séparateur réel est `→`, pas `:`. Résultat :
la condition `colon > 0` n'était jamais vraie, aucune entité n'était jamais ajoutée à
`WorldState.entityStates`.

Effet secondaire découvert au passage (non corrigé, hors périmètre de cette fiche) :
`WorldState.recentEvents()`/`plotDirectives()` ne sont lus nulle part hors de
`WorldState`/snapshot-restore DREAM/WHAT_IF — les lignes `EVENT:` de `StateExtractor` (qui, elles,
parsent correctement) ne sont donc pas exploitées pour la continuité du Writer non plus.

### Fix — `StoryFormatters.applyStateLines()`
Aucun prompt LLM modifié (le prompt `StateExtractor` produisait déjà le bon format). Pur fix
Java : le parseur cherche désormais `→` au lieu d'un second `:`. Doc-comment corrigé en
conséquence (`"ETAT: Entity → state"` au lieu de `"ETAT: Entity: state"`).

### Tests
`StoryFormattersTest` (nouveau, aucun test ne couvrait ce parseur avant) : format flèche peuple
bien `entityStates`, formatage `entityState()`, plusieurs lignes ETAT/EVENT, sentinelle `AUCUN`,
entrée `null`/vide, `WorldState` vide → chaîne vide. Régression construite directement sur la
réponse réelle du bug (`ETAT: Thierry → en difficulté respiratoire`).

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec. Non encore
observé en usage réel (prochain run devrait faire apparaître la section `"État actuel des
entités"` dans les prompts Writer, avec un état vivant par personnage/entité en plus — pas en
remplacement — des blocs `État des personnages` de `ChapterSummarizer`, qui restent inchangés
et continueront d'accumuler des snapshots par chapitre).

## Note — pas traité ici

Le problème initial signalé par l'utilisateur (blocs `État des personnages` obsolètes qui
s'accumulent dans le résumé, sans étiquette de recency) n'est PAS résolu par ce fix — le fix
répare le canal parallèle (`WorldState`) qui devrait déjà limiter l'impact réel de cette
obsolescence sur le Writer, mais les blocs `ChapterSummarizer` continuent d'être générés et
concaténés tels quels. À rediscuter si l'utilisateur souhaite aussi traiter la staleness du
résumé narratif lui-même (ex. n'inclure "État des personnages" que pour le dernier chapitre,
ou étiqueter chaque bloc avec son numéro de chapitre).
