# 2026-07-16 09h20 - Fix : le repli aléatoire de SpeakerSelector ignorait le réglage 💬

## 1. Demande

Retour direct de l'utilisateur en jouant : "BUg : j'ai décoché le bouton qui fait que les
personnages ne sont actifs que visés par leur nom et d'autre personnages ont réagis". Clarifié via
question : le PNJ dont le 💬 avait été décoché a lui-même réagi sans être visé par son nom.

## 2. Diagnostic

`SpeakerSelector.select(cast, presentIds, playerMessage, random)` — utilisé quand le message du
joueur ne nomme personne — piochait 1 ou 2 PNJ au hasard parmi **tous les PNJ présents**, sans
jamais consulter `interjectingIds`. Le bouton 💬 (tooltip : "Ne réagit que si visé par son nom")
ne protégeait donc que la moitié des cas : il empêchait bien un PNJ décoché de réagir en
interjection quand un AUTRE PNJ était visé (`rollInterjectors`, qui respectait déjà correctement ce
réglage), mais pas d'être pioché comme PRIMARY quand personne n'était nommé du tout — le cas
précis que l'utilisateur a rencontré.

## 3. Ce qui a été touché

`SpeakerSelector.select` : nouveau paramètre `Set<String> interjectingIds`. Le repli aléatoire ne
pioche désormais que parmi les PNJ présents ET éligibles à l'interjection ; si aucun n'est éligible
(tout le monde a décoché son 💬), repli sur tous les PNJ présents quand même — le joueur doit
toujours obtenir une réponse.

`ChatServiceImpl.resolveSpeakers` : passe `session.interjectingNpcIds()` au nouvel appel.

Tests (`SpeakerSelectorTest`) : les 9 appels existants à `select(...)` mis à jour avec le nouveau
paramètre (réglé sur "tout le monde éligible" pour préserver leur intention d'origine). Deux
nouveaux tests :
- `randomFallbackNeverPicksAnNpcThatOptedOutOfInterjecting` — 20 seeds différentes, jamais le PNJ
  décoché.
- `randomFallbackFallsBackToEveryPresentNpcWhenNoneAreInterjectionEligible` — cas limite où tout le
  monde a décoché, quelqu'un doit quand même répondre.

`chatscenarios/rules.md` (§4.2) mis à jour pour documenter le comportement corrigé.

## 4. Résultat

`mvn -pl chat -am clean test` : 192 tests (+2), tous verts.
