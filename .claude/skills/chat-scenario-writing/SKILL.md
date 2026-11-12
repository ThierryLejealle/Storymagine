---
name: chat-scenario-writing
description: Écrire ou modifier un scénario du module chat (chatscenarios/) — mono ou multi-personnages, fiches personnage, scenario.txt, actes et mécanisme [NEXT ACT]. À utiliser dès qu'on crée ou édite un scénario de chat.
---

# Écrire un scénario de chat

Référence complète : `chatscenarios/guide-redaction-chat.md`. Spec technique : `specs/chat.md`. Ce guide est le
raccourci pratique. Les textes du scénario sont en français ; le code et la doc en anglais/français
selon le fichier.

## 1. Les fichiers

Un exemple minimal par mécanisme (pick, repeat, actes, PNJ, secret, activité, style…) : `chatscenarios/demos/` (`chat.bat`, choix 3). À compléter à chaque nouveau mécanisme.

Pour démarrer un scénario : `init-chat.bat` crée le squelette (`scenario.txt` et une fiche par PNJ),
chaque champ est une consigne entre parenthèses à remplacer.

```
chatscenarios/mon-scenario/
  scenario.txt       obligatoire
  scenario-1.txt     optionnel, suite des actes (scénario long) ; numéro décimal : 1, 1.1, 2, 3.1.2…
  characters/
    elena.txt        un fichier par PNJ, nom simple sans espace ni accent
  picks/
    lieu.txt         optionnel, paragraphes séparés par `---` pour {pick random lieu} ou {repeat order lieu} (voir plus bas)
```
Les fiches personnage vont dans `characters/` (structure cible, voir `chatscenarios/guide-redaction-chat.md` §1).
Ne jamais créer ni éditer à la main le dossier `run/` (partie en cours, sauvegardes, archives, logs :
voir `chatscenarios/guide-redaction-chat.md` §6). Pour partager un scénario, copier le dossier sans `run/`.
Dans `characters/`, tout `.txt` dont la première ligne n'est pas `# Nom` est ignoré (la validation le signale).

## 2. Mono ou multi-personnage

- **Mono** : un seul fichier PNJ. Rien d'autre à faire.
- **Multi** : un fichier par PNJ. Tous sont présents dès le tour 1. Donc :
  - **nommer tous les PNJ dans les `[...]` du tout premier acte**, même discrets, sinon un PNJ
    tiré au hasard ne sait pas s'il est dans la pièce et peut boucler ;
  - **donner le contexte de départ du joueur** (qui il est, où, pourquoi, avec qui) dans ces mêmes `[...]`,
    jamais dans la prémisse : la prémisse est relue à chaque tour jusqu'à la fin, le premier acte seulement
    tant qu'il est courant (guide §2.4, §7.1) ;
  - donner à chacun une fiche publique distincte et un `# SECRET` qui répond à ceux des autres
    (aspirations, non-dits, jamais de trahison gratuite) ;
  - la fiche publique de chaque PNJ est lue par les autres : n'y mettre que ce que l'équipe sait.

## 3. La fiche personnage (`elena.txt`)

```
# Elena

Elena, la trentaine, tient l'auberge du Cerf Blanc. Directe, chaleureuse, méfiante envers les
nobles. Parle court, ponctue de proverbes. (ce que tous savent, ton, manières de parler)

# SECRET
Elena a volé autrefois dans un temple et craint d'être reconnue. (ce que seul elle sait)
```
- Ligne 1 `# Nom` obligatoire (devient le nom affiché).
- `# SECRET` seul sur sa ligne, optionnel. Absent = tout est public.
- Le PNJ qui parle reçoit public + secret ; les autres PNJ ne voient que le public.
- `## tag commentaire` : section qui n'entre dans la fiche que quand `{npc-story on nom tag}` l'active
  (le premier mot est le tag, le reste est ignoré). Avant `# SECRET` : public une fois actif ; après :
  secret. La place décide de la visibilité : ne pas déplacer un `##` de l'autre côté sans y penser. Le
  texte hors tag est permanent. Guide §2.8 bis et §3.
- Écrire des traits observables (comportement, voix, motivation) plutôt qu'une liste d'adjectifs.
  Petits modèles : court, concret, un exemple de réplique si le ton est particulier.

## 4. `scenario.txt`

Langue : `Langue : English` (défaut si la ligne manque ; préférer l'anglais pour écrire, le joueur peut passer en français
en jeu avec le sélecteur 🌐 du panneau ⚙, « Auto » = la langue du scénario). Voir le guide §2.1.

```
Joueur : Mila
Langue : English

#SCENARIO
Monde : ...
Prémisse : ...
Ton : ...

# Le colis suspect
[Mila, élève de troisième année, vient de recevoir un colis dans la Grande Salle.]
[À côté d'elle, Odile observe le paquet d'un air intrigué : il vibre et sent le soufre.]
Point de départ : la situation telle qu'elle est à l'ouverture de la scène.
Odile : ce qu'elle fait ou tente dans CETTE scène (ex. ouvre le colis à contrecœur).
{private Odile} Odile cache qu'elle connaît l'expéditeur.  (seulement si les autres PNJ ne doivent pas le savoir)
Quand écrire [NEXT ACT] : Dès que la créature s'est échappée de la table.

# La poursuite dans les couloirs
...
```

Règles non négociables :
1. **3e personne partout** (prémisse et `[...]`) : « Alex arrive à Ambaya », jamais « Tu es Alex ».
   Ces textes sont recopiés dans le prompt de chaque PNJ.
2. **Scène jouée et conteneur.** Une **scène jouée** est le titre le plus profond d'une branche : elle porte
   le `[NEXT ACT]` et seule elle est jouée (c'est l'« acte » du moteur). Un **conteneur** est un titre qui a
   des sous-titres : il n'est jamais joué seul, son texte s'ajoute à toutes les scènes jouées en dessous.
   Scènes séquentielles = titres du même niveau (`#` / `#`) ; n'imbriquer (`#` / `##`) que pour un vrai
   chapitre contenant plusieurs scènes.
3. **Un `[...]`** = crochet ouvrant en début de ligne, fermant en fin de ligne. Il est affiché au
   joueur à l'entrée de l'acte et lu par les PNJ.
3 bis. **Transitions entre scènes.** Pour que le flux soit logique (deux amis se battent, la scène d'après ils
   étudient ensemble), mettre une petite transition au début du `[...]` d'ouverture de la scène suivante : une
   ou deux phrases à la 3e personne, des faits seulement (temps écoulé, lieu, ambiance, paix faite) : « [Les
   deux amis ont fait la paix après la dispute. Le lendemain, ils révisent ensemble à la bibliothèque.] ».
   Pas besoin d'une scène jouée. Ne dire que ce que le `[NEXT ACT]` précédent garantit, ou rester neutre
   (« Plus tard dans la journée »). Ne jamais décider ce que le joueur a fait ou ressenti. C'est une
   proposition : dire à l'utilisateur lesquelles on a ajoutées, pour qu'il les garde ou les retire.
4. **Le premier acte** doit dire au joueur qui il est, où il est, qui est présent (nommé) et le
   but général. Une ligne purement atmosphérique laisse le joueur perdu.
5. **Sans acte**, un seul bloc `[...]` est affiché : pour plusieurs scènes, faire de vrais actes.
6. `Joueur : X` en première ligne (défaut `Alex`).
7. `scenario-N.txt` : lus dans l'ordre de leur numéro **décimal** (`1`, `1.1`, `1.1.1`, `2`, `3.1.2`…), trous
   permis, `1` et `1.1` peuvent coexister. **Recommandé : numéroter en hiérarchie** pour couper un fichier ou
   en insérer un sans rien renommer (`scenario-2.1.txt` entre 2 et 3, `scenario-2.1.1.txt` entre 2.1 et 3,
   `scenario-0.txt` entre `scenario.txt` et `scenario-1.txt`).
   Chaque fichier commence par un titre `# ...` de premier niveau ; le numéro ne donne que l'ordre (les
   numéros d'actes viennent des titres).
8. **Ne pas fuiter : rien du futur dans une scène.** Un PNJ agit sur tout ce qu'il lit, tout de suite. S'il
   lit ce qui doit arriver plus tard, il le provoque (parfois en sortant de son rôle) et le modèle ne
   comprend pas qu'il n'a pas le droit de s'en servir.
   - **Scène jouée** : seulement la situation actuelle (`Point de départ`) et, pour chaque PNJ, ce qu'il fait
     ou tente dans CETTE scène, comme une consigne à un acteur (« Franck veut gagner et va tricher en tirant
     le fil d'Alex »). Jamais un événement d'une scène suivante, jamais un résultat. Une ou deux actions par
     PNJ, trois moments au plus ; au-delà, couper la scène en deux.
   - **Conteneur** : seulement ce qui est vrai pour toutes ses scènes (lieu, époque, qui est là). Jamais un
     résumé de ce qui « doit » s'y passer.
   - **Prémisse** : le monde, le ton, la situation de départ. Jamais la trame.
   - **Pas de ligne « Ce qui doit se passer »** (ni « le but de la séquence ») : elle pousse les PNJ à
     brûler les étapes. La fin d'une scène, c'est le fait du `[NEXT ACT]`, et rien d'autre.
   - **Le mobile, pas le plan.** Le désir ou la loyauté ultime d'un PNJ peut aller dans son `# SECRET`, écrit
     comme un état (« travaille pour ceux qui veulent Sam, a besoin que Sam lui fasse confiance »), jamais
     comme des actions à exécuter (« il suivra Sam puis l'enlèvera ce soir »). On peut le compléter en cours
     d'histoire avec `{npc-delta secret}` dans la scène où il devient utile.
   - Ce qu'un autre PNJ ne doit pas savoir dans la scène : `{private Nom}` (voir plus bas), pas le texte
     partagé. Guide §7.1.

## 5. Le mécanisme `[NEXT ACT]`

Fonctionnement : le LLM voit l'acte courant dans son prompt. Quand la condition est remplie, il
écrit `[NEXT ACT]` à la fin de sa réponse ; le marqueur est retiré à l'affichage, l'acte suivant
devient courant et ses `[...]` s'affichent. Jamais sur le dernier acte (rien à avancer).

**C'est la présence du texte `[NEXT ACT]` dans le corps de l'acte qui bascule en mode strict.**
Avec une ligne `Quand écrire [NEXT ACT] : <condition>`, le LLM n'avance qu'une fois la condition
réellement survenue (et finit l'échange en cours avant). Sans, il avance sur « conclusion
naturelle » : imprécis, à éviter.

Écrire une bonne condition :
- **Fait externe observable dans l'histoire** : « Dès que la créature s'est réfugiée dans un
  endroit clos », « Dès qu'Alex a trouvé la clé ». Pas « quand la scène est finie ».
- **État interne d'un PNJ** : formuler ce qu'il doit avoir dit ou décidé en scène (« Dès qu'Elena
  a avoué le vol à voix haute »), pas une émotion vague.
- Une seule condition par acte, courte, sans négation absolue (« jamais avant que… » incite les
  petits modèles à boucler ; préférer « dès que… »).
- **La condition doit être réalisable avec ce que la scène fait faire** aux PNJ et à la situation (voir
  « Vérification des `[NEXT ACT]` » plus bas). Contre-exemple : Nell lance un défi au joueur et la condition
  est « le joueur accepte le défi de Hobb » : Hobb n'a rien proposé, la scène ne peut pas se terminer.
- Conditions qui s'enchaînent : chaque acte doit contenir de quoi satisfaire sa propre
  condition, sinon l'histoire reste bloquée (le joueur peut forcer avec Suivant).
- Séquences qui se ressemblent (plusieurs combats…) : un marqueur unique par scène (adversaire,
  lieu) dans la condition et un fait d'ancrage dans `Point de départ`. Ce que voit chaque PNJ
  (anti-spoil, budget de contexte), texte commun hérité des titres parents, méthode pour un long
  scénario en petit contexte, pièges connus : `chatscenarios/guide-redaction-chat.md` §2.3, §2.7, §7.
- Ne pas écrire de ligne seule `[NEXT ACT]` entre crochets ; garder le marqueur au milieu de la
  phrase (`Quand écrire [NEXT ACT] : ...`) — c'est ce qui l'empêche d'être pris pour une
  réplique `[...]` à afficher.

### Toutes les commandes en un coup d'œil

Les commandes `{…}` `npc-…` et `cast` tiennent sur une ligne seule. Détail et portée : `chatscenarios/guide-redaction-chat.md` §2.0.

| Famille | Commande | Rôle |
|---|---|---|
| Texte aléatoire | `{pick random\|unique\|N\|all nom}` | Paragraphes d'un fichier `picks/nom.txt` (ou `/lib/nom`) |
| | `{repeat order\|random [N] nom}` … `{end-repeat}` | Gabarit répété ; `{n}`, `{item}` |
| | `{repeat N}` … `{end-repeat}` | Gabarit répété N fois, sans fichier |
| Casting | `{cast NOM [unique] [reuse-possible] [(a, b)]}` | Choisit un personnage, `%NOM%` ensuite |
| | `{repeat order\|random [N] cast NOM}` … `{end-repeat}` | Une copie par personnage |
| Consigne privée | `{private noms} texte` ou `{private noms}` … `{end-private}` | Texte lu par ces PNJ seulement, pour cette scène |
| Fiche | `{npc-delta public\|secret nom : texte}` | Ajoute un texte (une ou plusieurs lignes) à la fiche (jamais de retrait) |
| | `{npc-story on\|off nom tag}` | Active/désactive une section `## tag` |
| Présence | `{npc-active\|present\|passive\|absent noms}`, `{npc-reset}` | Niveau d'activité par titre |
| | `{npc-mode do on\|off nom\|*}` | Le PNJ tient la plume du narrateur |
| Entrée / sortie | `{npc-only noms}` | Au départ seuls ces PNJ sont dans l'histoire (avant le 1er `[NEXT ACT]`, une fois ; sans elle tous y sont) |
| | `{npc-arrives noms}` / `{npc-arrives modèle Nom1, Nom2}` | Fait entrer des PNJ, ou en crée depuis une fiche modèle (`%NPC%` = le nom) |
| | `{npc-leaves noms}` | Fait sortir des PNJ de l'histoire (≠ `absent` : plus de vignette ni de fiche chez les autres) |
| Ouverture | `{npc-starts [Nom]}`, `{npc-do [Nom]}` | Un PNJ parle en premier (le second avec la plume) |
| Joueur / progression | `[texte]`, `Quand écrire [NEXT ACT] : …` | Ligne affichée au joueur ; condition de l'acte suivant |

### Une consigne que seul un PNJ lit : `{private Nom}`

Le texte d'une scène est lu par **tous** les PNJ présents. Pour qu'une consigne ou un fait n'atteigne que
certains PNJ (Franck triche, Alex ne le sait pas), l'écrire avec `{private Nom}` :
```
{private Franck} Franck veut gagner et va tricher en tirant le fil d'Alex.

{private Alex, Mina}
Ils se doutent que le colis est piégé.
Ils n'en parlent pas devant Franck.
{end-private}
```
- Avec du texte après la commande, elle ne couvre que cette ligne. Seule sur sa ligne, elle ouvre un bloc
  fermé par `{end-private}` (pas d'imbrication, à fermer avant le titre suivant).
- Valable pour cette scène seulement. Les autres PNJ découvrent ce que le PNJ a fait quand c'est arrivé
  dans la conversation (puis dans le résumé).
- Noms comme `{npc-starts}` (séparés par des virgules ; nom inconnu = scénario refusé). Pas de
  `[NEXT ACT]` ni de ligne `[affichée au joueur]` à l'intérieur (refusé au chargement).
- Les agents d'analyse (🔍, état d'esprit) voient tout ; le débogage 🐞 affiche le texte brut avec les
  commandes.

### Quel mécanisme pour quel besoin

Un mécanisme seulement si la scène en a besoin : la plupart n'ont qu'un texte, une ligne par PNJ et un
`[NEXT ACT]`.

| Besoin | Mécanisme | Durée |
|---|---|---|
| Ce que tous savent ou voient, ce que chaque PNJ doit faire | Texte de la scène | Cette scène |
| Une consigne ou un fait que seuls certains PNJ lisent | `{private Nom}` | Cette scène |
| Ce qu'un PNJ veut au fond depuis le début | `# SECRET` de la fiche, comme un état | Toute l'histoire |
| Un changement définitif (bras blessé, serment) | `{npc-delta}` | Jusqu'à la fin, sans retrait |
| Un trait ou sentiment par paliers, ou temporaire | `{npc-story on/off}` | Jusqu'au prochain on/off |
| Qui est là, qui parle | `{npc-arrives}`, `{npc-leaves}`, `{npc-active}`… | Selon la commande |
| Ambiance, variété | `{style}`, `{N%}` | Cette scène et en dessous |

Un sentiment qui grandit par paliers (n'aime pas → déteste → estime) : une section `## tag` par palier,
et à chaque palier `{npc-story on Nom palier-suivant}` **et** `{npc-story off Nom palier-précédent}`.
Ne jamais empiler les paliers : le PNJ les lirait tous à la fois.

### Faire parler un PNJ en premier : `{npc-starts}` ou `{npc-do}`

Toutes les commandes de l'auteur s'écrivent entre **accolades** (`[...]` reste réservé à la narration
et à `[NEXT ACT]`). Celle-ci s'écrit **seule sur sa ligne** (sinon le chargement échoue) :
`{npc-starts}` (PNJ au hasard) ou `{npc-starts Elena}`. Dans l'en-tête avant le premier acte, il
ouvre l'histoire ; dans un acte, il ouvre cet acte. Effet : un PNJ parle, **sans message du joueur**
(ouverture normale). Plusieurs noms séparés par des virgules (`{npc-starts Elena, Marcus}`) : Elena ouvre, Marcus
enchaîne dans la même ronde, dans l'ordre écrit (un PNJ peut revenir : `Elena, Marcus, Elena` ; nom inconnu ou PNJ pas encore dans l'histoire = refus). `{npc-do}` / `{npc-do Elena}` : même chose, mais le PNJ ouvre avec la plume du narrateur
(comme un clic sur DO sans texte, il peut narrer et agir pour les autres ; avec plusieurs noms, tous ont la plume). Nom inconnu = scénario refusé au chargement. La ligne est retirée du texte vu par le LLM.
Ne pas le mettre sur chaque acte « par défaut » : réserver-le aux scènes où quelqu'un agit
réellement avant le joueur (arrivée, interpellation, événement).

### Varier le scénario à chaque partie : `{pick type nom}`

Une commande dans `scenario.txt` / `scenario-N.txt` est remplacée, au lancement d'une nouvelle partie,
par un ou plusieurs paragraphes de `picks/nom.txt` (séparés par une ligne `---` ou plus). Le type est
obligatoire : `{pick random nom}` (un, doublon possible), `{pick unique nom}` (un, jamais déjà
distribué), `{pick 3 nom}` (3 différents), `{pick all nom}` (tous, dans l'ordre). Le tirage est figé pour la durée de la partie (copie dans
`run/current/resolved/`) : modifier `scenario.txt` en cours de partie n'a pas d'effet jusqu'à
« Recommencer ». Sur une ligne `[...]`, les paragraphes doivent tenir sur une seule ligne. Exemple et
détails : `chatscenarios/guide-redaction-chat.md` §2.6.

### Bibliothèque commune à plusieurs scénarios : `/lib/nom`

Un nom de bibliothèque écrit `/lib/nom` (ou `/lib/dossier/nom`) lit `lib/nom.txt` dans le dossier `lib/` **à côté
des dossiers de scénarios** (la racine choisie au menu), pour `{pick …}` comme pour `{repeat …}` ; même format
que `picks/`. Le scénario n'est alors plus autonome (le copier demande son `lib/`). Tirage figé pour la partie,
`lib/` pas listé comme scénario. Exemple : `chatscenarios/demos/demo-lib/`. Guide §2.6.

### Répéter une séquence : `{repeat mode nom}`

Un bloc `{repeat order|random [N] nom}` … `{end-repeat}` (chaque commande seule sur sa ligne) est un gabarit
recopié une fois par paragraphe de `picks/nom.txt` (tout le fichier sans N). `{n}` = numéro de la copie,
`{item}` = paragraphe entier, `{item1}`, `{item2}`… = morceaux d'un paragraphe, séparés par une ligne qui ne
contient qu'un `-`. Tous les paragraphes doivent avoir autant de morceaux que le plus grand `{itemK}` du
gabarit. Déplié une fois par partie, avant `pick`. Exemple : `chatscenarios/demos/demo-repeat/`. Détails :
`chatscenarios/guide-redaction-chat.md` §2.6 bis. Deux autres formes : `{repeat 3}` (3 copies sans fichier,
seul `{n}` existe) et `{repeat order|random [N] cast NOM [(jack, mia)]}` (une copie par personnage).

### Choisir un personnage : `{cast NOM [unique] [reuse-possible] [(jack, mia)]}` et `%NOM%`

Une ligne seule (qui ne s'affiche pas) choisit un personnage du casting (la liste facultative le restreint ;
un seul mot par personnage) et le garde dans `%NOM%`, utilisable plus bas dans le texte, les `[...]` et les
commandes (`{npc-starts %NOM%}`). Par défaut le personnage est **différent de ceux des autres variables** ;
`reuse-possible` le permet ; `unique` : la variable ne reprend jamais un personnage déjà eu (dans un `repeat`,
un différent par copie). Dans `{repeat order cast CHEF}`, `%CHEF%` est le personnage de la copie et un
`{cast COMMIS}` du bloc en prend un autre ; les variables d'une copie sont oubliées à sa fin. Le tirage est
cherché globalement (retour en arrière) : erreur seulement si aucune combinaison n'existe. Une fois par
partie. Exemple : `chatscenarios/demos/demo-cast/`. Détails : guide §2.6 ter.

### Mode DO d'un PNJ : `{npc-mode do on|off nom|*}`

Le PNJ tient la plume du narrateur (comme le `DO:` du joueur) pendant un moment : il peut avancer la scène et
forcer le personnage du joueur (drogué, hypnotisé, possédé…). Une ligne seule dans un acte, héritée par ses
sous-actes, annulée par `off`, avec un nom ou `*` ; ★ sur la vignette. **Un seul PNJ à la fois** : plusieurs
PNJ avec la plume se doublent. Un `{npc-starts Nom}` de ce PNJ devient une ouverture avec la plume.
Guide §2.12, démo `chatscenarios/demos/demo-npc-do`.

### Style commun à tous les personnages : `style.txt`

Fichier optionnel à la racine du scénario, donné à chaque personnage sous `STYLE ADVICE:`. Décrire la
manière d'écrire (voix, rythme, ton), en positif et court (moins de 1 500 caractères), jamais des
événements ni des commandes `{...}`. Absent ou vide : aucun effet. Une ligne rare s'écrit avec `{20%}`
n'importe où dans la ligne : elle n'est donnée au modèle que dans 20 % des réponses (tirage à chaque
réponse), sinon elle disparaît entière. Guide §2.10.

### Style par scène et détail au hasard : `{style nom}` et `{N%}`

`{style foret-sombre}` seule sur sa ligne sous un titre ajoute `styles/foret-sombre.txt` au style global, pour la
scène et ses sous-scènes (il s'ajoute après `style.txt`, ne le remplace pas ; un `{style}` plus bas remplace celui du
parent). Plusieurs `{style}` sous un même titre s'ajoutent dans l'ordre ; dans la prémisse, ils s'ajoutent à
`style.txt` pour toute l'histoire. `{30%}` en tête d'une ligne de scène : gardée ou supprimée une fois au début de la partie (comme un pick),
lue par tous les PNJ à chaque tour : l'écrire comme un état, pas comme une action ponctuelle. Guide §2.10 bis, démo
`demo-scene-style`.

### Faire évoluer un personnage : `{npc-delta public|secret nom : texte}`

Le petit delta : une commande qui commence sur sa propre ligne dans un acte, fermée par son `}` (son texte
peut tenir sur plusieurs lignes, sans `}` ni ligne commençant par `#`), ajoute un texte à la fiche du
personnage à partir de cet acte : `public` (tout le monde la lit : blessure, changement visible) ou `secret` (seul lui : nouvel
objectif, découverte gardée pour soi). La visibilité est obligatoire. La ligne est retirée du texte de
l'acte, donc un secret ne fuit pas. Il sait ajouter, jamais retirer ; son texte peut dépendre d'un tirage
(`{repeat}`) et il complète une fiche partagée sans la modifier. Ancien nom `{delta-pnj}` : refusé.
Détails : `chatscenarios/guide-redaction-chat.md` §2.8.

### Activer / désactiver un texte de la fiche : `{npc-story on|off nom tag}`

Une ligne seule : active (`on`) ou retire (`off`) la section `## tag` de la fiche du personnage. Le tag est
le dernier mot. L'état vaut à partir de l'acte où la commande est écrite et pour **tous les suivants**, même
hors du titre qui la porte, jusqu'à un autre `on`/`off`. Au départ aucun tag n'est actif. `SECRET` n'est pas
un tag. Un secret devenu public = deux tags (`off` du secret, `on` du public). Interdit dans la prémisse ;
tag ou personnage inconnu = erreur ; tag jamais activé = avertissement. Le tag pour ce qui se défait ou
revient, le delta pour un fait unique. Guide §2.8 bis.

### Qui parle dans une scène : `{npc-active|present|passive|absent noms}`, `{npc-reset}`

Quatre niveaux d'activité par PNJ : **active** 😎 (parle à chaque échange), **present** 🙂 (défaut : répond
si nommé), **passive** 🤨 (réaction brève occasionnelle, ne répond pas parce qu'on le nomme), **absent** 😴
(ne parle jamais). Une ligne seule dans un titre fixe le niveau des PNJ nommés (séparés par des espaces) pour
ce titre et dessous ; sans nom, la commande ne touche que les PNJ dont le niveau n'est pas encore
explicite (jamais nommés). `{npc-active *}` (idem autres niveaux) vise tous les PNJ en nommage explicite ;
un nom du même titre l'emporte, `*` seul et une fois par titre. Le plus profond gagne ; `{npc-reset}`
remet tout le monde en present, explicite.
Interdit dans la prémisse ; nom inconnu, PNJ nommé deux fois dans un titre ou scène sans personne = erreur.
Le joueur peut forcer un niveau en cliquant la vignette (point rouge). Guide §2.11 et §4.1.
Bouton **▶ Continue** (joueur) : un tour de PNJ sans message du joueur, prompt habituel, sans interjection
(guide §4.9).

Pour écrire un scénario **entier** avec une IA à petit contexte : skill `chat-scenario-builder` (`chat/scenario-builder/`).

### Faire arriver / partir un PNJ : `{npc-only}`, `{npc-arrives}`, `{npc-leaves}`

Un PNJ **hors de l'histoire** (pas encore arrivé, ou parti) n'a ni vignette ni fiche chez les autres, et ne parle
jamais. `{npc-arrives modèle Nom1, Nom2}` crée des figurants depuis `characters/modèle.txt` (le fichier utilisé
avec un nom est un modèle, pas un PNJ ; `%NPC%` = le nom ; l'hostilité etc. via `{npc-delta}`). Limites : l'historique
est commun, un arrivant sait tout ce qui s'est dit avant (le faire mettre au courant) ; l'arrivée est liée à un acte,
pas à un événement ; un figurant n'a ni secret ni tag propre ; le joueur ne peut pas ajouter/retirer un PNJ.
`{npc-only}` mal placé (après le 1er `[NEXT ACT]`) = erreur. Guide §2.13, démo `demo-arrivals`.

Outils de diagnostic en jeu : 🔍 (analyse où en est la condition, sans écrire dans l'historique),
❓ (pistes : quels passages du prompt correspondent à la dernière réponse, lesquels de ses éléments
n'en ont aucun ; guide §4.3 bis),
Précédent / Suivant (correction manuelle), ⟳ (recharger les fichiers après correction).

## 6. Vérifier avant de jouer

`validation-scenarios.bat` (guide §2.9) liste toutes les erreurs et avertissements d'un scénario sans lancer
de partie ; la même vérification a lieu à chaque chargement.

Charger le scénario par le code (script jetable ou `ScenarioTesterCli`) et contrôler : nombre
d'actes obtenu = nombre voulu, nombre de PNJ, tours NARRATOR d'ouverture clairs (voir §5 de
`chatscenarios/guide-redaction-chat.md`). Ne pas se fier à la relecture seule : le piège des scènes jouées et des
conteneurs (un titre qui a des sous-titres n'est pas jouable) ne se voit pas à l'œil.

### Vérification des `[NEXT ACT]` (avant de rendre un scénario)

Le PNJ qui joue décide seul d'écrire `[NEXT ACT]` : si la condition ne peut pas survenir avec ce que la
scène lui fait faire, l'histoire reste bloquée (ou avance pour une mauvaise raison). À faire en relisant les
fichiers, pas le plan. Le jeu ne ramifie pas : l'histoire a une seule route et le joueur est censé la suivre.
Pas de sortie alternative à écrire ; on rend la route claire en faisant mener les PNJ vers la condition.
**D'abord, avant toute question de cohérence : as-tu pensé à formuler un `[NEXT ACT]` pour chaque scène
jouable ?** Les auteurs l'oublient. Lister tous les titres des fichiers, dans l'ordre de lecture ; repérer les
scènes jouées (sans sous-titre) et les conteneurs (avec sous-titres, qui n'en ont pas besoin) ; pour **chaque
scène jouée sauf la toute dernière de l'histoire**, écrire « oui » ou « MANQUE » : y a-t-il, dans la scène, une
ligne `Quand écrire [NEXT ACT]` avec UN fait ? Pour chaque « MANQUE », formuler une condition tout de suite,
même provisoire (les contrôles suivants la jugeront). Une ligne dans un conteneur ne compte pas : la descendre
dans les scènes jouées. La toute dernière scène n'en a aucune. Ne passer à la suite que lorsque toutes les
scènes jouables sauf la dernière ont leur ligne. Puis, pour chaque scène, remplir un tableau :

| Scène | Le fait qui la termine | La consigne de PNJ qui y mène (à citer) | Verdict |
|---|---|---|---|

Trois contrôles, tous à oui :
1. **Possible** : avec la situation et les PNJ présents, le fait peut survenir ; chaque personne, lieu ou objet
   de la condition figure dans la scène (point de départ, consigne de PNJ ou `[...]` d'ouverture) et chaque PNJ
   nommé est présent.
2. **Cohérent avec les consignes des PNJ, qui mènent vers lui.** Au moins une consigne pousse vers le fait, et
   la condition nomme le PNJ qui agit vraiment dans la scène. Ça peut prendre plusieurs échanges, mais la scène
   doit aller dans ce sens. Faux : Nell défie le joueur et la condition est « le joueur accepte le défi de
   Hobb » (Hobb n'a rien proposé). Juste : « le joueur accepte le défi de Nell » (ou confier le défi à Hobb).
   Aucune consigne ne mène au fait ? En ajouter une : c'est ainsi qu'on guide les PNJ.
3. **Pas déjà vrai au départ.** Une condition vraie dès le premier tour est un défaut du scénario. Faux :
   point de départ « ils sont dans l'auberge », condition « ils sont dans l'auberge ».

Enfin la **continuité** : si la scène suivante ne découle pas naturellement du fait qui termine celle-ci
(temps écoulé, lieu, ambiance : une bagarre, puis une étude paisible), la combler par une transition au début
du `[...]` de la scène suivante (règle 3 bis). Lister à l'utilisateur les transitions ajoutées.
Corriger en ajoutant ou en changeant une consigne quand c'est la scène qui est fausse, en changeant la condition
quand c'est elle. Refaire la vérification des fuites sur toute scène modifiée, et montrer le tableau corrigé à
l'utilisateur.

### Vérification des fuites (avant de rendre un scénario)

La validation ne voit pas les fuites : à faire par relecture, scène jouée par scène jouée. Pour chacune,
lister ce que ses PNJ lisent (prémisse, conteneurs au-dessus, texte de la scène, fiches ; une ligne
`{private Nom}` n'est lue que par ce PNJ), puis :
1. Chercher dans cette liste ce qui n'arrive que dans les scènes **suivantes** (leur `[NEXT ACT]`, ce que leurs
   PNJ y font, les changements, les révélations) : rien ne doit y figurer, même suggéré (« finira par »,
   « ensuite », « plus tard »).
2. Prémisse et conteneurs : aucune phrase sur ce qui se passe en dessous, aucun résumé de la trame.
3. Fiches et `# SECRET` : des états (désir, loyauté, ce qu'on cache), pas une suite d'actions ni un futur.
4. Chaque ligne de PNJ d'une scène : une tentative dans CETTE scène, à partir de maintenant, pas un
   résultat ni un événement pour plus tard.
5. Chaque ligne partagée : un autre PNJ agirait-il autrement en la lisant ? Alors `{private Nom}`.
6. Aucune ligne « Ce qui doit se passer », « ce qui arrive ensuite », « le but de la séquence ».

En jeu, le bouton 🐞 montre le texte réellement envoyé aux PNJ : si un PNJ fait allusion à quelque chose qui
n'est pas encore arrivé, ou presse l'histoire vers sa fin, une fuite reste : trouver la ligne et l'enlever.

## 7. Modèle de départ

Les modèles sont **dans ce dossier de skill** (ils voyagent avec lui), à copier puis adapter :
- `models/ecole-brumeval/` : 2 amis, 3 scènes, conditions `[NEXT ACT]` bien formées, une consigne par PNJ.
- `models/auberge-deux-pnj/` : 2 PNJ aux secrets croisés (`# SECRET` écrits comme des états).

Chaque dossier contient `scenario.txt` et `characters/`. Ils respectent la règle de non-fuite (§4, règle 8) :
une scène ne dit que la situation actuelle et ce que chaque PNJ fait dans CETTE scène. Les mêmes scénarios
vivent aussi dans `chatscenarios/` du projet (copies jouables) : si on corrige l'un, corriger l'autre.
