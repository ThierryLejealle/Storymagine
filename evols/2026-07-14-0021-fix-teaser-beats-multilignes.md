# 2026-07-14 00h21 - Fix Teaser : beats `[...]` multi-lignes acceptés

## Demande

L'utilisateur a écrit un nouveau scénario avec une accroche `[...]` répartie sur plusieurs lignes
("la phrase d'accroche peut faire plusieurs lignes et démarrer par [ et finir par ] ?") — le beat
n'apparaissait jamais dans la session. Diagnostic confirmé : la regex de `Teaser` exigeait que
CHAQUE ligne soit intégralement encadrée par `[` et `]` (`^[ \t]*\[([^\[\]\n]+)][ \t]*$`,
`[^\n]` excluant explicitement les retours à la ligne) — un bloc réparti sur plusieurs lignes ne
matchait ni sur sa première ni sur sa dernière ligne, et disparaissait silencieusement, sans
erreur ni avertissement.

## Ce qui a été touché

- `chat/coeur/domaine/scenario/Teaser.java` : nouvelle regex
  `^[ \t]*\[([^\[]*?)][ \t]*$` (multiline, non-greedy, contenu autorisé à contenir des retours à
  la ligne mais pas de `[` imbriqué) — le `[` d'ouverture doit rester le premier caractère non
  blanc de sa ligne, le `]` de fermeture le dernier de la sienne, mais les deux peuvent être sur
  des lignes différentes. Les lignes internes sont nettoyées (strip) et jointes par un espace pour
  ne former qu'une seule ligne de narration. Vérifié que ça n'ouvre pas la porte à une capture
  accidentelle de l'occurrence inline `[NEXT ACT]` dans "Quand écrire [NEXT ACT] : ..." (le `[` y
  est précédé de texte sur la même ligne, donc jamais en début de ligne).
- `TeaserTest.java` : 3 nouveaux tests (bloc multi-lignes seul, bloc multi-lignes suivi d'un bloc
  simple, bloc multi-lignes cohabitant avec une occurrence inline de `[NEXT ACT]` qui doit rester
  ignorée).

## Résultat

`mvn -pl chat test` au vert (8/8 sur `TeaserTest`, 94/94 côté chat). Un auteur peut désormais
écrire une accroche `[...]` sur plusieurs lignes wrappées, comme le reste du texte du fichier.
