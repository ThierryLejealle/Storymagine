# 2026-10-31 - Répertoire de démos : un scénario minimaliste par mécanisme

## Évolution demandée
Créer `chatscenarios/demos`, proposé comme choix au démarrage, avec un scénario par mécanisme construit,
nommé d'après le mécanisme (`demo-pick`, `demo-repeat`, `demo-pnj`…). Contraintes de l'auteur : à chaque
fois un scénario très très simple, ultra minimaliste, sur un sujet totalement anodin ; tous publics
(« safe for work ») ; personnages adultes uniquement. Les scénarios existants de `chatscenarios` ne sont
pas touchés, à part les scénarios de test `test-*`, remplacés par des démos.

## Ce qui a été touché
- `ChatCliSupport` : le choix de la racine des scénarios gagne l'option « 3 - Démos du projet »
  (`chatscenarios/demos`). `chatscenarios.root.default` n'est pas modifié.
- `ValidateScenarioCli` : valide aussi `chatscenarios/demos` ; `validation-scenarios.bat` : texte d'aide.
- `.gitignore` : `chatscenarios/demos/*/run/`.
- `chatscenarios/demos/` : 10 démos, chacune avec ses fiches personnage (adultes) et un `README.md`
  (ce qu'elle montre, quoi regarder, renvoi au guide), plus un `README.md` d'index :
  `demo-pick` (goûter), `demo-repeat` (ménage du samedi), `demo-actes` (promenade),
  `demo-multi-fichiers` (salade de fruits), `demo-pnj` (deux amis au café), `demo-npc-starts`
  (boulangère), `demo-delta-pnj` (anniversaire), `demo-secret` (carton mystérieux), `demo-activite`
  (le palier), `demo-style` (la météo).
- Supprimés : `chatscenarios/test-picks` (remplacé par `demo-pick`) et `chatscenarios/test-repeat`
  (remplacé par `demo-repeat`).
- Test `DemoScenariosTest` : les démos listées sont exactement les 10 attendues, chacune se valide sans
  erreur ni avertissement et se charge avec le nombre d'actes et de personnages prévu (sur une copie).
- Documentation : guide (§1, exemple du §2.6 bis), skill `chat-scenario-writing`, `specs/chat.md`.
- Aucun prompt d'agent modifié.

## Résultat
`chat.bat` propose les démos au choix 3 ; chaque mécanisme du chat a un exemple jouable minimal, protégé
par un test contre la dérive. Les démos n'ont pas été jouées avec un vrai LLM : seule leur structure est
vérifiée.
