# 2026-06-23 18h13 — FocusActionFilter mis en sommeil dans agent/temp

## Évolution demandée
Retirer FocusActionFilter du workflow actif et le placer au frigo dans `/agent/temp`,
car l'analyse montre qu'il ne produit rien d'utile dans le câblage actuel.

## Diagnostic

`FocusActionFilterStep.run()` appelait l'agent avec :
- `actionCategories = List.of()` (vide)
- `actionsText = ""` (vide)

Conséquence dans `FocusActionFilter.call()` :
- La condition `if (!input.actionsText().isBlank())` → `false` → section ACTIONS jamais ajoutée au prompt
- `parseResponse` reçoit `validActions = List.of()` → retourne toujours `List.of()`

Conséquence dans `WriteWorkflow` :
- `actionsText = String.join("\n", focusOut.actionCategories())` → toujours `""`
- `focusOut.focusGroups()` → immédiatement jeté, non utilisé

L'agent coûtait un appel LLM par séquence et produisait `actionsText = ""` à chaque fois.

## Ce qui a été touché

### Déplacé : `agent/writer/focusactionfilter/` → `agent/temp/focusactionfilter/`
- `FocusActionFilter.java` — package mis à jour : `agent.temp.focusactionfilter`
- `FocusActionFilterInput.java` — idem
- `FocusActionFilterOutput.java` — idem
- `FocusActionFilter.md` — copié tel quel

### Supprimé
- `orchestrator/write/FocusActionFilterStep.java`

### Modifié : `WriteWorkflow.java`
- Suppression import `FocusActionFilterOutput`
- Suppression champ `focusActionFilterStep`
- Suppression paramètre constructeur `FocusActionFilterStep`
- Suppression assignment `this.focusActionFilterStep`
- Remplacement du bloc d'appel (5 lignes) par `String actionsText = ""`

### Modifié : `RedacteurModule.java`
- Suppression imports `FocusActionFilter`, `FocusActionFilterStep`
- Suppression instanciation `focusActionFilter`, `focusActionFilterStep`
- Suppression de `focusActionFilterStep` dans l'appel au constructeur `WriteWorkflow`

## Résultat
- Compilation OK, BUILD SUCCESS.
- Aucun appel LLM perdu à chaque séquence.
- L'agent est conservé dans `agent/temp` pour réactivation future si le câblage
  (fourniture de vraies `actionCategories` et `actionsText`) est implémenté.
