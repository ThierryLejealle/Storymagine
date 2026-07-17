# Règles du module chat — scénarios, personnages, mécanismes

Référence pour écrire ou modifier un scénario dans `chatscenarios/`. Tout ce qui suit a été vérifié
dans le code (pas seulement supposé) — voir les fiches `evols/` citées pour le détail.

## 1. Structure d'un dossier de scénario

```
chatscenarios/mon-scenario/
  scenario.txt        obligatoire — monde, prémisse, actes
  elena.txt            un fichier par personnage (nom libre)
  marcus.txt
  ...
  history.md            générés/gérés par l'appli, jamais à écrire à la main
  summary.md
  act.txt
  present.txt
  interject.txt
  saves/
  archive/
  logs/
```

**Règle de chargement du Cast (`ChatFileStorageAdapter.loadCast`)** : tous les fichiers `.txt` du
dossier sont des personnages, SAUF `scenario.txt`, `act.txt`, `present.txt`, `interject.txt`
(réservés). Le nom de fichier (sans `.txt`) devient l'id interne du personnage — choisir un nom de
fichier simple, sans espace ni accent, ex. `marcus.txt` pas `Marcus Dupont.txt`.

Un scénario à un seul personnage (juste `character.txt`) fonctionne sans rien de spécial — devient
un `Cast` à un seul PNJ automatiquement.

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

**Piège vécu** (voir `evols/2026-07-15-2157-...`) : écrire `# Acte 1` suivi d'un SEUL `## Acte 2`
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

```
[Le tonnerre gronde alors qu'Alex referme derrière lui la porte de l'auberge.]
```

**⚠️ Même règle que la Prémisse : toujours à la 3e personne** (voir `chatscenarios/CLAUDE.md`).
Ces lignes ne sont pas seulement affichées au joueur — elles sont aussi injectées telles quelles
dans le prompt de CHAQUE PNJ via `act.text()` (voir `Teaser.java`, qui les laisse volontairement
dans le texte "so the LLM reads them too, in place"). Écrites à la 2e personne ("Tu es Alex...",
"vous venez de..."), elles contredisent l'assignation de personnage donnée à chaque PNJ dans son
propre prompt — bug réel corrigé le 2026-07-16 sur les 5 scénarios existants.

**Règle d'or (voir `evols/2026-07-15-2157-...`, audit d'introduction)** : le TOUT PREMIER acte du
scénario doit avoir des lignes `[...]` qui disent clairement au joueur : qui il/elle est, où il/elle
se trouve, qui est présent (nommé), et quel est le but général. Une seule ligne purement
atmosphérique ("le tonnerre gronde...") sans ces informations laisse le joueur perdu — c'est arrivé
et corrigé une fois, ne pas répéter l'erreur sur un nouveau scénario.

Un chapitre parent (`#` non-feuille) peut aussi avoir ses propres lignes `[...]` : elles ne
s'affichent qu'une seule fois, la première fois qu'un de ses descendants devient courant — jamais
répétées à chaque sous-scène suivante du même chapitre.

**Deuxième piège vécu, multi-PNJ** (voir `evols/2026-07-15-2318-...`) : tous les PNJ sont présents
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

## 3. Format d'un fichier personnage (`nom.txt`)

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
`present.txt`.

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

**Reprise sur réponse vide** (voir `evols/2026-07-15-2318-...`) : un petit modèle peut parfois
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
`evols/2026-07-15-2231-...` et `evols/2026-07-16-...` (refonte).

### 4.4 Réflexion / thinking (case "Afficher la réflexion", icône 🧠)

La réflexion est **toujours demandée** au modèle à chaque appel, indépendamment de l'affichage
(meilleure qualité de réponse, voir `evols/2026-07-15-1602-...`). L'affichage dans le chat est
séparé, actif par défaut (case décochable). Certains modèles ne savent pas produire de réflexion
séparée (dépend de comment ils sont enregistrés dans Ollama) — rien à faire côté scénario, c'est
une capacité du modèle, pas du scénario.

### 4.5 Progression d'actes

Le LLM écrit `[NEXT ACT]` dans sa réponse (retiré avant affichage) quand la condition de l'acte
courant est remplie → avance à l'acte suivant, ses lignes `[...]` s'affichent. Boutons manuels
"Précédent"/"Suivant" pour corriger. Bouton 🔍 pour demander au LLM (sans rien persister) où en est
la condition de l'acte suivant.

### 4.6 Sauvegardes et redémarrage

- Sauvegarde automatique après chaque tour (slot unique, écrasé à chaque fois).
- 💾 Sauvegarder : point de sauvegarde manuel, horodaté, illimité en nombre.
- 🔄 Recommencer : remet la session à zéro sur le même scénario (confirmation demandée) — l'ancien
  historique est archivé, jamais perdu.
- ⟳ (à côté de "Personnages") : recharge `scenario.txt` et les fichiers personnages depuis le
  disque sans toucher à la conversation en cours — pratique pour corriger une fiche en cours de
  partie.

### 4.7 Compaction du contexte

Quand la transcription dépasse le budget de tokens disponible, les tours les plus anciens sont
résumés (`ChatSummarizer`) et remplacés par ce résumé dans le prompt — jamais perdus pour de bon,
archivés verbatim dans `full-history.md`.

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
