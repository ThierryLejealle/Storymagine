# 2026-10-31 - Interjection : chance totale partagée entre les candidats

## Évolution demandée
Avec un actif et quatre presents, tous les PNJ réagissaient à chaque tour (chance par défaut 0,5,
doublée et plafonnée à 1 pour un present). Demande : une chance dégressive selon le nombre de
personnages, avec un total d'environ 100 % de réactions par tour.

## Ce qui a été touché
- `SpeakerSelector.rollInterjectors` : le réglage devient une chance totale partagée entre les
  candidats (present = poids 2, passive = poids 1, actifs et principaux exclus). Plafond de 60 % par
  PNJ, proportionnel au réglage au-delà de 1.
- `RoleplayNarrator.INTERJECTION_CHANCE_DEFAULT` : 0,5 -> 1,0 (nouvelle signification).
- `GenerationSettings` (javadoc), `chat.html` (placeholder et infobulle du champ).
- `SpeakerSelectorTest` : tests réécrits en taux sur de nombreux tirages (partage, poids double,
  plafond, total proche de 1 pour 4 presents). `ChatServiceImplTest` : `alwaysInterject()` passe à 100.
- `chatscenarios/guide-redaction-chat.md` §4.3 : nouvelle règle et tableau des chances.
- Aucun prompt modifié.

## Résultat
Un actif et quatre presents : environ une réaction par tour (25 % chacun) au lieu de quatre sûres.
Une partie existante dont le réglage valait 0,5 est maintenant deux fois plus calme.
