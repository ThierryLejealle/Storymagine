# 2026-11-07 - Le prompt du PNJ donne explicitement son nom

## Évolution demandée
Dire au PNJ qui il est, au lieu de compter sur l'en-tête `# Nom` de sa fiche et sur l'étiquette finale `Nom:`.
Les textes de scène, à la troisième personne et partagés par tous les PNJ, nomment d'autres personnages et leurs
actions : le modèle doit savoir lequel est « moi ». Modification validée avec son texte exact avant écriture.

## Ce qui a été touché
- `ChatPromptBuilder.build` : l'en-tête `YOUR CHARACTER:` devient `YOUR CHARACTER — you are <Nom>:`
  (`<Nom>` = `Npc.label()` : le nom déclaré par `# Nom`, ou l'identifiant du fichier à défaut). Aucune autre
  ligne du prompt ne change.
- Test `ChatPromptBuilderTest.systemStatesWhoTheSpeakerIs` (deux PNJ : le prompt de Marcus le nomme, pas Elena).
- Guide de rédaction : tableau du prompt (§ des sections envoyées).

## Résultat
Tests verts. Aucun essai avec un vrai modèle (règle du projet) : l'effet se jugera en jouant. Un PNJ sans ligne
`# Nom` est nommé par l'identifiant de son fichier (ex. `captain-ione`) : mieux vaut donner un `# Nom` à chaque fiche.
