# 2026-10-25 - Refonte de la structure scénario / run : journal numéroté, un fichier par tour, retour en arrière

## 1. Évolution demandée

Le dossier d'un scénario de chat mélangeait le scénario écrit à la main (scenario.txt, fiches
personnage) et les fichiers générés pendant le jeu (history.md, summary.md, act.txt, present.txt,
interject.txt, full-history.md, saves/, archive/, logs/, tests/). Décisions validées avec
l'utilisateur :

- Le scénario seul à la racine : `scenario.txt`, `scenario-N.txt`, `characters/` (fiches dans un
  sous-dossier).
- Tout ce qui est généré dans `run/` : `run/current/` (partie en cours), `run/saves/` et
  `run/archive/` (zips de `current/`, même forme), `run/logs/`, `run/tests/`.
- Un numéro de tour (`TurnNumber`) qui s'incrémente à chaque tour (joueur, PNJ, narrateur), avec un
  séparateur `----- #N · acte A -----` en première ligne de chaque fichier de tour, jamais visible
  du LLM (champ du tour, pas texte).
- Un fichier par tour dans `run/current/turns/`. Une compaction est un fichier de la même suite,
  nommée d'après son index (`{R}-compaction-f{F}.md`) : pas d'UID. Le résumé est cumulatif.
- Retour en arrière au tour N (`rollbackTo(TurnNumber)`, bouton ↩ sur chaque bulle) : destructif,
  confirmation explicite avec proposition de sauvegarde ; restaure tours, résumé (bonne
  compaction) et acte ; ne restaure pas présence, interjection ni réglages.
- Logs et dossier `logs/` non générés par défaut : seulement avec l'option `--logs`.
- Migration : `migration.bat` conserve le scénario (déplace les fiches vers `characters/`) et jette
  l'historique, les sauvegardes, les archives et les logs. Pas de conversion des anciennes parties.
- Correction annexe : l'ancien `full-history.md` n'était pas archivé par « Recommencer » et mélangeait
  plusieurs parties ; la nouvelle structure archive la partie complète dans un zip.
- Documentation détaillée, le scénario d'abord, la structure `run/` en fin de document.

## 2. Ce qui a été touché

Documentation :
- `chatscenarios/rules.md` : réécrit autour de deux moitiés (§1 à §3 le scénario, avec un exemple
  minimal complet au §1.1 ; §6 le run), ajout du §4.8 (retour en arrière), mises à jour de §4.1,
  §4.6, §4.7.
- `specs/chat.md` : sections « Le scénario » et « Le run » (formats, algorithmes A à E, cas limites,
  logs, migration), compaction, interface web, outils CLI.
- `.claude/skills/chat-scenario-writing/SKILL.md` : arbre et règles.

Code, module `chat` :
- Domaine (`coeur/domaine/session`) : `TurnNumber`, `NumberedTurn`, `Compaction` (nouveaux) ;
  `ChatSession` (fenêtre de tours numérotés avec l'acte de chaque tour, `compactInto` qui rend la
  `Compaction`, `restored`, `restoreFrom`, `restoreConversationFrom`) ; `SavePoint` porte le numéro
  de son dernier tour. `ChatTurn` est inchangé : le numéro n'est jamais dans le texte du tour.
- Port `ChatStoragePort` : `loadSessionAt`, `writeCompaction`, `truncateAfter` ajoutés,
  `archiveFoldedTurns` supprimé.
- Service : `rollbackTo` ; `undo` et `retry` passent par le même noyau (`rewindTo`, donc l'acte et le
  résumé reviennent aussi) ; chaque tour est écrit dès qu'il existe ; la compaction écrit son
  fichier ; `save` persiste avant de zipper ; `loadSavePoint` restaure tout.
- Infra : `ChatFileStorageAdapter` réécrit ; nouveaux `RunLayout`, `TurnFileCodec`, `RunState`,
  `RunArchiver`, `LegacyLayout`, `ScenarioLayoutMigrator`.
- UI : `ChatWebServer` (`/rollback`), `ChatHistoryView` (`lastTurnNumber`), `chat.html` (numéros
  `#N`, bouton ↩ avec double confirmation, numéro dans la liste des sauvegardes) ; `ChatCli` et
  `ScenarioTesterCli` (option `--logs`, rapports du testeur toujours dans `run/tests/`) ;
  `MigrateScenarioLayoutCli` (nouveau).
- `chat/pom.xml` : la classe principale d'`exec:java` passe par la propriété `exec.mainClass`
  (défaut `ChatCli`). Elle était figée sur `ChatCli`, ce qui rendait `-Dexec.mainClass=...` sans
  effet : `test-scenario.bat` lançait donc le chat et non le testeur de scénario. Corrigé au passage,
  ce qui permet aussi à `migration.bat` de fonctionner.
- Scripts : `migration.bat` (nouveau), `chat.bat` et `test-scenario.bat` transmettent leurs arguments,
  `clean-logs.bat` vise `run/logs` et `run/tests`, `.gitignore` ignore `chatscenarios/*/run/`.
- Tests : `ChatFileStorageAdapterTest` réécrit ; nouveaux `TurnFileCodecTest`, `RunLayoutTest`,
  `RunArchiverTest`, `ScenarioLayoutMigratorTest` ; `ChatSessionTest` et `ChatServiceImplTest`
  étendus (numérotation, compaction, retour en arrière, acte, présence non restaurée, sauvegarde
  complète, écriture immédiate de chaque tour).

## 3. Résultat

Compilation et tests verts depuis la racine du projet (`mvn test`, tous modules ; 293 tests pour le
module chat). Le dry-run de `MigrateScenarioLayoutCli` sur `chatscenarios/` du projet liste 7
scénarios à migrer sans rien modifier.

Non fait volontairement : la migration réelle n'a PAS été lancée. Elle supprime les anciennes
parties, sauvegardes, archives et logs : c'est à l'utilisateur de lancer `migration.bat` (qui traite
`chatscenarios/` du projet et le dossier `chatscenarios.root.default` de `chat.properties`, après
affichage de ce qui sera déplacé/supprimé et confirmation). Tant qu'il n'est pas lancé, un scénario à
l'ancien format est listé mais refuse de se charger avec un message qui renvoie à `migration.bat`.

Non vérifié : l'interface (`chat.html` : numérotation des bulles, bouton ↩) n'a pas été testée dans un
navigateur, seuls le serveur Java et le service le sont.
