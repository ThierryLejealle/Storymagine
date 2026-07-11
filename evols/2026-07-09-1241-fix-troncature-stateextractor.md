# 2026-07-09 12h41 - Fix troncature agressive StateExtractor

## Demande

Analyse des `[WARN]` du run `scenarios/1998/generated/2026-07-09_01h43/master-log.txt` pour
repérer les troncatures trop agressives, dans un contexte où le modèle tourne avec un
contexte 64k largement sous-utilisé.

## Constat

Sur ce run, 4 types de WARN apparaissent :
- `SequenceStateExtractor: troncature — sequenceText` : 17 occurrences sur 22 appels (77%)
- `StyleCorrector: seuil encore depasse apres relance` : dépassement du seuil de corrections/mot
  après 3 passes (qualité, pas lié au contexte)
- `StyleCorrector`/`DeusInMachinaCorrector`: `replace miss` : citation LLM ne correspondant pas
  au texte exact (fiabilité du corrector, pas lié au contexte)
- `Sequence : note eliminatoire franchie` : relance forcée malgré une moyenne suffisante
  (logique de scoring, pas lié au contexte)

Seul le premier est directement lié au dimensionnement du contexte. `StateExtractor.java`
tronquait `sequenceText` à une constante fixe `MAX_TEXT_CHARS = 1200` caractères (~200 mots),
alors que les séquences réellement écrites font 300 à 1400 mots. Résultat : l'extracteur ne
voyait quasiment jamais que le tout début de la séquence, quel que soit le contexte réel
configuré (32k par défaut, 64k dans ce run) — un changement d'état survenant après le début
du texte pouvait donc être manqué.

Tous les autres agents qui reçoivent un texte de séquence complet (`CheckCritic`,
`PlanFidelityCritic`, `StyleCorrector`, `NaturalityCorrector`, `ProofreaderCorrector`)
dimensionnent déjà leur budget dynamiquement via `llm.contextWindow() * 4 / N`.
`StateExtractor` était la seule exception avec une valeur fixe — vraisemblablement un oubli
lors de l'introduction du dimensionnement dynamique dans les autres agents.

## Ce qui a été touché

- `redacteur/.../agent/sequence/stateextractor/StateExtractor.java` : suppression de la
  constante `MAX_TEXT_CHARS = 1200`, remplacée par un budget dynamique
  `llm.contextWindow() * 4 * 60 / 100` (même ratio que `NaturalityCorrector`), calculé à
  chaque appel de `call()`.

## Résultat

Avec un contexte configuré à 64k, le budget passe de 1200 caractères fixes à environ
157 000 caractères — largement suffisant pour ne plus jamais tronquer une séquence réelle
(max observé ~1400 mots, ~9000 caractères). Le budget reste proportionnel au contexte réel du
modèle utilisé, donc se réduit automatiquement si un modèle à plus petit contexte est
sélectionné, au lieu d'échouer silencieusement à charger tout le texte.

Compilation vérifiée (`mvn -pl redacteur -am compile`) : OK.
