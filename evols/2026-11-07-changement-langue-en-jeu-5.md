# 2026-11-07 - Changement de langue en cours de partie (anglais par défaut)

## Évolution demandée
Un sélecteur de langue dans l'interface pour changer la langue de la partie en cours de jeu. Deux valeurs
(English, Français) plus « Auto » = la langue prévue par le scénario. Anglais par défaut. Recommandation de
rédaction : écrire les scénarios en anglais, sauf certitude qu'ils ne seront joués qu'en français.

## Ce qui a été touché
- `ScenarioLanguage` : constantes `ENGLISH` et `FRENCH` ; `ChatScenario.withLanguage`.
- `ScenarioHeader` : sans ligne `Langue :`, la langue est l'anglais (les 22 scénarios du dépôt en ont une, aucun ne change).
- `ChatSession` : `chooseLanguage` (null = Auto), `chosenLanguage`, `authoredLanguage` ; la langue choisie est
  réappliquée après `reloadScenario`. Tous les agents lisent déjà `scenario().language()` : aucun n'a changé.
- `RunState` / `ChatFileStorageAdapter` : clé `language=` dans `state.txt`, relue par `loadSession`.
- `ChatService.changeLanguage`, route `POST /language`, champs `languageChoice` / `authoredLanguage` dans `ChatHistoryView`,
  sélecteur 🌐 dans le panneau ⚙ de `chat.html`.
- Tests : `ChatSessionTest`, `ChatFileStorageAdapterTest` (dont deux attentes passées de « non précisée » à « English »),
  `RunStateTest`.
- Documentation : guide §2.1 (défaut, recommandation, sélecteur et limites), `specs/chat.md`, skill
  `chat-scenario-writing`, `chat/scenario-builder/` (`ref-format.md`, `phase-1-brief.md`).

## Résultat
Tests verts. Aucun prompt d'agent modifié : la phrase de langue existait déjà (`ScenarioLanguage.phrase`).
Limite documentée : l'historique, la prémisse, les textes de scène et les fiches restent dans leur langue d'origine
(le modèle traduit à la volée) ; mieux vaut changer au début de la partie. Non essayé avec un vrai modèle.
