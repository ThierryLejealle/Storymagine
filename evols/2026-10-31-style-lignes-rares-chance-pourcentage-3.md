# 2026-10-31 - Style : lignes rares avec un marqueur de chance {N%}

## Évolution demandée
Un petit modèle ne sait pas faire « de temps en temps » : une consigne de style comme « mets en valeur
la tenue des filles » est appliquée à chaque réponse. Ajouter dans `style.txt` un marqueur `{N%}`,
placé n'importe où dans une ligne, qui conditionne la ligne entière, tiré à la volée à chaque réponse
(pas figé comme les `{pick}`).

## Ce qui a été touché
- `ScenarioStyle` : `CHANCE_MARKER` et `drawn(DoubleSupplier)` (ligne gardée sans marqueur, ou
  supprimée entière tiret compris ; style sans marqueur renvoyé tel quel).
- `ChatScenario.withStyle` : scénario portant le style tiré pour une réponse.
- `ChatServiceImpl.generateRepliesAndFinish` : tirage à chaque réponse de PNJ avec le hasard du service.
- `ScenarioValidator.checkStyleText` : refuse pourcentage hors 1-100, plusieurs marqueurs par ligne,
  marqueur sans texte, `{20}` sans `%`.
- Tests : `ScenarioStyleTest` (nouveau), `ScenarioValidatorTest`. Deux tests de `ChatServiceImplTest`
  dépendaient du hasard depuis la fiche d'interjection du jour (plafond 60 %) : ils utilisent
  maintenant `alwaysInterject()`.
- Debug 🐞 : `ScenarioPromptView.styleReceived`, `ChatSession.lastStyleReceived`, `StyleReceived` et
  `chat.html` affichent le style réellement reçu par le dernier PNJ après tirage, à côté du fichier.
- 🐞 n'affiche plus l'avertissement de taille de `style.txt` (`ChatServiceImpl.warningsForDebugView`) :
  avec des lignes `{N%}` la taille du fichier n'est pas ce que le modèle reçoit. Le chargement et
  `validation-scenarios.bat` l'affichent toujours.
- Test d'intégration `ChatServiceImplTest` : le prompt capturé ne contient jamais `{` ni `%` ; un tirage
  par locuteur, mesuré sur 300 tours ; `ScenarioStyleTest` : 4 lignes à 10 % sur 40 000 tirages.
- Guide §2.10 et §7.4 (piège « de temps en temps »), `SKILL.md`, `specs/chat.md`.
- Aucun prompt modifié : le texte des consignes est inchangé, seul le contenu du style est filtré.

## Résultat
Tests du module chat verts (3 exécutions de suite). Exemple : `- {20%} Mets en valeur la tenue
impeccable d'une des filles.` n'apparaît dans le prompt que dans environ 20 % des réponses.
