# 2026-07-21 - Fix prompt : ambiguïté "OOC:" clarifiée

## Demande
Le joueur a signalé que le LLM ne comprend pas toujours qu'une ligne "OOC: ..." est une consigne
hors-jeu — il arrive que le personnage y réagisse comme si c'était une réplique dite ou entendue
dans la scène.

## Diagnostic
Le texte existant disait "the player is stepping outside the story to talk to you directly".
Or "you" désigne le personnage partout ailleurs dans le prompt ("You are the character
described below... stay in character in every reply") — cette phrase ne levait jamais
l'ambiguïté du pronom, laissant le modèle libre d'interpréter "you" comme le personnage lui-même.

## Delta (validé explicitement par le joueur avant écriture)
Bullet "OOC:" de `ChatPromptBuilder.SYSTEM_FORMATTING` :

Ancien :
```
- "OOC: ..." means the player is stepping outside the story to talk to you directly — a
  question, an instruction, or a request to change something. Answer as yourself, not as
  your character, and stop there: don't move the scene forward in the same reply. Resume
  the story only once the player sends their next in-character message.
  Example:
  OOC: be more suspicious of me from now on.
  Wrong: "Got it, I'll have him distrust you now." *His eyes narrow and he takes a step
  back, studying my hands.*
  Right: "Got it — I'll play him more guarded going forward."
```

Nouveau :
```
- "OOC: ..." is a direct instruction, not a line of dialogue — nobody in the story said it
  or heard it. Treat it like a piece of the scenario that just changed: a technical request
  (skip ahead, adjust something) or a shift in the story itself, as if an event had just
  happened or your character had a sudden idea. Answer as yourself, not as your character,
  and stop there: don't move the scene forward in the same reply. Resume the story only
  once the player sends their next in-character message.
  Example — the player writes OOC: be more suspicious of me from now on.
  Wrong (treats it as part of the scene): *He frowns, puzzled by the strange remark.* "Why
  are you saying that out loud?"
  Wrong (answers correctly but still moves the story forward): "Got it, I'll have him
  distrust you now." *His eyes narrow and he takes a step back, studying my hands.*
  Right: "Got it — I'll play him more guarded going forward."
```

Changements : (1) supprime "talk to you directly" (ambiguïté de pronom identifiée par le joueur),
remplacé par "nobody in the story said it or heard it" ; (2) ajoute l'analogie "treat it like a
piece of the scenario that just changed" (proposée par le joueur — réutilise le mécanisme déjà
connu du modèle pour SCENARIO/CURRENT SCENE : contenu qui influence le personnage sans jamais
être perçu par lui) ; (3) ajoute un second exemple Wrong ciblant précisément la confusion
rapportée (le personnage traite la ligne OOC comme une réplique de la scène) ; l'exemple Wrong et
Right existants (relance la scène malgré une bonne réponse) sont conservés tels quels.

## Résultat
Suite de tests `chat` toujours verte (aucun test n'asserte sur le texte exact du prompt). Aucun
autre fichier touché.
