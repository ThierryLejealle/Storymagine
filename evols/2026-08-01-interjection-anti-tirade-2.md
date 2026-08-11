# 2026-08-01 - Interjection : anti-tirade / anti-conseil

## Évolution demandée
Retour utilisateur en playtest (scénario multi-PNJ, 4 personnages présents) : un PNJ
interjectant après un échange anodin (le joueur évoquait ce qu'il emportait dans la jungle) a
répondu par une tirade moralisatrice de deux paragraphes (métaphores, mise en garde, conseil de
vie), au lieu d'une réaction courte. `INTERJECTION_RULE` existait déjà et l'interdisait en
substance ("Default to one short line or gesture"), mais rien n'y nommait explicitement le cas
"donner une leçon/un conseil" — seule la gravité de l'échange (menace, violence, révélation)
était cadrée par un exemple.

## Ce qui a été touché
- `ChatPromptBuilder.INTERJECTION_RULE` : ajout de "never a lesson, a speech, or advice, no
  matter how relevant the topic feels to your character" à la règle générale, et d'un troisième
  couple Wrong/Right (Elena/le joueur discutent d'équipement, rien d'urgent) qui colle au cas
  réel observé. Delta présenté et validé par l'utilisateur avant écriture (règle CLAUDE.md
  globale sur les modifications de prompt).

## Résultat
Tests du module `chat` verts (`mvn -pl chat -am test`, `ChatPromptBuilderTest` inchangé côté
assertions car il ne teste que la présence du marqueur "This is an interjection", pas le texte
complet). Reste à confirmer en playtest que la tirade ne se reproduit plus sur ce type
d'échange.
