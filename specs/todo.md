## Agents en attente (dossier temp/)

- [ChapterStyleChecker] Refaire en couple Checker/Corrector à l'image des autres critics de chapitre
  (format AMELIORATION / DEFAUT_MINEUR / DEFAUT_SIGNIFICATIF). À intégrer dans [EVALUER CHAPITRE].

- [CharacterChecker] Critic de chapitre sur la cohérence des personnages. Pourrait aussi s'appliquer
  au plan (vérifier que le plan respecte les fiches personnages avant de rédiger).
  À réactiver avec le format standard critic et feedback loop.

## Backlog

- Corriger le Français en Anglais dans les specs et objets
- Corriger les fiches personnage, lore, focus, .. selon le nouveau modèle (et impacter le code)
- retirer toute mention à actionsText
- faire le tableau qui explique quel agent utilise quelle donnée ?
- le moteur reboucle sur le plan alors que les agents donnent 10/10 ?
- les prompts disent : en français. On doit pouvoir rédiger dans une autre langue
- le prompt plan mentionne état intérieur, mais ce n'est pas explicite avec les fiches personnages
- vérifier qu'on accumulle pas les critiques à chaque retry
- enregistrer le dernier profil de génération pour gagner du temps
- passer les directives de séquences (type imperative, non vides) aux 3 agents critiques de plan (PlanNarrativeCritic, PlanCoherenceCritic, GoalPlanChecker) : afficher avant les beats avec label "Directives :", + note de cadrage dans le prompt user : "Le plan peut enrichir librement ces directives. Signale uniquement les violations de CONTRAINTE ou LIMITE explicites."
- Pour le plan on demande exactement 6 lignes par sequence. C'est trop limitant
- Vérifier s'il ne faudrait pas numéroter les séquences S1 à Sn comme dans les retours en cas d'erreur

## Chat - idees (point au 2026-10-28)

Realise le 2026-10-28 (voir evols/2026-10-28-*) : commandes de scenario entre accolades
`{commande sous-type argument : texte}` (pick random / unique / all / N, npc-starts, npc-delta),
`[...]` et `[NEXT ACT]` inchanges, tableau des tirages Fichier / Type / Valeur, npc-delta public / secret
calcule depuis les actes, validation du scenario a chaque chargement (+ `validation-scenarios.bat`),
migration de la syntaxe (faite, programme supprime ensuite). Fiches "lore" : remplacees par `{pick all nom}`.

Reste a faire / a decider :

- `{pick ...}` utilisable dans le texte d'un `{npc-delta ...}` (aujourd'hui interdit : un paragraphe tire
  peut contenir des sauts de ligne ou des accolades).
- Deltas decides en cours de partie par un agent : imposerait de les stocker (comme les compactions) au
  lieu de les recalculer depuis les actes.
- Les deltas s'accumulent et pesent sur le budget de contexte du PNJ : a surveiller sur un long scenario.
- Validation : verifications possibles en plus (premier acte qui ne presente pas tous les PNJ de facon plus
  fine, blocs `[...]` mal fermes, fiches sans section attendue, personnages jamais utilises).

## Chat - variables de casting (realise le 2026-11-03)

Realise : `{cast NOM [unique] [reuse-possible] [(liste)]}`, `%NOM%`, `{repeat … cast NOM}`, `{repeat N}`, retour en
arriere sur les tirages (voir `evols/2026-11-03-variables-de-casting-cast.md`, guide §2.6 ter, demo `demo-cast`).

Reste a faire / a decider :

- Essayer avec un vrai LLM une partie de `demo-cast`.
- Bibliotheques communes `/lib/...` : evolution suivante (fiche `2026-11-03-bibliotheque-commune-lib-2`).
- Une variable pour une valeur autre qu'un personnage (un lieu tire une fois, reutilise plus bas) : non fait.

## Chat - sections de fiche et noms de commandes (realise le 2026-11-02)

Realise : `## tag` dans les fiches et `{npc-story on|off nom tag}` (etat qui se prolonge, public ou secret selon
la place par rapport a `# SECRET`), `{delta-pnj}` renomme `{npc-delta}` (ancien nom refuse avec le nouveau),
demo `demo-npc-story` (voir `evols/2026-11-02-npc-story-et-renommage-npc-delta.md`, guide §2.8 et §2.8 bis).

Reste a faire / a decider :

- Essayer avec un vrai LLM : changement de ton d'un PNJ quand un tag est remplace par un autre.
- Recapitulatif "tags publics / tags secrets" par fiche dans le rapport de validation (il n'existe que
  ERREUR et AVERTISSEMENT, pas de niveau d'information) : parade a la visibilite implicite par la place.
- Separation du style en deux sections, `global-adds-content.txt` / `{import xxx}` / `/contents` : decrite, pas
  decidee ni codee.

## Chat - mode DO temporaire pour les PNJ (realise le 2026-11-01)

Realise : `{npc-mode do on|off nom|*}` (mode permanent, herite des titres, etoile sur la vignette), `{npc-do}` /
`{npc-do Nom}` (ouverture avec la plume) et `{npc-starts}` devenu une ouverture normale sans bulle `DO:`
(voir `evols/2026-11-01-demarrage-recommencer-contexte-et-mode-do.md`, guide §2.12 et §4.5, demo `demo-npc-do`).

Reste a faire / a surveiller :

- Essayer avec un vrai LLM : la plume sur un PNJ (le prompt `DO:` n'a pas ete modifie), l'ouverture normale sans
  message du joueur, et la collision de plusieurs PNJ en mode DO dans la meme scene.
- D'autres modes `{npc-mode autre on|off}` pourront s'ajouter (`NpcMode` est generique).
- Un nom de PNJ peut-il etre ecrit sans `on|off` ? Non, pour l'instant.
