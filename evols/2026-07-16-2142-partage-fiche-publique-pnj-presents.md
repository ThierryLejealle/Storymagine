# 2026-07-16 21h42 - Les PNJ présents partagent leur fiche publique entre eux

## 1. Demande

Repris d'un point mis de côté plus tôt dans la journée : "je pensait partager la partie publique"
— confirmation de scinder ce que sait un PNJ des autres présents, entre publique (partagé) et
secret (jamais partagé).

## 2. Diagnostic

`scene.otherPresent()` (voir `ChatServiceImpl.sceneFor`) ne contient JAMAIS le joueur — uniquement
d'autres PNJ, toujours des coéquipiers déjà établis dès la prémisse du scénario (ex. "Laina, Mina,
Sheera se connaissent bien sûr"). Le prompt disait pourtant à chaque PNJ "tu ne sais rien d'eux, à
part leur nom" — cohérent pour une toute première rencontre, mais bizarre pour une équipe qui
voyage ensemble depuis plusieurs chapitres. Pas de risque de fuite de secret puisque cette section
ne concernait déjà que le nom : le changement consiste à y ajouter la fiche PUBLIQUE (jamais le
`# SECRET`, qui reste strictement réservé au prompt de son propriétaire).

## 3. Delta du prompt (validé avant écriture, voir CLAUDE.md)

`ChatPromptBuilder.build()`, bloc "ALSO PRESENT" :
- Avant : `ALSO PRESENT IN THE SCENE (names only — you know nothing else about them):` suivi du nom
  seul de chaque autre PNJ présent.
- Après : `ALSO PRESENT IN THE SCENE — what you know about them (never anything from a private note
  that isn't yours):` suivi de `- Nom: <fiche publique>` pour chacun.

`OTHER_NPCS_RULE` : "you know only their names, nothing else about them" devient "that's the only
thing you know about them, nothing more" (renvoie au bloc ci-dessus au lieu d'affirmer l'ignorance
totale) — le reste de la règle (ne jamais parler/agir à la place d'un autre PNJ présent) inchangé.

## 4. Ce qui a été touché

`ChatPromptBuilder.java` : les deux blocs de texte ci-dessus, javadoc de `build()` mise à jour.
`chatscenarios/rules.md` (§3) : règle "fiche publique partagée / secret jamais partagé" documentée.

## 5. Tests

`ChatPromptBuilderTest.otherPresentNpcsShareTheirPublicSheetButNeverTheirSecret` (renommé et
réécrit depuis `otherPresentNpcsAreListedByNameOnlyNeverTheirSheet`) — vérifie que la fiche
publique d'un autre PNJ présent apparaît dans le prompt, et que son `# SECRET` n'y apparaît jamais.
`mvn -pl chat -am clean test` : 202 tests, tous verts (aucun test en plus/en moins, un test
existant réécrit pour matcher le nouveau comportement).

## 6. Limite connue

Partager la fiche publique de CHAQUE PNJ présent à CHAQUE tour augmente la taille du prompt
proportionnellement au nombre de PNJ en scène — non problématique à 3-4 PNJ (le format actuel des
scénarios), à surveiller si un scénario avec un cast plus large est créé un jour.

Non testé en conditions réelles (nécessite Ollama + modèle chargé) — à vérifier par l'utilisateur.
