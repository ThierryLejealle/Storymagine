# ChapterPlanner

## Rôle
Produit le plan d'un chapitre. Deux modes :
- **Libre** (jsonMode=false) : plan narratif en prose libre
- **Structuré** (jsonMode=true) : tableau JSON, un objet par séquence, avec `beats`, `sensoriels`, `intention_de_scene`

## Modes de fonctionnement
| Mode | Condition | Description |
|------|-----------|-------------|
| Nouveau plan libre | jsonMode=false, previousPlan=null | Plan narratif impératif en prose |
| Nouveau plan JSON | jsonMode=true, previousPlan=null | Tableau JSON (un objet/séquence) |
| Réécriture | isRewrite=true | Révision avec problèmes en contexte (cohérence) |
| Correction | previousPlan!=null | Correction ciblée d'un plan rejeté |

## Format de sortie JSON
```json
[
  {
    "sequence": 1,
    "beats": ["beat 1", "beat 2", ...],
    "sensoriels": "sons, textures, odeurs, lumière, températures, mouvements visibles",
    "intention_de_scene": "effet recherché sur le lecteur — max 15 mots"
  }
]
```
Parsé en interne par `ChapterPlanner` via un mini-parser JSON (pas de Jackson dans le coeur).
Le résultat `ChapterPlannerOutput.sequencePlans()` contient les chaînes formatées pour le Writer.

## Slots de contexte (proportionnels à contextWindow)
- bookGoal : 1/16 du contexte
- characters : 1/8
- constraints, focusText, loreText, entityState, coherence : 1/12 chacun
- summary : 1/8 (`SummaryBudget`, même fraction que le slot `summary` du Writer)

## Contraintes
- Règles sur les beats : au moins 6 par séquence, filmables à la caméra (action/réaction/parole/perception sensorielle)
- `forbiddenPhrases` injectées dans le system prompt
- En correction, le plan précédent est injecté dans le user prompt (slot 1/4 du contexte)
- Consomme le côté `constraint()` des `Requirement` du scénario (jamais `check()`) —
  préfixées dans le prompt par "Assure-toi que chacun des points suivants est respecté :"

## Source Redacteur
`story.context.ScenarioPlannerContext` (méthodes `planInternal`, `planChapterWithCorrection`, `parseAndFormatJson`)
