# 2026-07-14 12h45 - Nouveau chatscenario : "Carnet bleu" (romance shojo, personnage féminin)

## Demande

"Écris-moi une autre histoire dans un style Shojo ? Le personnage est féminin et l'histoire est
axée pour un joueur masculin." Cadrage validé via questions : romance de type scolaire mais avec
des personnages MAJEURS (université, pas lycée) ; structure en actes progressifs comme
`temple-noir-actes`.

## Ce qui a été touché

- `chatscenarios/carnet-bleu/character.txt` (nouveau) — Hina Asakura, 20 ans, étudiante en arts
  plastiques, réservée mais pas fragile ; explicitement présentée comme étudiante d'université,
  aucune ambiguïté sur l'âge.
- `chatscenarios/carnet-bleu/scenario.txt` (nouveau) — 6 actes principaux × 4 sous-actes (24 au
  total), fil rouge sur un carnet de croquis anonyme partagé en bibliothèque universitaire (motif
  "identité secrète" classique shojo : le joueur et Hina correspondent anonymement par dessins/
  mots interposés avant de se rencontrer en vrai sans faire le lien), motif saisonnier des
  cerisiers pour marquer le temps qui passe (bourgeon → pleine fleur → chute des pétales, sur un
  an). Arc classique : découverte → rencontre → rapprochement → doute/malentendu → révélation de
  l'identité → résolution.
- Vérifié via un test temporaire (chargement réel du scénario, comptage actes/beats — supprimé
  après coup, même méthode que pour `temple-noir-actes`) : 24 feuilles chargées, numérotation
  1.1–6.4 correcte, beats d'ouverture d'acte qui ne se déclenchent qu'une fois par branche (2 sur
  chaque `X.1`, 1 sur les suivants).

## Résultat

`mvn -pl chat test` au vert. Nouveau chatscenario jouable via `chat.bat` (choix "carnet-bleu") ou
testable statiquement via `test-scenario.bat`.
