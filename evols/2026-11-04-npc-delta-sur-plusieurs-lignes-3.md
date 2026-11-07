# 2026-11-04 - `{npc-delta}` : le texte peut tenir sur plusieurs lignes

## Évolution demandée
Anomalie signalée par l'auteur : `{npc-delta public|secret nom : texte}` imposait que toute la commande tienne sur
une seule ligne. Les accolades suffisent à délimiter le bloc : le texte doit pouvoir s'écrire sur plusieurs lignes.

## Ce qui a été touché
- `CharacterDeltaMarker` : le texte du delta accepte les retours à la ligne et s'arrête au premier `}` ; chaque ligne
  est débarrassée de son indentation, les retours à la ligne sont conservés dans la fiche. Une commande qui commence
  toujours sur sa propre ligne, sans rien après son `}`. Une commande jamais refermée reste une erreur (jamais laissée
  dans le texte de l'acte, donc pas de fuite d'un secret).
- `ScenarioValidator` : la validation, qui lisait ligne par ligne, lit maintenant le bloc de la ligne d'ouverture
  jusqu'à la ligne du `}` (ou jusqu'au titre suivant si la commande n'est jamais fermée) ; l'erreur est rapportée à la
  ligne d'ouverture et les lignes suivantes restent vérifiées.
- Tests : `CharacterDeltaMarkerTest` (texte sur plusieurs lignes, commande jamais fermée), `ScenarioValidatorTest`
  (delta multi-ligne valide, lignes suivantes toujours vérifiées, delta jamais fermé signalé à sa première ligne),
  `CharacterDeltaScenarioLoadingTest` (delta multi-ligne : fiche complétée, texte de l'acte intact, aucun secret
  dans le texte envoyé aux PNJ).
- Documentation : guide §2.0 et §2.8 (syntaxe, exemple, limites : pas de `}` ni de ligne commençant par `#`) et §2.9,
  skill `chat-scenario-writing`, `specs/chat.md`. Aucun prompt d'agent modifié.

## Résultat
Tests verts depuis la racine du projet. Les deltas d'une seule ligne se comportent comme avant.
**Limites** : le texte ne peut pas contenir de `}` ; une ligne du texte commençant par `#` est un titre (elle coupe
l'acte). `{npc-story}` et les autres commandes `npc-…` restent sur une ligne seule : seul `{npc-delta}`, qui porte un
texte libre, a été étendu.
