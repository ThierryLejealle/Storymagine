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
      "Pierre quitte le tarmac et s'installe dans son Spitfire Mk IX.",
      "Il reçoit les ordres d'escorte : accompagner un Hurricane RAF endommagé jusqu'à la côte.",
      "Le moteur du Hurricane peine à maintenir une vitesse constante, sa trajectoire est hésitante.",
      "Pierre maintient une vigilance maximale en surveillant chaque quadrant autour de l'appareil plus lent. Il vérifie le fonctionnement des canons Hispano 20mm et des quatre mitrailleuses Browning .303 pour s'assurer qu'ils sont parfaitement opérationnels malgré la situation non-combat, un réflexe technique.",
      "Il ressent le besoin physique d'ouvrir les ailes et de manœuvrer agressivement pour se positionner idéalement, mais il doit rester proche du Hurricane.",
      "La mission dure trente minutes intenses, exigeant une concentration absolue sur la protection plutôt que sur l'attaque. Le bruit des moteurs est constant, le manche transmet chaque turbulence.",
      "Le duo atteint finalement le point désigné près des côtes avant que les conditions ne changent."
    ],
    "sensoriels": "Son rauque et régulier du moteur Merlin de Spitfire contrastant avec les bruits plus saccadés du Hurricane ; la sensation vibratoire puissante sous les pieds dans le cockpit ; l'odeur d'essence chaude mélangée au sel marin, persistante sur sa peau.",
    "ton_et_rythme": "Rythmé par des actions concentrées et répétitives, ton sérieux et vigilant."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre entre dans le mess. La sensation de chaleur résiduelle du cockpit et l'odeur d'huile s'estompe lentement au contact de l'air intérieur.",
      "Jules Meca déploie un jeu de cartes usé sur une table en bois sombre, ses doigts bougent avec précision.",
      "Henri Leclair lance son coup du jour : il bluffant avec une assurance presque excessive au milieu des joueurs.",
      "Pierre ne participe pas activement au jeu ; il observe les interactions et les micro-expressions de chacun. Il fixe notamment Jules, dont la main s'arrête brièvement sur le plateau pour effleurer un rivet imaginaire, rappelant la minutie d'une inspection méticuleuse en vol.",
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

AMELIORATION : Le fait que Pierre vérifie les canons Hispano 20mm et les quatre mitrailleuses Browning .303 est un bon détail technique, mais pour renforcer la contrainte [MACHINE], il serait pertinent de préciser *comment* cette inspection se déroule (ex: le bruit métallique des leviers, l'odeur du métal froid) plutôt que de simplement énoncer qu'il les vérifie.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]