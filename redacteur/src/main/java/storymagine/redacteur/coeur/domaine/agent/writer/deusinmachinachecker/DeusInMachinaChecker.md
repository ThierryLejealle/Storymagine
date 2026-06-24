# DeusInMachinaChecker

## Rôle
Détecte les passages où la mécanique de fabrication est devenue visible dans la prose —
les "fuites" où le lecteur perçoit les instructions qui ont créé le texte.

## Les 5 types de fuites

| Type | Description | Exemple de fuite |
|------|-------------|-----------------|
| 1. Négation verbalisée | Consigne interdit X → texte mentionne l'absence de X | "Il n'y avait pas de nuage ce jour-là" (si consigne interdit les nuages) |
| 2. Fiche dans la bouche | Trait de personnage cité comme étiquette permanente | "Bertrand, taciturne comme toujours, garda le silence" |
| 3. Artefact de scénario | Mots du script de fabrication dans la fiction | "Dans cette scène, Pierre comprend que..." |
| 4. Liste narrativisée | ≥4 phrases séparées cochant chacune une case | "Pierre arriva. Il observa. Il déposa. Il chercha." |
| 5. Absence justifiée | Texte explique pourquoi qqch n'arrive pas (justification causée par une contrainte) | "Il n'y eut pas de combat — comme si la guerre avait décidé de souffler" |

## Règle préalable
Si une consigne de séquence est fournie dans `constraints`, le checker la lit en premier.
Un passage qui réalise fidèlement la consigne n'est PAS une fuite.

## Format de sortie
```
FUITE
- "citation exacte" → type [1-5] — explication

ou

OK
```

## Source Redacteur
`story.context.DeusInMachinaContext`
