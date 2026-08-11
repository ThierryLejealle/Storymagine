# 2026-08-06 - Scénario en plusieurs fichiers (scenario-N.txt), rétrocompatible

## Évolution demandée
Le scénario "temple-ilyria" (issu du fix de transition de la veille) fait plus de 1280 lignes en un
seul `scenario.txt`, difficile à faire évoluer chapitre par chapitre. L'utilisateur a demandé un
formalisme permettant d'agréger plusieurs fichiers scénario tout en restant rétrocompatible avec les
scénarios existants à un seul fichier.

Contraintes posées par l'utilisateur :
- `scenario.txt` reste le fichier principal ; `scenario-1.txt`, `scenario-2.txt`, ... le complètent,
  dans l'ordre numérique, tant qu'ils existent.
- Chaque fichier de continuation doit commencer à un titre `#` de premier niveau — jamais couper en
  plein milieu d'un `#`.
- L'en-tête commun de `scenario.txt` (ligne `Joueur :`, bloc `#SCENARIO`) reste valable pour toute la
  suite.

## Ce qui a été touché
- `chat/src/main/java/storymagine/chat/infra/ChatFileStorageAdapter.java` : `loadScenario` concatène
  désormais `scenario.txt` avec `scenario-1.txt`, `scenario-2.txt`, ... (numéros consécutifs,
  s'arrête au premier numéro manquant) avant de passer le tout à `ScenarioOutlineParser`, qui reste
  inchangé — il ne voit qu'un seul bloc de texte, la provenance multi-fichiers lui est transparente.
  Chaque fichier de continuation doit commencer sur un titre `#` de premier niveau (`TOP_LEVEL_HEADING`),
  sinon `IllegalStateException` explicite au chargement. `loadCast` exclut désormais aussi les
  `scenario-N.txt` du Cast (ils ressemblent à des fiches PNJ car ils commencent par `# Titre`).
- `chat/src/test/java/storymagine/chat/infra/ChatFileStorageAdapterTest.java` : 5 nouveaux tests
  (agrégation dans l'ordre, comportement inchangé sans fichier de continuation, arrêt au premier
  numéro manquant, rejet d'un fichier ne commençant pas par un titre de premier niveau, non-pollution
  du Cast).

## Résultat
Un scénario peut désormais être écrit et maintenu chapitre par chapitre dans des fichiers séparés,
sans rien changer côté code pour les scénarios existants à un seul fichier (aucun `scenario-1.txt` =
comportement strictement identique à avant). Tests chat au complet passés (suite verte).
