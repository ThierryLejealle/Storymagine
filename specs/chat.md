# Module chat

Console de roleplay avec un LLM local (Ollama ou llama-server) : le joueur discute avec un ou
plusieurs personnages (PNJ) tenus par le LLM, dans un décor et une progression fixés par un
"chatscenario". Contrairement à `redacteur`, pas de plan ni de chapitres générés : une conversation
qui continue tant qu'on veut, guidée par des actes écrits à la main, avec compaction automatique de
l'historique pour ne jamais saturer le contexte du modèle.

Documents liés :
- `chatscenarios/guide-redaction-chat.md` — référence détaillée pour écrire un scénario (pièges, mécanismes) ; sa
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
  picks/{nom}.txt       optionnel — paragraphes pour {pick type nom} et {repeat mode nom} (voir « Contenu aléatoire »)
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
  encore à l'ancien format est listé aussi, pour que le choisir affiche le message « format non pris en charge ».
- **Scénario en plusieurs fichiers** : `scenario-1.txt`, `scenario-2.txt`… sont concaténés après
  `scenario.txt`, numéros consécutifs (arrêt au premier manquant). Chacun doit commencer par un
  titre `# ...` de premier niveau, sinon le chargement échoue explicitement.
- **Ancien format** : si un fichier de session de l'ancienne structure est détecté à la racine du
  scénario (`history.md`, `summary.md`, `act.txt`, `present.txt`, `interject.txt`, `full-history.md`,
  ou un dossier `saves/`, `archive/`, `logs/`, `tests/`), ou un PNJ à la racine au lieu de
  `characters/`, le chargement échoue avec un message « format non pris en charge » (jamais de
  perte silencieuse d'une ancienne partie).

# Format de `scenario.txt`

1. En-tête optionnel, lignes consécutives en tête de fichier : `Joueur : Prénom` (défaut `Alex`) — nom
   du joueur dans la transcription ; `Langue : français` (ou `fr`, `en`…) — langue de tous les appels du module (réponses des
   PNJ, résumé, analyses, testeur ; `ScenarioLanguage`, absente = non précisée). Sans cette ligne, la langue est non précisée.
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

# Ouverture par un PNJ — `{npc-starts}` et `{npc-do}`

Une ligne seule `{npc-starts}`, `{npc-starts Nom}`, `{npc-do}` ou `{npc-do Nom}`, dans l'en-tête du scénario
(ouverture de l'histoire, valeur `ChatScenario.storyOpening`) ou dans un acte (`ScenarioAct.opening`, ouverture
de cet acte). `starts` = ouverture normale, `do` = ouverture avec la plume du narrateur (`NpcOpening.doPen`).
Concepts : `NpcOpening` (aucune / un PNJ au hasard / un PNJ nommé, plus l'indicateur de plume), `NpcOpeningMarker`
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
- **Deux chemins** (`ChatSession.pendingOpeningUsesPen`, exposé par la vue web : `npcOpeningWithPen`) :
  ouverture **normale** (`{npc-starts}`) : la page appelle `POST /open-scene`, `ChatServiceImpl.openScene` fait
  parler le PNJ nommé sans tour joueur (même flux que `/continue`, factorisé dans `ChatWebServer.streamRound`) ;
  ouverture **avec la plume** (`{npc-do}`, ou `{npc-starts Nom}` d'un PNJ en mode DO) : voir ci-dessous.
- **Exécution (avec la plume)** : la page envoie un `DO:` vide (comme un clic sur DO) après le chargement, un
  redémarrage ou un changement d'acte ; `ChatServiceImpl.sendMessage` reconnaît ce cas
  (`PlayerMessage.isEmptyHandoff`) et donne la parole à `SpeakerSelector.openingSpeaker` : le PNJ nommé
  s'il est présent, sinon un PNJ présent au hasard ; **un seul locuteur**, aucune interjection. Le
  retry d'un tel tour retrouve le même PNJ. Aucun prompt n'est modifié : le LLM voit `DO:`.
- Pas d'ouverture après un undo, un rollback ou une sauvegarde chargée, ni après un arrêt ou une
  erreur ; une pression sur Stop pendant l'ouverture retire le `DO:` et la relancera au prochain
  chargement de la page.

# Modes d'un PNJ — `{npc-mode do on|off nom|*}`

Guide §2.12 pour l'auteur. Un mode se superpose au niveau d'activité ; aujourd'hui un seul, `do` (`NpcMode.DO`),
mais la forme `{npc-mode mode on|off nom|*}` est générique (mode inconnu : erreur listant `NpcMode.keywords()`).

- **Concepts** : `NpcMode` (enum, `fromKeyword`), `ActivityDirective` (kinds `MODE_ON` / `MODE_OFF` portant le mode,
  à côté de `LEVEL` et `RESET`), `ActivityDirectiveMarker` (retire la ligne, qui doit être seule), `ActivityResolver.modeIds`
  (rejoue sur le chemin de titres, racine d'abord, dans l'ordre écrit : un `on` ajoute, un `off` retire ; `*` = tout le
  `Cast`), `ChatScenario.npcIdsInMode(acte, mode)` et `ChatSession.npcIdsInMode(mode)`. Rien n'est stocké : recalculé
  depuis les actes, donc retour arrière, sauvegarde et rechargement redonnent l'état de l'acte. Les noms inconnus
  font échouer le chargement (`ChatScenario`, `ScenarioValidator`) ; interdit dans la prémisse.
- **Effet du mode DO** : `ChatServiceImpl.turnsSeenBy` présente au PNJ en mode DO le dernier tour joueur avec le préfixe
  `DO: ` (sauf ligne `OOC:`, ligne déjà `DO:`, ou tour sans message joueur : Continue, ouverture). Aucun prompt n'est
  modifié : la règle `DO:` de `ChatPromptBuilder` s'applique telle quelle. L'historique et l'écran gardent le texte du joueur.
  Les PNJ d'un tour répondent l'un après l'autre et le suivant lit la réponse du précédent (collision limitée, pas supprimée).
- **Écran** : `NpcView.doMode` → étoile ★ sur la vignette (`chat.html`).
- **Ouverture** : `ChatSession.pendingOpeningUsesPen` (voir ci-dessus).

# Contenu aléatoire — `{pick type nom}`

Syntaxe, format de `picks/nom.txt` et exemple complet : `chatscenarios/guide-redaction-chat.md` §2.6. Ici, la mécanique.

- **Concepts** : `PickTag` (nom = fichier sans `.txt`, `[\p{L}\p{N}_-]+`, jamais un chemin),
  `PickParagraphs` (contenu d'un fichier coupé sur les lignes `---` ou plus ; CRLF toléré, paragraphes
  vides ignorés), `PickLibrary` (nom → paragraphes ; `PickFileLibrary` lit `picks/`), `PickResolver`
  (domaine, pur), port `RandomnessPort` (`JavaRandomnessAdapter` ; un test branche un tirage scripté).
- **Règles** : `PickMode` = `random` (un, indépendant), `unique` (un, hors déjà distribués), `all`
  (tous, ordre du fichier, sans hasard) ou N (N différents entre eux, tirés parmi tous). Ce que
  distribue une commande, quel que soit son mode, est exclu d'un `unique` ultérieur sur le même
  fichier ; `random` et N ignorent l'historique. La mémoire couvre tous les fichiers du scénario, lus
  dans l'ordre (`scenario.txt`, `scenario-1.txt`…). Plus aucun candidat pour un `unique`, ou N supérieur
  à la taille du fichier : `IllegalStateException` nommant le fichier. Commande mal formée (type
  manquant ou inconnu) : `IllegalArgumentException`, jamais laissée dans le texte. Plusieurs paragraphes
  sont joints par une ligne vide. Une commande dans un paragraphe tiré n'est pas résolue. Insensible à
  la casse. L'ancienne écriture `[PICK: x]` est refusée (`LegacyCommandSyntax`, voir « Validation »).
- **Quand** : `ChatFileStorageAdapter.loadScenario` → `scenarioTextDir`. Si `run/current/resolved/
  scenario.txt` existe, il est lu (les tirages sont figés pour la durée du run). Sinon, si un fichier
  du scénario contient un tag, tout est résolu en une fois puis écrit dans `resolved/` (écriture
  atomique) ; sans tag, rien n'est écrit et le scénario est lu en place.
- **Tirages affichés** : `PickResolver.resolve` renvoie aussi les `PickDraws` (un `PickDraw` = type, `PickTag`,
  paragraphe, dans l'ordre des tags), écrits dans `resolved/picks.txt` **avant** les `scenario*.txt`.
  `ChatStoragePort.pickDraws` les relit (vide pour un run ancien) ; `ChatService.pickDraws` →
  `GET /picks` → bouton 🎲 (tableau Fichier / Type / Valeur, une ligne par commande `pick`).
- **Séquence répétée `{repeat order|random [N] nom}` … `{end-repeat}`** (guide §2.6 bis) : un gabarit
  recopié une fois par paragraphe d'un `picks/nom.txt`. Concepts : `RepeatMode` (`order` : les N premiers
  du fichier ; `random` : N différents tirés avec `RandomnessPort` ; sans N, tout le fichier),
  `PickParagraph` (un paragraphe et ses morceaux, coupés sur les lignes ne contenant que `-`),
  `RepeatExpander` (domaine, pur, lignes `{repeat}` / `{end-repeat}` seules, pas d'imbrication),
  `RepeatExpansion` (textes dépliés + un `PickDraw` par bloc). Zones : `{n}` (numéro 1…N dans l'ordre de
  jeu), `{item}` (paragraphe sans les lignes `-`), `{itemK}` (K-ième morceau) ; remplacement en une
  seule passe. Erreurs (`IllegalArgumentException` avec `ligne N`) : bloc non fermé, imbriqué ou vide,
  zone hors d'un bloc, mode ou nombre invalide, N supérieur à la taille du fichier, paragraphe dont le
  nombre de morceaux diffère du plus grand `{itemK}` du gabarit. `PickResolver.resolve` déplie d'abord
  (le `{pick}` d'un gabarit est donc tiré par copie) ; les tirages des blocs précèdent ceux des `pick`
  dans `picks.txt`, avec les types `repeat-order` / `repeat-random` (`PickMode.parseRecorded`). Le
  scénario déplié est ce qu'écrit `resolved/` : le jeu ne voit jamais de `repeat`.
- **Variables de casting `{cast NOM [unique] [reuse-possible] [(liste)]}` et `%NOM%`** (guide §2.6 ter) :
  concepts `CastCommand` (une ligne ; erreurs de syntaxe), `CastPool` (la liste ou tout le casting, un mot par
  personnage via `Cast.findAllByName` : inconnu, ambigu ou doublon = erreur), `CastResolver`. Ordre du
  pipeline dans `PickResolver.resolve` : dépliage des `repeat`, tirages `pick`, puis casts (un `%NOM%` écrit
  dans un paragraphe tiré est donc remplacé aussi). `RepeatExpander` accepte `{repeat N}` (copies sans
  éléments) et `{repeat order|random [N] cast NOM [(liste)]}` ; une copie qui contient un `cast` ou lie une
  variable est entourée de deux lignes internes (`{copy-start}` / `{copy-end}`, plus `{cast-bind NOM id}` pour
  la variable du repeat) que lit `CastResolver` et que l'auteur n'écrit jamais (inconnues du validateur).
  `CastResolver` lit les lignes dans l'ordre, puis cherche les personnages **tous ensemble par retour en
  arrière** (ordre des candidats mélangé par `RandomnessPort`, budget `MAX_NODES` de 200 000 appels) : une
  commande sans candidat défait le tirage précédent. Candidats d'une commande = sa liste, moins les
  personnages tenus par les autres variables (sauf `reuse-possible`), moins ceux déjà eus par cette variable
  (si `unique`) ; l'état (variables par copie, historique par variable) est immuable, ce qui rend le retour en
  arrière gratuit. La variable d'un `repeat … cast` est liée sans contrainte. Échec : `IllegalStateException`
  nommant la commande la plus avancée qui n'a trouvé personne. `PickMode.CAST` étiquette les tirages (🎲 :
  type `cast`, tag = nom de variable).
  Validation : `ScenarioValidator.checkLine` contrôle la syntaxe et la liste de chaque `{cast`,
  `checkVariables` (par fichier et ligne, une variable d'un bloc `repeat` est oubliée à son `{end-repeat}`)
  les `%NOM%` ; une ligne qui contient un `%NOM%` est contrôlée avec un personnage du casting à sa place ;
  `checkUniqueDoesNotRunOut` lance la résolution complète (hasard fixé) et rapporte une combinaison impossible.
- **Debug 🐞** : `ChatService.scenarioPrompt` → `GET /scenario-prompt` : la prémisse et le texte de l'acte
  courant tels que `ChatPromptBuilder` les insère (aucune requête LLM, pas d'historique).
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

Chaque PNJ a un **niveau d'activité** (`NpcActivity`) : active 😎 (parle à chaque échange), present 🙂
(défaut : répond quand il est nommé), passive 🤨 (réaction brève occasionnelle), absent 😴 (ne parle jamais,
listé « pas dans la scène » dans le prompt des autres). `SpeakerSelector`, quatre étapes : (1) les actifs ;
(2) les PNJ active/present nommés dans le message du joueur puis dans les réponses du tour précédent,
avant les autres actifs ; (3) sinon le PNJ unique du tour précédent ; (4) sinon tirage aléatoire parmi
present et passive (un present deux fois plus probable). Ensuite, chaque autre PNJ present ou passive tire
sa chance d'interjecter : le réglage « Chance d'interjection » (défaut 1) est une chance totale partagée
entre les candidats (un present pèse double d'un passive, plafond de 60 % par PNJ), donc dégressive selon
leur nombre.

Le niveau vient du scénario (`{npc-active|present|passive|absent noms}`, `{npc-reset}` : `ActivityDirective`,
résolues par `ActivityResolver` le long du chemin de titres, jamais stockées : `ChatScenario.activityAt`),
avec un statut explicite (nommé, ou `*` = tous nommés d'un coup, un nom du même titre l'emportant) ou
implicite (défaut, commande sans nom). Le joueur peut le forcer en
cliquant la vignette (`POST /cycle-activity`) : `ForcedActivity`, accroché au titre parent de l'acte courant,
disparaît quand l'histoire en sort, persisté dans `state.txt`. Pastille : point blanc explicite, gris
implicite, rouge forcé. Détail : `chatscenarios/guide-redaction-chat.md` §2.11 et §4.

**Continue** : `ChatService.continueScene` (`POST /continue`, bouton ▶ Continue) joue un tour de PNJ sans
message du joueur : `SpeakerSelector.select` avec un message vide, jamais d'interjection, aucun tour joueur
(`ChatTurnResult.playerTurn` nul). Le prompt reste le prompt habituel. Annuler et Régénérer inchangés : ils
reviennent au dernier vrai message du joueur.

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
`/analyze-next-act`, `/analyze-mind-state`, `/analyze-reply-origin`, `/picks`, `/scenario-prompt`,
`/cycle-activity`, `/reload-scenario`, `/save`, `/saves`, `/load-save`, `/delete-save`.
Les réponses sont streamées (texte + réflexion). Les requêtes sont traitées par un pool de fils qui
grandit (`newCachedThreadPool`) : un appel LLM long ne retient jamais un simple affichage (🐞, 🎲, `/stop`)
— avec un pool fixe de 2 fils, deux appels longs laissaient le clic suivant sans réponse. La session
n'est pas protégée contre deux écritures simultanées : l'interface grise les boutons pendant une
génération. Les points d'accès de debug (`/scenario-prompt`, `/picks`) renvoient une erreur JSON
lisible, et leur pop-up affiche le statut HTTP et le début de la réponse quand elle n'est pas le JSON
attendu ou que le rendu est vide (jamais un cadre muet). Chaque tour est
écrit sur disque dès qu'il existe ; sauvegardes manuelles illimitées ; ⟳ recharge scénario et
fiches sans toucher à la conversation. Chaque bulle affiche son numéro `#N` et un bouton ↩ qui
appelle `/rollback` (`/undo` et `/retry` en sont des cas particuliers).

# Outils CLI

`ChatCli` (jeu, `chat.bat` ; ne propose « Continuer ? » que s'il existe une partie jouée, voir
`ChatService.hasSavedSession` — sinon il démarre une nouvelle partie directement), `ScenarioTesterCli` (revue d'un scénario par agents reviewers,
`test-scenario.bat`), `InitChatCli` (nouveau scénario : dossier, nom, nombre et noms des PNJ, puis
`ScenarioScaffolder` crée `scenario.txt` et `characters/*.txt` à remplir, `init-chat.bat`),
`ValidateScenarioCli` (validation des scénarios, `validation-scenarios.bat` ; contrôle aussi `chatscenarios/demos`).
Au démarrage, `ChatCliSupport` propose la racine des scénarios : par défaut, projet, autre chemin, ou **3 = `chatscenarios/demos`** (un scénario minimaliste, tous publics, par mécanisme ; `DemoScenariosTest` les valide et vérifie leurs actes et personnages).
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
spécification technique ; sa version « utilisateur » est `chatscenarios/guide-redaction-chat.md` §6.

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
    state.txt                           acte courant, niveaux d'activité forcés
    resolved/scenario[-N].txt           scénario dont les {pick} sont tirés (absent sans pick)
    resolved/picks.txt                  les tirages dans l'ordre : "@@ mode fichier" puis les paragraphes (séparés par "@@ --")
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
forced=elena:active@1.2;marcus:absent@
```

`act` : l'acte en vigueur maintenant (il peut différer de l'acte du dernier tour après un « Précédent »
manuel, qui ne crée aucun tour). `forced` : les niveaux d'activité forcés par le joueur, `id:niveau@ancre`
séparés par `;` (l'ancre est le numéro du titre parent, vide = racine). Fichier absent, ou clé absente :
rien de forcé, chaque PNJ a le niveau que donne le scénario à l'acte courant. Les niveaux du scénario ne
sont jamais écrits (recalculés). Les anciennes clés `present` / `interjecting` sont encore lues (un PNJ
absent de `present` est rouvert forcé absent), jamais réécrites. Les réglages de génération ne sont pas
persistés. Écrit après chaque `saveSession`, de façon atomique
(fichier temporaire puis renommage).

**Zips** : `turns/`, `state.txt` et `resolved/` (dont `picks.txt`) de `current/`, chemins relatifs à `current/`. Fichiers immuables,
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
6. Niveaux forcés (`forced`) : lus dans `state.txt`, jamais déduits du tour ; ceux dont l'ancre ne couvre
   plus l'acte restauré sont abandonnés. Les niveaux du scénario se recalculent depuis l'acte.
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

## Ancienne structure

Un scénario à l'ancienne structure (fichiers de session ou dossiers générés à la racine, fiches personnage hors de `characters/`) est refusé au chargement avec un message « format non pris en charge » ; il n'y a plus de programme de conversion.

# Évolution d'un personnage — `{npc-delta public|secret nom : texte}`

Syntaxe et usage : `chatscenarios/guide-redaction-chat.md` §2.8. Ici, la mécanique.

- **Concepts** : `CharacterDelta` (visibilité `PUBLIC`/`SECRET`, nom, texte), `CharacterDeltaMarker`
  (extrait les lignes seules et complètes ; toute occurrence restante de `{npc-delta` est une erreur),
  `ScenarioAct.deltas` (deltas des nœuds nouvellement entrés, comme les beats et l'ouverture),
  `ChatScenario.sheetAt(npc, acte)`.
- **Chargement** : `ScenarioOutlineParser` retire les lignes du corps de chaque titre (le texte d'acte va à
  tous les PNJ : un secret ne doit pas y rester). Dans la prémisse : refusé. Nom inconnu du `Cast` :
  `ChatScenario` refuse le scénario.
- **Calcul** : `sheetAt` cumule, dans l'ordre, les deltas des actes 1..N qui visent le PNJ ; `PUBLIC`
  ajoute une ligne à `Npc.publicInfo`, `SECRET` à `Npc.secretInfo` (créé si absent). Rien n'est stocké
  dans le run : l'acte courant est déjà restauré par undo, rollback, « Précédent », sauvegarde et
  redémarrage, donc les fiches suivent. Appliqué au moment de construire le prompt
  (`ChatPromptBuilder`, `NextActReadinessPromptBuilder`, `NpcMindStatePromptBuilder`), jamais gravé dans
  le `Cast` : ⟳ recharge les fiches sans perdre les deltas. Aucune chaîne de prompt n'est modifiée : les
  lignes s'ajoutent à la fiche comme si l'auteur les y avait écrites.

# Sections de fiche — `## tag` et `{npc-story on|off nom tag}`

Syntaxe et usage : guide §2.8 bis et §3. Ici, la mécanique.

- **Concepts** : `StoryTag` (nom, visibilité, texte) porté par `Npc.tags`, `NpcSheetParser` (lit la fiche :
  coupe sur `# SECRET`, puis chaque zone sur les lignes `##`), `StoryDirective` / `StoryDirectiveMarker`
  (le tag est le dernier mot de la commande), `ScenarioAct.stories`, `StoryResolver`.
- **Fiche** : le texte hors `##` de chaque zone est permanent (`publicInfo` / `secretInfo`, comme avant ; une
  fiche sans `##` se lit à l'identique). Le premier mot d'un `##` est le tag, le reste de la ligne est un
  commentaire jamais envoyé. Visibilité = zone de la section. Erreurs : tag sans nom, `SECRET`, section vide,
  nom en double (casse ignorée, toutes zones).
- **Durée** : celle des deltas, pas celle des modes. Les commandes sont rattachées au premier acte du titre qui
  les porte (`ScenarioOutlineParser`, comme `deltas`) ; `StoryResolver.activeTags` rejoue les actes 1..N dans
  l'ordre. Rien n'est stocké : undo, rollback, « Précédent », sauvegarde et ⟳ redonnent l'état de l'acte.
- **Prompt** : `ChatScenario.sheetAt` applique d'abord `Npc.withActiveTags` (texte des tags actifs, dans
  l'ordre de la fiche, ajouté en bloc à la partie publique ou secrète), puis les deltas. Aucune chaîne de
  prompt n'est modifiée. Un tag secret n'atteint jamais un autre PNJ (même chemin que `# SECRET`).
- **Validation** : `checkSources` (commande seule sur sa ligne, `SECRET` interdit, personnage et tag connus),
  `ChatScenario` refuse un tag inconnu au chargement, la prémisse est refusée, `checkScenario` avertit d'un tag
  jamais activé et d'un `off` sans effet. Une fiche invalide est rapportée par `validateScenario` sans lever.
- **Renommage** : `{delta-pnj}` est devenu `{npc-delta}` ; l'ancien nom est refusé par `LegacyCommandSyntax`
  avec le nouveau.

# Validation du scénario

Syntaxe des erreurs : guide §2.9. Ici, la mécanique.

- **Concepts** : `ScenarioValidator` (domaine, pur) → `ScenarioReport` de `ScenarioIssue`
  (`ERROR` / `WARNING`, lieu `fichier:ligne` ou acte, message). `ScenarioValidationException`
  (`IllegalStateException`) liste toutes les erreurs.
- **`checkSources`** (erreurs, sur le texte écrit par l'auteur, ligne par ligne) : ancienne syntaxe,
  commande inconnue ou mal écrite, `npc-starts` / `npc-do` / `npc-delta` / `npc-active|present|passive|absent|reset` / `npc-mode`
  pas seuls sur leur ligne, personnage inconnu (dont les noms d'une commande d'activité), bibliothèque `picks/` absente ou trop courte, `unique` plus nombreux que les paragraphes, blocs `repeat` incorrects (`RepeatExpander`, fichier par fichier).
- **`checkScenario`** (avertissements, sur le scénario analysé) : acte non final sans `[NEXT ACT]`, PNJ
  jamais cité dans la prémisse ni le premier acte.
- **Quand** : `ChatFileStorageAdapter.scenarioTextDir` valide les textes à chaque lecture depuis le
  répertoire source (un run déjà résolu a été validé à son démarrage). `validateScenario` (port
  `ChatStoragePort`) le fait à la demande **sans effet de bord** : tirage en mémoire, copie temporaire
  supprimée, aucun run créé. CLI : `ValidateScenarioCli` / `validation-scenarios.bat`. Les avertissements
  s'affichent au lancement de la CLI de chat et dans le bouton 🐞.
- **Ancienne syntaxe** : `[PICK: x]`, `[NPC STARTS]` sont refusés au chargement (`LegacyCommandSyntax`).
  La conversion des scénarios a été faite une fois ; il n'y a plus de programme de migration.

# Style commun — `style.txt`

Syntaxe et conseils d'écriture : `chatscenarios/guide-redaction-chat.md` §2.10. Ici, la mécanique.

- **Concept** : `ScenarioStyle` (texte nettoyé, `NONE` si absent ou vide), 8e composant de `ChatScenario`
  (les constructeurs plus courts restent, avec `NONE`).
- **Lecture** : `ChatFileStorageAdapter.readStyle` lit `style.txt` à la racine du dossier du scénario, **à
  chaque chargement** (jamais copié dans `resolved/`, absent des sauvegardes : ⟳ relit le fichier).
  `LegacyLayout` l'exclut de la détection des anciennes fiches (un `style.txt` peut commencer par `# Titre`).
- **Prompt** : `ChatPromptBuilder` insère `STYLE ADVICE:` + le texte entre `STORY SO FAR` et les règles de
  format, seulement si le style est présent. Seul le prompt du personnage qui joue le reçoit ; ni le
  résumeur, ni `NextActReadinessAnalyst`, ni `NpcMindStateAnalyst`.
- **Validation** : `ScenarioValidator.checkStyleText` refuse l'ancienne syntaxe et toute commande entre
  accolades (erreur au chargement) ; `checkScenario` avertit au-delà de 1 500 caractères.
- **Lignes rares** : un marqueur `{N%}` dans une ligne (`ScenarioStyle.CHANCE_MARKER`) la rend
  conditionnelle. `ChatServiceImpl` tire le style à chaque réponse de PNJ (`ScenarioStyle.drawn`, hasard
  du service, non reproductible) et donne au narrateur un scénario portant ce style (`ChatScenario.withStyle`) :
  la ligne est gardée sans son marqueur, ou supprimée. `checkStyleText` refuse un marqueur mal formé
  (hors 1-100, plusieurs par ligne, sans texte). Le texte du prompt lui-même est inchangé.
- **Debug** : `ScenarioPromptView.style` (fichier) et `styleReceived` (style tiré pour la dernière
  réponse, `ChatSession.lastStyleReceived`, en mémoire), affichés par le bouton 🐞.

# Origine d'une réponse — bouton ❓

Usage et limites côté auteur : `chatscenarios/guide-redaction-chat.md` §4.3 bis. Ici, la mécanique.

- **Concepts** : `ReplyTrace` (id du PNJ, prompt système et utilisateur tels qu'envoyés, réponse,
  réflexion), `ReplyOriginAnalyst` (entrée, sortie `traced` / `untraced` / `reading`, constructeur de
  prompt, fiche `.md`).
- **Capture** : `RoleplayNarratorOutput.prompt` porte le prompt réellement construit ;
  `ChatServiceImpl` le garde avec la réponse (`ChatSession.rememberReplyTrace`), **en mémoire seulement**.
  `ChatSession.lastReplyTrace()` ne le rend que si cette réponse est encore le dernier tour LLM
  (undo, rollback, redémarrage, chargement : rien).
- **Service** : `ChatService.analyzeLastReplyOrigin` (lecture seule, `IllegalStateException` s'il n'y a
  rien à analyser ou si l'agent n'est pas branché) → `POST /analyze-reply-origin` → pop-up d'analyse.
- Aucun appel au LLM dans les tests : un faux port répond.
