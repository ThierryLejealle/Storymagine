# Règles du module chat — scénarios, personnages, mécanismes

Référence pour écrire ou modifier un scénario dans `chatscenarios/`. Tout ce qui suit a été vérifié
dans le code (pas seulement supposé) — voir les fiches `evols/` citées pour le détail.

**Plan du document** : §1 à §3 décrivent le SCÉNARIO (ce que vous écrivez). §4 décrit les
mécanismes du jeu. §5 explique comment vérifier un scénario. §6, en fin de document, décrit le
dossier `run/` (ce que l'appli génère pendant qu'on joue).

## 1. Le scénario : ce que vous écrivez

Un scénario est un dossier. Il contient **uniquement des fichiers que l'auteur écrit**. L'appli n'y
écrit jamais rien, sauf dans le sous-dossier `run/` (voir §6).

```
chatscenarios/mon-scenario/
  scenario.txt        obligatoire — ligne Joueur, prémisse, actes
  scenario-1.txt      optionnel — suite des actes (scénario long)
  scenario-2.txt      optionnel — numéros consécutifs
  characters/         obligatoire — un fichier par personnage
    elena.txt
    marcus.txt
  run/                généré par l'appli, jamais à écrire à la main (voir §6)
```

- **Copier ou partager un scénario** = copier son dossier sans `run/`.
- **`scenario.txt` + `scenario-N.txt`** : le texte de l'histoire. Les `scenario-N.txt` sont
  concaténés après `scenario.txt` (numéros consécutifs, arrêt au premier manquant). Chacun doit
  commencer par un titre `# ...` de premier niveau, sinon le chargement échoue explicitement.
  Format : voir §2.
- **`characters/`** : un fichier `.txt` par personnage. Format : voir §3.

**Règle de chargement du Cast** : tout fichier `.txt` de `characters/` dont la première ligne est
`# Nom` est un personnage. Un `.txt` sans cette ligne est ignoré silencieusement (brouillon, note).
Le nom de fichier (sans `.txt`) devient l'id interne du personnage — choisir un nom de fichier
simple, sans espace ni accent, ex. `marcus.txt` pas `Marcus Dupont.txt`. Comme seul `characters/`
est lu, un `.txt` égaré à la racine du scénario ne peut jamais devenir un personnage.

Un scénario à un seul personnage (un seul fichier dans `characters/`) fonctionne sans rien de
spécial — devient un `Cast` à un seul PNJ automatiquement.

Un scénario apparaît dans la liste de l'appli s'il a un `scenario.txt` et au moins un personnage
valide dans `characters/`.

### 1.1 Exemple minimal complet

**Pour créer le squelette d'un nouveau scénario, lancer `init-chat.bat`** : il demande le dossier (le
même choix que `chat.bat`), le nom du scénario (lettres, chiffres, `-` et `_`, sans espace ni accent),
le nombre de PNJ puis leurs noms, et crée `scenario.txt` et une fiche par PNJ dans `characters/`.
Chaque champ est une consigne entre parenthèses, à remplacer ; le premier acte nomme déjà tous les PNJ
saisis (voir §2.4) ; `run/` n'est pas créé. Un scénario existant n'est jamais écrasé.

Le plus petit scénario jouable : deux fichiers.

`chatscenarios/auberge/scenario.txt`

```
Joueur : Mila

#SCENARIO
Monde : Une auberge de campagne, un soir d'orage.
Prémisse : Mila, voyageuse trempée jusqu'aux os, cherche un abri pour la nuit.
Ton : chaleureux, un peu mystérieux.

# La nuit à l'auberge
[Mila pousse la porte de l'auberge du Cerf Blanc, trempée par l'orage. Derrière le comptoir, Elena, l'aubergiste, lève les yeux.]
[Mila cherche un lit pour la nuit et un repas chaud.]
Point de départ : Mila vient d'entrer, la salle est presque vide.
Ce qui doit se passer : Elena l'accueille et la conversation s'engage.
```

`chatscenarios/auberge/characters/elena.txt`

```
# Elena

Elena, la trentaine, tient l'auberge du Cerf Blanc. Directe, chaleureuse, parle court.

# SECRET
Elena a volé autrefois dans un temple et craint d'être reconnue.
```

Points à remarquer :

- Un seul acte, donc pas de `Quand écrire [NEXT ACT]` (rien à quoi avancer, voir §2.5).
- Les lignes `[...]` du premier acte disent qui est le joueur, où il se trouve, qui est présent
  (nommé) et son but (voir §2.4), toutes à la 3e personne.
- La fiche d'Elena n'a pas de nom de fichier à accent ni d'espace : `elena.txt` donne l'id `elena`.
- Pour un personnage sans secret, la fiche se réduit à `# Nom` et sa description : sans ligne
  `# SECRET`, tout est public.
- Encore plus court : un `scenario.txt` sans aucun acte (juste `Joueur :` et une prémisse avec un bloc
  `[...]` d'ouverture) est valide. La partie démarre alors sans progression d'actes (voir §2.4).

Ce scénario, une fois joué, crée `run/` à côté de ces fichiers (voir §6) ; il n'y a rien d'autre à
écrire.

## 2. Format de `scenario.txt`

### 2.1 Ligne "Joueur" (optionnelle, tout en premier)

```
Joueur : Alex
```

Si absente, le joueur s'appelle **"Alex"** par défaut (`ChatScenario.playerName`, normalisé dans
le code — jamais vide). Ce nom remplace "Player" partout où le joueur est désigné dans la
transcription envoyée au modèle. Mettre cette ligne explicitement dans chaque scénario par
convention du projet, même si "Alex" serait pris par défaut de toute façon.

### 2.2 Prémisse

```
#SCENARIO
Monde : ...
Prémisse : ...
Ton : ...
```

Le `#SCENARIO` est optionnel : s'il est absent, tout le texte avant le premier `#`/`##` devient la
prémisse. S'il est présent, son corps devient la prémisse et la lecture des actes commence après.
Convention interne (pas une contrainte du code) : `Monde :` / `Prémisse :` / `Ton :`, parfois
`Fil conducteur discret :`. La prémisse est envoyée au LLM à chaque tour, jamais montrée telle
quelle au joueur (seules les lignes `[...]`, voir §2.4, sont montrées).

**⚠️ Toujours écrire la Prémisse à la 3e personne** (voir `chatscenarios/CLAUDE.md`) — elle est
transmise telle quelle dans le prompt de CHAQUE personnage, pas seulement narrée au joueur. "Tu es
Alex..." contredit l'assignation de personnage donnée à un PNJ dans son propre prompt. Écrire
"Alex est un jeune mage..." (comme un narrateur décrivant la situation de l'extérieur), jamais
"Tu es...".

### 2.3 Structure des actes — ⚠️ piège à connaître

Les actes sont écrits en Markdown imbriqué (`#`, `##`, `###`...). **Règle du parseur
(`ScenarioOutlineParser`) : seules les FEUILLES (un heading SANS sous-heading) sont des actes
réellement jouables.** Un heading qui a un enfant n'est jamais "l'acte courant" lui-même — son
corps de texte est simplement hérité par tous ses descendants (ancêtre de chapitre).

```
# Chapitre A          <- PAS un acte jouable s'il a un enfant ci-dessous
## Scène A.1          <- acte jouable (numéro 1.1)
## Scène A.2          <- acte jouable (numéro 1.2)
# Chapitre B          <- idem, pas jouable seul
## Scène B.1          <- acte jouable (numéro 2.1)
```

**Piège vécu** (voir `evols/2026-07-15-...`) : écrire `# Acte 1` suivi d'un SEUL `## Acte 2`
ne donne PAS deux actes séquentiels — ça donne un seul acte (2 fusionné avec le contexte hérité de
1), parce que "Acte 1" n'est jamais une feuille. **Pour deux actes séquentiels indépendants, les
mettre tous les deux au même niveau (`#`/`#`, jamais `#`/`##`).** N'utiliser l'imbrication `#`/`##`
que pour un vrai regroupement thématique voulu (un chapitre avec plusieurs scènes dedans, comme
`temple-noir-actes` ou `quete-des-amazones`).

Toujours vérifier le nombre d'actes obtenu après avoir écrit un scénario (voir §5, script de
vérification).

### 2.4 Lignes `[...]` — ce que le joueur voit réellement

Dans le corps de chaque acte (ou dans la prémisse pour un scénario sans actes), toute ligne entre
crochets `[...]` devient une réplique NARRATOR affichée au joueur au moment où cet acte devient
courant :

**⚠️ Scénario SANS actes (`scenario.acts()` vide) : un seul bloc `[...]` compte.** Si la prémisse
en contient plusieurs, seul le PREMIER est affiché à l'ouverture — les autres sont ignorés (revu
le 2026-07-17, voir `evols/2026-07-17-...`). Un scénario sans actes n'a qu'un seul état d'ouverture,
pas une pile à déverser d'un coup ; sans ce plafond, une prémisse plate contenant plusieurs blocs
`[...]` (plusieurs scènes narratives pensées pour s'enchaîner) les affichait TOUTES avant même le
premier message du joueur, sans jamais appeler le LLM. Si votre scénario a plusieurs scènes
d'ouverture distinctes, utilisez de vrais actes (`#`/`##`, voir §2.3) plutôt qu'une prémisse plate —
c'est le seul moyen d'obtenir une progression normale entre elles.

```
[Le tonnerre gronde alors qu'Alex referme derrière lui la porte de l'auberge.]
```

**⚠️ Même règle que la Prémisse : toujours à la 3e personne** (voir `chatscenarios/CLAUDE.md`).
Ces lignes ne sont pas seulement affichées au joueur — elles sont aussi injectées telles quelles
dans le prompt de CHAQUE PNJ via `act.text()` (voir `Teaser.java`, qui les laisse volontairement
dans le texte "so the LLM reads them too, in place"). Écrites à la 2e personne ("Tu es Alex...",
"vous venez de..."), elles contredisent l'assignation de personnage donnée à chaque PNJ dans son
propre prompt — bug réel corrigé le 2026-07-16 sur les 5 scénarios existants.

**Règle d'or (voir `evols/2026-07-15-...`, audit d'introduction)** : le TOUT PREMIER acte du
scénario doit avoir des lignes `[...]` qui disent clairement au joueur : qui il/elle est, où il/elle
se trouve, qui est présent (nommé), et quel est le but général. Une seule ligne purement
atmosphérique ("le tonnerre gronde...") sans ces informations laisse le joueur perdu — c'est arrivé
et corrigé une fois, ne pas répéter l'erreur sur un nouveau scénario.

Un chapitre parent (`#` non-feuille) peut aussi avoir ses propres lignes `[...]` : elles ne
s'affichent qu'une seule fois, la première fois qu'un de ses descendants devient courant — jamais
répétées à chaque sous-scène suivante du même chapitre.

**Deuxième piège vécu, multi-PNJ** (voir `evols/2026-07-15-...`) : tous les PNJ sont présents
dès le tour 1 par défaut (§4.1) — même si le premier acte ne met en scène qu'un seul personnage
narrativement. Si le joueur ne nomme personne, `SpeakerSelector` peut choisir au hasard un PNJ que
l'acte 1 n'a pas encore "montré". Le modèle, ne sachant pas s'il est censé être dans la pièce, peut
boucler indéfiniment sur cette ambiguïté au lieu de répondre (observé : 54s de réflexion, réponse
vide). **Toujours mentionner TOUS les PNJ dans les lignes `[...]` du tout premier acte**, même en
retrait/silencieux, si la mise en scène narrative se concentre sur un seul d'entre eux au début.

### 2.5 Texte d'accompagnement de chaque acte (jamais montré au joueur, lu par le LLM)

```
Point de départ : ...
Ce qui doit se passer : ...
Quand écrire [NEXT ACT] : ...
```

- `Point de départ` : situe la scène pour le LLM.
- `Ce qui doit se passer` : ce que la scène doit accomplir avant de pouvoir avancer.
- `Quand écrire [NEXT ACT]` : condition précise de transition. Omis sur le tout dernier acte
  (rien à quoi avancer). Si absent sur un acte qui n'est pas le dernier, le LLM utilise une règle
  par défaut ("quand la scène semble arrivée à sa conclusion naturelle") — moins précis, à éviter
  si possible.

Ces libellés sont une convention du projet, pas une syntaxe imposée par le code — seuls `#SCENARIO`,
les headings `#`/`##`, et les crochets `[...]` sont réellement parsés.

## 3. Format d'un fichier personnage (`characters/nom.txt`)

```
# Elena

Elena, la trentaine, tient l'auberge... (fiche publique)

# SECRET
Elena a autrefois volé dans un temple... (fiche secrète)
```

- Première ligne `# Nom` **obligatoire** (revu le 2026-07-17) : devient le nom affiché
  (`Npc.label()`) — gardée verbatim dans la fiche publique, pas retirée. Un fichier `.txt` sans
  cette ligne en premier n'est PAS chargé comme PNJ — il est silencieusement ignoré (voir
  `ChatFileStorageAdapter.hasNameHeading`). Avant, n'importe quel `.txt` égaré dans le dossier
  (note, brouillon, sauvegarde manuelle) devenait un PNJ fantôme sans avertissement, avec son id de
  fichier comme nom — risque réel signalé par l'utilisateur.
- Ligne `# SECRET` seule (insensible à la casse) optionnelle : sépare fiche publique / fiche
  secrète. Absente : tout le fichier est public, pas de partie secrète.
- **Le PNJ qui parle reçoit toujours sa fiche complète (publique + secrète)** — il se connaît
  lui-même. **Les autres PNJ présents dans la scène reçoivent leur fiche PUBLIQUE** (jamais leur
  `# SECRET`, qui ne fuite jamais vers un autre PNJ — règle anti-triche, voir `ChatPromptBuilder
  .OTHER_NPCS_RULE`). Revu le 2026-07-16 : avant, les autres PNJ n'étaient connus que par leur nom
  ("tu ne sais rien d'eux"), ce qui lisait bizarrement pour une équipe qui voyage ensemble depuis
  plusieurs chapitres — `scene.otherPresent()` ne contient jamais le joueur, seulement des PNJ déjà
  établis comme coéquipiers dès la prémisse du scénario, donc partager leur fiche publique entre
  eux est toujours cohérent narrativement.
- Bonne pratique pour donner du relief : des secrets qui se répondent entre eux sans jamais être
  méchants ni casser l'histoire (aspirations, envies, non-dits) — voir `quete-des-amazones` pour un
  exemple à trois PNJ dont les secrets s'entrecroisent.

## 4. Mécanismes du jeu (panneau de gauche, réglages, boutons)

### 4.1 Présence (vignette 🔊/🔇)

Chaque PNJ présent ou absent de la scène. Tous présents par défaut. Impossible de rendre absent le
dernier PNJ présent (garde-fou : quelqu'un doit toujours pouvoir répondre). Persisté dans
`run/current/state.txt` (voir §6.5).

### 4.2 Qui répond (`SpeakerSelector`) — trois étapes, revu le 2026-07-16

1. **Mention** : le message du joueur nomme un ou plusieurs PNJ présents → TOUS ceux nommés
   répondent, dans le même tour, dans l'ORDRE D'APPARITION de leur nom dans le message (pas
   alphabétique — un PNJ juste cité en passant après le PNJ réellement visé ne doit jamais répondre
   avant lui). Détection insensible aux accents/majuscules, et seuls les 5 premiers caractères
   normalisés du nom doivent matcher (mot entier au sens large : "Cel" ou une coquille légère
   suffisent à viser "Céleste", même principe que SillyTavern).
2. **Continuité** : personne n'est nommé, mais UN SEUL PNJ a parlé au tour précédent → il reprend
   la parole en premier. Un simple "et après ?" continue donc la conversation avec la bonne
   personne au lieu de retomber sur un tirage au hasard. Si ce PNJ n'est plus présent (mute entre
   temps), on retombe sur l'étape 3.
3. **Repli** : personne n'est nommé, et l'étape 2 ne s'applique pas (premier échange, ou plusieurs
   PNJ avaient parlé au tour précédent) → UN SEUL PNJ est tiré au hasard, uniquement parmi ceux
   éligibles à l'interjection (icône 💬 activée, voir §4.3) — un PNJ dont le 💬 est désactivé ne
   doit jamais être pioché ici, sinon le bouton ne tient pas sa promesse ("ne réagit que si visé
   par son nom"). Si personne n'est éligible (tout le monde a désactivé son 💬), repli sur tous les
   PNJ présents quand même — quelqu'un doit répondre au joueur.

**Reprise sur réponse vide** (voir `evols/2026-07-15-...`) : un petit modèle peut parfois
épuiser tout son budget de génération en réflexion et ne rien répondre du tout (`RoleplayNarrator`
détecte une réponse vide et retente une fois, même prompt — abandonne après ce seul essai plutôt
que de boucler). Si ça arrive régulièrement sur un scénario donné, c'est souvent le signe d'une
ambiguïté de mise en scène à corriger (voir le piège du §2.4) plutôt qu'un simple aléa du modèle.

### 4.3 Interjection (icône 💬, réglage "Chance d'interjection") — revu le 2026-07-16

Après l'étape ci-dessus, quelle qu'elle soit (mention, continuité ou tirage), chaque PNJ présent,
éligible (icône 💬 activée — actif par défaut, à désactiver PNJ par PNJ si besoin) et qui n'a pas
déjà été choisi comme principal tire indépendamment sa chance de réagir quand même. Pas de
restriction sur le nombre de PNJ principaux : même si deux ont été nommés, un troisième présent
peut encore interjecter. Répond après le ou les PNJ principaux, voit déjà leur réponse ; si
plusieurs interjectent, leur ordre entre eux est aléatoire (pas d'ordre stable). Réglage global
"Chance d'interjection" dans le panneau (0 à 1, défaut 0.5) — pas de réglage par PNJ. Voir
`evols/2026-07-15-...` et `evols/2026-07-16-...` (refonte).

### 4.4 Réflexion / thinking (case "Afficher la réflexion", icône 🧠)

La réflexion est **toujours demandée** au modèle à chaque appel, indépendamment de l'affichage
(meilleure qualité de réponse, voir `evols/2026-07-15-...`). L'affichage dans le chat est
séparé, actif par défaut (case décochable). Certains modèles ne savent pas produire de réflexion
séparée (dépend de comment ils sont enregistrés dans Ollama) — rien à faire côté scénario, c'est
une capacité du modèle, pas du scénario.

### 4.5 Progression d'actes

Le LLM écrit `[NEXT ACT]` dans sa réponse (retiré avant affichage) quand la condition de l'acte
courant est remplie → avance à l'acte suivant, ses lignes `[...]` s'affichent. Boutons manuels
"Précédent"/"Suivant" pour corriger. Bouton 🔍 pour demander au LLM (sans rien persister) où en est
la condition de l'acte suivant.

#### Faire ouvrir la scène par un PNJ — `[NPC STARTS]`

Par défaut, c'est le joueur qui parle en premier. Pour qu'un PNJ prenne la parole à sa place, écrire
sur **une ligne seule** (comme un bloc, mais sans phrase autour) :

```
[NPC STARTS]
[NPC STARTS: Elena]
```

- Placé dans l'en-tête du scénario (avant le premier acte) : il ouvre l'histoire. Placé dans un
  acte : il ouvre cet acte, dès qu'il devient courant (passage automatique ou bouton « Suivant »).
- Effet : exactement comme si le joueur avait cliqué **DO** sans rien écrire. Une bulle `DO:` du
  joueur s'affiche, puis le PNJ parle ; lui seul (pas d'interjection des autres).
- Sans nom : un PNJ présent est tiré au hasard (inutile s'il n'y en a qu'un). Avec un nom : celui-là.
  Le nom peut être l'identifiant du fichier, le nom complet ou un seul mot du nom (`Elena` pour
  `Elena Voss`), sans tenir compte des accents ni de la casse. Un nom inconnu empêche le scénario de
  se charger (message d'erreur explicite) ; un PNJ nommé mais retiré de la scène par le joueur laisse
  la place à un autre PNJ présent.
- La ligne est retirée du texte : le LLM ne la lit jamais. Une mention au milieu d'une phrase (comme
  dans les consignes de la graine d'`init-chat`) n'est jamais prise pour le mot-clé.
- Si le joueur écrit avant que le PNJ n'ait parlé, c'est son message qui compte : pas d'ouverture.

### 4.6 Sauvegardes et redémarrage

- Chaque tour est écrit sur disque dès qu'il existe, dans son propre fichier (voir §6.2) : il n'y a
  plus de « slot unique écrasé à chaque fois », la partie en cours est toujours à jour.
- 💾 Sauvegarder : point de sauvegarde manuel, horodaté, illimité en nombre. C'est un zip complet de
  la partie en cours (voir §6.7). Charger une sauvegarde remplace la partie en cours (confirmation
  demandée) et on peut ensuite continuer à remonter dans l'historique (§4.8), comme si on n'avait
  jamais quitté la partie.
- 🔄 Recommencer : remet la session à zéro sur le même scénario (confirmation demandée) — la partie
  qui se termine est archivée dans un zip, jamais perdue.
- ⟳ (à côté de "Personnages") : recharge `scenario.txt` (et ses suites `scenario-N.txt`) et les
  fiches de `characters/` depuis le disque sans toucher à la conversation en cours — pratique pour
  corriger une fiche en cours de partie.

### 4.7 Compaction du contexte

Quand la transcription dépasse le budget de tokens disponible, les tours les plus anciens sont
résumés (`ChatSummarizer`) et remplacés par ce résumé dans le prompt. Rien n'est perdu : tous les
tours restent sur disque, un fichier chacun, et chaque compaction est elle-même un fichier placé
dans la même suite (voir §6.4). La compaction a lieu **à la fin d'un tour complet** (après la
réponse de tous les PNJ du tour), et elle garde toujours les derniers tours en clair.

### 4.8 Retour en arrière (bouton ↩ « revenir ici » sur chaque bulle)

Chaque bulle porte son numéro `#N` (voir §6.2). Le bouton ↩ d'une bulle ramène la partie à
l'état exact où elle se trouvait juste après ce tour — conversation, résumé et acte.

- **Destructif** : tout ce qui suit le tour choisi est supprimé définitivement. L'appli demande une
  confirmation explicite et propose de faire une sauvegarde avant (jamais de sauvegarde silencieuse).
- **Restauré** : les tours, le résumé tel qu'il était à ce moment-là (la bonne compaction, voir §6.6),
  l'acte courant.
- **Non restauré** : la présence 🔊/🔇, l'interjection 💬 et les réglages de génération. Ce sont des
  réglages que le joueur change lui-même : remonter dans la conversation ne doit pas réactiver un
  PNJ qu'on vient de couper.
- Le tour suivant reprend le numéro `N+1`.
- « Annuler » et « Régénérer » sont des cas particuliers de ce mécanisme (retour au dernier tour
  du joueur, puis nouvelle génération).

## 5. Vérifier un scénario avant de le tester en vrai

Le plus fiable : charger le scénario par le code et regarder le résultat, pas juste relire le texte
(le piège du §2.3 ne se voit pas à la simple lecture). Un script Java jetable suffit :

```java
ChatFileStorageAdapter storage = new ChatFileStorageAdapter();
ChatScenario scenario = storage.loadScenario(Path.of("chatscenarios"), "mon-scenario");
System.out.println("Joueur : " + scenario.playerName());
System.out.println("Cast : " + scenario.cast().npcs().size());
System.out.println("Actes : " + scenario.acts().size());
ChatSession session = ChatSession.fresh(scenario);
session.turns().forEach(t -> { if (t.speaker() == ChatTurn.Speaker.NARRATOR) System.out.println("[" + t.text() + "]"); });
```

Vérifier en particulier : le nombre d'actes correspond à ce qui était voulu, et les tours NARRATOR
d'ouverture orientent bien le joueur (voir §2.4).

## 6. Le dossier `run/` : la partie en cours (généré par l'appli)

Tout ce qui précède (§1 à §3) est le **scénario** : des textes écrits à la main, jamais modifiés par
l'appli. Cette section décrit l'autre moitié : ce que l'appli **génère** pendant qu'on joue. Tout y
vit dans `run/`, sous-dossier du scénario. Un scénario neuf n'a pas de `run/`. Rien dans `run/` ne
s'écrit ni ne s'édite à la main. La spécification technique (formats exacts, algorithmes) est dans
`specs/chat.md`, section « Le run ».

### 6.1 Vue d'ensemble

```
chatscenarios/mon-scenario/
  scenario.txt, scenario-N.txt, characters/      <- le scénario (§1)
  run/
    current/                                     la partie en cours
      turns/
        000001.md                                un fichier par tour
        000002.md
        ...
        000013.md
        000013-compaction-f000012.md             une compaction : un fichier dans la même suite
        000014.md
        ...
      state.txt                                  présence et interjection des PNJ
    saves/
      2026-10-25-143012-123.zip                  sauvegardes manuelles (zip de current/)
    archive/
      2026-10-25-150400-567.zip                  parties archivées par « Recommencer »
    logs/                                        seulement si lancé avec --logs (§6.8)
    tests/                                       rapports du testeur de scénario (§6.8)
```

Trois familles, trois durées de vie :

| Dossier | Nature | Durée de vie |
|---|---|---|
| `current/` | État de la partie en cours | Remplacé à chaque « Recommencer », réduit par un retour en arrière |
| `saves/`, `archive/` | Parties mises de côté | Précieux, jamais supprimés par l'appli |
| `logs/`, `tests/` | Diagnostics | Jetables |

### 6.2 Les tours : un fichier par tour, numérotés

Chaque tour de la conversation — réplique du joueur, d'un PNJ, ou ligne du narrateur (les `[...]`
d'ouverture d'un acte, §2.4) — est un fichier dans `run/current/turns/`. Il est écrit dès que le
tour existe.

- **Numéro** (`TurnNumber`) : entier qui s'incrémente de 1 à chaque tour, à partir de 1 au début
  de la partie. Il ne redescend jamais, sauf par un retour en arrière (§4.8). La compaction
  (§6.4) ne le remet pas à zéro : le numéro identifie le tour pour toute la partie.
- **Nom du fichier** : numéro sur 6 chiffres, `000013.md`. Le nom suffit à ordonner et à retrouver
  un tour, sans ouvrir le fichier.
- **Contenu** : une ligne séparatrice avec le numéro et l'acte en vigueur quand le tour a été
  écrit, puis l'en-tête du locuteur, puis le texte du tour.

```
----- #13 · acte 2 -----
### LLM: elena
*Elena essuie un verre sans quitter le colis des yeux.* « Posez ça sur le comptoir, doucement. »
```

Les trois en-têtes de locuteur sont `### PLAYER`, `### LLM: {id du PNJ}` et `### NARRATOR`. Pour un
scénario sans actes, l'acte écrit est `acte 0`.

**Le numéro n'existe jamais pour le LLM.** Il est porté par un champ du tour, jamais par son texte.
Le prompt est construit à partir du texte et des champs du tour : la ligne `----- #13 · acte 2 -----`
n'existe que dans les fichiers du disque (et le numéro `#13` sur les bulles de l'interface).

Pour relire toute l'histoire d'une partie, ouvrir les fichiers de tour dans l'ordre : mis bout à bout,
ils reproduisent un journal lisible grâce à la ligne séparatrice (il n'y a pas d'export automatique).

### 6.3 La fenêtre vivante

Ce que le LLM reçoit comme « conversation » n'est pas toute la suite des tours : c'est le résumé de
la dernière compaction (§6.4), puis les tours qui la suivent en clair. Les tours déjà repliés dans le
résumé restent sur disque mais ne sont plus envoyés au modèle.

### 6.4 Les compactions : un fichier dans la suite

Une compaction replie les anciens tours dans un résumé (`ChatSummarizer`). Elle se produit **à la fin
d'un tour complet**, après la réponse de tous les PNJ, et seulement si le budget de contexte est
dépassé. Elle se caractérise par deux numéros :

- **R** : le numéro du dernier tour existant au moment de la compaction. C'est lui qui la place
  dans la suite : la compaction vient juste après le tour R.
- **F** : le numéro du dernier tour **replié** dans le résumé. Normalement `F < R` : les tours de
  `F+1` à `R` restent en clair (F = R seulement si une compaction a tout replié, fenêtre vide). F ne
  peut pas se deviner à partir de R, parce que le nombre de tours gardés en clair varie d'une
  compaction à l'autre.

Le fichier s'appelle `{R sur 6 chiffres}-compaction-f{F sur 6 chiffres}.md` et contient **le texte du
résumé, rien d'autre**. Exemple : `000013-compaction-f000012.md` = compaction faite après le tour 13,
qui a replié les tours jusqu'au 12 ; les tours 13 et 14 sont donc encore en clair à l'étape suivante.

Un résumé est **cumulatif** : il inclut déjà le contenu du résumé précédent. Pour reprendre une
partie à un moment donné, on n'a donc jamais besoin que d'**une seule** compaction, la plus récente
qui existait à ce moment-là.

Il n'y a pas d'identifiant propre à une compaction : R suffit, il est unique dans une histoire linéaire
(un retour en arrière supprime toutes les compactions de R supérieur).

### 6.5 `state.txt` : acte courant, présence et interjection

Les seuls réglages de la partie qui ne se déduisent pas des tours :

```
act=2
present=elena,marcus
interjecting=elena
```

- `act` : l'acte en vigueur maintenant. Il peut différer de l'acte écrit sur le dernier tour après un
  « Précédent » manuel (qui ne crée aucun tour) : c'est ce fichier qui le retient.
- `present`, `interjecting` : un identifiant de PNJ par valeur, séparés par des virgules (les ids sont
  les noms de fichier de `characters/`, sans `.txt`). Clé absente : tous les PNJ sont présents et
  éligibles à l'interjection (comme une partie neuve). Voir §4.1 et §4.3.

### 6.6 Reprendre la partie à un numéro donné

Au chargement d'une partie, comme lors d'un retour en arrière au tour N, l'appli reconstruit l'état
« juste après le tour N » (au chargement, N est le dernier tour). Règle :

1. Lister les noms de fichiers de `turns/`, sans les ouvrir.
2. Choisir la **compaction la plus récente dont R ≤ N** ; s'il n'y en a aucune, il n'y a pas de résumé.
3. Le résumé est le contenu de ce fichier (ou vide). La conversation en clair est la suite des
   tours de `F+1` à `N` (ou de `1` à `N` sans compaction).
4. L'acte courant est celui écrit sur le tour N. Seule exception : au chargement d'une partie (N =
   dernier tour), c'est l'acte de `state.txt` qui prime, pour ne pas perdre un « Précédent » manuel.

Exemple. Partie jouée : tours 11, 12, 13, puis une compaction après le 13 (qui a replié jusqu'au 12),
puis les tours 14, 15, 16.

```
turns/  000011.md  000012.md  000013.md  000013-compaction-f000012.md  000014.md  000015.md  000016.md

Reprendre au #16 : résumé de la compaction + tours 13, 14, 15, 16
Reprendre au #14 : résumé de la compaction + tours 13, 14      (la compaction a bien existé avant le 14)
Reprendre au #13 : résumé de la compaction + tour 13           (la compaction suit le tour 13)
Reprendre au #12 : pas de résumé (la compaction n'a pas encore eu lieu) + tours 1 à 12
```

Un **retour en arrière** ajoute une dernière étape : tout fichier dont le numéro est supérieur à N est
supprimé — un fichier de tour par son propre numéro, une compaction par son R. La compaction de
l'exemple ci-dessus disparaît donc dans un retour au #12 et survit à un retour au #13. Ensuite, si le
texte en clair dépasse le budget de contexte (cas d'un retour avant la première compaction, où il
n'y a plus de résumé pour alléger), une nouvelle compaction est faite immédiatement.

### 6.7 Sauvegardes et archives : des zips de `current/`

Une sauvegarde manuelle (💾) et une archive de « Recommencer » ont exactement la même forme : un zip de
`run/current/` (le dossier `turns/` et `state.txt`), nommé d'après l'horodatage (précision à la
milliseconde, pour que deux sauvegardes rapprochées ne s'écrasent jamais) :
`run/saves/2026-10-25-143012-123.zip`, `run/archive/2026-10-25-150400-567.zip`.

- Une sauvegarde est **complète** : elle contient tout l'historique, donc on peut remonter dans le
  temps après l'avoir chargée.
- Charger une sauvegarde **remplace** `current/` par le contenu du zip (confirmation demandée).
- La liste des sauvegardes affiche, pour chacune, le numéro de son dernier tour.
- Les zips sont des fichiers immuables : l'appli n'en modifie jamais un après l'avoir écrit.

### 6.8 `logs/` et `tests/`

- **`logs/`** : par défaut, **rien n'est écrit**. Pour activer les logs d'une session, lancer l'appli
  avec l'option `--logs` (`chat.bat --logs`). Il se crée alors un sous-dossier horodaté par lancement,
  avec `master-log.txt` et `llm_calls/` (un fichier par appel au modèle, prompt compris).
- **`tests/`** : le testeur de scénario (`test-scenario.bat`) y écrit `{horodatage}/report.html`. Le
  rapport est le produit du testeur : il est toujours écrit. Les logs détaillés de ses appels au
  modèle suivent la même règle que `logs/` (`test-scenario.bat --logs`, dans le même sous-dossier
  horodaté que le rapport).

### 6.9 Que peut-on supprimer ?

| Pour... | Supprimer |
|---|---|
| Repartir de zéro en gardant sauvegardes et archives | `run/current/` |
| Libérer de la place sans toucher aux parties | `run/logs/` et `run/tests/` (`clean-logs.bat`) |
| Effacer toute trace de jeu sur ce scénario | `run/` |
| Partager ou archiver le scénario | copier le dossier sans `run/` |

### 6.10 Ancien format et migration

Avant cette structure, les fichiers de session (`history.md`, `summary.md`, `act.txt`, `present.txt`,
`interject.txt`, `full-history.md`) et les dossiers `saves/`, `archive/`, `logs/`, `tests/` vivaient à
la racine du scénario, à côté des fiches personnage. Si l'appli en détecte, le scénario reste dans la
liste mais refuse de se charger, avec un message qui demande de lancer `migration.bat`. Ce programme,
à lancer une seule fois :

- déplace les fiches personnage de la racine vers `characters/` ;
- **supprime** les anciens fichiers de session et dossiers générés : **les anciennes parties,
  sauvegardes et archives sont jetées**, seul le scénario est conservé ;
- traite le dossier `chatscenarios/` du projet et le dossier configuré dans `chat.properties`
  (`chatscenarios.root.default`), ou un chemin passé en argument ;
- offre un `--dry-run` qui liste ce qui serait déplacé et supprimé sans rien toucher, et demande une
  confirmation avant d'agir.
