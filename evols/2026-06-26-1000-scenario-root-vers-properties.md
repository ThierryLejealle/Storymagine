# 2026-06-26 10h00 - Déplacement du répertoire de scénarios par défaut vers les propriétés

## Evolution demandée
Mettre la valeur du répertoire de scénarios par défaut (`C:\dev\llm\story`) dans le fichier de propriétés du module redacteur, et rendre `lancer.bat` "bête" (appel mvn seul, sans logique de menu).

## Ce qui a été touché

- `redacteur/src/main/resources/redacteur.properties` — ajout de `scenario.root.default=C:\dev\llm\story`
- `redacteur/src/main/java/.../ui/cli/RedacteurCli.java` — ajout de `chooseScenarioRoot()` : le menu de sélection du répertoire (anciennement dans le .bat) est maintenant géré par le CLI Java ; lecture du défaut depuis les propriétés
- `lancer.bat` — simplifié à un appel `mvn -pl redacteur -am compile exec:java` sans aucune logique de menu ni variable de répertoire

## Résultat
Le .bat est réduit à son rôle de lanceur pur. Toute la logique interactive (choix du répertoire, validation) est dans le CLI Java. La valeur par défaut est configurable via `redacteur.properties`.
