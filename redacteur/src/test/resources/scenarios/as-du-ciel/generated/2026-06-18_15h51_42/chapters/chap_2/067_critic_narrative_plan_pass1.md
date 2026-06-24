# SYSTEM PROMPT

Tu es un editeur narratif. Tu evalues tres soigneusement le PLAN d'un chapitre, pas le texte final.
Tu verifies point par point tous les elements et aspects du plan, en te focalisant exclusivement sur la progression de l'arc narratif : ton objectif est de lister tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif. Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
Si l'objectif de ce chapitre est fourni, tout element du plan qui en decoule directement n'est pas un defaut — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis attentivement le plan et l'objectif du chapitre et trouve tous les defauts meme mineurs par rapport a l'arc narratif.
2. Qualifie chaque point selon ces definitions :
   AMELIORATION: point qui pourrait etre affine ; la faiblesse, si elle existe, est quasi imperceptible et n'impacte pas l'arc.
   Exemple : La S1 pose bien l'atmosphere ; une image plus evocatrice renforcerait encore l'immersion sans changer l'arc.
   Si la faiblesse est plus marquee et affaiblit une sequence entiere ou brise un lien narratif entre sequences, sans contredire l'objectif, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : Le lien entre la tension du Debarquement (S1) et l'insomnie de Pierre (S4) est absent — la progression thematique de l'arc est interrompue.
   Si un element CONTREDIT DIRECTEMENT l'objectif du chapitre, meme si le reste du plan est bien construit, c'est un DEFAUT_MAJEUR.
   Exemple : La S3 montre Pierre s'integrant chaleureusement dans l'escadrille, ce qui contredit directement l'objectif : pas de chaleur, pas d'integration.
FORMAT STRICT :
AMELIORATION : avec une ligne par amelioration, ou [RIEN] si aucune.
DEFAUT_SIGNIFICATIF : avec une ligne par defaut significatif, ou [RIEN] si aucun.
DEFAUT_MAJEUR : avec une ligne par defaut majeur, ou [RIEN] si aucun.
Rien d'autre : ni texte avant ni texte apres ces trois lignes.
Exemple 1 - deux defauts significatifs, rien d'autre :
AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : Le heros n'a pas d'epee
DEFAUT_SIGNIFICATIF : l'ours est en peluche
DEFAUT_MAJEUR : [RIEN]
Exemple 2 - aucun probleme trouve (aucune amelioration, aucun defaut significatif, aucun defaut majeur) :
AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]
En francais.

---

# USER PROMPT

### Plan du chapitre
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

### Objectif de ce chapitre
Montrer deux couleurs opposées de la guerre : la tension asymétrique de l'escorte (protéger un appareil plus faible) et l'absurde ordinaire de la détente entre pilotes. Les deux séquences doivent résonner l'une avec l'autre — le silence du jeu de cartes répond à la concentration muette du vol d'escorte.


### Objectif du livre
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




Evalue le plan.

---

# RESPONSE

AMELIORATION : Assurer une transition plus fluide entre le stress physique de la S1 et l'ambiance du mess en S2, peut être fait par un détail sensoriel ou corporel qui persiste chez Pierre après son vol.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]