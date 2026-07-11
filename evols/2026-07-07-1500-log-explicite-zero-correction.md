# 2026-07-07 15h00 - Log explicite "0 correction(s)" pour les correcteurs

## Demande
Dans les logs de WriteWorkflow, quand un correcteur (DeusInMachina, Naturality, Style,
Proofreader) ne trouve aucune correction, la ligne de log n'affichait rien (note = null),
ce qui rendait ambigu "0 correction" vs "champ non renseigné". Les correcteurs qui trouvaient
des corrections affichaient bien le compte, créant une impression d'incohérence entre correcteurs
alors que le comportement était en réalité uniforme (tous suivaient le même pattern
`isEmpty() ? null : "-> N correction(s)"`).

Demande utilisateur : afficher explicitement "0 correction(s)" au lieu de rien.

## Ce qui a été touché
`redacteur/src/main/java/storymagine/redacteur/coeur/domaine/orchestrator/write/WriteWorkflow.java`
Dans `applyCorrectorsPhase` : suppression du ternaire `isEmpty() ? null : ...` pour les
4 appels `log.step(...)` de DeusInMachinaCorrector, NaturalityCorrector, StyleCorrector et
ProofreaderCorrector (première passe + passes de relance), remplacé par un affichage
inconditionnel `"-> N correction(s)"`.

Non touché volontairement : `RepetitionFilterOutput` (ligne ~249), qui suit le même pattern
mais concerne un concept différent (filtrage de répétitions, pas des corrections) et n'était
pas dans le périmètre de la demande.

## Résultat
Les logs affichent désormais systématiquement le nombre de corrections par correcteur,
y compris `-> 0 correction(s)`, pour la passe initiale et pour chaque passe de relance.
Compilation vérifiée (`mvn -pl redacteur -am compile`) : OK.
