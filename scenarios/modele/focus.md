<!--
  Tags de focus et groupes de focus. Injectés dans le prompt Writer + Planner.

  DEUX FORMATS :

  1. TAG SIMPLE
     [NOM_TAG] suivi de sa description sur plusieurs lignes.
     Déclaré via focus: ["NOM_TAG"] dans une séquence ou dans defaults:.

     Exemple :
       [PEUR]
       Montrer la peur par les sensations physiques, jamais par les mots "peur" ou "terreur".
       Souffle court, mains qui bougent, regard qui scanne sans s'arrêter.
       Ce que le corps fait quand le cerveau ne décide plus.

  2. GROUPE DE FOCUS (chapitres loop/foreach uniquement)
     ## NOM_GROUPE
     recurrence: occasional | frequent | systematic
     pick: N  |  N-M  |  all
     condition: "..." (optionnel)
     - élément 1
     - élément 2

     recurrence :
       occasional  -> rarement, seulement si narrativement pertinent
       frequent    -> souvent, plusieurs fois par chapitre
       systematic  -> toujours dès que la condition est vraie

     pick :
       N     -> exactement N éléments tirés au sort
       N-M   -> entre N et M éléments
       all   -> tous les éléments
-->
