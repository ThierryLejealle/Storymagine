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
      "Pierre arrive à la base de Thorney Island au lever du jour.",
      "Le son lointain et régulier des moteurs Merlin se fait audible au-dessus de la Manche.",
      "Il traverse le tarmac où les Spitfires sont alignés dans une brume matinale grise.",
      "Pierre pose son sac à dos sur le sol en béton, observant l'immobilité des hélices sous la voûte nuageuse.",
      "Il lève les yeux pour balayer le ciel, notant sa couleur bleutée et dense malgré l'aube naissante.",
      "L'air frais et humide de l'Angleterre enveloppent son visage tandis qu'il prend une première grande inspiration."
    ],
    "sensoriels": "Vue : Brume grise, Spitfires alignés. Son : Bourdonnement des Merlin au loin, sifflement du vent. Toucher : Air froid sur le visage.",
    "ton_et_rythme": "Serein mais alerte ; cadence lente et contemplative."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre entre dans le bureau austère du Commandant Bertrand.",
      "Il rencontre l'officier en uniforme impeccable qui lui adresse un bref signe de tête.",
      "Bertrand énumère les détails logistiques avec une concision militaire et sans émotion.",
      "Le silence s'installe brièvement, chargé du poids des responsabilités militaires.",
      "Pierre reçoit son numéro d'appareil et la consigne pour le vol du lendemain à l'aube.",
      "Il quitte le bureau et reprend sa marche vers les hangars, laissant derrière lui l'autorité silencieuse de Bertrand."
    ],
    "sensoriels": "Vue : Bureau sombre, uniforme net. Son : Bruits étouffés des bureaux. Toucher : Le poids du silence.",
    "ton_et_rythme": "Formel et concis ; cadence rapide pour l'échange, puis lente pour la réflexion."
  },
  {
    "sequence": 3,
    "beats": [
      "Jules Meca s'approche de Pierre près du hangar où se trouve le Spitfire.",
      "Le mécanicien commence à parler avec une ferveur presque excessive du 'Grey Ghost'.",
      "Il guide Pierre pour inspecter les détails techniques : l'hélice, la jauge de carburant et les canons Hispano 20mm.",
      "Jules décrit ses rituels de maintenance, notamment le toucher précis sur l'hélice avant chaque décollage.",
      "Pierre observe ces gestes méticuleux, notant que Jules parle beaucoup pour masquer sa vigilance.",
      "Le mécanicien termine en assurant qu'il est prêt à recevoir son pilote."
    ],
    "sensoriels": "Vue : Détails du Spitfire (couleurs, rivets). Odorat : Odeur d'huile moteur et d'essence brûlée. Toucher : Métal froid de l'appareil.",
    "ton_et_rythme": "Animé mais précis ; cadence soutenue par les explications techniques."
  },
  {
    "sequence": 4,
    "beats": [
      "Pierre passe la soirée dans le dortoir, observant les autres pilotes dans une quiétude forcée.",
      "Il ne parvient pas à s'endormir, son esprit restant éveillé sur l'imminence du combat.",
      "Avant l'aube, il quitte le dortoir et se dirige vers la zone des avions.",
      "L'air est plus frais ; les premières lueurs de l'est commencent à éclaircir la brume matinale.",
      "Pierre s'approche du Grey Ghost et pose sa main sur le fuselage en métal froid.",
      "Il ressent la vibration subtile du moteur au repos, tandis que le ciel se transforme progressivement d'un gris sombre à un pâle azur."
    ],
    "sensoriels": "Vue : Ciel qui blanchit. Toucher : Froid du métal et de l'air matinal.",
    "ton_et_rythme": "Introspectif et calme ; cadence lente, presque méditative."
  }
]

### Objectif de ce chapitre
Installer Pierre comme un étranger dans un groupe soudé depuis des mois. Le lecteur doit ressentir son isolement : il observe, il ne participe pas encore, il ne comprend pas les codes tacites. Pas de chaleur, pas d'intégration.


### Questions de coherence
- Pierre agit-il de façon cohérente avec sa personnalité (réservé, observateur, loyal) ?
- Les personnages secondaires restent-ils dans leur rôle sans empiéter sur l'arc de Pierre ?
- La mort de Pierre au chapitre 5 est-elle préparée sans être annoncée ? (tension croissante, pas de révélation prématurée)
- L'objectif narratif global progresse-t-il : Pierre passe-t-il de l'arrivée innocente à une forme d'acceptation silencieuse de ce qu'il est devenu ?
- Les contraintes temporelles sont-elles respectées (8 jours, 6-13 juin 1944) ?

### Contraintes

## Séquence 1
- Pierre est décrit comme observateur passif — il regarde, il ne touche pas encore
- L'escadrille est présente mais distante, Pierre n'est pas intégré dans le groupe
- Le Spitfire est nommé 'Grey Ghost' ou 'Spitfire' — jamais un autre nom

## Séquence 2
- Bertrand reste froid et autoritaire — aucune chaleur humaine, aucun encouragement
- La scène dure deux minutes maximum dans le récit — pas de longue conversation
- Pierre repart avec uniquement son numéro de machine, rien de plus

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

AMELIORATION : Pour renforcer la précision technique du Spitfire Mk IX, il serait pertinent de mentionner explicitement les quatre mitrailleuses Browning .303 en plus des deux canons Hispano 20mm lors de l'inspection avec Jules Meca.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]