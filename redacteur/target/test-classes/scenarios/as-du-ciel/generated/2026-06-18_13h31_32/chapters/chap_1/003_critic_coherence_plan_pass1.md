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