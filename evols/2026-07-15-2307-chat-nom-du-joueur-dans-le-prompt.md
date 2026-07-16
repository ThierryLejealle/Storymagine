# 2026-07-15 23h07 - Nom du joueur dans le prompt (au lieu de "Player" generique)

## 1. Demande

Repere en discutant du bloc d'interjection : "watching you closely" contre "eyes on the player"
sonnait bien plus immersif — remonte a une question de fond : pourquoi le joueur n'a-t-il pas de
nom dans le prompt ? Demande de regarder, puis delta presente et valide, puis extension : "change
tout et met 'Alex' par défaut si le joueur n'est pas nommé. Et met Alex par défaut dans tous nos
scénarios."

## 2. Delta de prompt valide avant ecriture (regle CLAUDE.md respectee)

Deux points precis, presentes et confirmes avant implementation :
- Le label de transcript ("Player: ...") et la sequence d'arret qui empeche le modele de continuer
  a ecrire a la place du joueur ("\nPlayer:") utilisent desormais le nom reel.
- Les explications mecaniques ("OOC: ...", "DO: ...", "the player's character" dans les regles)
  restent generiques — ce sont des regles sur le MECANISME, pas de la prose narrative a imiter,
  donc pas concernees par l'immersion.

## 3. Ce qui a ete touche

- **`ChatScenario`** : nouveau champ `playerName`. Constructeur compact normalise tout
  `null`/blanc en `"Alex"` — invariant du record, jamais un `ChatScenario` avec un nom vide nulle
  part dans le code (tests compris). Constructeur 4-arg conserve (delegue avec `null` → normalise).
- **`ChatFileStorageAdapter.loadScenario`** : lit une ligne optionnelle `Joueur : X` en toute
  premiere ligne de `scenario.txt` (avant meme `#SCENARIO`), la retire du reste avant de passer au
  `ScenarioOutlineParser` — aucun changement necessaire dans ce parseur.
- **`ChatPromptBuilder.transcript(...)`** : nouveau parametre `playerName`, remplace le `"Player: "`
  fige. Tous les appelants mis a jour : `build()`, `NextActReadinessPromptBuilder`,
  `NpcMindStatePromptBuilder`, `ChatServiceImpl.compactIfNeeded` (x2, generation ET pliage dans le
  resume).
- **`RoleplayNarrator`** : la sequence d'arret `"\nPlayer:"` devient `"\n{playerName}:"`, calculee a
  partir de `input.scenario().playerName()`.
- **Les 5 scenarios existants** (`auberge-deux-pnj`, `carnet-bleu`, `temple-noir`,
  `temple-noir-actes`, `quete-des-amazones`) ont chacun reçu la ligne `Joueur : Alex` en tete de
  leur `scenario.txt` — meme si le defaut code (`"Alex"`) l'aurait donne de toute façon, l'utilisateur
  a explicitement demande la ligne explicite partout.

## 4. Tests

- `ChatScenarioTest` (nouveau fichier) : defaut a "Alex" (constructeur 4-arg, `null` explicite,
  chaine blanche), nom explicite conserve tel quel.
- `ChatFileStorageAdapterTest` : 2 tests ajoutes — ligne `Joueur : X` seule, puis combinee avec
  `#SCENARIO` (le cas reel utilise dans tous les scenarios du projet) ; verifie aussi que la
  premisse ne contient jamais la ligne Joueur.
- `ChatPromptBuilderTest` : 3 assertions existantes corrigees ("Player:" → "Alex:", le scenario de
  test n'ayant pas de ligne Joueur, retombe sur le defaut) + 1 test dedie a un nom personnalise.

## 5. Resultat

`mvn clean test` (racine, 4 modules) : 186 tests cote chat (+6 vs avant), tous verts. Verifie aussi
par script direct que les 5 scenarios existants chargent bien `playerName="Alex"` et que la ligne
`Joueur :` ne pollue jamais la premisse envoyee au LLM.

## 6. Suite prevue

L'utilisateur a demande un `rules.md` documentant toutes les regles scenario/character/mecanismes
du module chat — a faire dans la foulee.
