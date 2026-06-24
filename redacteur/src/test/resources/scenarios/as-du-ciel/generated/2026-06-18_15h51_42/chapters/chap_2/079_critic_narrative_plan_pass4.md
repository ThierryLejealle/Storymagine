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
      "Pierre Moreau quitte le tarmac de Thorney Island et s'installe dans son Spitfire Mk IX (Grey Ghost).",
      "Il reçoit l'ordre d'accompagner un Hurricane RAF dont le moteur est compromis. La mission : escorte jusqu'à la côte.",
      "Le Hurricane lutte pour maintenir sa vitesse, sa trajectoire est irrégulière et vulnérable ; Pierre doit constamment ajuster son vol pour rester suffisamment proche sans se mettre en danger.",
      "Pierre effectue une vérification mentale rapide des systèmes (Hispano 20mm, Browning .303), utilisant cette routine technique comme un ancrage mental face à la pression physique du pilotage.",
      "Le besoin de manœuvre est constant. Le Spitfire réagit avec une puissance intense ; le bruit du moteur Merlin et les vibrations transmises au cockpit sont ressenties non pas comme une performance, mais comme une fatigue physique écrasante sous l'effort continu d'escorte.",
      "Pendant trente minutes, Pierre maintient un état de vigilance absolue. Son attention est focalisée sur la protection du Hurricane contre toute menace potentielle, exigeant une concentration totale qui dépasse le simple rôle de pilote.",
      "Le duo atteint finalement le point désigné près des côtes."
    ],
    "sensoriels": "La puissance vibratoire et physique du moteur Merlin sous les pieds ; l'odeur âcre d'huile chaude et d'essence brûlée mêlée au sel marin ; le contraste entre la cadence puissante du Spitfire et la lutte plus saccadée du Hurricane.",
    "ton_et_rythme": "Rythmé par des efforts physiques intenses, ton sérieux et extrêmement vigilant."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre pénètre dans le mess. La brusque diminution du bruit et l'odeur de cockpit s'estompent au contact de la chaleur intérieure.",
      "Jules Meca déploie un jeu de cartes usé sur une table en bois sombre ; ses gestes sont précis, presque mécaniques.",
      "Henri Leclair lance son coup avec une confiance excessive, montrant sa nervosité par l'audace de son bluff.",
      "Pierre ne joue pas. Il utilise la concentration acquise dans le cockpit pour observer les micro-expressions et les interactions des autres hommes ; il analyse leurs gestes comme s'il lisait un indicateur d'alerte sur son tableau de bord.",
      "La radio diffuse une mélodie jazzy douce, créant une bulle sonore qui remplace le silence opérationnel du vol.",
      "Un échange silencieux et bref se produit entre Pierre et Jules ; ce regard est la reconnaissance mutuelle du danger passé et de l'effort fourni sur le Grey Ghost.",
      "Chacun joue selon son mode : Henri par l'impulsion, Jules par la rigueur méticuleuse, Pierre par une observation intense et silencieuse."
    ],
    "sensoriels": "Le contraste entre les bruits du vol et le calme feutré du mess ; le son des cartes frottant sur le bois ; l'odeur subtile de café et de tabac dans la pièce.",
    "ton_et_rythme": "Rythme plus lent, contemplatif, ton calme mais chargé d'une vigilance interne."
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

AMELIORATION : Rendre le lien entre la concentration de Pierre en vol et son observation dans le mess plus concret ; au lieu d'une simple analogie ("analyse leurs gestes comme s'il lisait un indicateur"), montrer une réaction physique ou mentale immédiate (ex: il repère un signe de stress chez Henri qui lui rappelle l'urgence du Hurricane).
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]