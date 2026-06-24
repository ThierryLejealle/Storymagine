# 2026-06-24 10h00 - Fix exec:java silencieux dans testllm

## Evolution demandée
`test-llm.bat` ne produisait aucune sortie et semblait ne rien faire.

## Cause
Le `pom.xml` parent définit `<skip>true</skip>` sur `exec-maven-plugin` pour empêcher son exécution accidentelle lors d'un build global. Le module `testllm/pom.xml` n'avait pas de configuration exec-maven-plugin et héritait donc de ce `skip=true`. Maven exécutait `exec:java` sans erreur ni avertissement, mais ne lançait tout simplement pas la classe Java.

Le module `redacteur` avait déjà la bonne configuration (`<skip>false</skip>`).

## Ce qui a été touché
- `testllm/pom.xml` : ajout du bloc `exec-maven-plugin` avec `<skip>false</skip>`, `<mainClass>`, `<cleanupDaemonThreads>false</cleanupDaemonThreads>`

## Résultat
`test-llm.bat` lance désormais correctement `TestLlmCli` et affiche le menu interactif.
