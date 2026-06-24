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
      "Pierre arrive sur le tarmac de Thorney Island au petit matin du 6 juin.",
      "Le grondement sourd des moteurs Merlin résonne à travers la brume matinale. L'air est saturé d'humidité et l'odeur âcre du kérosène froid s'élève.",
      "Il marche vers les Spitfires alignés, le silence pesant malgré le bourdonnement lointain des machines en veille.",
      "Les hélices sont immobiles dans le brouillard. Pierre observe la ligne de Spitfire Mk IX, reconnaissable à ses deux canons Hispano 20mm et quatre mitrailleuses Browning .303."
    ],
    "sensoriels": "Le son sourd des moteurs au loin contrastant avec le silence oppressant du tarmac. L'odeur âcre de l'herbe mouillée et du kérosène froid. La vue d'un ciel bas, texturé par la brume.",
    "ton_et_rythme": "Calme mais tendu. Phrases descriptives et lentes pour installer l'atmosphère."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre est invité dans le bureau du Commandant Bertrand.",
      "Bertrand s'assied derrière son grand bureau en bois. Il examine Pierre avec un regard attentif qui ne cherche pas à juger, mais à évaluer.",
      "Ils échangent quelques mots sur les conditions de vol et la nécessité d'une vigilance constante. Le commandant parle des défis logistiques du combat.",
      "Pierre reste silencieux, observant le cadran d'horloge au-dessus du bureau, puis les listes de vol avec une concentration calme.",
      "Bertrand lui remet son numéro de machine et donne une directive concise pour sa mission du lendemain. Le ton est professionnel mais empreint d'une certaine familiarité tacite.",
      "Pierre hoche la tête en signe de reconnaissance avant de quitter le bureau."
    ],
    "sensoriels": "Le contraste entre l'odeur d'encre et de vieux bois du bureau et le vent frais qui s'engouffre par les fenêtres. Le son étouffé des pas sur le tapis.",
    "ton_et_rythme": "Formel, concis, rythmé par des échanges mesurés."
  },
  {
    "sequence": 3,
    "beats": [
      "Jules Meca commence l'inspection méthodique du 'Grey Ghost'. Il parle à la machine comme s'il lui donnait ses ordres.",
      "Un autre membre de l'équipe au sol arrive pour passer un outil près de Jules ; ils échangent une blague rapide et familière sur le temps, illustrant leur lien habituel. Pierre observe cette interaction avec calme mais ne parvient pas à identifier la source du rire ou son sujet précis.",
      "Jules reprend son travail : il touche l'hélice du 'Grey Ghost' avec sa main droite dans un geste cérémoniel. Il inspecté les deux canons Hispano 20mm et quatre mitrailleuses Browning .303 en particulier.",
      "Il explique à Pierre ses rituels de maintenance, détaillant le fonctionnement des systèmes et la nécessité d'une attention constante aux détails de l'appareil.",
      "Pierre observe attentivement le cockpit ; il remarque la chaleur du moteur Merlin qui monte dans l'habitacle. Il note que les procédures sont extrêmement précises, mais ne sait pas encore quel outil est essentiel pour certaines tâches."
    ],
    "sensoriels": "Le toucher rugueux du métal froid sous les doigts. Le son métallique des outils et le léger sifflement du moteur au démarrage initial.",
    "ton_et_rythme": "Méticuleux, ponctué par de brèves interactions humaines qui soulignent la cohésion de l'équipe."
  },
  {
    "sequence": 4,
    "beats": [
      "La première nuit passe dans le dortoir. Pierre observe les autres pilotes : ils se parlent doucement et partagent des rires légers, créant une bulle de camaraderie dont il ne peut saisir l'essence ou la raison.",
      "Pierre reste en retrait, observant cette interaction du groupe avec un calme pensif qui le distingue légèrement des autres. Il est présent physiquement, mais mentalement déconnecté de leur dynamique sociale.",
      "Il décide de sortir avant l'aube pour prendre l'air frais. Le ciel commence à virer d'un gris sombre à un rose pâle à l'horizon est.",
      "Pierre marche vers le tarmac et s'approche du Spitfire, dont la silhouette se détache contre les couleurs changeantes.",
      "Il pose sa main sur le fuselage de Grey Ghost ; le métal froid sous ses doigts semble être une connexion plus immédiate et tangible que n'importe quelle conversation humaine."
    ],
    "sensoriels": "Le contraste entre l'odeur étouffée du dortoir et l'air pur et glacial à l'extérieur. La vue progressive des couleurs changeantes dans le ciel.",
    "ton_et_rythme": "Contemplatif, lent, avec une transition vers un rythme plus doux au lever du jour."
  }
]



Analyse si ce plan remplit l'objectif narratif, puis conclus avec tes PROBLEME: et SCORE: N.

---

# RESPONSE

Le plan est très efficace et couvre l'objectif narratif de manière complète et nuancée. Il utilise des contrastes sensoriels et comportementaux pour illustrer l'isolement de Pierre, ce qui correspond parfaitement au ton sobre et intimiste recherché.

L'utilisation de la "bulle de camaraderie" dans le dortoir (Séquence 4) et l'incapacité de Pierre à décoder les interactions sociales (Séquence 3), sont des mécanismes narratifs très forts pour établir son statut d'étranger sans avoir besoin de dialogues explicites.

SCORE: 9
PROBLEME: Pour renforcer l'isolement, on pourrait ajouter un bref moment de réflexion interne chez Pierre lors de la Séquence 4, où il ne se contente pas d'observer le groupe, mais ressent activement une forme de décalage ou de solitude face à leur lien soudé.