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
      "Ils échangent sur les conditions de vol et la nécessité d'une vigilance constante. Bertrand mentionne que le vent du nord est imprévisible ce matin, ajoutant une précision opérationnelle qui montre sa connaissance des opérations locales.",
      "Pierre reste silencieux, observant le cadran d'horloge au-dessus du bureau, puis les listes de vol avec une concentration calme.",
      "Bertrand lui remet son numéro de machine et donne une directive concise pour sa mission du lendemain. Son ton est direct mais empreint d'une connaissance implicite des procédures établies par le groupe.",
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
      "Pierre reste en retrait, observant cette interaction du groupe avec un sentiment actif de décalage ; bien qu'il soit physiquement présent, il ressent le poids d'une solitude étrange face à leur lien soudé et partagé.",
      "Il décide de sortir avant l'aube pour prendre l'air frais. Le ciel commence à virer d'un gris sombre à un rose pâle à l'horizon est.",
      "Pierre marche vers le tarmac et s'approche du Spitfire, dont la silhouette se détache contre les couleurs changeantes.",
      "Il pose sa main sur le fuselage de Grey Ghost ; le métal froid sous ses doigts semble être une connexion plus immédiate et tangible que n'importe quelle conversation humaine."
    ],
    "sensoriels": "Le contraste entre l'odeur étouffée du dortoir et l'air pur et glacial à l'extérieur. La vue progressive des couleurs changeantes dans le ciel.",
    "ton_et_rythme": "Contemplatif, lent, avec une transition vers un rythme plus doux au lever du jour."
  }
]

### Objectif de ce chapitre
Installer Pierre comme un étranger dans un groupe soudé depuis des mois. Le lecteur doit ressentir son isolement : il observe, il ne participe pas encore, il ne comprend pas les codes tacites. Pas de chaleur, pas d'intégration.


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

AMELIORATION : Le passage de la séquence 2 à la séquence 3 pourrait bénéficier d'une transition plus explicite pour relier l'issue du rendez-vous avec Bertrand au début des activités sur le tarmac, afin que le changement de décor et de rythme soit parfaitement fluide.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]