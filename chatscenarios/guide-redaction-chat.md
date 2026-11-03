# Guide de rédaction des scénarios de chat

Référence pour écrire ou modifier un scénario dans `chatscenarios/`. Les faits décrits ont été
vérifiés dans le code.

**Plan.** *Spécification* (ce que fait l'appli) : §1 fichiers, §2 format de `scenario.txt`, §3 fiche
personnage, §4 mécanismes de jeu, §5 vérification, §6 dossier `run/`. *Conseils et pièges*
(recommandations, hors spécification) : §7. À lire avant d'écrire les actes : §2.3, §2.7 et §7.

**Maintenance.** Ce guide est mis à jour à chaque fonctionnalité du module chat qui touche l'auteur
d'un scénario, et à chaque limite ou piège découvert.

## 1. Fichiers

Un scénario est un dossier. L'auteur n'y écrit que `scenario*.txt`, `style.txt`, `characters/` et `picks/` ;
l'appli n'écrit que dans `run/` (§6).

```
chatscenarios/mon-scenario/
  scenario.txt        obligatoire — ligne Joueur, prémisse, actes
  scenario-1.txt      optionnel — suite des actes (numéros consécutifs)
  style.txt           optionnel — conseil de style commun à tous les personnages (§2.10)
  characters/         obligatoire — un fichier par personnage
    elena.txt
    marcus.txt
  picks/              optionnel — paragraphes tirés par {pick … nom} (§2.6) ou déroulés par {repeat … nom} (§2.6 bis)
    lieu.txt
  run/                généré par l'appli, jamais écrit à la main (§6)
```

| Élément | Règle |
|---|---|
| `scenario-N.txt` | Concaténés après `scenario.txt`, numéros consécutifs, arrêt au premier manquant. Chacun commence par un titre `# ...` de premier niveau, sinon le chargement échoue avec un message explicite. |
| `characters/*.txt` | Un fichier = un personnage si sa **première ligne** est `# Nom`. Sinon le fichier est ignoré sans avertissement. Seul `characters/` est lu. |
| Id d'un personnage | Nom du fichier sans `.txt` : simple, sans espace ni accent (`marcus.txt`, pas `Marcus Dupont.txt`). |
| Nombre de PNJ | Un seul fichier suffit : le scénario est alors mono-personnage. |
| Liste de l'appli | Un scénario y figure s'il a un `scenario.txt` et au moins un personnage valide. |
| Copier ou partager | Copier le dossier sans `run/`. |

**Démos** : `chatscenarios/demos/` contient un scénario minimaliste par mécanisme (`demo-pick`,
`demo-repeat`, `demo-actes`, `demo-pnj`…), chacun avec un `README.md` qui dit ce qu'il montre et quoi regarder.
Au démarrage de `chat.bat`, le choix « 3 - Démos du projet » les ouvre. À consulter comme exemple de
chaque commande ; à compléter à chaque nouveau mécanisme (une démo par mécanisme, sujet anodin, tous publics,
personnages adultes).

**Squelette** : `init-chat.bat` demande le dossier (même choix que `chat.bat`), le nom du scénario
(lettres, chiffres, `-`, `_`), le nombre de PNJ et leurs noms. Il crée `scenario.txt` et une fiche par
PNJ, chaque champ étant une consigne entre parenthèses à remplacer, avec un premier acte qui nomme
tous les PNJ. Il n'écrase jamais un scénario existant et ne crée pas `run/`.

### 1.1 Exemple minimal

`chatscenarios/auberge/scenario.txt`

```
Joueur : Mila
Langue : français

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

- Un seul acte : pas de `Quand écrire [NEXT ACT]` (rien à quoi avancer, §2.5).
- Plus court encore : un `scenario.txt` sans acte (ligne `Joueur :` et prémisse contenant un bloc
  `[...]` d'ouverture) est valide ; la partie démarre sans progression d'actes (§2.4).
- Sans ligne `# SECRET`, toute la fiche est publique.

## 2. Format de `scenario.txt`

### 2.1 En-tête : lignes `Joueur` et `Langue`

```
Joueur : Alex
Langue : français
```

Lignes optionnelles, **consécutives, tout en haut** de `scenario.txt`, dans n'importe quel ordre. La
première autre ligne (même vide) termine l'en-tête ; une ligne `Joueur :` ou `Langue :` placée plus bas
est du texte ordinaire.

| Ligne | Effet | Défaut |
|---|---|---|
| `Joueur : X` | Nom du joueur : il remplace « Player » dans la transcription envoyée au modèle. | `Alex` |
| `Langue : X` | Langue dans laquelle les PNJ écrivent leurs réponses. Code (`fr`, `en`, `es`, `de`, `it`, `pt`), nom français ou anglais, sans tenir compte de la casse ni des accents. Toute autre valeur est transmise telle quelle. | Non précisée : le PNJ choisit lui-même la langue (fiche et scénario). |

Convention du projet : toujours écrire les deux lignes. Sans `Langue :`, un petit modèle peut hésiter
entre les langues du prompt, de la fiche et du joueur. La langue est donnée de la même façon à tous les
appels du module (réponses des PNJ, résumé, analyses 🔍 et état d'esprit, testeur de scénario).

**Scénarios sans ligne `Langue :`** : ils restent valides, la langue est alors simplement non précisée.

### 2.2 Prémisse

```
#SCENARIO
Monde : ...
Prémisse : ...
Ton : ...
```

- Avec `#SCENARIO`, son corps est la prémisse et les actes commencent après. Sans lui, le texte avant le
  premier titre `#` est la prémisse.
- `Monde :`, `Prémisse :`, `Ton :` (parfois `Fil conducteur discret :`) sont une convention, pas une
  syntaxe.
- La prémisse est envoyée à chaque PNJ à chaque tour (§2.7). Le joueur ne la voit jamais telle quelle.

**3e personne obligatoire**, pour la prémisse, les lignes `[...]` et le texte des actes : ces textes
sont recopiés dans le prompt de chaque PNJ. « Alex est un jeune mage… », jamais « Tu es Alex… » : la
2e personne contredit l'assignation de personnage donnée au PNJ dans son propre prompt.

### 2.3 Structure des actes

Les actes s'écrivent en titres Markdown imbriqués (`#`, `##`, `###`…). **Seules les feuilles (titre sans
sous-titre) sont des actes jouables.** Un titre qui a un enfant n'est jamais l'acte courant : son corps
est hérité par ses descendants.

```
# Chapitre A          <- pas jouable (a des enfants)
## Scène A.1          <- acte 1.1
## Scène A.2          <- acte 1.2
# Chapitre B          <- pas jouable
## Scène B.1          <- acte 2.1
```

Deux actes séquentiels indépendants s'écrivent au même niveau (`#` / `#`). `# Acte 1` suivi d'un seul
`## Acte 2` donne **un seul acte** (le second, qui hérite du premier). N'imbriquer que pour un vrai
regroupement : un chapitre contenant plusieurs scènes.

| Sujet | Règle |
|---|---|
| Texte d'une feuille | Corps de chaque ancêtre (racine → feuille) puis corps de la feuille, séparés par une ligne vide. C'est le `CURRENT SCENE` du PNJ (§2.7). |
| Texte commun | Le corps d'un parent est partagé par toutes ses scènes enfants et renvoyé au modèle à chaque tour de chacune. |
| Titres | Jamais envoyés au modèle ; utilisés par l'interface et le testeur de scénario. |
| `[...]` d'un parent | Affichés une seule fois, à la première entrée dans le chapitre. Restent dans le texte envoyé au modèle. |
| `Quand écrire [NEXT ACT]` dans un parent | Hérité par toutes les feuilles du chapitre. À écrire dans la feuille. |
| `{npc-starts}`, `{npc-do}` | Pris en compte à la première entrée dans le titre qui le porte ; la première commande rencontrée, de la racine à la feuille, décide (§4.5). |
| `{npc-mode do on\|off …}` | Appliqué dès l'entrée dans le titre qui le porte, puis pour tous les sous-actes (§2.12). |
| `{npc-delta …}` | Appliqué dès l'entrée dans le titre qui le porte, puis pour tous les actes suivants (§2.8). |
| `{npc-story on\|off …}` | Comme `{npc-delta}` : appliqué dès l'entrée dans le titre qui le porte, puis pour tous les actes suivants, **même hors de ce titre**, jusqu'à ce qu'un autre `on` ou `off` le change (§2.8 bis). |
| Numérotation | Racines `1`, `2`… ; enfants `1.1`, `1.2`… ; petits-enfants `1.1.1`. |
| Saut de niveau | `#` puis `###` : traité comme `##`. |
| Ligne commençant par `#` | Toujours un titre, même `#mot` : elle coupe le corps de l'acte. |
| Parent sans corps | Valide (regroupement et numéro). |
| Plusieurs fichiers | Chaque `scenario-N.txt` commence par un `#` de premier niveau : un chapitre et ses `##` tiennent dans un seul fichier. |
| Fins de ligne | CRLF et CR sont normalisés en LF. |

Exemple : texte commun au chapitre, une condition par scène.

```
# Le col de Vaur
Le col est battu par le vent. Les bandits de Varek tendent des embuscades depuis les rochers.
## Embuscade
[Des archers surgissent des rochers.]
Quand écrire [NEXT ACT] : Dès que les archers ont fui ou sont à terre.
## Poursuite
[Varek s'enfuit vers la vallée.]
Quand écrire [NEXT ACT] : Dès que Varek est capturé ou hors de vue.
```

Toujours vérifier le nombre d'actes obtenu (§5).

### 2.4 Lignes `[...]` : ce que le joueur voit

Une ligne `[...]` du corps d'un acte devient un tour NARRATOR affiché au joueur quand l'acte devient
courant. Aucun appel au modèle n'est fait pour l'afficher.

- Le `[` est le premier caractère non blanc de sa ligne, le `]` le dernier d'une ligne (la même ou une
  suivante). Un bloc sur plusieurs lignes est joint en une seule ligne de narration.
- Le texte reste aussi dans le prompt de chaque PNJ (§2.7).
- **Scénario sans actes** : seul le premier bloc `[...]` de la prémisse est affiché ; les autres sont
  ignorés. Plusieurs scènes d'ouverture exigent de vrais actes.
- **Chapitre parent** : ses `[...]` ne s'affichent qu'à la première entrée dans le chapitre.
- **Tout premier acte** : ses `[...]` disent qui est le joueur, où il est, qui est présent (**tous les PNJ
  nommés**, même discrets) et quel est le but général. Tous les PNJ sont présents dès le tour 1 (§4.1) ;
  un PNJ non présenté peut être tiré au hasard et rester sans répondre (§7.4).
- **Contexte de départ du joueur : à écrire, et ici.** Un joueur qui arrive dans une histoire a besoin de
  quelques lignes de situation (qui il est, où, pourquoi, avec qui) : sans elles, il ne sait pas quoi
  répondre. Pas de mécanisme à part : ce sont les `[...]` du tout premier acte, affichés au joueur au
  début. Les écrire là plutôt que dans la prémisse : la prémisse est relue à **chaque tour jusqu'à la fin**
  par tous les PNJ, alors que le texte du premier acte ne pèse que tant que cet acte est courant, puis
  glisse dans l'historique et le résumé (§2.7, §7.1). Un acte d'introduction court, avec une condition
  `[NEXT ACT]` simple (par exemple « dès que le joueur a répondu »), limite encore ce coût.
- Même règle de 3e personne que la prémisse (§2.2).

```
[Le tonnerre gronde alors qu'Alex referme derrière lui la porte de l'auberge.]
```

### 2.5 Texte d'accompagnement de l'acte

Jamais affiché au joueur ; lu par les PNJ.

```
Point de départ : ...
Ce qui doit se passer : ...
Quand écrire [NEXT ACT] : ...
```

| Libellé | Rôle |
|---|---|
| `Point de départ` | Situe la scène. |
| `Ce qui doit se passer` | Ce que la scène doit accomplir avant d'avancer. |
| `Quand écrire [NEXT ACT]` | Condition de transition. À omettre sur le dernier acte. |

- Les libellés sont une convention. Seuls `#SCENARIO`, les titres et les `[...]` sont analysés.
- Le mode strict de `[NEXT ACT]` (§4.5) s'active dès que le texte `[NEXT ACT]` figure dans le texte
  résolu de l'acte. Écrit **dans une phrase**, il est ignoré comme ligne `[...]` ; **seul sur une
  ligne entre crochets**, il serait affiché au joueur comme narration.
- Sans condition sur un acte non final, le PNJ avance quand la scène lui semble conclue (moins précis).

### 2.6 Contenu aléatoire : `{pick type nom}`

Une commande `pick` de `scenario.txt` ou `scenario-N.txt` est remplacée, au début de chaque nouvelle
partie, par un ou plusieurs paragraphes de `picks/nom.txt`.

**Fichier de picks** : paragraphes séparés par une ligne ne contenant que `---` (trois tirets ou plus).
Un paragraphe peut tenir sur plusieurs lignes. Sans séparateur : un seul paragraphe. Paragraphes vides
ignorés. Le nom est le nom du fichier sans `.txt` (lettres, chiffres, `_`, `-`).

**Le type est obligatoire** (deuxième mot), le nom du fichier est le troisième :

| Commande | Comportement |
|---|---|
| `{pick random lieu}` | Un paragraphe, tirage indépendant : le même peut sortir plusieurs fois. |
| `{pick unique lieu}` | Un paragraphe jamais déjà distribué pour `lieu` pendant cette partie. Si tous sont sortis, le chargement échoue avec un message. |
| `{pick 3 lieu}` | 3 paragraphes **différents entre eux**, au hasard, séparés par une ligne vide. Erreur si le fichier en contient moins. |
| `{pick all lieu}` | **Tous** les paragraphes, dans l'ordre du fichier, sans hasard (par exemple pour importer une fiche de lore fixe). |

Ce que distribue n'importe quelle commande (`random`, `unique`, nombre ou `all`) compte comme « déjà
distribué » pour un `unique` ultérieur sur le même fichier. Un `random` ou un nombre, eux, ignorent ce
qui est déjà sorti.

```
Prémisse : Alex arrive dans une ville portuaire où quelque chose cloche. {pick random lieu}
...
Point de départ : Alex vient de quitter la rue. En face de lui : {pick unique lieu}
Ce qui doit se passer : Elena met Alex en garde. {pick 2 intrigue}
```

Ici le `unique` ne peut pas retomber sur le lieu déjà donné à la prémisse. Avec 4 `{pick unique lieu}`
pour 3 paragraphes, le chargement s'arrête sur `{pick unique lieu} : les 3 paragraphe(s) de
picks/lieu.txt ont deja tous ete tires.`

- Le tirage se fait **une fois par partie** (premier lancement, « Recommencer »). Le résultat est écrit
  dans `run/current/resolved/` (copie des `scenario*.txt` avec les tags remplacés) ; le jeu lit cette
  copie. Les paragraphes sont donc stables, y compris après redémarrage. Une sauvegarde 💾 emporte ses
  tirages.
- Le bouton 🎲 de la partie affiche le tableau des tirages : une ligne par commande `pick`, avec le
  fichier, le type tel qu'écrit (`random`, `unique`, `3`, `all`) et la valeur obtenue (un paragraphe par
  ligne quand il y en a plusieurs), dans l'ordre du scénario. Le bouton 🐞 (debug) affiche le texte de scénario réellement envoyé aux personnages : la
  prémisse puis l'acte courant (le texte des `#`/`##`/`###` parents inclus, §2.3), sans l'historique.
  Les tirages ne sont enregistrés que pour une partie démarrée après cette fonction ; une partie plus
  ancienne affiche « Aucun tirage enregistré ».
- **Modifier `scenario*.txt` en cours de partie n'a pas d'effet** ; ⟳ recharge la copie, pas les
  fichiers d'origine. Pour voir une modification : « Recommencer ». Un scénario sans tag est lu
  directement.
- Le paragraphe est inséré tel quel. Dans une ligne `[...]`, il doit tenir sur **une seule ligne**.
  Ailleurs (prémisse, texte d'accompagnement), plusieurs lignes sont acceptées.
- Le joueur ne voit un paragraphe que s'il tombe dans un texte affiché (une ligne `[...]`).
- Pas de tirage imbriqué (une commande dans un paragraphe tiré n'est pas résolue ; un `%NOM%` de `{cast}`
  l'est, §2.6 ter). Les fiches personnage ne sont pas concernées.
- Fichier `picks/nom.txt` absent ou sans paragraphe : échec du chargement, avec le nom du fichier.

**Bibliothèque commune à plusieurs scénarios : `/lib/nom`.** Au lieu de recopier le même fichier dans les
`picks/` de chaque scénario, on le range une fois dans un dossier `lib/` **à côté des dossiers de scénarios**
(dans la racine choisie au menu de `chat.bat`, par exemple `chatscenarios/lib/evenements.txt`) et on l'appelle
avec un chemin qui commence par `/lib/` :

```
{pick random /lib/evenements}
{pick unique /lib/lieux/ville}
{repeat order /lib/defis}
```

- Valable partout où un nom de bibliothèque est attendu : `{pick …}` (tous les types) et `{repeat …}`. Le
  fichier a le même format que ceux de `picks/` (paragraphes séparés par `---`, morceaux par `-`).
- Sous-dossiers permis (`/lib/lieux/ville` lit `lib/lieux/ville.txt`). Chaque partie du chemin ne contient que
  lettres, chiffres, `_` et `-` : impossible de sortir de `lib/`.
- `unique` compte ce que la bibliothèque a déjà donné, comme pour un fichier de `picks/`.
- La racine est celle du menu : les démos (`chatscenarios/demos/`) lisent `chatscenarios/demos/lib/`.
- Le dossier `lib/` n'est pas un scénario et n'apparaît pas dans la liste.
- Le tirage est figé dans la partie comme tout `pick` : modifier la bibliothèque ne change pas une partie en
  cours. En revanche **le scénario n'est plus autonome** : copié ou envoyé ailleurs, il doit être accompagné
  du dossier `lib/` ; sinon le chargement échoue avec « lib/nom.txt : fichier introuvable ».
- Le tableau 🎲 affiche la bibliothèque sous le nom `/lib/nom`.

Exemple : `chatscenarios/demos/demo-lib/`.

### 2.6 bis Séquence répétée : `{repeat mode nom}`

Pour une série de scènes qui font toutes la même chose (une suite d'épreuves, de visites, d'interrogatoires…),
on écrit **le gabarit une seule fois** ; il est recopié une fois par paragraphe de `picks/nom.txt`. Le
fichier est celui d'un `pick` (§2.6) : paragraphes séparés par `---`.

```
{repeat random 3 defis}
# Défi {n}
[Megumin annonce le défi numéro {n} d'un grand geste.]
Point de départ : {item2}
Ce qui doit se passer : Megumin explique le défi. {item1}
Quand écrire [NEXT ACT] : {item3}
{end-repeat}
```

| Commande | Comportement |
|---|---|
| `{repeat order nom}` | Tout le fichier, dans l'ordre du fichier. |
| `{repeat order 2 nom}` | Les 2 premiers paragraphes du fichier. |
| `{repeat random nom}` | Tout le fichier, dans un ordre aléatoire. |
| `{repeat random 3 nom}` | 3 paragraphes **différents**, tirés au hasard. |
| `{repeat 3}` | 3 copies, **sans fichier** : seul `{n}` existe (pas de `{item}`). |
| `{repeat order cast NOM}` | Une copie par **personnage** du casting, dans l'ordre du casting ; `{item}` et `%NOM%` = son nom (§2.6 ter). |
| `{repeat random 2 cast NOM (jack, mia, john)}` | 2 personnages **différents** de la liste, au hasard (liste et nombre facultatifs). |

- Le mode est obligatoire ; le nombre est facultatif (sans lui, tout le fichier). Un paragraphe ne sort
  jamais deux fois dans un même bloc ; deux blocs sur le même fichier sont indépendants. Un nombre
  supérieur au nombre de paragraphes est une erreur.
- `{repeat …}` et `{end-repeat}` sont chacun **seuls sur leur ligne**. Les blocs ne s'imbriquent pas. Le
  gabarit contient en général un titre `#` : chaque copie devient un acte.
- Zones remplacées dans le gabarit : `{n}` = numéro de la copie (1, 2, 3… dans l'ordre de jeu, pas du
  fichier) ; `{item}` = le paragraphe entier ; `{item1}`, `{item2}`… = les morceaux du paragraphe. Elles
  ne valent qu'à l'intérieur d'un bloc.
- **Morceaux** : une ligne ne contenant qu'un seul `-` coupe un paragraphe en morceaux. Exemple de
  paragraphe : la description du défi, `-`, la mise en situation, `-`, la condition de fin. Une ligne
  `- texte` n'est pas un séparateur. Si le gabarit utilise `{itemK}` jusqu'à K = 3, **chaque** paragraphe
  du fichier doit avoir exactement 3 morceaux, sinon le chargement échoue en nommant le paragraphe.
  `{item}` n'impose rien (il retire simplement les lignes `-`).
- La condition de fin `Quand écrire [NEXT ACT] : {item3}` rend chaque copie précise : écrire le morceau
  comme une phrase complète (« Dès qu'Alex a… »), pas la fin d'une phrase du gabarit (élision, accord).
- Ne pas mettre `{item…}` dans une ligne `[...]` si le paragraphe tient sur plusieurs lignes (§2.6).
- Le dépliage se fait **avant** `pick` : un `{pick}` écrit dans le gabarit est tiré une fois par copie, et
  une commande contenue dans un paragraphe du fichier est exécutée. Les zones, elles, ne sont lues qu'une
  fois (un `{n}` écrit dans un paragraphe reste tel quel).
- Comme `pick`, le dépliage se fait **une fois par partie** : `run/current/resolved/` contient le
  scénario déplié, sans aucune commande `repeat`. Le bouton 🎲 affiche une ligne par bloc (type
  `repeat-order` ou `repeat-random`) avec les paragraphes dans l'ordre de jeu. « Recommencer » pour voir
  une modification.

Exemple complet : `chatscenarios/demos/demo-repeat/`.

### 2.6 ter Choisir un personnage : `{cast NOM …}` et `%NOM%`

Pour que le scénario choisisse lui-même un personnage du casting (qui est le chef du jour, qui est le
coupable…) et le désigne ensuite dans le texte et les commandes.

```
{cast PNJ_1 (jack, mia, john)}
{cast PNJ_2 unique (jack, mia, john)}
{cast PNJ_3 reuse-possible}
```

| Écriture | Effet |
|---|---|
| `{cast NOM}` | Un personnage de **tout le casting**, **autre que ceux que tiennent les autres variables** à cet endroit. |
| `(jack, mia, john)` | Liste facultative : le tirage se limite à ces personnages. **Un seul mot par personnage** (son identifiant, ou un mot de son nom). |
| `unique` | Cette variable ne reprend **jamais un personnage qu'elle a déjà eu** : dans un `{repeat}`, un personnage différent à chaque copie. |
| `reuse-possible` | Peut reprendre le personnage d'une autre variable (le même personnage dans deux rôles). |

`unique` et `reuse-possible` se combinent. La commande est **seule sur sa ligne** et ne s'affiche pas.

- **`%NOM%`** vaut le nom complet du personnage, de la commande jusqu'à la fin du scénario (ou du bloc
  `repeat` qui l'a déclarée). Il s'écrit dans le texte, dans les `[...]` et **dans les commandes** : `{npc-starts %PNJ_1%}`,
  `{npc-story on %PNJ_1% dette}`, `{npc-delta secret %PNJ_2% : …}`. Un nom : une lettre puis lettres,
  chiffres ou `_`. Utilisé avant d'être déclaré, c'est une erreur.
- **Exclusivité** : par défaut, deux variables ne désignent jamais le même personnage. Sans `reuse-possible`,
  écrire `{cast A}` puis `{cast B}` donne deux personnages différents. Attention : le casting doit avoir assez
  de personnages pour toutes les variables en même temps.
- **Dans un `repeat`** : `{repeat order cast CHEF}` fait une copie par personnage et `%CHEF%` vaut le
  personnage de la copie ; `{cast COMMIS}` dans le bloc choisit un autre personnage que le chef (par défaut).
  Les variables déclarées dans une copie sont **oubliées à la fin de la copie** ; celles déclarées avant le
  `repeat` restent valables dedans, et leur personnage est exclu. La variable d'un `repeat … cast` parcourt
  toute la liste quoi qu'il arrive : elle n'est pas soumise à l'exclusivité, seuls les `cast` le sont.
- **Les commandes sont résolues dans l'ordre**, mais le tirage est cherché **globalement** : si un tirage
  empêche une commande suivante de trouver quelqu'un, il est défait et un autre est essayé. Le chargement
  échoue seulement quand **aucune combinaison** n'existe (par exemple deux variables sur un seul
  personnage, ou `unique` dans un `{repeat 4}` pour 3 personnages) ; le message nomme la commande.
- **Dans une bibliothèque** : un `%NOM%` écrit dans un paragraphe tiré par `{pick}` est lui aussi remplacé,
  à condition que le `{cast NOM}` soit plus haut dans le texte.
- Comme `pick`, le tirage se fait **une fois par partie** (« Recommencer » pour en changer). Le bouton 🎲
  affiche une ligne par `cast` (type `cast`, nom de la variable, personnage).
- La validation vérifie : les mots clés, que chaque personnage de la liste existe, qu'il est désigné sans
  ambiguïté et une seule fois, que chaque `%NOM%` est déclaré plus haut, et qu'une combinaison existe.
  Une commande qui contient un `%NOM%` est contrôlée avec un personnage du casting à sa place (le tag d'un
  `{npc-story}` n'est vérifié qu'une fois le personnage tiré).

Exemple complet : `chatscenarios/demos/demo-cast/`.

### 2.7 Ce que voit chaque PNJ

Prompt système envoyé au PNJ qui parle, à chaque tour, dans cet ordre :

| # | Bloc | Contenu |
|---|---|---|
| 1 | Règles de jeu | Texte fixe : incarner le personnage, portée du tour, longueur, fiche = note privée |
| 2 | `YOUR CHARACTER` | Sa fiche **complète** (publique + `# SECRET`) |
| 3 | `ALSO PRESENT` | Fiche **publique** des autres PNJ présents |
| 4 | `NOT IN THE SCENE` | Fiche **publique** des PNJ absents (ne jamais les faire parler) |
| 5 | `SCENARIO` | La **prémisse**, en entier |
| 6 | `CURRENT SCENE` | Texte de l'**acte courant** (§2.3), puis la règle `[NEXT ACT]` (§4.5) si ce n'est pas le dernier acte |
| 7 | `STORY SO FAR` | Résumé des tours anciens (§4.7) |
| 7 bis | `STYLE ADVICE` | Contenu de `style.txt` (§2.10), seulement s'il existe et n'est pas vide |
| 8 | Format | Conventions `*action*`, `OOC:`, `DO:` (§4.9) ; règle sur les autres PNJ ; règle d'interjection si besoin |

Le prompt utilisateur contient les derniers tours en clair.

| Information | PNJ qui parle | Autres PNJ | Joueur |
|---|---|---|---|
| Fiche publique | oui | oui | n'est pas dans le chat |
| `# SECRET` | oui (le sien) | jamais | n'est pas dans le chat |
| Prémisse | oui, à chaque tour | oui | non |
| Texte de l'acte courant | oui | oui | non, sauf les lignes `[...]` |
| Lignes `[...]` d'un acte | oui | oui | oui, à l'entrée de l'acte |
| Actes futurs | **jamais** | **jamais** | non |
| Actes passés | via résumé et derniers tours | idem | tours affichés |

Seul l'acte courant (ancêtres inclus) est payé à chaque tour, et une information écrite dans un acte
n'existe pour le modèle qu'à partir de l'arrivée dans cet acte : un acte futur ne peut ni être spoilé
ni alourdir le contexte.

### 2.8 Faire évoluer un personnage : `{npc-delta public|secret nom : texte}`

**Le petit delta** : un fait ponctuel, écrit en une ligne là où l'acte se passe. Il sait **seulement ajouter,
jamais retirer**. Pour un état qu'il faudra défaire (un avis qui change, un secret révélé), utiliser un tag
(§2.8 bis). Il garde deux atouts que le tag n'a pas : son texte peut dépendre d'un tirage (`{n}` ou `{item}`
dans un gabarit `{repeat}`, §2.6 bis), et il complète une fiche partagée entre plusieurs scénarios sans la
modifier.

Ajoute une ligne à la fiche d'un personnage **à partir de l'acte où la commande est écrite**, pour tous
les actes suivants. La commande tient sur **une ligne seule** ; la visibilité est obligatoire (deuxième
mot), le nom du personnage vient en troisième.

> Ancien nom : `{delta-pnj …}` est refusé au chargement avec le message « la commande s'appelle maintenant
> npc-delta ». Remplacer le nom, le reste de la commande ne change pas.

```
{npc-delta public Elena : Elle a perdu un œil dans l'embuscade.}
{npc-delta secret Marcus : Il a reconnu le chef des bandits et décide de le suivre.}
```

| Visibilité | Effet |
|---|---|
| `public` | La ligne rejoint la partie **publique** de la fiche : le personnage et tous les PNJ présents ou coéquipiers la lisent (blessure, changement visible). |
| `secret` | La ligne rejoint la partie **`# SECRET`** (créée si la fiche n'en a pas) : seul ce personnage la lit (nouvel objectif, découverte gardée pour soi). |

- Le nom suit les mêmes règles que `{npc-starts Nom}` (§4.5) ; un nom inconnu empêche le chargement.
- La ligne est retirée du texte de l'acte, qui va à tous les PNJ : un `secret` ne fuit jamais vers les autres.
- Dans un titre parent, la commande s'applique à l'entrée dans le chapitre, puis reste valable.
- Interdit dans la prémisse (aucun acte auquel l'attacher). Pas de `pick` dans le texte d'un delta.
- Les deltas sont recalculés à partir des actes 1 à l'acte courant : revenir en arrière (↩, « Précédent »,
  sauvegarde, ⟳) donne toujours la fiche qui correspond à l'acte. Les tours déjà joués restent dans
  l'historique.
- Chaque delta est relu à chaque tour par le personnage concerné : en garder peu, courts.

### 2.8 bis Activer et désactiver un texte de la fiche : `{npc-story on|off nom tag}`

Une fiche peut contenir des sections `## tag` (§3) : du texte qui **n'est pas dans la fiche** tant qu'une
commande ne l'active pas. `{npc-story on Léa chatons}` ajoute à la fiche de Léa le texte de sa section
`## chatons` ; `{npc-story off Léa chatons}` le retire. La commande tient sur **une ligne seule**, le tag est
son **dernier mot** (le nom du personnage peut avoir plusieurs mots).

```
# Le voisin
{npc-story on Léa froide}
{npc-story on Léa soupcon}
...
# Le soir
{npc-story off Léa froide}
{npc-story off Léa soupcon}
{npc-story on Léa amicale}
{npc-story on Léa chatons}
```

- **L'état se prolonge.** Une commande vaut à partir de l'acte où elle est écrite (ou du premier acte du
  titre qui la porte) **pour tous les actes suivants**, même hors de ce titre, jusqu'à un autre `on` ou
  `off`. C'est la durée de `{npc-delta}`, pas celle de `{npc-mode}` ni des niveaux d'activité (valables pour
  la seule section où ils sont écrits).
- Au départ **aucun tag n'est actif**. Un tag actif depuis le début s'active dans le titre ou le premier acte.
- **Public ou secret** : la place de la section dans la fiche décide (§3). Un tag public actif est lu aussi
  par les autres PNJ ; un tag secret n'est lu que par son personnage. Un tag ne change jamais de camp.
- **Du secret au public** : deux tags, un secret et un public, avec un texte différent (le personnage sait
  tout, mais ne dit publiquement que ce qu'il avoue) : `off` sur le premier, `on` sur le second, dans le même
  acte.
- Ce qui est permanent (le texte hors tag, avant et après `# SECRET`) ne se désactive pas.
- `SECRET` n'est pas un tag : `{npc-story … SECRET}` est une erreur.
- Interdit dans la prémisse. Un personnage ou un tag inconnu empêche le chargement.
- Recalculé à partir des actes 1 à l'acte courant : retour en arrière, sauvegarde et ⟳ redonnent l'état de
  l'acte retrouvé. Seuls les tags actifs partent dans le prompt : un tag désactivé ne coûte rien.
- Le validateur avertit d'un tag que rien n'active jamais, et d'un `off` sur un tag qui n'est pas actif.

**Tag ou delta ?** Le tag pour ce qui peut se défaire ou revenir, ou qui est long : le texte vit dans la fiche,
avec le reste du personnage. Le delta pour un fait unique, court, qu'on n'aura jamais à retirer.

Démo : `demo-npc-story`.

### 2.9 Validation du scénario

À chaque chargement d'un scénario, ses fichiers sont vérifiés et **toutes** les erreurs sont listées avec
leur fichier et leur ligne ; une erreur empêche le chargement.

| Erreur | Exemple |
|---|---|
| Ancienne syntaxe | `[PICK: x]`, `[NPC STARTS]` (à réécrire avec les accolades) |
| Commande inconnue ou mal écrite | `{teleport}`, `{pick lieu}` (type manquant), `{npc-delta Elena : …}` (visibilité manquante), `{npc-story on Léa}` (tag manquant) |
| Ancien nom de commande | `{delta-pnj …}` (devenu `{npc-delta …}`) |
| Variable de casting incorrecte | `{cast A libre}` (mot clé inconnu), personnage inconnu ou ambigu dans la liste, `%A%` avant `{cast A}`, `{cast …}` au milieu d'une phrase, nom de variable invalide |
| Combinaison de casting impossible | `{cast A (jack)}` puis `{cast B (jack)}` ; `unique` dans un `{repeat 4}` pour 3 personnages ; `{repeat 3}` sur une liste de 2 personnages |
| Commande qui doit être seule sur sa ligne | `{npc-starts}`, `{npc-do}`, `{npc-delta …}` ou `{npc-story …}` au milieu d'une phrase |
| Tag de fiche incorrect | `## SECRET`, `##` sans nom, tag vide, deux tags de même nom dans une fiche (§3) |
| Tag ou personnage inconnu | `{npc-story on Léa inconnu}` (pas de section `## inconnu` dans sa fiche), `{npc-story on Léa SECRET}` |
| Fichier ou paragraphes manquants | `picks/x.txt` ou `lib/x.txt` (`/lib/x`) absent, `{pick 9 x}` pour 3 paragraphes, trop de `unique` |
| Nom de bibliothèque invalide | `{pick random /lib/../x}`, `{pick random /lib/}`, `{pick random a.b}` |
| Bloc `repeat` incorrect | pas de `{end-repeat}`, bloc imbriqué ou vide, `{n}` ou `{item}` hors d'un bloc, `{repeat plain x}` (mode manquant), nombre trop grand, paragraphe qui n'a pas le même nombre de morceaux que les `{itemK}` du gabarit |
| Personnage inconnu | `{npc-starts Zorglub}`, `{npc-do Zorglub}`, `{npc-delta secret Zorglub : …}`, `{npc-active Zorglub}`, `{npc-mode do on Zorglub}` |
| Mode inconnu ou mal écrit | `{npc-mode fly on Elena}`, `{npc-mode do on}` (nom manquant) |
| Activité incohérente | `{npc-active …}` au milieu d'une phrase ; scène où tout le monde est absent ; même PNJ nommé deux fois dans un titre |

Avertissements (affichés au lancement et dans 🐞, sans bloquer) : un acte non final sans
`Quand écrire [NEXT ACT]`, un PNJ jamais cité dans la prémisse ni dans le premier acte.
`validation-scenarios.bat` vérifie tous les scénarios sans lancer de partie ni figer de tirage.

### 2.10 Style commun : `style.txt`

Fichier **optionnel**, à la racine du scénario (à côté de `scenario.txt`). Son contenu est donné à
**chaque personnage** sous le titre `STYLE ADVICE:`, après le résumé de l'histoire et avant les règles de
format. Il factorise le style quand plusieurs personnages l'ont en commun, et le sépare du contenu des
fiches. Fichier absent ou vide : le prompt est exactement le même que sans la fonction.

```
Phrases courtes, vocabulaire simple.
Un détail sensoriel par réplique, jamais plus.
Humour sec, sans exclamations.
```

- Décrire **la manière d'écrire** (voix, rythme, vocabulaire, ton), jamais des événements : le texte
  est lu à chaque tour par tous les personnages, il n'est pas protégé contre les spoilers.
- Formuler en positif (« phrases courtes ») plutôt qu'en interdits absolus (« jamais de longues phrases »),
  qui font boucler un petit modèle. Court : au-delà de 1 500 caractères, un avertissement apparaît au chargement et dans
  `validation-scenarios.bat` (pas dans 🐞 : avec des lignes `{N%}`, la taille du fichier n'est pas ce que le
  modèle reçoit).
- Une consigne de longueur (« réponses très longues ») entre en conflit avec les règles de longueur du
  prompt et avec l'interjection (une ligne courte) : ne pas s'en servir pour la longueur.
- Même langue que le scénario. Aucune commande entre accolades dans ce fichier (sauf la chance `{N%}`
  ci-dessous) : elle serait envoyée telle quelle au modèle, le chargement la refuse.
- **Ligne rare : `{N%}`.** Un petit modèle ne sait pas faire « de temps en temps » : il le fait toujours ou
  jamais. Mettre `{N%}` n'importe où dans une ligne la rend conditionnelle : à **chaque réponse de PNJ**, un
  tirage aléatoire garde la ligne entière (marqueur retiré, tiret conservé) avec N % de chances, sinon elle
  disparaît du prompt, tiret compris. Les lignes sans marqueur sont toujours données.

  ```
  Phrases courtes, vocabulaire simple.
  - {20%} Mets en valeur la tenue impeccable d'une des filles, en une courte phrase.
  - {10%} Glisse un détail sensoriel (odeur, lumière, bruit de la rue).
  ```

  N entier de 1 à 100, un seul marqueur par ligne, avec un texte à garder ou écarter. Le tirage est
  indépendant à chaque réponse (donc, dans un tour à plusieurs PNJ, plusieurs peuvent tirer la ligne) et
  non reproductible : un retry ou un retour en arrière peut tirer autrement. Un marqueur mal formé
  (`{20}`, `{0%}`, `{150%}`, deux sur une ligne) empêche le chargement.
- Relu à chaque chargement : ⟳ prend en compte une modification en cours de partie. Reçu par les
  personnages qui jouent (dont une interjection ou un `DO:`), pas par le résumé ni par les analyses 🔍 et 🧠.
  Le bouton 🐞 l'affiche (« Conseil stylistique »). Si le style contient des lignes `{N%}`, 🐞 montre,
  une fois qu'une réponse a été générée, le style tel que le dernier PNJ l'a réellement reçu après tirage
  (vide si toutes les lignes ont été écartées), à la place du fichier, pour voir quelles lignes rares sont
  passées. Avant la première réponse, il montre le fichier.

### 2.11 Qui parle dans une scène : `{npc-active|present|passive|absent noms}` et `{npc-reset}`

Fixent le **niveau d'activité** des personnages (§4.1) pour l'acte où la commande est écrite et tous ceux
qui sont dessous. Chaque commande tient sur **une ligne seule** ; les noms suivent la commande, séparés
par des espaces (un nom de plusieurs mots est reconnu, « Céleste Dubois »).

```
{npc-active Elena}
{npc-passive Marcus Clara}
{npc-absent}
```

| Commande | Effet |
|---|---|
| `{npc-active noms}` | parlent à chaque échange |
| `{npc-present noms}` | répondent quand ils sont nommés (niveau par défaut) |
| `{npc-passive noms}` | ne réagissent que brièvement, de temps en temps |
| `{npc-absent noms}` | ne sont pas dans la scène : ne parlent jamais. Attention : à son retour, un PNJ lit tout l'historique, y compris ce qui s'est dit en son absence (il n'est pas « tenu dans l'ignorance ») |
| `{npc-active *}` (idem `present`, `passive`, `absent`) | **tous** les PNJ d'un coup, comme si chacun était nommé : niveau **explicite** |
| `{npc-reset}` | tout le monde repasse en present, explicite |

**`*`** (étoile, jamais un nom de personnage) : `{npc-active *}` remet tous les PNJ en actif, y compris ceux
qu'un titre parent avait rendus absents ou passifs. C'est un nommage **explicite** : une commande sans nom
écrite ensuite ne les touche pas (contrairement à `{npc-active}` seul, qui ne touche que les PNJ
implicites). Un nom écrit dans le même titre est plus précis et l'emporte : `{npc-active *}` +
`{npc-passive Marcus}` = tous actifs sauf Marcus passif. `*` doit être seul dans sa commande (pas
`{npc-active * Elena}`) et ne peut apparaître qu'une fois par titre.

**Commande sans nom** : elle s'applique à **tous les PNJ dont le niveau n'est pas encore explicite**.
Avec J1 à J4, `{npc-active J1}`, `{npc-passive J3}` puis `{npc-absent}` laisse J1 actif, J3 passif, et
met J2 et J4 absents.

**Héritage** : chaque PNJ a un niveau et un statut, *explicite* (nommé par une commande) ou *implicite*
(par défaut, ou touché par une commande sans nom). On parcourt les titres de la racine à la feuille :
1. une commande **avec** noms fixe le niveau et le rend explicite ; un titre plus profond peut le changer ;
2. une commande **sans** nom ne touche que les PNJ implicites, qui le restent ;
3. dans un même titre, l'ordre d'écriture ne compte pas (les noms d'abord, le sans-nom ensuite) ;
4. un titre sans commande hérite de son parent ; un acte sans aucune commande sur son chemin laisse tout
   le monde present, implicite.

Exemple :

```
# Scène 1
{npc-active Elena}
{npc-absent}

## Sous-scène
{npc-passive Marcus}
{npc-active}
```

Scène 1 : Elena active (explicite), les autres absents (implicites). Sous-scène : Marcus passif
(explicite), la commande sans nom active Clara et tout autre PNJ implicite, Elena reste active.

- Un nom inconnu, un PNJ nommé deux fois dans le même titre, deux commandes sans nom dans le même titre ou
  une scène où tout le monde est absent empêchent le chargement.
- Interdit dans la prémisse (aucun acte auquel l'attacher). La ligne est retirée du texte de l'acte.
- Le niveau se recalcule depuis l'acte : retour arrière, sauvegarde et ⟳ donnent toujours celui de l'acte.
  Les clics du joueur (§4.1) s'ajoutent par-dessus.
- Un PNJ **passive** qui est nommé ne répond pas pour autant : c'est ce qui le distingue de **present**.
  Pour qu'un personnage réponde dès qu'on l'appelle, le laisser present (ou active).

### 2.12 Modes d'un PNJ : `{npc-mode do on|off nom|*}`

Un **mode** est un réglage qui se superpose au niveau d'activité (§2.11) : un PNJ est actif, présent, passif
ou absent, et peut en plus être dans un ou plusieurs modes. Écrit sur **une ligne seule**, dans un acte (jamais
dans la prémisse), avec un nom de PNJ ou `*` pour tous. Le seul mode aujourd'hui est `do` ; d'autres pourront
s'ajouter sous la même forme `{npc-mode mode on|off nom|*}` (un mode inconnu empêche le chargement).

```
{npc-mode do on Elena}
{npc-mode do off *}
```

**Mode DO : le PNJ tient la plume du narrateur.** C'est l'équivalent du `DO:` du joueur (§4.9), décidé par le
scénario et valable **pendant** un moment, au lieu d'un seul message. Le joueur continue à taper normalement ;
mais le PNJ en mode DO peut avancer la scène, narrer, agir pour les autres personnages et **forcer le
personnage du joueur dans une direction** (il est drogué, hypnotisé, sous contrôle, possédé…). Hors de ce mode,
un PNJ ne parle jamais à la place du personnage du joueur.

- **Héritage** : comme les niveaux d'activité. `on` vaut pour l'acte où il est écrit et tous ses sous-actes ;
  un titre plus profond ou une ligne plus bas peut l'annuler avec `off`. Retour arrière, sauvegarde et
  rechargement donnent toujours l'état de l'acte. Dans un même titre, les lignes sont rejouées dans l'ordre.
- **À l'écran** : une petite étoile ★ s'affiche sur la vignette du PNJ en mode DO.
- **Pas de nom dans le message du joueur** : le mode ne change pas qui répond (§4.2), seulement ce que le PNJ
  qui répond a le droit de faire.
- **Ouverture** : un `{npc-starts Nom}` (§4.5) d'un PNJ en mode DO devient une ouverture avec la plume,
  comme `{npc-do Nom}`. Un `{npc-mode do off ...}` retire seulement cet effet : un `{npc-do}` explicite
  reste toujours une ouverture avec la plume.
- ⚠️ **Collision de plusieurs PNJ** : avec `{npc-mode do on *}` ou plusieurs PNJ en mode DO dans la même scène,
  plusieurs personnages tiennent la plume en même temps et peuvent se doubler, par exemple en narrant tous
  les mêmes actions des autres. Les PNJ répondent l'un après l'autre et le second lit la réponse du premier,
  ce qui limite le problème sans le supprimer. Préférer **un seul PNJ** en mode DO à la fois.
- Technique : le moteur présente au PNJ en mode DO le dernier message du joueur avec le préfixe `DO: ` (une
  ligne `OOC:` ou déjà `DO:` n'est pas modifiée ; rien ne change pour le bouton Continue). Aucun prompt n'est
  modifié : la règle `DO:` du §4.9 s'applique telle quelle.

## 3. Fiche personnage (`characters/nom.txt`)

```
# Elena

Elena, la trentaine, tient l'auberge... (fiche publique)

# SECRET
Elena a autrefois volé dans un temple... (fiche secrète)
```

| Élément | Règle |
|---|---|
| Première ligne `# Nom` | Obligatoire. Devient le nom affiché ; reste dans la fiche publique. Sans elle, le fichier est ignoré (§1). |
| Ligne `# SECRET` | Optionnelle, seule sur sa ligne, insensible à la casse. Sépare fiche publique et fiche secrète. Absente : tout est public. |
| Qui lit quoi | Le PNJ qui parle reçoit sa fiche complète. Les autres PNJ, présents ou absents, reçoivent la fiche **publique** ; un `# SECRET` ne va jamais à un autre PNJ (§2.7). |
| Statut de la fiche | Le modèle la reçoit comme une note privée de l'auteur : il ne doit pas la citer. |
| Section `## tag` | Optionnelle. Du texte qui n'entre dans la fiche que lorsqu'un `{npc-story on nom tag}` l'active (§2.8 bis). Le **premier mot** de la ligne est le tag, le reste est un commentaire pour l'auteur, jamais envoyé au modèle. La section va jusqu'au prochain `##` ou jusqu'à `# SECRET`. |
| Visibilité d'un tag | Donnée par sa **place** : un `##` **avant** `# SECRET` est **public** une fois actif (les autres PNJ le lisent aussi), un `##` **après** est **secret** (son personnage seul). Il ne change jamais de camp. |
| Texte hors tag | Permanent : le texte sous `# Nom` (public) et sous `# SECRET` (secret) est toujours en vigueur, comme avant. Les `# xxx` simples restent du texte ordinaire. |

Les fiches publiques sont lues par les autres PNJ comme des coéquipiers déjà établis. Des secrets qui se
répondent entre eux (aspirations, envies, non-dits) donnent du relief sans casser l'histoire.

Avec des tags (§2.8 bis) :

```
# Léa
Léa, vingt-six ans, réservée.             ← permanent, public

## froide  distante au début               ← tag PUBLIC : le premier mot est le tag
Avec Marc, elle reste distante.

# SECRET
Léa déteste se tromper sur les gens.      ← permanent, secret

## chatons  ce qu'elle a vu dans le bois    ← tag SECRET (après # SECRET)
Elle l'a vu nourrir des chatons : elle le trouve adorable.
```

**Attention à la place d'un `##`** : déplacer une section de l'autre côté de `# SECRET` change sa
visibilité sans aucun message. Un secret écrit au-dessus de `# SECRET` serait lu par tous les autres PNJ une
fois actif. Le texte de base (hors tag) s'écrit **avant** le premier `##` de sa zone.

**Rôle de la fiche : qui est le personnage, pas ce qui lui arrive.** La fiche est envoyée en entier dès
le tour 1, à chaque tour, sans aucun filtre par acte. Tout ce qu'elle contient est donc su du PNJ
immédiatement (et, pour la partie publique, de tous les autres PNJ). L'histoire, elle, appartient aux
actes, qui n'arrivent au modèle qu'un par un (§2.7).

| Dans la fiche | Dans un acte (jamais dans la fiche) |
|---|---|
| Identité, âge, rôle, apparence utile | Événements à venir et rebondissements |
| Voix et manières : débit, tics, ce qu'il dit ou ne dit jamais | Conditions de transition `[NEXT ACT]` (inopérantes dans une fiche : seul le texte de l'acte les déclenche) |
| Comportements typiques, y compris « quand rien ne se passe » | Réactions précises à un événement précis de l'intrigue |
| Buts et valeurs de départ | Ce que le joueur fait ou ressent |
| Histoire passée connue du PNJ dès le tour 1 (`# SECRET` si les autres l'ignorent) | Lieux, décor et situation de la scène en cours |
| Relations de départ avec les autres PNJ | Ce qui n'est vrai qu'à partir d'un moment de l'histoire |

Test à appliquer à chaque phrase de la fiche :

1. Est-ce vrai et su du PNJ dès le tour 1, quoi que fasse le joueur ? Sinon : dans un acte.
2. Cette fiche servirait-elle telle quelle dans un autre scénario ? Sinon, elle contient du scénario.
3. Cette phrase décrit-elle un trait observable (comment le PNJ agit ou parle) ? Une étiquette (« timide »)
   ressort telle quelle dans les répliques ; la remplacer par un comportement (« baisse les yeux,
   répond en phrases courtes »).

Conséquences d'un scénario écrit dans une fiche : spoil dès le tour 1, fuite vers les autres PNJ si c'est
dans la partie publique, contexte payé à chaque tour par tous, deux sources de vérité qui peuvent se
contredire, fiche non réutilisable.

## 4. Mécanismes du jeu

### 4.1 Niveaux d'activité (vignette de chaque PNJ)

Chaque PNJ a un niveau d'activité, affiché par l'emoji de sa vignette :

| Niveau | Emoji | Comportement |
|---|---|---|
| active | 😎 | parle à **chaque** échange |
| present | 🙂 | répond quand il est nommé, reprend la parole en cas de continuité ; peut être tiré au sort et réagir (poids double d'un passive, §4.3). **Niveau par défaut** |
| passive | 🤨 | ne répond pas parce qu'on le nomme ; peut être tiré au sort et réagir brièvement |
| absent | 😴 | ne parle jamais et n'est pas dans la scène : le prompt des autres le liste comme « pas dans la scène en ce moment » (ils peuvent le mentionner, jamais parler à sa place) |

Le point dans le coin de la pastille dit d'où vient le niveau : **blanc** = fixé par une commande du
scénario qui nomme le PNJ (§2.11), **gris** = par défaut ou fixé par une commande sans nom, **rouge** =
forcé par le joueur. Survoler la vignette donne le détail.

**Cliquer** sur une vignette fait défiler les niveaux forcés : à partir du niveau du scénario, active →
present → passive → absent (dans cet ordre circulaire, sans le niveau de départ), puis le niveau du
scénario **forcé** (un verrou que les commandes ne défont plus), puis un dernier clic retire le
forçage. Un cran qui laisserait la scène vide est sauté. Un niveau forcé s'accroche au **titre parent**
de l'acte où l'on a cliqué : il tient pour tous les actes sous ce titre et disparaît dès que l'histoire en
sort (revenir ensuite ne le ressuscite pas). Sur un titre de premier niveau, il tient toute la partie.

Persisté dans `run/current/state.txt` (§6.5).

### 4.2 Qui répond

Étapes dans l'ordre, tous ceux qui sont retenus répondent :

1. **Les actifs** répondent toujours, dans l'ordre des identifiants.
2. **Mention** : un PNJ active ou present dont le nom apparaît dans le dernier échange répond aussi. Le
   dernier échange est le message du joueur **et** les réponses du tour précédent (J1 parle de J2 : J2
   peut répondre) ; un PNJ ne se « nomme » pas lui-même. Ordre : les noms du message du joueur dans leur
   ordre d'apparition, puis ceux des réponses ; les nommés passent avant les autres actifs. Détection
   insensible aux accents et à la casse ; les 5 premiers caractères normalisés du nom suffisent
   (« Cel » vise « Céleste »).
3. **Continuité** : personne d'actif ni de nommé, et un seul PNJ a parlé au tour précédent : il reprend la
   parole (s'il est encore dans la scène).
4. **Tirage** : sinon, un seul PNJ present ou passive est tiré au hasard, un present deux fois plus
   probable qu'un passive.

**Réponse vide** : un petit modèle peut épuiser son budget en réflexion sans répondre. L'appli détecte la
réponse vide et relance une fois avec le même prompt, puis abandonne.

### 4.3 Interjection (réglage « Chance d'interjection »)

Après l'étape 4.2, chaque PNJ present ou passive non retenu tire sa chance de réagir **brièvement** (une
courte phrase ou un geste, jamais mener la scène). Le réglage est une **chance totale**, partagée entre
tous les candidats : un present pèse **double** d'un passive. Plus il y a de candidats, plus la chance de
chacun baisse, et le nombre de réactions attendu par tour reste proche du réglage. Un PNJ seul ne dépasse
jamais 60 % (au-dessus de 1, le plafond monte en proportion). Un actif parle déjà, et un absent ne parle
jamais. Le nombre de PNJ principaux ne limite pas les interjections. L'interjecteur répond après les
principaux, voit leurs réponses ; plusieurs interjecteurs répondent dans un ordre aléatoire. Réglage
global, défaut 1 (environ une réaction par tour), 0 = aucune, sans réglage par PNJ.

| Candidats (hors actifs et principaux) | Chance de chacun, réglage = 1 |
|---|---|
| 1 present | 60 % (plafond) |
| 2 presents | 50 % chacun |
| 3 presents | 33 % chacun |
| 4 presents | 25 % chacun |
| 5 presents | 20 % chacun |
| 2 presents + 2 passifs | present 33 %, passif 17 % |
| 1 present + 3 passifs | present 40 %, passif 20 % |
| 4 passifs | 25 % chacun |
| 1 passif | 60 % (plafond) |

Pour une scène plus calme, baisser le réglage ou mettre les figurants en `{npc-passive}`.

### 4.3 bis Comprendre une réponse : le bouton ❓

Quand un personnage invente une partie de l'histoire ou une action « hors cadre », le bouton ❓ (à côté
du 🧠) met en regard la **dernière réponse du dernier PNJ** et le prompt exact qu'il a reçu. Il liste
les éléments de la réponse qui ont un passage correspondant (avec le bloc du prompt, une citation et
`[direct]` ou `[indirect]`), ceux qui n'en ont aucun, et une courte lecture.

- Ce sont des **pistes**, pas l'explication du modèle (un modèle ne peut pas dire pourquoi il a écrit
  ce qu'il a écrit). Le plus utile : les éléments **sans passage correspondant** viennent du modèle
  lui-même, ni du scénario ni des règles du jeu. Un élément **indirect** signale souvent une
  extrapolation à partir d'une phrase du scénario trop vague ou trop ouverte.
- Lecture seule : rien n'est écrit dans l'historique ni archivé. Disponible tant que la dernière
  réponse est encore la dernière (plus rien après un ↩, un retour en arrière ou un redémarrage).
- Il reçoit le prompt entier plus la réponse : sur un petit contexte, l'appel peut dépasser la limite.
- Si « Afficher la réflexion » est cochée, la réflexion du modèle est jointe à l'analyse.

### 4.4 Réflexion (case « Afficher la réflexion », icône 🧠)

La réflexion est toujours demandée au modèle ; la case ne règle que son affichage (actif par défaut).
Certains modèles ne produisent pas de réflexion séparée selon leur enregistrement dans Ollama : c'est une
capacité du modèle, pas du scénario.

### 4.5 Progression d'actes

Le PNJ écrit `[NEXT ACT]` dans sa réponse quand la condition de l'acte courant est remplie. Le marqueur
est retiré avant affichage, l'acte suivant devient courant et ses lignes `[...]` s'affichent. Jamais sur
le dernier acte.

Contrôles du joueur : « Précédent » / « Suivant » (corrections manuelles), 🔍 (analyse sans rien écrire
dans l'historique).

**Qui décide d'avancer** : le PNJ qui joue, à chaque tour, dans sa propre réponse, sans autre appel.
Règle ajoutée à son prompt (bloc `CURRENT SCENE`, §2.7) :

| Cas | Règle envoyée |
|---|---|
| Dernier acte | Aucune |
| Le texte de l'acte contient `[NEXT ACT]` (§2.5) | Mode strict : écrire le marqueur seulement quand la condition s'est produite. Fait externe : constaté dans l'histoire. État interne du PNJ : jugé sur le comportement du joueur pendant plusieurs échanges récents. Échange sans rapport en cours : le terminer dans la même réponse, puis écrire le marqueur. Rappel court répété dans les conventions de format. |
| Sinon | Mode par défaut : avancer quand la scène semble conclue |

Dans tous les cas, le marqueur est une note de raisonnement sans influence sur le comportement du
personnage, écrite à la fin de la réponse. Le bouton 🔍 appelle un agent séparé
(`NextActReadinessAnalyst`) : lecture seule, mêmes réglages d'échantillonnage que le jeu, il ne pilote
rien.

#### `{npc-starts}` et `{npc-do}` : faire ouvrir la scène par un PNJ

Par défaut, le joueur parle en premier. Pour qu'un PNJ ouvre, écrire sur **une ligne seule** :

```
{npc-starts}
{npc-starts Elena}
{npc-do}
{npc-do Elena}
```

- `{npc-starts}` : le PNJ ouvre **en mode normal** : il parle le premier, **sans aucun message du joueur** (pas
  de bulle `DO:`), comme avec le bouton Continue. Il joue son personnage seulement.
- `{npc-do}` : le PNJ ouvre **avec la plume du narrateur** : comme un clic sur **DO** sans texte (une bulle `DO:`
  du joueur, puis le PNJ parle). Il peut avancer la scène, narrer et agir pour les autres.
- Un `{npc-starts Nom}` devient une ouverture avec la plume si ce PNJ est en mode DO (`{npc-mode do on …}`, §2.12).
  Sans nom, `{npc-starts}` reste une ouverture normale.

| Sujet | Règle |
|---|---|
| Emplacement | En-tête du scénario (avant le premier acte) : ouvre l'histoire. Dans un acte : ouvre cet acte quand il devient courant (automatiquement ou par « Suivant »). |
| Effet | Le PNJ parle seul, sans interjection. `{npc-starts}` : sans bulle du joueur ; `{npc-do}` : après une bulle `DO:` vide du joueur. |
| Sans nom | Un PNJ présent est tiré au hasard. |
| Avec nom | Id de fichier, nom complet ou un seul mot du nom (`Elena` pour `Elena Voss`), sans tenir compte des accents ni de la casse. Nom inconnu : le scénario refuse de se charger. PNJ nommé mais retiré de la scène : un autre PNJ présent le remplace. |
| Texte | La ligne est retirée : le modèle ne la lit jamais. Une mention au milieu d'une phrase est refusée au chargement (la commande doit être seule sur sa ligne). |
| Joueur plus rapide | Si le joueur écrit avant que le PNJ ait parlé, son message compte : pas d'ouverture. |

### 4.6 Sauvegardes et redémarrage

- Chaque tour est écrit sur disque dès qu'il existe (§6.2) : la partie en cours est toujours à jour.
- 💾 **Sauvegarder** : point de sauvegarde manuel, horodaté, en nombre illimité ; zip complet de la
  partie (§6.7). Charger une sauvegarde remplace la partie en cours (confirmation) ; on peut ensuite
  remonter dans l'historique (§4.8).
- 🔄 **Recommencer** : remet la session à zéro sur le même scénario (confirmation). La partie terminée
  est archivée dans un zip. Le scénario est **validé en premier** : s'il contient une erreur, une
  fenêtre liste chacune (fichier, ligne) et rien n'est touché (la partie en cours reste intacte).
- ⟳ (à côté de « Personnages ») : recharge `scenario*.txt` et `characters/` sans toucher à la
  conversation. Exception : un scénario avec `{pick}` est relu depuis sa copie résolue (§2.6). Une erreur
  de scénario s'affiche aussi dans une fenêtre.
- **Au lancement de `chat.bat`**, la question « Continuer la conversation ? » est posée **avant** le
  chargement du scénario. Répondre « n » archive d'abord la partie (`run/archive/`), donc un `run/` copié
  d'un autre scénario ne peut plus bloquer le démarrage. Si le chargement échoue malgré tout alors qu'une
  partie existe, le programme affiche l'erreur et propose de la remettre à zéro (archivée) avant de réessayer.
  Copier un scénario avec son `run/` reste déconseillé : pour partager ou dupliquer, copier le dossier sans `run/` (§1, §6.9).

### 4.7 Compaction du contexte

Quand la transcription dépasse le budget de tokens, les tours les plus anciens sont résumés
(`ChatSummarizer`) et remplacés par ce résumé dans le prompt. Tous les tours restent sur disque (§6.4). La
compaction a lieu à la fin d'un tour complet (après les réponses de tous les PNJ) et garde toujours les
derniers tours en clair.

### 4.8 Retour en arrière (bouton ↩ sur chaque bulle)

Chaque bulle porte son numéro `#N` (§6.2). Le bouton ramène la partie à l'état exact juste après ce tour.

- **Destructif** : tout ce qui suit est supprimé définitivement. Confirmation explicite, avec
  proposition de sauvegarde préalable.
- **Restauré** : les tours, le résumé tel qu'il était à ce moment (§6.6), l'acte courant.
- **Non restauré** : les niveaux d'activité forcés par vos clics (§4.1), réglages de génération. Les niveaux
  fixés par le scénario suivent l'acte, donc le retour arrière les rétablit.
- Le tour suivant reprend le numéro `N+1`. « Annuler » et « Régénérer » sont des retours au dernier tour
  du joueur suivis d'une nouvelle génération.

### 4.9 Conventions de message du joueur

Injectées dans le prompt de chaque PNJ :

| Écriture | Sens |
|---|---|
| `*texte*` | Action ou détail de scène, jamais une réplique |
| Texte sans astérisques | Parole |
| `(texte)` | Pensée du personnage du joueur. Reste affichée et sauvegardée, mais **supprimée du texte envoyé aux LLM** (PNJ, résumé, analyses) : aucun PNJ ne peut y réagir. Un message fait uniquement de pensées n'est pas transmis. |
| `OOC: …` | Instruction hors-jeu, que personne dans l'histoire n'a dite ni entendue. Le PNJ répond en tant que lui-même et s'arrête là : la scène n'avance pas dans cette réponse. |
| `DO: …` | Le joueur confie la plume du narrateur au PNJ pour une seule réponse : avancer la scène, narrer, agir pour d'autres personnages. Au tour suivant, retour à son seul personnage. |
| `DO:` vide | Le PNJ parle en premier, avec la plume (`{npc-do}`, §4.5) |
| Bouton **▶ Continue** | Aucun message du joueur : les PNJ continuent « comme si le joueur n'avait rien répondu ». Le prompt est le prompt habituel, sans consigne en plus ; sa dernière ligne est simplement la dernière réplique d'un PNJ. Sélection habituelle (actifs, PNJ cités dans les dernières répliques, continuité, tirage), **sans interjection**. Rien n'est ajouté pour le joueur dans l'historique. Refusé tant que personne n'a parlé. Annuler et Régénérer reviennent toujours au dernier vrai message du joueur : un Continue fait partie du tour qui le précède. |

Le message du joueur est présenté au modèle sous le nom du joueur (§2.1). La génération est coupée si le
modèle dérive vers le tour du joueur ou vers une narration à sa place.

## 5. Vérifier un scénario avant de le tester en vrai

Commencer par `validation-scenarios.bat` (§2.9) : il liste toutes les erreurs et avertissements sans
lancer de partie. Pour aller plus loin, charger le scénario par le code et regarder le résultat, pas
juste relire le texte (le piège du §2.3 ne se voit pas à la simple lecture). Un script Java jetable
suffit :

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
      state.txt                                  acte courant, niveaux d'activité forcés
      resolved/                                  scenario*.txt avec les {pick} tirés (§2.6) ; absent sans pick
        picks.txt                                les tirages dans l'ordre (tableau 🎲) ; absent pour un ancien run
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

### 6.5 `state.txt` : acte courant et niveaux forcés

Les seuls réglages de la partie qui ne se déduisent pas des tours :

```
act=2
forced=elena:active@1.2;marcus:absent@
```

- `act` : l'acte en vigueur maintenant. Il peut différer de l'acte écrit sur le dernier tour après un
  « Précédent » manuel (qui ne crée aucun tour) : c'est ce fichier qui le retient.
- `forced` : les niveaux d'activité que le joueur a forcés en cliquant (§4.1), au format
  `id:niveau@ancre`, séparés par `;`. L'id est le nom de fichier de `characters/`, sans `.txt`. L'ancre
  est le numéro du titre parent auquel le forçage s'accroche (`1.2`), vide pour la racine (tient toute
  la partie). Clé absente : rien n'est forcé, chaque PNJ a le niveau que donne le scénario. Les niveaux
  fixés par le scénario (§2.11) ne sont **pas** écrits ici : ils se recalculent depuis l'acte.
- Un fichier d'avant les niveaux (clés `present` / `interjecting`) est encore lu : les PNJ absents de la
  liste `present` sont rouverts forcés absents.

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
`run/current/` (le dossier `turns/`, `state.txt` et, s'il existe, `resolved/` : les tirages `{pick}`, §2.6), nommé d'après l'horodatage (précision à la
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

### 6.10 Ancien format

Avant cette structure, les fichiers de session (`history.md`, `summary.md`, `act.txt`, `present.txt`,
`interject.txt`, `full-history.md`) et les dossiers `saves/`, `archive/`, `logs/`, `tests/` vivaient à
la racine du scénario, à côté des fiches personnage. Si l'appli en détecte, le scénario reste dans la
liste mais refuse de se charger, avec un message « format non pris en charge ». Il n'y a plus de
programme de conversion : déplacer les fiches dans `characters/` et supprimer les anciens fichiers de
session à la main.

## 7. Conseils et avertissements

Cette section regroupe des recommandations issues de l'expérience (pas de la spec : §1 à §6). Elle
est écrite pour être lue seule, y compris par un petit modèle qui rédige un scénario. **À mettre à
jour à chaque nouvelle fonctionnalité qui touche l'auteur et à chaque piège ou limite découvert.**

### 7.0 Principe : des fiches et des événements, pas un script

L'histoire vient de la rencontre entre **des personnages bien définis** (fiches, §3) et **des situations
et événements** (actes, §2). Le PNJ agit selon sa fiche, l'objectif et la situation qu'on lui donne ; le
joueur apporte l'imprévu.

**Le piège : tout rédiger dans le scénario.** Un acte qui dicte répliques, gestes et enchaînements
transforme le chat en récitation : le PNJ cherche à jouer le texte plutôt qu'à réagir au joueur, et la
condition `[NEXT ACT]` devient l'unique enjeu de la scène.

| À écrire dans un acte | À éviter dans un acte |
|---|---|
| La situation (lieu, moment, ce qui vient de se passer) | Des répliques toutes faites |
| Un enjeu ou un objectif (ce que les PNJ veulent, ce qui presse) | Un déroulé minute par minute |
| Un obstacle ou un événement déclencheur | Ce que le joueur va faire ou ressentir |
| Une seule condition de sortie observable (§7.2) | L'état émotionnel qu'un PNJ doit atteindre sans que le joueur y contribue |

Pour la fiche : un but, un trait de caractère, une façon de parler, ce que le PNJ fait quand rien ne se
passe. Tout cela se joue sans script.

**Limites de ce principe.** Il repose sur l'expérience du projet, pas sur une mesure. Trop peu
d'information produit aussi un échec : un PNJ sans but ni pression attend le joueur et la scène stagne.
Chaque acte doit donc donner au moins une raison d'agir (un objectif, une urgence ou un événement), sans
dire comment.

### 7.1 Budget de contexte et anti-spoil

Conséquences de §2.7 :

1. La prémisse est envoyée à chaque tour à tous les PNJ : la garder courte, **sans spoil** (monde,
   ton, situation de départ). Le contexte d'accueil du joueur (qui, où, pourquoi) va dans les `[...]` du
   premier acte (§2.4), pas dans la prémisse : il est affiché au joueur et ne pèse plus ensuite que
   dans l'historique.
2. Un rebondissement s'écrit dans l'acte où il devient utile, pas avant.
3. Ce qu'un seul PNJ sait depuis le début va dans son `# SECRET` ; ce qui n'est vrai qu'à partir d'un
   moment de l'histoire va dans l'acte.
4. Le texte commun d'un chapitre parent est renvoyé à chaque tour de chaque scène : le garder court.
   Ce qui ne sert que dans une scène va dans cette scène.
   **Piège de la synthèse** : sous un titre parent qui contient des sous-parties (par exemple un `#` suivi
   de `##`, ou un `##` suivi de `###`), ne pas écrire un résumé des événements à venir du type « ce qui
   va se passer = d'abord A, puis B, enfin C ». Ce texte est lu à chaque tour de **chaque** sous-partie par
   **tous** les PNJ : le modèle *sait* déjà B et C pendant la sous-partie A, va droit au dénouement et saute
   des étapes au lieu de dérouler la scène en cours. Le prompt lui demande bien d'ignorer ce qui n'est pas
   encore arrivé, mais il ne comprend pas qu'il n'a pas le droit d'utiliser une information qu'il a sous
   les yeux : la seule parade fiable est de ne pas la lui montrer avant la sous-partie où elle se joue. Sous le titre parent, ne mettre que ce qui est vrai
   dans toutes les sous-parties (lieu, ton, personnages) ; l'enchaînement des événements se lit dans
   l'ordre des sous-parties, un événement chacune.
   **Un plan de l'histoire n'est pas une synthèse.** Quand les personnages se donnent réellement un plan
   (« on passe devant les orcs, puis… »), on peut l'écrire, mais **comme un plan** : « les personnages
   mettent en place le plan suivant : … », jamais comme un fait à venir. Un plan peut être partagé avec le
   joueur, il peut y en avoir plusieurs à la suite, et l'action peut différer ou échouer selon ce qui se
   passe : la formulation doit le laisser ouvert. (Hypothèse d'écriture à vérifier selon le modèle : un
   petit modèle peut quand même suivre le plan à la lettre.) Un plan connu d'un seul PNJ peut aussi aller
   dans son `# SECRET` ou un `{npc-delta secret nom : …}` (§2.8), mais ce n'est pas obligatoire.
5. Les lignes `[...]` d'un acte sont connues de tous les PNJ dès l'arrivée de l'acte.
6. Le PNJ ne voit pas l'acte suivant : une condition ne peut s'appuyer que sur le texte de l'acte
   courant.

### 7.2 Réussir ses `Quand écrire [NEXT ACT]`

Le PNJ qui joue décide seul (§4.5), avec l'acte courant, le résumé et les derniers tours. Deux pannes
opposées :

| Panne | Cause fréquente |
|---|---|
| Scénario bloqué | Condition trop stricte, jamais atteignable dans l'acte, ou impossible à constater dans la conversation |
| Passage trop rapide à l'acte suivant | Condition vague, ou semblable à un événement déjà arrivé (visible dans le résumé) |

Règles :

1. Un seul fait observable et nommé (personnage, lieu, objet, décision dite à voix haute). Jamais
   « quand la scène est finie ».
2. Formuler « dès que… », jamais « jamais avant… » (une négation absolue fait boucler un petit modèle).
3. Une seule condition par acte, courte, sans cascade de « et / ou ». Au plus deux faits si une sortie
   de secours est nécessaire (« Varek mort ou en fuite, ou le groupe a quitté la clairière »).
4. `Ce qui doit se passer` doit mener à la condition, sinon l'histoire reste bloquée.
5. Pas de condition vraie dès le début de l'acte.
6. État interne d'un PNJ : formuler ce qu'il doit avoir dit ou décidé (« Dès qu'Elena a avoué le vol
   à voix haute »), pas une émotion vague.
7. Écrire la condition dans la feuille, pas dans un parent (§2.3).

**Scène qui boucle alors que la condition est remplie.** Le contrôle de disponibilité (bouton d'analyse)
est un diagnostic manuel : son verdict n'est pas renvoyé au PNJ qui joue. Pour limiter les boucles, le
prompt lui demande d'envisager `[NEXT ACT]` quand l'action devient répétitive et que la condition est
proche. Cela ne remplace pas une condition claire : si la scène boucle, vérifier d'abord la condition
(règles 1 à 6).

**Séquences qui se ressemblent** (plusieurs combats, négociations, enquêtes). Le résumé dit « le combat
contre les bandits s'est terminé » ; à l'acte suivant, « le combat est terminé » paraît déjà vrai.
Parades, à combiner :

- Un marqueur unique par scène (adversaire nommé, lieu, enjeu), cité dans la condition.
- Un fait d'ancrage dans `Point de départ` : une situation nouvelle, distincte de la précédente.
- Une condition qui nomme cette scène, jamais le genre d'événement.

```
# Le guet-apens du col
Point de départ : Des bandits surgissent sur le sentier du col de Vaur.
Quand écrire [NEXT ACT] : Dès que Varek, le chef des bandits, est mort ou a fui, et qu'aucun bandit du col n'est encore debout.

# Les loups de la forêt de Saule
Point de départ : La nuit tombe dans la forêt de Saule et une meute de loups encercle le camp.
Quand écrire [NEXT ACT] : Dès que la meute de loups est morte ou a fui dans les bois.
```

Mauvais pour ces deux actes : « Dès que le combat est terminé ». Cette phrase serait vraie dès le
premier tour du second acte.

Diagnostic en jeu : 🔍 (§4.5), ❓ origine de la dernière réponse (§4.3 bis), 🎲 tirages `{pick}` et 🐞 texte de scénario envoyé (§2.6), boutons Précédent / Suivant.

### 7.3 Écrire un long scénario avec un petit contexte

1. **Tableau d'actes dans un fichier temporaire** hors du dossier du scénario (l'appli ne le lit
   jamais), supprimé à la fin. Une ligne par acte :

   | N° | Lieu / adversaire | Point de départ (fait d'ancrage) | Ce qui doit se passer | Condition `[NEXT ACT]` (fait unique, nommé) | Distinct de l'acte précédent par |
   |---|---|---|---|---|---|
   | 1 | Col de Vaur / Varek | Des bandits surgissent sur le sentier | Combat, Varek cède | Varek mort ou en fuite | Embuscade, de jour |
   | 2 | Forêt de Saule / loups | Nuit, la meute encercle le camp | Défense du camp | La meute est morte ou en fuite | Animaux, de nuit |

   Une dernière colonne impossible à remplir = deux actes trop semblables : les fusionner ou les
   différencier.
2. **Plusieurs fichiers** : `scenario.txt` (ligne Joueur, `#SCENARIO`, premiers actes), puis
   `scenario-1.txt`, `scenario-2.txt`… (§1). Un acte par fichier si le contexte est très serré.
   Chaque fichier commence par un titre `# ...` de premier niveau. Actes indépendants : un acte par
   fichier. Scènes qui partagent un texte commun : un chapitre `#` et ses `##` dans le même fichier
   (§2.3).
3. **Écrire un fichier à la fois**, avec en contexte : sa ligne du tableau, la ligne de l'acte
   précédent, les fiches personnage. Ne pas relire les fichiers déjà écrits. Écrire d'abord les
   fiches (test du §3 sur chaque phrase), puis les actes.
4. **À rappeler dans chaque fichier** : 3e personne dans les `[...]` et le texte d'acte ; une ligne
   `Quand écrire [NEXT ACT]` sur tous les actes sauf le dernier ; premier acte qui nomme tous les PNJ.
5. **Vérifier** avec le script du §5 : nombre d'actes = nombre de lignes du tableau. Puis jouer et
   utiliser 🔍.

### 7.4 Pièges connus

| Piège | Symptôme | Parade côté auteur |
|---|---|---|
| Fuite de la fiche | « Puisque je suis timide… » dans une réplique | Décrire des comportements observables, pas des étiquettes (§3) |
| Scénario dans la fiche | Le PNJ anticipe un événement ou le révèle trop tôt ; un autre PNJ en parle | Test du §3 : seul ce qui est vrai et su dès le tour 1 va dans la fiche ; le reste dans l'acte |
| Langue hésitante | Le modèle passe sa réflexion à choisir entre français et anglais | Écrire `Langue :` dans l'en-tête (§2.1) |
| Mirroring | Le PNJ décrit les sensations ou actions du joueur | Ne pas écrire de pensées ou d'actions du joueur dans le scénario |
| Spoil | Le PNJ anticipe un événement scripté | Écrire l'événement dans l'acte où il arrive (§7.1) |
| Synthèse sous un titre parent | Le PNJ sait déjà la suite : il anticipe, saute des étapes et va droit au dénouement | Ne pas écrire « ce qui va se passer = … » sous un titre qui a des sous-parties : son texte est relu à chaque tour de chacune. Un vrai plan des personnages s'écrit comme un plan (« les personnages mettent en place le plan : … »), qui peut échouer (§7.1) |
| PNJ jamais présenté | Un PNJ tiré au hasard réfléchit longtemps et répond vide | Nommer tous les PNJ dans le premier acte (§2.4) |
| Négation absolue | Boucle de réflexion, réponse vide | « Dès que… » (§7.2) |
| Émotion jouée comme preuve | Le PNJ sur-joue sa colère pour déclencher l'acte | Ancrer la condition sur le comportement du joueur |
| Acte sauté | `[NEXT ACT]` dès le premier tour | Marqueur unique, fait d'ancrage (§7.2) |
| Commande citée en exemple | Chargement refusé : `{npc-starts}` « doit être seul sur sa ligne », ou `{pick …}` mal formé | Une commande écrite entre accolades est toujours exécutée ou validée, même dans une note : décrire la commande avec des mots (`npc-starts`) sans les accolades (§2.9) |
| Style qui parle d'histoire | Le style annonce ou impose des événements, tous les PNJ le lisent à chaque tour | `style.txt` ne décrit que la manière d'écrire (§2.10) ; les événements vont dans les actes |
| « De temps en temps » appliqué à chaque réponse | « Mets en valeur la tenue des filles de temps en temps » : le modèle le fait à chaque réplique | Un petit modèle n'a pas de hasard interne et lit toute consigne présente dans son prompt. Pour la rareté, calculer la probabilité **avant** de construire le prompt : `{N%}` dans une ligne de `style.txt` (§2.10) ; la ligne n'est donnée que N % du temps. L'idée vaut pour toute consigne à fréquence voulue |
| Secret dans le texte de l'acte | Un autre PNJ réagit à une information privée | Écrire l'information privée avec `{npc-delta secret nom : …}` (§2.8), jamais dans le corps de l'acte, lu par tous |
| Échange coupé | `[NEXT ACT]` en plein dialogue | Condition qui laisse de la marge ; un filet existe dans le prompt |
| Saut de scène | Le `[...]` du nouvel acte ne raccorde pas à la scène précédente | Un `[...]` n'est pas généré par le modèle : l'écrire en lien avec l'état précédent |
| Tirade d'interjection | Un PNJ secondaire fait un discours | Fiche à la voix courte ; un filet existe dans le prompt |
| Longueur uniforme | Toutes les réponses font la même taille | Un exemple de réplique courte et un de longue dans la fiche |
| Actes fusionnés | Un seul acte au lieu de deux | Même niveau `#` / `#` (§2.3) |
| Titres non détectés | Aucun acte, tout en prémisse | Pas de `#` parasite en début de ligne ; fichier en UTF-8 |
| Plusieurs `[...]` sans actes | Seul le premier est affiché | Faire de vrais actes (§2.4) |
| Fiche ignorée | Un PNJ absent de la liste | Première ligne `# Nom` obligatoire (§1, §3) |
| Modification sans effet | Scénario avec `{pick}` : rien ne change | « Recommencer » (§2.6) |
| `[NEXT ACT]` entre crochets seul | Pris pour une réplique à afficher | Le garder dans la phrase `Quand écrire [NEXT ACT] : …` |
| Réponse vide du modèle | Une seule relance puis abandon | Souvent une ambiguïté de mise en scène à corriger (§4.2) |
