# 2026-10-28 - Chat : commande {delta-pnj} (évolution de la fiche d'un personnage)

## 1. Évolution demandée

Faire évoluer la fiche d'un personnage en cours d'histoire, écrit en dur dans les actes :
`{delta-pnj public nom : texte}` (connu de tous) et `{delta-pnj secret nom : texte}` (seul le
personnage). Visibilité obligatoire, nom du personnage toujours en troisième mot. Pas de LLM ; les
deltas se recalculent depuis les actes, rien n'est stocké dans le run.

## 2. Ce qui a été touché

- Domaine : `CharacterDelta`, `CharacterDeltaMarker` (lignes seules et complètes ; toute occurrence
  restante de `{delta-pnj` est une erreur), `ScenarioAct.deltas`, `ChatScenario.sheetAt(npc, acte)`,
  `Npc.withPublicAddition` / `withSecretAddition`, `Scene.mapNpcs`.
- `ScenarioOutlineParser` : retire les lignes du texte de l'acte (sinon un secret irait à tous les PNJ),
  cumule les deltas des titres nouvellement entrés, refuse un delta dans la prémisse.
- Prompts : `ChatPromptBuilder`, `NextActReadinessPromptBuilder` et `NpcMindStatePromptBuilder` lisent la
  fiche à l'acte courant (`sheetAt`). **Aucune chaîne de prompt modifiée** : les lignes s'ajoutent à la
  fiche. Le calcul se fait au moment de construire le prompt, donc ⟳, undo, rollback, « Précédent » et
  les sauvegardes donnent la fiche qui correspond à l'acte.
- Tests : `CharacterDeltaMarkerTest`, `CharacterDeltaScenarioLoadingTest`, `ChatPromptBuilderTest`
  (le secret n'atteint jamais un autre PNJ, le public oui, rien avant l'acte).

## 3. Résultat

`mvn clean test` : tous les tests passent. Limite connue : « Précédent » retire le delta du prompt, les
tours déjà joués restent dans l'historique.
