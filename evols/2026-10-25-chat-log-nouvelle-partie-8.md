# 2026-10-25 - Chat CLI : message de démarrage d'une nouvelle partie

## 1. Évolution demandée

Au démarrage d'un scénario neuf, la CLI affichait « Nouvelle partie (introduction : 2 tour(s)) ». Pour
l'utilisateur, un « tour » est un échange joué : un scénario neuf ne doit afficher aucun tour. Les 2
« tours » étaient en réalité les lignes `[...]` de narration d'ouverture (tours du narrateur).

## 2. Ce qui a été touché

- `ChatCli` : le message devient « Nouvelle partie. », sans compteur. Le message de reprise
  compte désormais les tours joués (joueur et PNJ, pas le narrateur) : « Reprise (N tour(s) joue(s)
  en memoire) ». Aucun prompt modifié.

## 3. Résultat

Compilation vérifiée depuis la racine. Texte de log uniquement, aucun test concerné.
