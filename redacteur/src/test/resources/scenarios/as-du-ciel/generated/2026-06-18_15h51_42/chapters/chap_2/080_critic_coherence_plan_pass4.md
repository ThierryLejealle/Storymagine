# SYSTEM PROMPT

Tu es un verificateur de coherence. Tu evalues tres soigneusement le PLAN d'un chapitre.
Tu verifies point par point tous les elements et aspects du plan : ton objectif est de relever toutes les incoherences, erreurs factuelles ou violations de contraintes, meme mineures. Mais ne te force pas a inventer une incoherence si tout est correct.
Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks), fiches personnage (faits et psychologie des personnages), continuite factuelle.
Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.
Si l'objectif du chapitre est fourni, tout element qui en decoule directement n'est pas une incoherence — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis le plan, les checks et les fiches personnage et releve toutes les incoherences meme mineures.
2. Qualifie chaque point :
   AMELIORATION: un detail factuel (type de materiel, rang, toponyme, date) pourrait etre plus precis ou plus conforme a la fiche.
   Exemple : La S3 mentionne des Hurricanes en Afrique du Nord — la fiche de Pierre precise Spitfire Mk V ; verifier si c'est une erreur ou un autre pilote.
   Si une sequence contient une information qui contredit partiellement un fait etabli, un check, ou le comportement attendu d'un personnage selon sa fiche, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : La S2 mentionne que Jules a ete stationne a Biggin Hill en 1942, or sa fiche indique qu'il n'a rejoint l'escadrille qu'en 1943.
   Si une sequence contredit directement un check explicite ou un fait fondamental du recit, c'est un DEFAUT_MAJEUR.
   Exemple : La S2 annonce la mission Gold Beach et les coordonnees radio — viole le check : le premier briefing ne mentionne pas encore de mission specifique.
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


### Questions de coherence
- Pierre agit-il de façon cohérente avec sa personnalité (réservé, observateur, loyal) ?
- Les personnages secondaires restent-ils dans leur rôle sans empiéter sur l'arc de Pierre ?
- La mort de Pierre au chapitre 5 est-elle préparée sans être annoncée ? (tension croissante, pas de révélation prématurée)
- Henri est-il vivant à la fin de chaque chapitre où il apparaît ?
- L'objectif narratif global progresse-t-il : Pierre passe-t-il de l'arrivée innocente à une forme d'acceptation silencieuse de ce qu'il est devenu ?

### Contraintes

## Séquence 1
- Les contraintes de vol liées au Hurricane sont présentes : lenteur, impossibilité de manœuvrer librement
- Pierre est décrit comme contraint par l'appareil à protéger, pas comme chasseur libre
- La tension vient de l'attente et de la surveillance, pas d'un combat actif

## Séquence 2
- Chaque personnage joue ou se comporte de façon distincte — pas un groupe homogène
- L'atmosphère est détendue en surface mais chargée en sous-texte
- Aucune mention explicite du lendemain ni de la mort — le non-dit porte tout

### Contraintes
- L'histoire se déroule sur exactement huit jours, du 6 au 13 juin 1944 (J+0 à J+7 du Débarquement)
- Pierre Moreau meurt le 13 juin 1944 — cette fin est immuable, ne pas la remettre en question
- Le Spitfire de Pierre est un Spitfire Mk IX, armé de deux canons Hispano 20mm et quatre
  mitrailleuses Browning .303 — respecter ces détails si mentionnés
- La base est Thorney Island (West Sussex, Angleterre) — réelle, près de Portsmouth
- Pierre n'a pas de famille proche : sa mère est morte en 1942, son père est inconnu.
  Il a une amie d'enfance, Lucie, à qui il pense mais qu'on ne voit jamais directement.
- Oberleutnant Wolf ne meurt pas dans ce livre — il repart après avoir abattu Pierre
- Henri Leclair survit à la guerre — ne pas le tuer même par allusion
- Pas de flash-forward. Le lecteur ne doit pas savoir que Pierre va mourir avant le chapitre 5.

### Elements de focus demandes
[CIEL] Le ciel est le vrai décor de ce livre. Décris-le avec précision : la couleur exacte
à cette altitude et cette heure, la texture des nuages, la lumière rasante du matin ou du
soir, le silence absolu à 6000 mètres que seul le moteur brise, l'horizon qui se courbe
légèrement. Pierre vit dans le ciel. C'est sa maison et sa tombe.


[MACHINE] Le Spitfire est un être vivant. Décris ses vibrations au démarrage — l'hélice qui
cherche sa cadence, la chaleur du moteur Merlin qui monte dans le cockpit, l'odeur d'huile
et d'essence brûlée. En vol : le manche qui transmet chaque turbulence, le bruit des
mitrailleuses qui fait trembler la cellule entière, la brutalité physique d'un virage à 6G.
La jauge de carburant. Le voyant d'huile. Ce sont les battements de cœur de la machine.


[CAMARADERIE] Les pilotes ne se disent pas qu'ils s'aiment. Ils se le montrent par des
petites choses : une cigarette tendue sans un mot, une main sur l'épaule deux secondes et
puis rien, une blague répétée jusqu'à ce qu'elle ne soit plus drôle mais qu'on la répète
quand même parce que c'est la leur. Les silences partagés valent plus que les discours.



Verifie la coherence du plan.

---

# RESPONSE

AMELIORATION : Ajouter une description plus détaillée du ciel (couleur exacte, texture des nuages, lumière) pour satisfaire le focus [CIEL] dans la séquence 1.
AMELIORATION : Intégrer davantage les sensations physiques de l'appareil (vibrations intenses, chaleur du moteur Merlin, odeur d'huile et d'essence brûlée) lors de la description des vols pour respecter le focus [MACHINE].
AMELIORATION : Préciser comment Pierre utilise ses compétences techniques/visuelles (Hispano 20mm, Browning .303) non seulement comme une routine mentale mais en les liant à l'observation active du danger ou de l'environnement.
AMELIORATION : Renforcer le focus [CAMARADERIE] dans la séquence 2 par un geste subtil et partagé entre Pierre et Jules qui dépasse le simple regard, renforçant leur lien sans nécessiter de dialogue explicite.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]