# 2026-10-01 - Création du scénario "ecole-brumeval"

## Demande
L'utilisateur a demandé une petite histoire de magie dans une école (type Harry Potter), jouable dans le module chat, où le joueur est une étudiante et le personnage IA est une amie d'enfance venue du même village. L'utilisateur ne voulait pas connaître le scénario à l'avance (surprise à la lecture/au jeu).

## Ce qui a été touché
- Création de `chatscenarios/ecole-brumeval/scenario.txt` (prémisse + 3 actes, format conforme à `chatscenarios/rules.md`).
- Création de `chatscenarios/ecole-brumeval/odile.txt` (fiche PNJ, fiche publique + section SECRET).
- Joueuse nommée "Mila" (validé explicitement par l'utilisateur).
- Aucun fichier existant modifié, aucun prompt d'agent touché.

## Résultat
Nouveau scénario jouable dans `chatscenarios/` (dossier du projet). Comme `chat.properties` pointe par défaut vers un répertoire externe (`chatscenarios.root.default=C:/dev/llm/story/chat`), l'utilisateur doit choisir l'option "répertoire du projet" dans le menu interactif au lancement de ChatCli pour que ce scénario apparaisse dans la liste.
