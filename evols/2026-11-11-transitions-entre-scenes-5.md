# 2026-11-11 - Transitions entre scènes

## Évolution demandée
- Que les skills proposent de petites transitions, simplement descriptives, pour que le flux du scénario soit
  logique. Exemple : deux amis se battent, la scène d'après ils étudient ensemble ; une phrase dit qu'ils ont
  fait la paix. Pas de scène jouée en plus, juste un élément de transition dans le narratif.

## Ce qui a été touché
- Aucun code : une transition est une phrase au début du `[...]` d'ouverture de la scène suivante, ce que le
  moteur sait déjà faire (la ligne est montrée au joueur et lue par les PNJ).
- `chat/scenario-builder/` : section « Transitions between scenes » dans `ref-writing-tips.md` (faits seulement,
  rien sur le joueur, ne dire que ce que le `[NEXT ACT]` précédent garantit ou rester neutre, signalée à
  l'utilisateur comme proposition) ; `phase-4-steps.md` ; étape « continuité » et
  rapport de la phase 5 ; `skeleton.md`. Taille annoncée du dossier : environ 12 000 tokens (README et
  `SKILL.md` du builder).
- Skill `chat-scenario-writing` : règle 3 bis et étape « continuité » de la vérification des `[NEXT ACT]`.
- Guide : §2.4 (transitions) et §7.2 ; `specs/chat.md`.

## Résultat
Documentation seulement ; aucun prompt modifié. Les transitions sont une proposition de l'IA rédactrice, que
l'utilisateur garde, change ou retire.
