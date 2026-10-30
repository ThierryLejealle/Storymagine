# 2026-10-27 - Chat : garde-fou anti-boucle dans la règle [NEXT ACT] conditionnelle

## 1. Évolution demandée

Une scène commençait à boucler alors que la consigne de l'acte demandait de rester court, et que le
contrôle de disponibilité répondait « tout est prêt ». Le PNJ n'écrivait pourtant pas `[NEXT ACT]`.
Demande : un garde-fou dans le prompt, assouplir la règle quand l'action devient répétitive.

## 2. Ce qui a été touché

- `ChatPromptBuilder.NEXT_ACT_RULE_CONDITIONAL` : ajout (texte validé) de « If the action starts to
  become repetitive and you are close to this act's condition, consider writing "[NEXT ACT]" now
  rather than prolonging the scene. » `NEXT_ACT_RULE_DEFAULT` inchangée.
- `ChatPromptBuilderTest` : test `repetitionEscapeHatchIsOnlyInTheConditionalNextActRule`.
- `RoleplayNarrator.md` et `guide-redaction-chat.md` §7.2 : documentation du garde-fou.

## 3. Résultat

Prompt modifié après validation du delta. Tests non exécutés dans cette évolution ; efficacité sur
petit modèle à observer en conditions réelles.
