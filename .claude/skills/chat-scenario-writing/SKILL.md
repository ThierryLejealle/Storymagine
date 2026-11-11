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
  scenario-1.txt     optionnel, suite des actes (scénario long)
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
Point de départ : ...
Ce qui doit se passer : ...
Quand écrire [NEXT ACT] : Dès que la créature s'est échappée de la table.

# La poursuite dans les couloirs
...
```

Règles non négociables :
1. **3e personne partout** (prémisse et `[...]`) : « Alex arrive à Ambaya », jamais « Tu es Alex ».
   Ces textes sont recopiés dans le prompt de chaque PNJ.
2. **Seules les feuilles sont des actes.** Actes séquentiels = titres du même niveau (`#` / `#`).
   N'imbriquer (`#` / `##`) que pour un vrai chapitre contenant plusieurs scènes.
3. **Un `[...]`** = crochet ouvrant en début de ligne, fermant en fin de ligne. Il est affiché au
   joueur à l'entrée de l'acte et lu par les PNJ.
4. **Le premier acte** doit dire au joueur qui il est, où il est, qui est présent (nommé) et le
   but général. Une ligne purement atmosphérique laisse le joueur perdu.
5. **Sans acte**, un seul bloc `[...]` est affiché : pour plusieurs scènes, faire de vrais actes.
6. `Joueur : X` en première ligne (défaut `Alex`).
7. `scenario-N.txt` : numéros consécutifs, chaque fichier commence par un titre `# ...` de premier
   niveau.
8. **Pas de synthèse de la suite avant les parties où elle se joue.** Sous un titre qui a des sous-parties
   (`#` puis `##`, `##` puis `###`…), ne pas écrire « ce qui va se passer = … » : le texte du parent est
   relu à chaque tour de chaque sous-partie par tous les PNJ, et le modèle ne comprend pas qu'il n'a pas le
   droit d'utiliser ce qu'il a sous les yeux : il anticipe et saute des étapes. Un vrai plan des
   personnages s'écrit comme un plan (« les personnages mettent en place le plan : … ») ; qu'un petit
   modèle comprenne qu'il peut échouer reste à vérifier. Guide §7.1.

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
| Fiche | `{npc-delta public\|secret nom : texte}` | Ajoute un texte (une ou plusieurs lignes) à la fiche (jamais de retrait) |
| | `{npc-story on\|off nom tag}` | Active/désactive une section `## tag` |
| Présence | `{npc-active\|present\|passive\|absent noms}`, `{npc-reset}` | Niveau d'activité par titre |
| | `{npc-mode do on\|off nom\|*}` | Le PNJ tient la plume du narrateur |
| Entrée / sortie | `{npc-only noms}` | Au départ seuls ces PNJ sont dans l'histoire (avant le 1er `[NEXT ACT]`, une fois ; sans elle tous y sont) |
| | `{npc-arrives noms}` / `{npc-arrives modèle Nom1, Nom2}` | Fait entrer des PNJ, ou en crée depuis une fiche modèle (`%NPC%` = le nom) |
| | `{npc-leaves noms}` | Fait sortir des PNJ de l'histoire (≠ `absent` : plus de vignette ni de fiche chez les autres) |
| Ouverture | `{npc-starts [Nom]}`, `{npc-do [Nom]}` | Un PNJ parle en premier (le second avec la plume) |
| Joueur / progression | `[texte]`, `Quand écrire [NEXT ACT] : …` | Ligne affichée au joueur ; condition de l'acte suivant |

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
`chatscenarios/guide-redaction-chat.md`). Ne pas se fier à la relecture seule : le piège des feuilles ne se voit
pas à l'œil.

## 7. Modèle de départ

Copier `chatscenarios/ecole-brumeval/` (2 amis, 3 actes, conditions bien formées) ou
`chatscenarios/auberge-deux-pnj/` (secrets croisés), puis adapter.
