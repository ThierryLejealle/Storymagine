# ChapterPlanner

## Rôle
Produit le plan d'un chapitre. Deux modes :
- **Libre** (jsonMode=false) : plan narratif en prose libre
- **Structuré** (jsonMode=true) : tableau JSON, un objet par séquence, avec `beats`, `sensoriels`, `ton_et_rythme`

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
    "sensoriels": "son, vue, toucher, odorat",
    "ton_et_rythme": "couleur émotionnelle et cadence"
  }
]
```
Parsé en interne par `ChapterPlanner` via un mini-parser JSON (pas de Jackson dans le coeur).
Le résultat `ChapterPlannerOutput.sequencePlans()` contient les chaînes formatées pour le Writer.

## Slots de contexte (proportionnels à contextWindow)
- bookGoal : 1/16 du contexte
- characters : 1/8
- constraints, focusText, loreText, entityState, coherence : 1/12 chacun
- storySoFar : 1/8

## Contraintes
- Règles sur les beats : au moins 6 par séquence, actions concrètes uniquement
- `forbiddenPhrases` injectées dans le system prompt
- En correction, le plan précédent est injecté dans le user prompt (slot 1/4 du contexte)

## Source Redacteur
`story.context.ScenarioPlannerContext` (méthodes `planInternal`, `planChapterWithCorrection`, `parseAndFormatJson`)
