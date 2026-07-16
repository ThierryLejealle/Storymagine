# 2026-07-15 23h18 - Fix : ambiguïté de présence faisait boucler le modèle sur l'acte 1

## 1. Constat

Log réel examiné (`002_RoleplayNarrator_1.md`, appel Sheera) : **54 secondes de réflexion, réponse
vide (`Réponse : ~0 tok`)**. Le modèle s'est redemandé une dizaine de fois s'il devait vraiment
jouer Sheera, a rédigé quasiment la même réplique 5-6 fois sans jamais se satisfaire ("Wait...
Wait!... *Wait*..."), et a épuisé tout le budget de génération (2500 tokens) en pure réflexion,
coupé en plein milieu d'une phrase.

## 2. Cause racine

Le joueur n'avait mentionné personne ("Bonjour euh... Mesdames ?") — `SpeakerSelector` a donc
choisi 2 PNJ au hasard parmi les 3 présentes, et Sheera en faisait partie. Or l'acte 1.1 ("Premiers
regards") ne montrait QUE Laina dans ses lignes d'ouverture visibles au joueur — les 3 amazones
sont pourtant mécaniquement présentes dès le tour 1 (présence par défaut, pas de mise en scène
progressive côté moteur). Le modèle a donc dû deviner si Sheera était même dans la pièce, et cette
ambiguïté l'a fait boucler indéfiniment plutôt que de répondre.

## 3. Correctif

`chatscenarios/quete-des-amazones/scenario.txt`, acte 1.1 :
- Les lignes `[...]` mentionnent désormais explicitement les trois silhouettes présentes dès
  l'entrée dans la salle (Laina au premier plan, Mina et Sheera "en retrait, près de la cheminée").
- "Point de départ" précise que Mina et Sheera peuvent réagir si sollicitées mais n'y sont pas
  obligées tant que Laina mène l'échange — donne au modèle une réponse claire à "dois-je parler ?"
  sans qu'il ait à la déduire.
- Acte 1.2 ("Mina et Sheera") légèrement retouché pour rester cohérent (elles se "précisent" en
  s'approchant plutôt que d'apparaître soudainement).

## 4. Leçon générale pour les futurs scénarios multi-PNJ

Quand tous les PNJ démarrent présents (cas par défaut), le tout premier acte doit les mentionner
TOUS dans ses lignes `[...]`, même si la mise en scène narrative se concentre sur un seul — sinon
un PNJ choisi par le repli aléatoire avant sa "scène d'introduction" prévue peut ne pas savoir s'il
est censé être dans la pièce, et un petit modèle peut boucler sur cette ambiguïté au lieu de
répondre. Ajouté au `chatscenarios/rules.md` (§2.4) comme rappel.

## 5. Résultat

Rechargé et vérifié par script : toujours 26 actes, lignes d'ouverture correctement mises à jour.
Pas de régression de structure. À reconfirmer en jeu (relancer une nouvelle conversation — la
session en cours, si elle existe, garde l'ancien texte tant qu'elle n'est pas rechargée via ⟳ ou
redémarrée).
