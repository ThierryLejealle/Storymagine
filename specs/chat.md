# Module chat

Console de roleplay avec un LLM local (Ollama ou llama-server) : le joueur discute avec un ou
plusieurs personnages (PNJ) tenus par le LLM, dans un décor et une progression fixés par un
"chatscenario". Contrairement à `redacteur`, pas de plan ni de chapitres générés : une conversation
qui continue tant qu'on veut, guidée par des actes écrits à la main, avec compaction automatique de
l'historique pour ne jamais saturer le contexte du modèle.

Documents liés :
- `chatscenarios/rules.md` — référence détaillée pour écrire un scénario (pièges, mécanismes) ; sa
  section 6 décrit le dossier `run/` côté utilisateur.
- `chatscenarios/CLAUDE.md` — règle d'écriture à la 3e personne.
- `.claude/skills/chat-scenario-writing/SKILL.md` — guide pratique d'écriture (mono/multi-PNJ, fiche
  perso, fichiers, `[NEXT ACT]`).
- `chat/src/main/java/.../coeur/domaine/agent/*/*.md` — un document par agent.

# Le scénario (ce que l'auteur écrit)

Un chatscenario est un dossier. Il ne contient que des fichiers écrits à la main ; l'appli n'y
écrit jamais, sauf dans `run/` (voir la dernière section).

```
chatscenarios/{nom}/
  scenario.txt          obligatoire — ligne Joueur, prémisse, actes
  scenario-1.txt ...    optionnel — suite des actes, un fichier par tranche (voir plus bas)
  characters/           obligatoire — un fichier .txt par personnage
    {pnj}.txt           première ligne "# Nom" obligatoire
  picks/{nom}.txt       optionnel — paragraphes pour [PICK: nom] (voir « Contenu aléatoire »)
  run/                  généré par l'appli — voir « Le run »
```

- **Cast** : tout `.txt` de `characters/` dont la première ligne est `# Nom` est un PNJ ; un `.txt`
  sans cette ligne est ignoré silencieusement. Seul `characters/` est lu : un `.txt` à la racine du
  scénario n'est jamais un PNJ, et il n'y a plus de liste de fichiers réservés. Un scénario à un
  seul PNJ est simplement un Cast d'un élément — aucun cas particulier. Le nom de fichier (sans
  `.txt`) est l'identifiant interne du PNJ.
- **Fichiers du scénario** (`scenario*.txt`, `characters/*.txt`) : jamais réécrits ni compactés par
  l'appli.
- **Scénario listé** : `scenario.txt` présent et au moins un PNJ valide dans `characters/`. Un scénario
  encore à l'ancien format est listé aussi, pour que le choisir affiche le message de migration.
- **Scénario en plusieurs fichiers** : `scenario-1.txt`, `scenario-2.txt`… sont concaténés après
  `scenario.txt`, numéros consécutifs (arrêt au premier manquant). Chacun doit commencer par un
  titre `# ...` de premier niveau, sinon le chargement échoue explicitement.
- **Ancien format** : si un fichier de session de l'ancienne structure est détecté à la racine du
  scénario (`history.md`, `summary.md`, `act.txt`, `present.txt`, `interject.txt`, `full-history.md`,
  ou un dossier `saves/`, `archive/`, `logs/`, `tests/`), ou un PNJ à la racine au lieu de
  `characters/`, le chargement échoue avec un message qui demande de lancer `migration.bat` (jamais de
  perte silencieuse d'une ancienne partie).

# Format de `scenario.txt`

1. Ligne optionnelle `Joueur : Prénom` (défaut `Alex`) — nom du joueur dans la transcription.
2. Prémisse : `#SCENARIO` optionnel + `Monde :` / `Prémisse :` / `Ton :`. Envoyée à chaque tour à
   chaque PNJ, jamais montrée telle quelle au joueur. **3e personne obligatoire.**
3. Actes : plan Markdown (`#`, `##`…). **Seules les feuilles sont des actes jouables** (un titre
   avec sous-titre est un chapitre : son texte est hérité par ses enfants). Deux actes séquentiels
   indépendants = deux titres du même niveau.
4. Dans chaque acte :
   - lignes `[...]` (crochet ouvrant en début de ligne, fermant en fin de ligne) : narration
     affichée au joueur quand l'acte devient courant, et aussi lue par les PNJ. 3e personne.
   - texte de consigne lu par le LLM seulement : `Point de départ`, `Ce qui doit se passer`,
     `Quand écrire [NEXT ACT]` (libellés = convention, non parsés).

Scénario sans acte : un seul bloc `[...]` est affiché à l'ouverture (les suivants sont ignorés).

# Format d'un fichier personnage

`# Nom` (obligatoire, première ligne) + fiche publique, puis optionnellement une ligne `# SECRET`
suivie de la fiche secrète. Le PNJ qui parle reçoit sa fiche complète ; les autres PNJ présents
ne reçoivent que la fiche publique des autres (le secret ne fuit jamais).

# Progression d'actes — `[NEXT ACT]`

- L'acte courant et son texte sont injectés dans le system prompt (`ChatPromptBuilder`).
- Sauf sur le dernier acte, une règle est ajoutée : si le texte de l'acte contient lui-même le
  marqueur `[NEXT ACT]` (détecté par `NextActMarker`, tolérant : casse, espaces, `_`, `-`), le LLM
  reçoit la règle **conditionnelle** (n'écrire le marqueur qu'une fois la condition réellement
  survenue ; finir l'échange en cours avant) ; sinon la règle **par défaut** (« conclusion
  naturelle de la scène »).
- Le LLM écrit `[NEXT ACT]` à la fin de sa réponse ; le marqueur est retiré avant affichage et
  l'acte suivant devient courant : ses lignes `[...]` sont ajoutées à l'historique comme tours
  NARRATOR.
- Correction manuelle : boutons Précédent / Suivant. Bouton 🔍 : `NextActReadinessAnalyst`
  analyse sans rien persister où en est la condition (voir son `.md`). Bouton d'analyse d'état
  d'esprit : `NpcMindStateAnalyst`.

# Ouverture par un PNJ — `[NPC STARTS]`

Une ligne seule `[NPC STARTS]` ou `[NPC STARTS: Nom]`, dans l'en-tête du scénario (ouverture de
l'histoire, valeur `ChatScenario.storyOpening`) ou dans un acte (`ScenarioAct.opening`, ouverture de
cet acte). Concepts : `NpcOpening` (aucune / un PNJ au hasard / un PNJ nommé), `NpcOpeningMarker`
(détection tolérante comme `NextActMarker`, mais la ligne doit être **seule** ; elle est retirée du
texte avant que le LLM ou `Teaser` ne le voie).

- **Chargement** : `ScenarioOutlineParser` retire le marqueur de l'en-tête et de chaque corps de titre ;
  l'ouverture d'une feuille vient des nœuds nouvellement entrés (même règle que les beats). Un nom
  inconnu du `Cast` (`Cast.findByName` : id, nom complet ou un mot du nom) fait échouer
  `ChatScenario` (`IllegalArgumentException`).
- **Déclenchement** : sans état stocké. `ChatSession.npcOpeningAt(index)` dit si un `DO:` vide placé là
  ouvrirait l'histoire ou l'acte : l'acte du tour a une ouverture et, avant lui, aucun tour joueur ou
  PNJ n'existe dans cet acte (seulement des NARRATOR). `pendingNpcOpening()` = la même question pour
  la suite de la conversation ; la vue web l'expose (`npcOpeningPending`).
- **Exécution** : la page envoie un `DO:` vide (comme un clic sur DO) après le chargement, un
  redémarrage ou un changement d'acte ; `ChatServiceImpl.sendMessage` reconnaît ce cas
  (`PlayerMessage.isEmptyHandoff`) et donne la parole à `SpeakerSelector.openingSpeaker` : le PNJ nommé
  s'il est présent, sinon un PNJ présent au hasard ; **un seul locuteur**, aucune interjection. Le
  retry d'un tel tour retrouve le même PNJ. Aucun prompt n'est modifié : le LLM voit `DO:`.
- Pas d'ouverture après un undo, un rollback ou une sauvegarde chargée, ni après un arrêt ou une
  erreur ; une pression sur Stop pendant l'ouverture retire le `DO:` et la relancera au prochain
  chargement de la page.

# Contenu aléatoire — `[PICK: nom]` et `[PICK-UNIQUE: nom]`

Syntaxe, format de `picks/nom.txt` et exemple complet : `chatscenarios/rules.md` §2.6. Ici, la mécanique.

- **Concepts** : `PickTag` (nom = fichier sans `.txt`, `[\p{L}\p{N}_-]+`, jamais un chemin),
  `PickParagraphs` (contenu d'un fichier coupé sur les lignes `---` ou plus ; CRLF toléré, paragraphes
  vides ignorés), `PickLibrary` (nom → paragraphes ; `PickFileLibrary` lit `picks/`), `PickResolver`
  (domaine, pur), port `RandomnessPort` (`JavaRandomnessAdapter` ; un test branche un tirage scripté).
- **Règles** : `PICK` tire indépendamment. `PICK-UNIQUE` exclut tout paragraphe déjà distribué pour ce
  tag pendant la résolution — par un `PICK-UNIQUE` ou un `PICK` ; un `PICK` ignore l'historique. La
  mémoire couvre tous les fichiers du scénario, lus dans l'ordre (`scenario.txt`, `scenario-1.txt`…).
  Plus aucun candidat : `IllegalStateException` nommant le tag. Un tag dans un paragraphe tiré n'est
  pas résolu. Insensible à la casse (`[pick-unique: x]`).
- **Quand** : `ChatFileStorageAdapter.loadScenario` → `scenarioTextDir`. Si `run/current/resolved/
  scenario.txt` existe, il est lu (les tirages sont figés pour la durée du run). Sinon, si un fichier
  du scénario contient un tag, tout est résolu en une fois puis écrit dans `resolved/` (écriture
  atomique) ; sans tag, rien n'est écrit et le scénario est lu en place.
- **Cycle de vie** : `resolved/` fait partie de `current/` — supprimé par `resetSession` (donc tirage
  neuf au chargement suivant), zippé dans les sauvegardes et archives (`RunArchiver`), restauré avec
  elles. Pour que le scénario en mémoire corresponde toujours au run : `openSession(reset=true)` et
  `restartSession` rechargent le scénario **après** le reset, `loadSavePoint` le relit depuis la
  sauvegarde. Corollaire : modifier `scenario*.txt` en cours de run est sans effet (⟳ relit la copie
  résolue) jusqu'au prochain « Recommencer ».
- Les fiches personnage ne sont pas résolues (extension possible plus tard). Aucun prompt modifié.

# Conventions de formatage du chat

Injectées dans chaque prompt (`ChatPromptBuilder`) :
- `*texte*` — action ou détail de scène, jamais une réplique. Les pensées privées d'un PNJ entre
  astérisques ne sont pas connues des autres (pas de metagaming).
- `OOC: ...` — le joueur sort du jeu pour parler directement au LLM.
- `DO: ...` — le joueur confie la plume du narrateur au LLM pour cette seule réponse.

# Qui répond (multi-PNJ)

`SpeakerSelector`, trois étapes : (1) PNJ nommés dans le message, dans l'ordre d'apparition ;
(2) sinon le PNJ unique du tour précédent ; (3) sinon tirage aléatoire parmi les PNJ éligibles
à l'interjection. Ensuite, chaque autre PNJ présent et éligible tire sa chance d'interjecter
(réglage « Chance d'interjection »). Présence (🔊/🔇) et interjection (💬) se règlent par PNJ.
Détail : `chatscenarios/rules.md` §4.

# Appel LLM

Tous les appels (tour de roleplay, résumé, analyses) passent par `ModelCallPort` (rôles system +
user), implémenté par `OllamaAdapter` ou `LlamaServerAdapter` — choix au démarrage, câblage dans
`ChatModule`. Le mode brut (`RawCompletionPort`) a été abandonné pour le roleplay : il retire le
template de chat appris par le modèle et provoque des dérives. La réflexion (thinking) est
toujours demandée ; son affichage est une option du joueur. La langue de réponse suit celle des
fiches et du scénario (aucune langue imposée par le code). Une réponse vide est retentée une
fois. Le bouton Stop interrompt réellement la génération côté serveur.

# Agents (un `.md` chacun dans son package)

| Agent | Rôle |
|---|---|
| `RoleplayNarrator` | Joue un PNJ pour un tour |
| `ChatSummarizer` | Replie les anciens tours dans le résumé |
| `NextActReadinessAnalyst` | Diagnostique la condition `[NEXT ACT]` (lecture seule) |
| `NpcMindStateAnalyst` | Diagnostique l'état d'esprit d'un PNJ (lecture seule) |
| `ScenarioClarityReviewer`, `ScenarioContinuityReviewer` | Testeur de scénario (`ScenarioTesterCli`) |

# Compaction de l'historique

Quand la transcription non compactée dépasse le budget lié à la fenêtre de contexte, les tours
les plus anciens sont foldés dans un résumé par `ChatSummarizer` ; les tours récents restent
verbatim. La compaction a lieu **à la fin d'un tour complet** (après la réponse de tous les PNJ),
jamais en cours de tour. Elle ne supprime aucun tour du disque : chaque compaction est un fichier
de plus dans la suite des tours (voir « Le run »), ce qui permet de restaurer l'état exact à
n'importe quel tour. Le résumé est une prose libre qui s'auto-régule (faits obsolètes remplacés,
pas accumulés) et **cumulative** : le nouveau résumé reçoit l'ancien en entrée. Détail :
`ChatSummarizer.md`.

# Interface web

`ChatCli` démarre un serveur HTTP local minimal (`ChatWebServer`, JDK `HttpServer`, pas de
framework) servant `chat.html` (vanilla JS/CSS). Endpoints JSON : `/history`, `/message`, `/retry`,
`/undo`, `/rollback`, `/stop`, `/restart`, `/settings`, `/next-act`, `/previous-act`,
`/analyze-next-act`, `/analyze-mind-state`, `/set-present`, `/set-interjecting`,
`/reload-scenario`, `/save`, `/saves`, `/load-save`, `/delete-save`. Les réponses sont streamées
(texte + réflexion). Un seul thread traite les requêtes — pas d'accès concurrent. Chaque tour est
écrit sur disque dès qu'il existe ; sauvegardes manuelles illimitées ; ⟳ recharge scénario et
fiches sans toucher à la conversation. Chaque bulle affiche son numéro `#N` et un bouton ↩ qui
appelle `/rollback` (`/undo` et `/retry` en sont des cas particuliers).

# Outils CLI

`ChatCli` (jeu, `chat.bat` ; ne propose « Continuer ? » que s'il existe une partie jouée, voir
`ChatService.hasSavedSession` — sinon il démarre une nouvelle partie directement), `ScenarioTesterCli` (revue d'un scénario par agents reviewers,
`test-scenario.bat`), `InitChatCli` (nouveau scénario : dossier, nom, nombre et noms des PNJ, puis
`ScenarioScaffolder` crée `scenario.txt` et `characters/*.txt` à remplir, `init-chat.bat`),
`MigrateScenarioLayoutCli` (migration de l'ancienne structure, `migration.bat`, voir « Le run »).
Lancés par des `.bat` minimaux qui transmettent leurs arguments ; la classe lancée par `exec:java`
vient de la propriété `exec.mainClass` du `pom.xml` du module (défaut `ChatCli`). Le choix du dossier
des scénarios et le chargement des propriétés sont partagés dans `ChatCliSupport`. Option commune `--logs` : active l'écriture des
logs détaillés (par défaut, aucun log n'est écrit sur disque).

**Plusieurs chats en parallèle** : `chat.web.port` (8765) est le port *préféré*. S'il est occupé,
`ChatWebServer` essaie les suivants (8766, 8767…, 20 essais) et la CLI affiche l'URL réellement
utilisée. Attention : aucun verrou n'empêche d'ouvrir **le même scénario** dans deux instances ; elles
écriraient toutes deux dans `run/` (à éviter). Ollama et la VRAM restent partagés entre instances.

# Le run : état de partie, historique, compactions

Tout ce que l'appli génère pour une partie vit dans `chatscenarios/{nom}/run/`. Cette section est la
spécification technique ; sa version « utilisateur » est `chatscenarios/rules.md` §6.

## Concepts (noms métier)

| Concept | Rôle |
|---|---|
| `TurnNumber` | Identifiant d'un tour : entier ≥ 1, +1 à chaque tour, ne redescend que par un retour en arrière |
| `ChatTurn` | Un tour (joueur, PNJ ou narrateur), porte son `TurnNumber` et l'acte en vigueur à l'écriture ; le numéro n'est **jamais** dans `text()` |
| `Compaction` | Un résumé cumulatif ; `createdAfter` (R) = dernier tour existant à sa création, `foldedUpTo` (F) = dernier tour replié, `F < R` ; contient le texte du résumé |
| `rollbackTo(TurnNumber)` | Opération de service : ramène la partie à l'état juste après ce tour, destructive |

## Arborescence

```
run/
  current/
    turns/
      {n:6}.md                          un tour
      {R:6}-compaction-f{F:6}.md        une compaction
    state.txt                           présence et interjection
    resolved/scenario[-N].txt           scénario dont les [PICK] sont tirés (absent sans tag)
  saves/{yyyy-MM-dd-HHmmss-SSS}.zip     zip de current/ (sauvegarde manuelle)
  archive/{yyyy-MM-dd-HHmmss-SSS}.zip   zip de current/ (archivage par « Recommencer »)
  logs/                                 seulement avec --logs
  tests/{horodatage}/report.html        rapport du testeur de scénario
```

## Formats

**Fichier de tour** `turns/{n:6}.md` — le nom porte le numéro (6 chiffres, complétés par des zéros) :

```
----- #13 · acte 2 -----
### LLM: elena
texte du tour
```

- Ligne 1 : `^----- #(\d+) · acte (\d+) -----$` (acte 0 = scénario sans actes).
- Ligne 2 : `### PLAYER`, `### NARRATOR` ou `### LLM: {npcId}`.
- Reste du fichier : le texte du tour, tel quel.
- Concaténés dans l'ordre, les fichiers forment un journal lisible (pas d'export automatique).
- La réflexion (`thinking`) n'est pas persistée (comme aujourd'hui).

**Fichier de compaction** `turns/{R:6}-compaction-f{F:6}.md` — R et F sont dans le nom (aucune
lecture du fichier n'est nécessaire pour les connaître) ; le contenu est exactement le résumé. Il
n'y a pas d'UID : R est unique dans une histoire linéaire.

**`state.txt`** (`RunState`) :

```
act=2
present=elena,marcus
interjecting=elena
```

`act` : l'acte en vigueur maintenant (il peut différer de l'acte du dernier tour après un « Précédent »
manuel, qui ne crée aucun tour). `present` / `interjecting` : un id de PNJ par valeur, séparés par des
virgules. Fichier absent, ou clé absente : tous les PNJ présents / éligibles à l'interjection. Les
réglages de génération ne sont pas persistés. Écrit après chaque `saveSession`, de façon atomique
(fichier temporaire puis renommage).

**Zips** : `turns/`, `state.txt` et `resolved/` de `current/`, chemins relatifs à `current/`. Fichiers immuables,
écrits via `java.util.zip`. Charger un zip remplace entièrement `current/`.

## Ordre

Les noms de fichiers de `turns/` sont analysés (regex, jamais un tri alphabétique) en une position
`(numéro, genre)` où le genre vaut 0 pour un tour et 1 pour une compaction de même R : une
compaction se place juste **après** le tour R.

## Algorithmes

**A. Écrire un tour.** `n = dernier numéro + 1` ; créer `turns/{n:6}.md` (écriture dans un fichier
temporaire puis renommage : un crash n'abîme jamais un tour au milieu).

**B. Compacter** (appelé à la fin d'un tour complet, comme `compactIfNeeded` aujourd'hui).
1. Déclenchement et découpage inchangés : `toFold` = les tours en clair jusqu'à F, `toKeep` = le reste.
2. `ChatSummarizer` reçoit le résumé courant et le transcript de `toFold`.
3. Résumé vide : rien n'est écrit, la compaction sera retentée au prochain tour.
4. Sinon créer `turns/{R:6}-compaction-f{F:6}.md` avec le résumé, où `R` est le dernier numéro existant.

**C. Reconstruire l'état au tour N** (chargement : N = dernier numéro ; retour : N demandé).
1. Lister `turns/` (noms seulement).
2. `C` = la compaction avec le plus grand R tel que `R ≤ N`, ou aucune.
3. `summary` = contenu du fichier de `C`, ou `""`.
4. `window` = les fichiers de tour de `(C ? C.F+1 : 1)` à `N` : seuls fichiers de tour lus.
5. `currentAct` = acte inscrit sur le tour N. Au chargement d'une partie (N = dernier tour),
   l'`act` de `state.txt` prime s'il est présent (cas du « Précédent » manuel).
6. `presentNpcIds`, `interjectingNpcIds` : lus dans `state.txt`, jamais déduits du tour.
La fenêtre n'est jamais vide quand `F < R ≤ N` ; elle l'est seulement si une compaction a tout replié
(`F = R`), l'acte est alors lu sur le fichier du tour N lui-même.

Implémentation : `ChatFileStorageAdapter.rebuildAt` (algorithme C), `loadSessionAt` (lecture seule),
`truncateAfter` (suppression), `saveSession` (écrit chaque tour de la fenêtre seulement s'il a changé,
supprime tout tour ou compaction numéroté au-delà du dernier tour de la session, réécrit `state.txt`).

**D. Retour au tour N** (`rollbackTo`).
1. Préconditions : `0 ≤ N ≤ dernier numéro` (sinon `IllegalArgumentException`) ; N = dernier numéro
   est sans effet. « Aucune génération en cours » est garanti par l'interface, qui désactive ses
   contrôles pendant un round (comme pour toutes les autres actions) ; le service ne le vérifie pas.
2. L'interface a demandé une confirmation explicite et proposé une sauvegarde préalable ; le service
   n'en fait jamais en silence.
3. Supprimer tout fichier de `turns/` dont la position (numéro de tour, ou R pour une compaction)
   est supérieure à `N`. Une compaction avec R = N est conservée.
4. Reconstruire l'état par C avec ce N et remplacer l'état en mémoire (`ChatSession.restore`).
5. Si la fenêtre dépasse le budget de contexte, lancer la compaction B immédiatement (le prochain
   appel du modèle ne doit pas partir d'un contexte trop gros ; cas d'un retour avant la première
   compaction).
6. Le tour suivant reçoit le numéro `N+1`.
Un crash entre 3 et 4 est sans conséquence : l'état se reconstruit par C depuis les fichiers.

**E. Sauvegarde et archivage.** Zipper `current/` vers `saves/{stamp}.zip` ou `archive/{stamp}.zip`
(stamp à la milliseconde). « Recommencer » archive puis recrée un `current/` vide (tours d'ouverture
du narrateur, numérotés à partir de 1). Charger une sauvegarde : confirmation, puis remplacer
`current/` par le zip. La liste des sauvegardes affiche le dernier numéro de tour de chacune (lu sur
les noms d'entrées du zip).

## Exemple complet

```
turns/ 000011.md 000012.md 000013.md 000013-compaction-f000012.md 000014.md 000015.md 000016.md

N=16 : résumé de la compaction, tours 13..16
N=14 : résumé de la compaction, tours 13..14
N=13 : résumé de la compaction, tour 13
N=12 : pas de résumé, tours 1..12
```

Retour au #12 : suppression de 000013.md, 000013-compaction-f000012.md, 000014.md, 000015.md et
000016.md. Retour au #13 : suppression de 000014.md à 000016.md seulement.

## Cas limites

- Retour au milieu d'un tour (réponse d'un PNJ parmi plusieurs) ou sur une ligne du narrateur :
  autorisé, aucun cas particulier.
- Retour sur le tour qui a déclenché un `[NEXT ACT]` : l'acte restauré est celui inscrit sur ce tour
  (avant l'avancée), et les lignes `[...]` du narrateur qui suivent sont supprimées avec le reste.
- Retour à N = dernier numéro : sans effet. Retour pendant une génération : empêché par l'interface.
- `undo` et `retry` partagent le même noyau destructif que `rollbackTo` (`ChatServiceImpl.rewindTo` :
  état reconstruit depuis le disque au tour visé, suppression de la suite, remplacement de la conversation
  en place ; un tour déjà en mémoire sous le même numéro et le même texte est conservé, pour que sa
  réflexion, jamais persistée, survive). Ils restaurent donc aussi l'acte et le résumé.
- Chaque tour est écrit sur disque dès qu'il existe (après le tour du joueur, après chaque réponse) ;
  un `GenerationCancelledException` avant toute réponse retire aussi le fichier du tour du joueur.
- Réinitialisation ou nouvelle partie : `current/` repart vide, numérotation à 1.

## Logs

`FileLogAdapter` n'écrit rien tant que `setOutputDir` n'est pas appelé. `ChatCli` ne l'appelle (avec
`run/logs`) que si l'option `--logs` est présente. `ScenarioTesterCli` écrit toujours son
`report.html` dans `run/tests/{horodatage}/` (c'est son produit) ; ses logs d'appels suivent
`--logs`. `clean-logs.bat` nettoie `run/logs/` et `run/tests/`.

## Migration depuis l'ancienne structure

`migration.bat` (qui ne fait que lancer `MigrateScenarioLayoutCli`) ne **convertit pas** les anciennes
parties : il garde le scénario et jette le reste.

- Pour chaque dossier de scénario : déplacer les fiches personnage (`.txt` dont la première ligne est
  `# Nom`, hors `scenario.txt` et `scenario-N.txt`) vers `characters/` ; les `.txt` sans cette ligne
  sont laissés en place et signalés.
- Supprimer `history.md`, `summary.md`, `act.txt`, `present.txt`, `interject.txt`, `full-history.md`
  et les dossiers `saves/`, `archive/`, `logs/`, `tests/`.
- Racines traitées : `chatscenarios/` du projet, le dossier `chatscenarios.root.default` de
  `chat.properties`, ou un chemin donné en argument (le programme n'est jamais lancé par l'assistant
  sur un dossier hors projet).
- `--dry-run` : liste les déplacements et suppressions (avec les tailles) sans rien modifier.
  Sans `--dry-run`, une confirmation explicite est demandée.
- Idempotent : un scénario déjà migré est ignoré. Un résumé final compte les scénarios migrés, déjà
  à jour et en conflit (par exemple `characters/` existe déjà avec un fichier du même nom : le
  scénario est ignoré avec un message, rien n'est écrasé).
