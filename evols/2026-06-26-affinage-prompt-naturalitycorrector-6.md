# 2026-06-26 - Affinage prompt NaturalityCorrector

## Evolution demandee

Appliquer 3 ameliorations au prompt NaturalityCorrector suite a analyse critique :
1. Contraindre la suggestion (modifier le moins de mots possible)
2. Ajouter la categorie "ambiance expliquee"
3. Ajouter des exemples supplementaires + un faux positif acceptable

## Ce qui a ete touche

### NaturalityCorrector — prompt uniquement

REGLES :
- Ajout d un exemple de faux positif acceptable :
  "Elle sentit le monde se fissurer sous ses pieds." — image poetique ancree dans
  le ressenti du personnage, pas une analyse du narrateur

Cherche en priorite :
- "phrases de synthese" : ajout d un second exemple
  "Une complicite tacite s etait installee entre eux."
- "langage conceptuel" : ajout d un troisieme exemple
  "il gerait la situation avec detachement"
- Ajout de la categorie "ambiance expliquee" avec 4 exemples :
  "Une atmosphere etrange regnait.", "La tension etait palpable.",
  "Il regnait une impression de menace.", "Une energie nouvelle envahissait la piece."

FORMAT :
- JUSTE : "reecriture plus naturelle" -> "reecriture qui supprime l effet artificiel
  en modifiant le moins de mots possible"
- ATTENTION : ajout "conserve le sens, le rythme et les informations — ne reecris
  que ce qui pose probleme"

## Resultat

Compilation OK. Aucune modification du parser ni des classes Java — prompt seul.