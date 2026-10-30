# 2026-10-27 - Chat : métadonnée `Langue :`, langue explicite dans tous les prompts, migration, rôle de la fiche

## 1. Évolution demandée

- Les petits modèles hésitent entre deux langues pour leur réponse et y passent une partie de leur
  réflexion. Ajouter une métadonnée de langue au scénario (comme le nom du joueur) et fixer la langue de
  façon explicite dans le prompt, une seule fois.
- Faire de même pour TOUS les appels au LLM du module chat.
- Programme de migration pour ajouter la langue aux scénarios : français pour ceux du projet, anglais pour
  ceux du répertoire des histoires par défaut.
- Guide : préciser le rôle de la fiche personnage pour éviter les fuites de scénario.

## 2. Ce qui a été touché

- Nouveau concept `ScenarioLanguage` (nom anglais de la langue, `UNSPECIFIED` si absente) ; `phrase()`
  donne la formulation commune « French, even though these instructions are in English ».
- `ChatScenario.language()` ; en-tête de `scenario.txt` lu par `ScenarioHeader` (lignes `Joueur :` et
  `Langue :` consécutives, ordre libre), partagé par le chargeur et la migration.
- Prompts (delta validé) : `ChatPromptBuilder` (« Reply in the same language as the character and
  scenario below. » devient « Write every reply in {langue}. »), `ChatSummarizer`,
  `NpcMindStatePromptBuilder`, `NextActReadinessPromptBuilder`, `ScenarioClarityReviewer`,
  `ScenarioContinuityReviewer`. Sans `Langue :`, l'ancien libellé « same language as ... » est conservé
  comme repli. Les entrées du résumeur et des deux reviewers portent la langue du scénario.
- `ScenarioLanguageMigrator`, `MigrateScenarioLanguageCli`, `migration-langue.bat` : ajoute `Langue :`
  après `Joueur :` (ou en tête), conserve les fins de ligne, idempotent, jamais d'écrasement d'une ligne
  existante. Lancé sur `chatscenarios/` du projet : 8 scénarios migrés en français. Le répertoire des
  histoires par défaut n'a pas été touché (à migrer avec `migration-langue.bat`).
- Graine d'`init-chat` : `Langue : français`.
- Tests : `ScenarioLanguageTest`, `LanguageInPromptsTest` (les six prompts, avec et sans langue),
  `ScenarioLanguageMigratorTest`, trois tests de lecture de l'en-tête. Suite complète au vert.
- Documentation : guide §2.1, §3 (rôle de la fiche, tableau fiche/acte, test en trois questions), §7.3 et
  §7.4 ; `specs/chat.md` ; `.md` des agents `ChatSummarizer`, `RoleplayNarrator`,
  `ScenarioContinuityReviewer`.

## 3. Résultat

Le modèle reçoit la langue comme une règle fixe, énoncée une fois par appel. À confirmer en jeu avec un
petit modèle : la consigne fixe la langue de la réponse mais n'impose rien sur la langue de la réflexion.
