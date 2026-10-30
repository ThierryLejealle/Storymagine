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
`{commande sous-type argument : texte}` (pick random / unique / all / N, npc-starts, delta-pnj),
`[...]` et `[NEXT ACT]` inchanges, tableau des tirages Fichier / Type / Valeur, delta-pnj public / secret
calcule depuis les actes, validation du scenario a chaque chargement (+ `validation-scenarios.bat`),
migration de la syntaxe (faite, programme supprime ensuite). Fiches "lore" : remplacees par `{pick all nom}`.

Reste a faire / a decider :

- `{pick ...}` utilisable dans le texte d'un `{delta-pnj ...}` (aujourd'hui interdit : un paragraphe tire
  peut contenir des sauts de ligne ou des accolades).
- Deltas decides en cours de partie par un agent : imposerait de les stocker (comme les compactions) au
  lieu de les recalculer depuis les actes.
- Les deltas s'accumulent et pesent sur le budget de contexte du PNJ : a surveiller sur un long scenario.
- Validation : verifications possibles en plus (premier acte qui ne presente pas tous les PNJ de facon plus
  fine, blocs `[...]` mal fermes, fiches sans section attendue, personnages jamais utilises).
