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
Installer Pierre comme un étranger dans un groupe soudé depuis des mois. Le lecteur doit ressentir son isolement : il observe, il ne participe pas encore, il ne comprend pas les codes tacites. Pas de chaleur, pas d'intégration.


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
      "Pierre arrive à Thorney Island et descend du transport militaire.",
      "Le son lointain des moteurs Merlin résonne sur le terrain humide.",
      "Il marche vers la zone d'alignement où les Spitfires sont stationnés dans la brume matinale.",
      "Les hélices immobiles des avions créent une mosaïque de métal gris et vert sous un ciel bas.",
      "Pierre pose son sac en toile sur le sol mou du tarmac.",
      "Il lève les yeux pour observer la texture uniforme du ciel qui peine à se dégager."
    ],
    "sensoriels": "L'air est froid, humide et saturé de brume. Le bruit des moteurs Merlin s'entend comme un grondement sourd et régulier au loin. La vue est dominée par les teintes grises et bleues du ciel et de l'herbe mouillée.",
    "ton_et_rythme": "Calme, contemplatif, lent."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre est conduit dans le bureau du Commandant Bertrand.",
      "Bertrand s'assied derrière un bureau encombré de cartes et de dossiers militaires.",
      "Un échange très bref a lieu où les mots sont mesurés et précis.",
      "Le poids silencieux du commandement se fait sentir entre les deux hommes.",
      "Pierre reçoit son numéro d'appareil et une directive concise concernant le vol de demain.",
      "Il quitte la pièce avec une détermination feutrée."
    ],
    "sensoriels": "L'odeur âcre du papier ancien, du café froid et du tabac dans le bureau. Le bruit des bottes sur le parquet est sec. La lumière filtrée à travers les fenêtres donne un aspect austère aux murs.",
    "ton_et_rythme": "Formel, concis, lourd."
  },
  {
    "sequence": 3,
    "beats": [
      "Jules Meca conduit Pierre vers le hangar où est stationné son Spitfire.",
      "Le mécanicien commence une inspection minutieuse de la machine devant Pierre.",
      "Jules explique en détail les caprices et les rituels de maintenance du Mk IX.",
      "Il touche l'hélice de sa main droite avant d'insister sur le bon état des moteurs Merlin.",
      "Pierre observe attentivement, écoutant chaque mot technique de Jules.",
      "Jules désigne l'appareil en lui donnant son nom : 'Grey Ghost'."
    ],
    "sensoriels": "L'odeur puissante et complexe du mélange huile/essence brûlée. Le bruit métallique des outils qui cliquent sur le moteur. La vue détaillée des rivets, de la peinture usée et des lignes aérodynamiques.",
    "ton_et_rythme": "Pratique, méthodique, affectueux."
  },
  {
    "sequence": 4,
    "beats": [
      "Pierre se réveille au milieu de la nuit sans avoir trouvé le sommeil.",
      "Il observe les activités matinales et silencieuses des autres pilotes dans la salle commune.",
      "Après un moment d'attente, il quitte son dortoir pour aller vers le tarmac.",
      "Le vent frais balaye l'allée du hangar où se trouve le Grey Ghost.",
      "Pierre s'approche de l'appareil et pose sa main sur le fuselage froid en métal.",
      "Il remarque que les couleurs du ciel ont commencé à passer du noir profond au blanc pâle à l'est."
    ],
    "sensoriels": "Le silence enveloppant, seulement troublé par des bruits mécaniques distants. Le toucher du métal glacé sous la paume de Pierre. L'odeur subtile et propre de l'aube qui chasse le froid nocturne.",
    "ton_et_rythme": "Contemplatif, silencieux, anticipatif."
  }
]



Analyse si ce plan remplit l'objectif narratif, puis conclus avec tes PROBLEME: et SCORE: N.

---

# RESPONSE

Le plan est très réussi sur le plan atmosphérique et sensoriel, ce qui contribue fortement à l'isolement de Pierre par la solitude du décor. Cependant, pour atteindre l'objectif spécifique d'installer Pierre comme un étranger dans un **groupe soudé**, il manque des interactions ou des observations plus directes concernant les codes sociaux et le lien préexistant entre les autres pilotes.

Actuellement, son isolement est davantage dû au fait qu'il est nouveau (un statut), plutôt qu'à une incapacité à se fondre dans un groupe déjà établi (une dynamique). Les séquences sont très descriptives (le ciel, le métal) et techniques (les moteurs), mais peu sociales.

Voici les axes d'amélioration :

PROBLEME: Le plan manque de scènes montrant l'interaction avec des pairs (autres pilotes) pour que le lecteur puisse percevoir ce qu'il lui manque ou ce qu'il ne comprend pas dans la dynamique du groupe soudé.
PROBLEME: L'isolement est principalement établi par l'observation passive, mais il faudrait une séquence où Pierre tente d'interagir ou de comprendre un code tacite (une blague interne, une routine partagée, une manière spécifique de s'adresser à quelqu'un) et échoue légèrement.
PROBLEME: L'interaction avec Jules Meca est très technique ; elle devrait être complétée par une touche plus humaine pour montrer que même les relations professionnelles sont déjà établies entre le mécanicien et les pilotes, laissant Pierre en marge de cette familiarité.

SCORE: 7