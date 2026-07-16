# 2026-07-15 23h23 - Reprise automatique quand RoleplayNarrator renvoie une réponse vide

## 1. Demande

Suite au diagnostic du log Sheera (boucle de réflexion de 54s, réponse vide) : "si la # REPONSE
est vide : je t'autorise à relancer une fois. Peut-être un bug du LLM ?"

## 2. Ce qui a été touché

`RoleplayNarrator.call()` : si `result.text()` est vide après le premier appel, un seul second
appel est tenté avec exactement le même prompt (l'échantillonnage n'est pas figé par une graine,
donc une deuxième tentative retombe rarement sur le même résultat vide). Si la deuxième tentative
est aussi vide, on abandonne — pas de boucle, pour ne jamais masquer un vrai problème persistant.

Portée : comme `RoleplayNarrator.call()` est le point unique utilisé par tous les PNJ d'un tour
(mention, repli aléatoire, interjection — voir `ChatServiceImpl.generateRepliesAndFinish`), la
reprise s'applique automatiquement partout, aucun autre fichier à toucher.

## 3. Tests

`RoleplayNarratorTest` (3 nouveaux, `SequencedModelCallPort` — un stub qui renvoie une réponse
différente à chaque appel successif) :
- `retriesOnceWhenTheFirstResponseComesBackBlank` — vide puis texte réel → le texte réel est
  utilisé, exactement 2 appels.
- `doesNotRetryWhenTheFirstResponseIsAlreadyUsable` — un premier texte non vide ne déclenche jamais
  de 2e appel (pas de gaspillage).
- `givesUpAfterOneRetryIfStillBlankRatherThanLoopingForever` — vide puis vide → résultat vide
  accepté tel quel, toujours exactement 2 appels (jamais plus).

## 4. Résultat

`mvn -pl chat -am test` : 190 tests (+3), tous verts. Documenté dans `chatscenarios/rules.md`
(§4.2) comme mécanisme de résilience, avec le rappel que des reprises fréquentes sur un scénario
donné signalent souvent une ambiguïté de mise en scène à corriger plutôt qu'un simple aléa.
