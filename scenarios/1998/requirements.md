<!--
  Vérifications globales. Injectées dans le prompt SequenceChecker pour chaque séquence
  (en plus des requirements déclarés dans chap_N.yaml).
  Format : liste avec tirets, un critère vérifiable par ligne.
  Syntaxe "contrainte | check" : une seule valeur = utilisée pour les deux ;
  "| check" = check seul ; "contrainte |" = contrainte seule.

  Exemple :
    - Le personnage principal est-il actif ou réactif ? (Cohérent avec son arc ?)
    - Le lieu est-il présent comme personnage (météo, ambiance, bruit) ou juste comme décor ?
    - Les dialogues comportent-ils des sous-entendus lisibles pour le lecteur mais opaques pour un personnage ?
    - Le temps qui passe est-il rendu concret (lumière, fatigue, faim) ?
-->
- ne pas utiliser les détails physiques des personnes de manière littérale en reprennant directement la fiche de personnage. Par exemple : c'est ok de mentionner la couleur des yeux et cheveux mais pas de recopier strictement la description du personnage de manière artificielle.
- | ne critique pas la vraissemblance de l'histoire ou des transitions de séquences un peu rapide : c'est une histoire vraie.