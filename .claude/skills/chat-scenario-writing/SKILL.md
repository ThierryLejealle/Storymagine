---
name: chat-scenario-writing
description: Écrire ou modifier un scénario du module chat (chatscenarios/) — mono ou multi-personnages, fiches personnage, scenario.txt, actes et mécanisme [NEXT ACT]. À utiliser dès qu'on crée ou édite un scénario de chat.
---

# Écrire un scénario de chat

Référence complète : `chatscenarios/rules.md`. Spec technique : `specs/chat.md`. Ce guide est le
raccourci pratique. Les textes du scénario sont en français ; le code et la doc en anglais/français
selon le fichier.

## 1. Les fichiers

Pour démarrer un scénario : `init-chat.bat` crée le squelette (`scenario.txt` et une fiche par PNJ),
chaque champ est une consigne entre parenthèses à remplacer.

```
chatscenarios/mon-scenario/
  scenario.txt       obligatoire
  scenario-1.txt     optionnel, suite des actes (scénario long)
  characters/
    elena.txt        un fichier par PNJ, nom simple sans espace ni accent
```
Les fiches personnage vont dans `characters/` (structure cible, voir `chatscenarios/rules.md` §1).
Ne jamais créer ni éditer à la main le dossier `run/` (partie en cours, sauvegardes, archives, logs :
voir `chatscenarios/rules.md` §6). Pour partager un scénario, copier le dossier sans `run/`.
Dans `characters/`, tout `.txt` dont la première ligne n'est pas `# Nom` est ignoré.

## 2. Mono ou multi-personnage

- **Mono** : un seul fichier PNJ. Rien d'autre à faire.
- **Multi** : un fichier par PNJ. Tous sont présents dès le tour 1. Donc :
  - **nommer tous les PNJ dans les `[...]` du tout premier acte**, même discrets, sinon un PNJ
    tiré au hasard ne sait pas s'il est dans la pièce et peut boucler ;
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
- Écrire des traits observables (comportement, voix, motivation) plutôt qu'une liste d'adjectifs.
  Petits modèles : court, concret, un exemple de réplique si le ton est particulier.

## 4. `scenario.txt`

```
Joueur : Mila

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
- Ne pas écrire de ligne seule `[NEXT ACT]` entre crochets ; garder le marqueur au milieu de la
  phrase (`Quand écrire [NEXT ACT] : ...`) — c'est ce qui l'empêche d'être pris pour une
  réplique `[...]` à afficher.

### Faire parler un PNJ en premier : `[NPC STARTS]`

À l'inverse de `[NEXT ACT]`, celui-ci s'écrit **seul sur sa ligne** (sinon il n'est pas reconnu) :
`[NPC STARTS]` (PNJ au hasard) ou `[NPC STARTS: Elena]`. Dans l'en-tête avant le premier acte, il
ouvre l'histoire ; dans un acte, il ouvre cet acte. Effet : comme un clic sur DO sans texte, un seul
PNJ parle. Nom inconnu = scénario refusé au chargement. La ligne est retirée du texte vu par le LLM.
Ne pas le mettre sur chaque acte « par défaut » : réserver-le aux scènes où quelqu'un agit
réellement avant le joueur (arrivée, interpellation, événement).

Outils de diagnostic en jeu : 🔍 (analyse où en est la condition, sans écrire dans l'historique),
Précédent / Suivant (correction manuelle), ⟳ (recharger les fichiers après correction).

## 6. Vérifier avant de jouer

Charger le scénario par le code (script jetable ou `ScenarioTesterCli`) et contrôler : nombre
d'actes obtenu = nombre voulu, nombre de PNJ, tours NARRATOR d'ouverture clairs (voir §5 de
`chatscenarios/rules.md`). Ne pas se fier à la relecture seule : le piège des feuilles ne se voit
pas à l'œil.

## 7. Modèle de départ

Copier `chatscenarios/ecole-brumeval/` (2 amis, 3 actes, conditions bien formées) ou
`chatscenarios/auberge-deux-pnj/` (secrets croisés), puis adapter.
