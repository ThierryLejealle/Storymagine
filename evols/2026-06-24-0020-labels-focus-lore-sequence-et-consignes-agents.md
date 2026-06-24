# 2026-06-24 00h20 — Harmonisation labels focus/lore/personnages + consignes d'utilisation

## Description de l'évolution

Deux corrections liées à la lisibilité des prompts LLM :

1. **Labels séquence harmonisés** : dans `sequenceDescriptions()`, les tags `[Focus : ...]`,
   `[Lore : ...]` et `[Personnages : ...]` sont remplacés par les termes établis dans les agents :
   - `Éléments à utiliser (focus) : ...`
   - `Informations utiles (lore) : ...`
   - `Personnages présents : ...`

2. **Consignes d'utilisation** : dans les sections focus et lore du prompt utilisateur,
   une ligne d'instruction précède désormais le contenu pour que le LLM sache quoi en faire,
   adaptée à chaque agent :
   - **ChapterPlanner** : "Efforce-toi d'intégrer ces éléments dans le plan de chaque séquence concernée."
     / "N'hésite pas à piocher dans ces informations pour étoffer le plan de chaque séquence."
   - **Writer** : "Efforce-toi d'utiliser ces éléments dans l'histoire que tu vas rédiger."
     / "N'hésite pas à piocher dans ces informations pour étoffer la rédaction."

## Ce qui a été touché

| Fichier | Nature du changement |
|---------|----------------------|
| `ScenarioFormatters.java` | `sequenceDescriptions()` : remplacement des 3 labels entre crochets |
| `ChapterPlanner.java` | `buildUser()` : instruction préfixée avant focusText et loreText |
| `Writer.java` | `buildUser()` : instruction préfixée avant focusText et loreText |

## Résultat

- Compilation : OK
- Les labels des additions de séquence sont identiques à ceux des sections de chapitres
- Chaque agent reçoit une consigne explicite sur l'utilisation du focus et du lore
