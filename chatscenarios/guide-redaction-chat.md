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
  picks/              optionnel — paragraphes tirés par {pick … nom} (§2.6)
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
| `{npc-starts}` | Pris en compte à la première entrée dans le titre qui le porte ; la première commande rencontrée, de la racine à la feuille, décide (§4.5). |
| `{delta-pnj …}` | Appliqué dès l'entrée dans le titre qui le porte, puis pour tous les actes suivants (§2.8). |
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
- Pas de tirage imbriqué (une commande dans un paragraphe tiré n'est pas résolue). Les fiches personnage ne
  sont pas concernées.
- Fichier `picks/nom.txt` absent ou sans paragraphe : échec du chargement, avec le nom du fichier.

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

### 2.8 Faire évoluer un personnage : `{delta-pnj public|secret nom : texte}`

Ajoute une ligne à la fiche d'un personnage **à partir de l'acte où la commande est écrite**, pour tous
les actes suivants. La commande tient sur **une ligne seule** ; la visibilité est obligatoire (deuxième
mot), le nom du personnage vient en troisième.

```
{delta-pnj public Elena : Elle a perdu un œil dans l'embuscade.}
{delta-pnj secret Marcus : Il a reconnu le chef des bandits et décide de le suivre.}
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

### 2.9 Validation du scénario

À chaque chargement d'un scénario, ses fichiers sont vérifiés et **toutes** les erreurs sont listées avec
leur fichier et leur ligne ; une erreur empêche le chargement.

| Erreur | Exemple |
|---|---|
| Ancienne syntaxe | `[PICK: x]`, `[NPC STARTS]` (à réécrire avec les accolades) |
| Commande inconnue ou mal écrite | `{teleport}`, `{pick lieu}` (type manquant), `{delta-pnj Elena : …}` (visibilité manquante) |
| Commande qui doit être seule sur sa ligne | `{npc-starts}` ou `{delta-pnj …}` au milieu d'une phrase |
| Fichier ou paragraphes manquants | `picks/x.txt` absent, `{pick 9 x}` pour 3 paragraphes, trop de `unique` |
| Personnage inconnu | `{npc-starts Zorglub}`, `{delta-pnj secret Zorglub : …}` |

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
  qui font boucler un petit modèle. Court : au-delà de 1 200 caractères, un avertissement apparaît.
- Une consigne de longueur (« réponses très longues ») entre en conflit avec les règles de longueur du
  prompt et avec l'interjection (une ligne courte) : ne pas s'en servir pour la longueur.
- Même langue que le scénario. Aucune commande entre accolades dans ce fichier : elle serait envoyée telle
  quelle au modèle, le chargement la refuse.
- Relu à chaque chargement : ⟳ prend en compte une modification en cours de partie. Reçu par les
  personnages qui jouent (dont une interjection ou un `DO:`), pas par le résumé ni par les analyses 🔍 et 🧠.
  Le bouton 🐞 l'affiche (« Conseil stylistique »).

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

Les fiches publiques sont lues par les autres PNJ comme des coéquipiers déjà établis. Des secrets qui se
répondent entre eux (aspirations, envies, non-dits) donnent du relief sans casser l'histoire.

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

### 4.1 Présence (vignette 🔊/🔇)

Chaque PNJ est présent ou absent de la scène. Tous sont présents par défaut. Le dernier PNJ présent ne
peut pas être rendu absent. Persisté dans `run/current/state.txt` (§6.5).

### 4.2 Qui répond

Trois étapes, la première qui s'applique décide :

1. **Mention** : le message du joueur nomme un ou plusieurs PNJ présents. Tous répondent, dans l'ordre
   d'apparition de leur nom dans le message. Détection insensible aux accents et à la casse ; les 5
   premiers caractères normalisés du nom suffisent (« Cel » vise « Céleste »).
2. **Continuité** : personne n'est nommé et un seul PNJ a parlé au tour précédent : il reprend la
   parole (s'il est encore présent).
3. **Repli** : un seul PNJ est tiré au hasard parmi ceux dont l'icône 💬 est activée. Si aucun ne l'est,
   tirage parmi tous les présents.

**Réponse vide** : un petit modèle peut épuiser son budget en réflexion sans répondre. L'appli détecte la
réponse vide et relance une fois avec le même prompt, puis abandonne.

### 4.3 Interjection (icône 💬, réglage « Chance d'interjection »)

Après l'étape 4.2, chaque PNJ présent, éligible (💬 activée, active par défaut, réglable par PNJ) et non
choisi comme principal tire sa chance de réagir. Le nombre de PNJ principaux ne limite pas les
interjections. L'interjecteur répond après les principaux, voit leurs réponses ; plusieurs interjecteurs
répondent dans un ordre aléatoire. Réglage global 0 à 1, défaut 0.5, sans réglage par PNJ. Le prompt de
l'interjecteur lui demande une réaction courte.

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

#### `{npc-starts}` : faire ouvrir la scène par un PNJ

Par défaut, le joueur parle en premier. Pour qu'un PNJ ouvre, écrire sur **une ligne seule** :

```
{npc-starts}
{npc-starts Elena}
```

| Sujet | Règle |
|---|---|
| Emplacement | En-tête du scénario (avant le premier acte) : ouvre l'histoire. Dans un acte : ouvre cet acte quand il devient courant (automatiquement ou par « Suivant »). |
| Effet | Comme un clic sur **DO** sans texte : une bulle `DO:` du joueur, puis le PNJ parle seul (pas d'interjection). |
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
  est archivée dans un zip.
- ⟳ (à côté de « Personnages ») : recharge `scenario*.txt` et `characters/` sans toucher à la
  conversation. Exception : un scénario avec `{pick}` est relu depuis sa copie résolue (§2.6).

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
- **Non restauré** : présence 🔊/🔇, interjection 💬, réglages de génération.
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
| `DO:` vide | Le PNJ parle en premier (`{npc-starts}`, §4.5) |

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
      state.txt                                  présence et interjection des PNJ
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
   ton, situation de départ).
2. Un rebondissement s'écrit dans l'acte où il devient utile, pas avant.
3. Ce qu'un seul PNJ sait depuis le début va dans son `# SECRET` ; ce qui n'est vrai qu'à partir d'un
   moment de l'histoire va dans l'acte.
4. Le texte commun d'un chapitre parent est renvoyé à chaque tour de chaque scène : le garder court.
   Ce qui ne sert que dans une scène va dans cette scène.
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
| PNJ jamais présenté | Un PNJ tiré au hasard réfléchit longtemps et répond vide | Nommer tous les PNJ dans le premier acte (§2.4) |
| Négation absolue | Boucle de réflexion, réponse vide | « Dès que… » (§7.2) |
| Émotion jouée comme preuve | Le PNJ sur-joue sa colère pour déclencher l'acte | Ancrer la condition sur le comportement du joueur |
| Acte sauté | `[NEXT ACT]` dès le premier tour | Marqueur unique, fait d'ancrage (§7.2) |
| Commande citée en exemple | Chargement refusé : `{npc-starts}` « doit être seul sur sa ligne », ou `{pick …}` mal formé | Une commande écrite entre accolades est toujours exécutée ou validée, même dans une note : décrire la commande avec des mots (`npc-starts`) sans les accolades (§2.9) |
| Style qui parle d'histoire | Le style annonce ou impose des événements, tous les PNJ le lisent à chaque tour | `style.txt` ne décrit que la manière d'écrire (§2.10) ; les événements vont dans les actes |
| Secret dans le texte de l'acte | Un autre PNJ réagit à une information privée | Écrire l'information privée avec `{delta-pnj secret nom : …}` (§2.8), jamais dans le corps de l'acte, lu par tous |
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
