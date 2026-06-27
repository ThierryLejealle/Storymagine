# Writer

## Rôle
Génère la prose d'une séquence narrative à partir de `WriterInput`.
C'est l'agent le plus riche en contexte : il reçoit personnages, plan, focus, lore, historique, état, etc.

## Slots de contexte (proportionnels à contextWindow, ~4 chars/token)
| Slot | Taille |
|------|--------|
| entityState | 1/16 |
| loopJournal | 1/20 |
| actionsText | 1/20 |
| focusText | 1/16 |
| loreText | 1/16 |
| redactionConstraints | 1/16 |
| styleGuide | 1/20 |
| charactersText | 1/8 |
| chapterPlan | 1/8 |
| storySoFar | 1/8 (last sentences) |
| writingExample | 1/16 |

Budget total utilisateur : 60% du contexte.

## Logique de la directive
- Si `sequencePlan` (plan de séquence issu de ChapterPlanner) est fourni → utilisé comme directive
- Sinon → `sequenceDescription` du scénario

## Règles d'ouverture (stitch)
- Si `stitch` fourni → utilisé directement
- Si `prevSentences` non vide → règle "raccorde sans résumer"
- Sinon → règle "point d'entrée varié"

## Mode réécriture
Si `isRewrite=true`, le system prompt commence par un préfixe imposant la correction des problèmes listés dans le plan.

## Source Redacteur
`story.context.WriterContext.writeSequence`
