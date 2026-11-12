# 2026-11-11 - Modèles de scénario corrigés et embarqués dans le skill

## Évolution demandée
- Le skill d'écriture renvoyait vers `chatscenarios/ecole-brumeval/` et `chatscenarios/auberge-deux-pnj/` comme
  modèles. Ces deux scénarios contenaient encore l'ancienne ligne `Ce qui doit se passer` (événements futurs,
  résultats) : un modèle qui contredisait la règle de non-fuite.
- Le skill est copié ailleurs : un modèle resté dans `chatscenarios/` y serait introuvable. Les modèles doivent
  donc vivre dans le dossier du skill.

## Ce qui a été touché
- `chatscenarios/ecole-brumeval/scenario.txt` : les 3 lignes `Ce qui doit se passer` remplacées par la situation
  de la scène et une ligne de consigne pour Odile ; la prémisse n'annonce plus la trame (« va rapidement
  transformer une journée banale en course-poursuite… » supprimé).
- `chatscenarios/auberge-deux-pnj/scenario.txt` : une ligne par PNJ (Elena, Marcus) dans les deux scènes, un
  `Point de départ` pour la scène 2 ; l'ancienne ligne « chacun peut apprendre un peu des deux autres, sans
  qu'aucun secret n'éclate » remplacée par des consignes positives.
- Les fiches des personnages n'ont pas changé (leurs `# SECRET` sont déjà des états).
- Copies corrigées dans `.claude/skills/chat-scenario-writing/models/` (`ecole-brumeval/`, `auberge-deux-pnj/`,
  chacun avec `scenario.txt` et `characters/`) ; la section « Modèle de départ » du skill pointe vers elles.
- `chatscenarios/CLAUDE.md` : la phrase qui citait « Ce qui doit se passer » comme libellé de section.

## Résultat
Les deux modèles passent `validation-scenarios.bat` sans erreur ni avertissement. Les copies du skill et les
scénarios de `chatscenarios/` sont identiques : si on corrige l'un, corriger l'autre. Les autres scénarios et démos
du dépôt gardent l'ancien libellé (non migrés, comme décidé).
