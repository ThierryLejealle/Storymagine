# 2026-06-23 08h50 — Fix exec:java sur storymagine-parent

## Evolution demandée
`lancer.bat` échouait immédiatement avec `ClassNotFoundException: RedacteurCli`.

## Cause
`exec-maven-plugin:exec:java` s'exécute sur **tous les modules du reactor Maven**,
y compris `storymagine-parent` (POM root, toujours premier dans le reactor).
Le classpath du parent ne contient pas les classes de `redacteur` → ClassNotFoundException.
`commun` et `redacteur` étaient SKIPPED — la classe n'était jamais exécutée.

## Ce qui a été touché

**`pom.xml`** (parent)
- Ajout de `exec-maven-plugin` dans `pluginManagement` avec `<skip>true</skip>` par défaut.

**`redacteur/pom.xml`**
- Ajout de `exec-maven-plugin` dans `<build><plugins>` avec :
  - `<skip>false</skip>` (override du défaut parent)
  - `<mainClass>storymagine.redacteur.ui.cli.RedacteurCli</mainClass>`
  - `<cleanupDaemonThreads>false</cleanupDaemonThreads>`

**`lancer.bat`**
- Suppression de `-Dexec.mainClass` et `-Dexec.cleanupDaemonThreads` (désormais dans le pom).
- Correction du quoting du chemin : `"-Dredacteur.scenario.root=!SCENARIO_ROOT!"` (tout le token entre guillemets, pas juste la valeur).
- Suppression du flag `-q` pour que les exceptions soient visibles.

## Résultat
`exec:java` est maintenant skippé sur le parent et tous les autres modules.
Il tourne uniquement sur `redacteur` avec le bon classpath.
