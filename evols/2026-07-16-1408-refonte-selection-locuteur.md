# 2026-07-16 14h08 - Refonte de la sélection du PNJ qui répond (mention, continuité, interjection)

## 1. Demande

Retour direct en jouant : "je mentionne un NPC et un autre parle en premier. C'est ok qu'il parle,
c'est bien géré. Mais en premier alors que je parle à un autre c'est bizarre. D'ailleurs le
thinking du modele était gêné !" — diagnostic : quand le message du joueur contient deux noms (le
PNJ visé + un autre juste cité en passant), les deux étaient traités comme "principaux" et triés
par ordre alphabétique d'id, pas par l'ordre du texte — le PNJ juste mentionné pouvait donc
répondre AVANT celui réellement visé.

Demande complète, précisée après plusieurs allers-retours de clarification :
```
0. Seuls des personnages PRESENTS peuvent parler
1. Je mentionne au moins un nom. Tu fais parler TOUS les persos mentionnés dans le texte (limiter
   la recherche aux 5 premiers caracteres, sans accent ni majuscule) dans l'ordre d'apparition de
   leur nom
2. Sinon si UN seul perso a parlé avant, tu refais parler ce perso
3. Sinon tu en tires UN au hasard.
ENSUITE : chaque perso qui n'a pas parlé et qui n'est pas 'muté' a une chance de répondre aussi
selon la règle déjà en place. Si tu en tires plusieurs : mets un ordre aléatoire.
```

## 2. Ce qui a été touché

`SpeakerSelector.select(cast, presentIds, interjectingIds, playerMessage, previousRoundSpeakerIds,
random)` — nouveau paramètre `previousRoundSpeakerIds`, logique réécrite en 3 étapes :
1. **Mention** : tri par position de première apparition dans le message (plus alphabétique).
   Détection assouplie : accents/majuscules normalisés, seuls les 5 premiers caractères du nom
   doivent matcher à une frontière de mot (`Normalizer.normalize` + suppression des marques
   combinantes, préserve la longueur de chaîne pour les caractères latins accentués utilisés ici).
2. **Continuité** (nouveau) : si personne n'est nommé et qu'un seul PNJ a parlé au tour précédent,
   il reprend. Retombe sur l'étape 3 si ce PNJ n'est plus présent.
3. **Repli aléatoire** : un seul PNJ tiré (plus 2) parmi les présents éligibles à l'interjection
   (comportement du 2026-07-16 matin conservé), repli sur tous les présents si personne n'est
   éligible.

`SpeakerSelector.rollInterjectors(...)` — suppression du paramètre `playerMessage` (devenu inutile)
et de la restriction "un seul PNJ principal issu d'une mention explicite" : le tirage d'interjection
s'applique désormais quel que soit le nombre de PNJ principaux et quelle que soit la façon dont ils
ont été choisis (mention, continuité, ou tirage). Ordre du résultat désormais aléatoire (mélangé),
plus trié par id.

`ChatServiceImpl.resolveSpeakers` : calcule `previousRoundSpeakerIds` via un nouveau helper privé
`previousRoundSpeakerIds(session)` qui remonte les tours depuis juste avant le tour joueur courant,
s'arrête au tour joueur précédent (limite de tour), ignore les tours NARRATOR sans arrêter le
parcours.

`chatscenarios/rules.md` (§4.2, §4.3) réécrit pour documenter les 3 étapes et la nouvelle portée de
l'interjection.

## 3. Tests

`SpeakerSelectorTest` largement réécrit (22 tests, +6 net) : ordre par apparition dans le texte
(nouveau test dédié prouvant qu'un PNJ nommé en second dans le texte mais alphabétiquement premier
ne passe pas devant), continuité (nouveau, y compris le cas où le PNJ à continuer n'est plus
présent), repli à UN seul PNJ (adapté), interjection sans restriction de nombre de principaux
(nouveau), correspondance floue accents/majuscules/préfixe 5 caractères (nouveau).

`ChatServiceImplTest` : `withNoMentionUpToTwoPresentNpcsReplyInTheSameRound` (testait l'ancien
comportement garanti à 2) remplacé par
`withNoMentionOnlyOneNpcAnswersByDefault` + `withNoMentionBothPresentNpcsCanReplyViaInterjectionAtCertainChance`
(le second scénario n'est plus garanti sans un chance d'interjection explicite à 1.0, il est
désormais probabiliste par design). Nouveau
`withNoMentionTheNpcWhoAloneSpokeLastRoundContinuesFirst` pour la continuité.

`mvn -pl chat -am clean test` : 202 tests (+8), tous verts. `mvn compile` (racine) : aucune
régression cross-module.
