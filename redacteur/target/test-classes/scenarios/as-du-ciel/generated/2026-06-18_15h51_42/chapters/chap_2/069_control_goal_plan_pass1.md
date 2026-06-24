# SYSTEM PROMPT

Tu évalues si un PLAN DE CHAPITRE remplit son objectif narratif spécifique.
Ne juge pas la qualité littéraire, ni la cohérence globale du roman.
Uniquement : le plan avance-t-il clairement et concrètement vers l'objectif narratif ?

Échelle de notation :
10 = objectif pleinement couvert
 9 = excellent — objectif très bien couvert, quelques légères imperfections
 8 = très bien — objectif couvert, quelques séquences à affiner
 7 = bien — objectif couvert mais quelques séquences peuvent mieux le servir
 6 = correct — plusieurs séquences ne servent pas assez l'objectif
 5 = insuffisant — l'objectif est traité de façon trop superficielle
 4 = plusieurs lacunes — l'objectif est secondaire dans le plan
 3 = mauvais — l'objectif n'est qu'en partie adressé
 2 = très mauvais — l'objectif est absent du plan
 1 = inutilisable — à replanifier intégralement

Procède dans cet ordre :
1. Analyse le plan entier.
2. Note tous les défauts et axes d'amélioration.
3. Détermine la note en fonction de la qualité globale.
4. Liste en sortie défauts et axes d'amélioration trouvés.

Format obligatoire :
PROBLEME: [défaut ou axe d'amélioration]
PROBLEME: [défaut ou axe d'amélioration suivant]
(une ligne PROBLEME: par défaut ou axe d'amélioration réellement constaté — ne pas en inventer)
Si score = 10 : aucune ligne PROBLEME:
SCORE: N  (entier 1-10)
En français.

---

# USER PROMPT

### Objectif narratif de ce chapitre
Montrer deux couleurs opposées de la guerre : la tension asymétrique de l'escorte (protéger un appareil plus faible) et l'absurde ordinaire de la détente entre pilotes. Les deux séquences doivent résonner l'une avec l'autre — le silence du jeu de cartes répond à la concentration muette du vol d'escorte.


### Objectif global du roman (contexte)
# L'As du Ciel

Juin 1944. Pierre Moreau, 24 ans, pilote de la FAFL (Forces Aériennes Françaises Libres),
vole sur Spitfire depuis la base de Thorney Island, sur la Manche. Pendant sept jours il
combat, survit, aime ses camarades en silence. Le huitième jour il ne rentre pas.

Ce n'est pas l'histoire d'un héros. C'est l'histoire d'un homme ordinaire dans
un monde extraordinairement violent, qui fait son travail aussi bien qu'il peut,
et que la guerre finit par briser comme elle brise tout le monde — à son heure.

## Ton et style

Sobre et précis. Phrases courtes pour l'action, longues pour la contemplation.
Le ciel est un personnage à part entière : lumière, altitude, silence entre les nuages.
Les émotions ne sont jamais nommées — elles passent dans les gestes, les silences, les corps.
Pas de glorification de la guerre. Pas de pathos facile. La mort est une chose ordinaire
et terrible à la fois.

S'inspirer de : Antoine de Saint-Exupéry (Vol de nuit), Romain Gary (La Promesse de l'aube),
Sebastian Faulks (Birdsong pour la retenue émotionnelle).

## Public cible

Adultes. Amateurs de récits de guerre intimistes et de littérature française classique.
Pas de lecteurs de techno-thrillers militaires — ici, le Spitfire est un outil de mort
et un objet de beauté, pas une fiche technique.

## Longueur cible

~15 000 mots. Cinq chapitres. Dense et court, chaque phrase doit peser.


### Plan à évaluer
[
  {
    "sequence": 1,
    "beats": [
      "Pierre quitte le tarmac et s'installe dans son Spitfire Mk IX.",
      "Il reçoit les ordres d'escorte : accompagner un Hurricane RAF endommagé jusqu'à la côte.",
      "Le moteur du Hurricane peine à maintenir une vitesse constante, sa trajectoire est hésitante.",
      "Pierre maintient une vigilance maximale en surveillant chaque quadrant autour de l'appareil plus lent.",
      "Il ressent le besoin physique d'ouvrir les ailes et de manœuvrer agressivement pour se positionner idéalement, mais il doit rester proche du Hurricane.",
      "La mission dure trente minutes intenses, exigeant une concentration absolue sur la protection plutôt que sur l'attaque.",
      "Le duo atteint finalement le point désigné près des côtes avant que les conditions ne changent."
    ],
    "sensoriels": "Son rauque et régulier du moteur Merlin de Spitfire contrastant avec les bruits plus saccadés du Hurricane ; la sensation vibratoire puissante sous les pieds dans le cockpit ; l'odeur d'essence chaude mélangée au sel marin.",
    "ton_et_rythme": "Rythmé par des actions concentrées et répétitives, ton sérieux et vigilant."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre entre dans le mess où l'ambiance est chaleureuse malgré la fraîcheur extérieure.",
      "Jules Meca déploie un jeu de cartes usé sur une table en bois sombre, ses doigts bougent avec précision.",
      "Henri Leclair lance son coup du jour : il bluffant avec une assurance presque excessive au milieu des joueurs.",
      "Pierre ne participe pas activement au jeu ; il observe les interactions et les micro-expressions de chacun.",
      "La radio commence à diffuser doucement une mélodie jazzy, remplissant le silence ambiant sans forcer la conversation.",
      "Un bref échange non verbal entre Pierre et Jules : un regard partagé qui valide l'état du Grey Ghost, puis retour au jeu.",
      "Les personnages jouent chacun selon leur personnalité : Henri avec audace, Jules avec rigueur silencieuse, Pierre par observation attentive."
    ],
    "sensoriels": "Le son feutré des cartes frottant sur le bois ; la chaleur d'une petite lampe de table à l'intérieur du mess ; les murmures bas et réguliers autour de la radio.",
    "ton_et_rythme": "Rythme lent, contemplatif, ton calme mais chargé de sous-entendus non exprimés."
  }
]



Analyse si ce plan remplit l'objectif narratif, puis conclus avec tes PROBLEME: et SCORE: N.

---

# RESPONSE

Le plan est extrêmement solide et atteint brillamment les objectifs narratifs fixés. Les deux séquences sont bien délimitées, respectent le ton sobre et précis demandé, et surtout, elles créent la résonance demandée entre l'action extérieure (l'escorte) et l'intimité sociale (le mess).

La séquence 1 illustre parfaitement la tension asymétrique par la concentration physique de Pierre. La séquence 2 réussit à ancrer le quotidien dans l'ordinaire en utilisant des détails sensoriels subtils (son feutré, murmures bas) et en transformant Pierre en observateur attentif plutôt qu'en participant actif. Le lien entre les deux est bien établi par la référence non verbale au "Grey Ghost".

Il n'y a pas de lacunes majeures ; le plan fonctionne très bien tel quel.

PROBLEME: Assurer que l'observation dans la séquence 2 ne soit pas trop générique, mais qu'elle reflète spécifiquement l'état d'esprit concentré du pilote (par exemple, en observant un détail précis chez Jules qui rappelle une vigilance similaire à celle requise en vol).
SCORE: 9