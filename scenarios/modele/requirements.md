<!--
  Requirements globaux. Injectés dans le prompt SequenceChecker pour chaque séquence
  (en plus des requirements déclarés dans chap_N.yaml), et dans le Planner/Writer
  sous forme de contraintes.

  Une ligne = une règle, avec deux formulations possibles séparées par "|" :
    - "texte"              -> même texte utilisé comme contrainte (Planner/Writer) et comme check (Critics)
    - "contrainte | check" -> formulation différente pour chaque audience
    - "contrainte |"       -> contrainte seule, ignorée par les Critics
    - "| check"            -> check seul, ignoré par le Planner/Writer

  Sections ## PLAN / ## RÉDACTION (optionnelles) : portée par phase.
  Sans section, la règle s'applique aux deux phases.

  Exemple :
    ## PLAN
    - Le personnage A ne résout pas l'affaire par déduction — il trébuche sur la vérité.
    - L'antagoniste ne doit jamais être soupçonné explicitement avant le dernier tiers.

    ## RÉDACTION
    - Le personnage principal ne sourit jamais.
    - Interdiction d'écrire "il sentit que" ou "il réalisa que" — montrer par l'action. | Le personnage montre ses émotions par l'action, jamais par une phrase du type "il sentit que"
    - Les lieux ne sont jamais nommés — "la ville", "la rue du marché", jamais de toponyme réel.
-->

## PLAN

## RÉDACTION
