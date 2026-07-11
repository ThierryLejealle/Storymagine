# CharacterCritic

## Rôle
Vérifie la cohérence des personnages dans un chapitre terminé :
1. Contre la fiche personnage (section GÉNÉRAL)
2. Ruptures d'état internes au chapitre

## Ce qu'il détecte
- Personnage qui agit/parle/réagit de façon incompatible avec sa fiche
- Blessure qui disparaît sans explication
- Objet posé qui réapparaît en main
- Tenue qui change sans transition narrative
- Position physique impossible

## Ce qu'il ignore
- Petites imprécisions stylistiques
- Ellipses narratives normales
- Tout ce qui n'impacterait pas la crédibilité pour un lecteur attentif

## Format de sortie
```
INCOHERENCE: [personnage — type (fiche / état) — description précise]
SCORE: N
```
Si aucune incohérence : `SCORE: 10` uniquement.

## Input
- `text` : texte complet du chapitre (non tronqué)
- `charactersText` : sections GÉNÉRAL des fiches personnage (pré-formatées par le service)

## Source Redacteur
`story.context.CharacterCheckerContext`
