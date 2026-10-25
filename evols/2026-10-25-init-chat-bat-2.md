# 2026-10-25 - init-chat.bat : créer le squelette d'un nouveau scénario de chat

## 1. Évolution demandée

Un `.bat` (nommé `init-chat.bat`, dans la logique de `init-story.bat` du module `redacteur` et de
`chat.bat`) qui demande le dossier (par défaut ou autre), le nom du scénario, le nombre de PNJ puis
leurs noms, et crée une structure par défaut avec un scénario quasi vide, exemple `[NEXT ACT]` ultra
léger. Décision : textes du modèle sous forme de consignes entre parenthèses à remplacer (option A),
pas d'exemple jouable.

## 2. Ce qui a été touché

- `ScenarioScaffolder` (infra, nouveau) : crée `scenario.txt` (prémisse, deux actes, premier acte
  nommant tous les PNJ, `Quand écrire [NEXT ACT]` sur le premier acte seulement) et une fiche par PNJ
  dans `characters/` (nom d'affichage + section `# SECRET` optionnelle). Nom de fichier dérivé du nom
  du PNJ (sans accent ni espace, `-2`/`-3` en cas de doublon). Valide le nom du scénario, refuse un
  scénario existant (jamais d'écrasement), ne crée pas `run/`.
- `InitChatCli` (ui/cli, nouveau) : le dialogue, avec relance jusqu'à une réponse valide.
- `ChatCliSupport` (ui/cli, nouveau) : choix du dossier des scénarios et chargement des propriétés,
  jusque-là copiés dans `ChatCli` et `ScenarioTesterCli` (et une copie des propriétés dans
  `MigrateScenarioLayoutCli`) ; les trois CLIs existantes l'utilisent désormais, plus de doublon.
- `init-chat.bat` (nouveau, CP1252).
- Tests : `ScenarioScaffolderTest` (12 tests, dont le chargement du squelette généré par
  `ChatFileStorageAdapter`).
- Docs : `chatscenarios/rules.md` §1.1, `specs/chat.md` (outils CLI), skill `chat-scenario-writing`.

## 3. Résultat

`mvn test` vert depuis la racine (305 tests pour le module chat). Essai de bout en bout de
`InitChatCli` avec des réponses simulées (nom invalide, nombre 0 et nom de PNJ vide relancés, puis
création correcte de `scenario.txt`, `characters/elena.txt` et `characters/marcus.txt`), dans un
dossier jetable ensuite supprimé. Au passage, un premier essai mal injecté (BOM de PowerShell sur
l'entrée standard) a créé un dossier vide `Elena` à la racine du projet : supprimé.

Non fait : `init-chat.bat` n'a pas été lancé tel quel (interactif) ; seule la classe Java l'a été.
