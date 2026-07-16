# 2026-07-15 23h12 - Fix : introduction manquante sur un scénario jamais joué

## 1. Demande

"Stop : j'ai pas d'introduction sur le scénario des amazones." — en testant `quete-des-amazones`
pour la première fois, aucune ligne narrative d'ouverture ne s'affichait.

## 2. Diagnostic

Bug reel, pre-existant (pas introduit aujourd'hui), revele par le premier vrai essai de ce nouveau
scenario. `ChatServiceImpl.openSession(reset=false)` (le chemin "continuer", choisi des qu'on ne
tape pas explicitement "n" au prompt CLI — donc y compris en appuyant juste sur Entree) appelle
`ChatFileStorageAdapter.loadSession` → `readSessionFiles`. Si `history.md` n'existe pas encore
(scenario jamais joue), cette methode renvoyait une session creuse (`turns = List.of()`) — les
lignes `[...]` de l'acte 1 ne sont generees QUE par `ChatSession.fresh(scenario)`, jamais rejouees
ici. Resultat : premiere partie sur un scenario neuf + choix "continuer" (le reflexe naturel) =
ecran de chat vide, sans intro.

Verifie que le scenario lui-meme etait correct (charge par script direct : 26 actes, 2 lignes
d'ouverture bien presentes) — le bug etait uniquement dans le chemin de chargement de session, pas
dans le contenu du scenario.

## 3. Ce qui a ete touche

`ChatFileStorageAdapter.readSessionFiles(...)` : si `history.md` est absent, delegue directement a
`ChatSession.fresh(scenario)` au lieu de construire une session creuse — meme introduction que le
chemin "reset explicite" obtient deja.

## 4. Tests

- `loadSessionWithNoHistoryYetIsEmptyForAnActLessScenarioWithNoBracketedBeats` (renomme, portee
  clarifiee : le cas ou une session vide est correcte parce que le scenario n'a ni actes ni lignes
  `[...]`, pas parce que "pas d'historique = vide" est la regle).
- `loadSessionWithNoHistoryYetStillGetsTheOpeningActsBeats` (nouveau) : verifie directement la
  regression — un scenario avec un acte contenant une ligne `[...]`, jamais joue, doit afficher
  cette ligne des le premier chargement.

## 5. Resultat

`mvn -pl chat -am test` : 187 tests (+1), tous verts. Verifie sur disque qu'aucune session cassee
n'avait ete persistee pour `quete-des-amazones` (pas de `history.md`) — le fix suffit, rien a
nettoyer, un simple rechargement de la page ou relance de `chat.bat` suffit.
