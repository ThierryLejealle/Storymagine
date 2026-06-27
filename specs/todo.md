## Agents en attente (dossier temp/)

- [NarrativeArcAnalyzer + CausalAnalyzer] Analyser TOUS les plans avant toute rédaction (pas chapitre par chapitre).
  Idée architecturale : planifier tous les chapitres d'abord, puis rédiger. Ces agents auraient alors
  tout le matériau disponible et pourraient détecter incohérences narratives et causales sur l'ensemble.
  Actuellement désactivés car ils opéraient sur les plans après coup (trop tard).

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
