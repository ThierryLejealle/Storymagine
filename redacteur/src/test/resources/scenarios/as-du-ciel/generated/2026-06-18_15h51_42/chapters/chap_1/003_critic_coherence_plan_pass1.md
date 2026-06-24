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
      "Pierre arrive sur le tarmac de Thorney Island au petit matin.",
      "Le grondement lointain des moteurs Merlin résonne à travers la brume matinale.",
      "Il marche vers les avions alignés, enveloppé par une humidité froide du bord de la Manche.",
      "Les Spitfires sont immobiles dans le brouillard, leurs hélices figées comme des insectes endormis.",
      "Pierre pose son sac à côté d'un groupe d'avions et lève les yeux vers un ciel encore gris-bleu.",
      "Il observe la ligne de Spitfire Mk IX, reconnaissable par ses deux canons Hispano 20mm visibles sous le capot."
    ],
    "sensoriels": "Le son sourd des moteurs au loin contrastant avec le silence oppressant du tarmac. L'odeur âcre de l'herbe mouillée et du kérosène froid. La vue d'un ciel bas, texturé par la brume.",
    "ton_et_rythme": "Calme mais tendu. Phrases descriptives et lentes pour installer l'atmosphère."
  },
  {
    "sequence": 2,
    "beats": [
      "Pierre est invité dans le bureau austère du Commandant Bertrand.",
      "Bertrand s'assied derrière un grand bureau en bois, son regard balayant Pierre avec une intensité calme.",
      "Le commandant énonce quelques mots sur la nécessité de l'alerte maximale sans jamais sourire.",
      "Pierre reste silencieux, observant les détails de la pièce : le cadran d'horloge et le papier des listes de vol.",
      "Bertrand lui remet son numéro de machine et une seule phrase directive concernant sa mission du lendemain.",
      "Pierre hoche la tête en signe de reconnaissance avant de quitter le bureau dans le silence."
    ],
    "sensoriels": "Le contraste entre l'odeur d'encre et de vieux bois du bureau et le vent frais qui s'engouffre par les fenêtres. Le son étouffé des pas sur le tapis.",
    "ton_et_rythme": "Formel, concis, rythmé par les silences lourds entre les échanges."
  },
  {
    "sequence": 3,
    "beats": [
      "Jules Meca arrive près du Spitfire de Pierre et commence son inspection méthodique.",
      "L'ouvrier explique à voix haute ses rituels : vérifier le train avant, nettoyer les surfaces.",
      "Il touche l'hélice du 'Grey Ghost' avec sa main droite dans un geste presque cérémoniel.",
      "Jules détaille les spécificités de la machine, parlant des 4x mitrailleuses Browning .303 comme si elles étaient des yeux.",
      "Pierre observe le cockpit et note l'odeur d'huile fraîche mélangée à celle du carburant brûlé.",
      "Après une série de vérifications finales, Jules confirme que tout est opérationnel."
    ],
    "sensoriels": "Le toucher rugueux du métal froid sous les doigts. Le son métallique des outils et le léger sifflement du moteur au démarrage initial.",
    "ton_et_rythme": "Méticuleux et rythmé par les gestes répétitifs de Jules."
  },
  {
    "sequence": 4,
    "beats": [
      "La première nuit passe dans le dortoir, un mélange d'activité bruyante et de fatigue.",
      "Pierre ne parvient pas à s'endormir malgré les bruits des autres pilotes qui se préparent pour la journée.",
      "Il décide de sortir avant l'aube pour prendre l'air frais du matin.",
      "Le ciel commence à virer d'un gris sombre à un rose pâle à l'horizon est.",
      "Pierre marche vers le tarmac et s'approche du Spitfire, dont la silhouette se détache contre le ciel clair.",
      "Il pose sa main sur le fuselage de Grey Ghost ; le métal est froid sous ses doigts."
    ],
    "sensoriels": "Le contraste entre l'odeur étouffée du dortoir et l'air pur et glacial à l'extérieur. La vue progressive des couleurs changeantes dans le ciel.",
    "ton_et_rythme": "Contemplatif, lent, avec une transition vers un rythme plus doux au lever du jour."
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

AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]