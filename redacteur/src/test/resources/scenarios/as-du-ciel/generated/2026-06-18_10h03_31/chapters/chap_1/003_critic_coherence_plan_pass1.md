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
## Séquence 1
Pierre Moreau arrive à la base de Thorney Island à l'aube du 6 juin 1944. La scène est dominée par une atmosphère lourde et brumeuse. Le son sourd mais puissant des moteurs Merlin résonne dans le silence matinal, contrastant avec l'immobilité apparente des Spitfires alignés sur le tarmac. Pierre dépose son sac à dos près de la piste. Il observe les machines, non pas avec excitation, mais avec une précision clinique et détachée. Sa première perception est le ciel : un gris pâle qui commence lentement à se teinter d'une lumière artificielle avant l'aube complète.

## Séquence 2
Pierre est convoqué dans le bureau du Commandant Bertrand. La rencontre dure exactement deux minutes. L'environnement est marqué par le poids palpable de la commandement et du contexte immédiat (l'urgence, la guerre). Les échanges verbaux sont minimaux ; les mots servent à établir une autorité silencieuse plutôt qu'à transmettre des informations complexes. Pierre reçoit son numéro de machine. Il répond avec une seule phrase concise : "Demain, à l'aube." Bertrand observe Pierre, notant sa capacité à absorber la gravité sans y réagir émotionnellement, et sort du bureau, laissant Pierre seul face à l'étendue du tarmac.

## Séquence 3
Jules Meca présente le Spitfire Mk IX à Pierre sur le terrain. Jules parle pour deux, utilisant des gestes et des observations techniques pour communiquer ce que les mots ne peuvent pas dire. Il explique de manière décontractée les habitudes et les rituels de maintenance du Spitfire : la façon dont il réagit aux sollicitations, ses caprices mécaniques. Pierre écoute attentivement, absorbant non seulement les informations techniques, mais aussi l'atmosphère particulière qu'émane de Jules – une forme de connexion silencieuse entre deux hommes habitués à la solitude et au travail acharné.

## Séquence 4
La nuit tombe sur la base. Pierre est dans le dortoir ou une salle commune, incapable de dormir. Il observe les autres pilotes, non pas avec jugement, mais comme des éléments d'un système complexe. Le chaos latent de la guerre se manifeste par les routines étranges et silencieuses des hommes autour de lui. Avant l'aube, il retourne sur le tarmac. Dans un geste presque inconscient, il pose sa main sur le fuselage froid du Spitfire "Grey Ghost". Ce contact physique est bref mais chargé d'une intention : une reconnaissance muette de son environnement. Le ciel commence à se blanchir doucement à l'est, marquant la transition entre la nuit et le jour, un spectacle qui semble être sa seule véritable maison.

### Objectif de ce chapitre
Installer Pierre comme un étranger dans un groupe soudé depuis des mois. Le lecteur doit ressentir son isolement : il observe, il ne participe pas encore, il ne comprend pas les codes tacites. Pas de chaleur, pas d'intégration.


### Questions de coherence
- Pierre agit-il de façon cohérente avec sa personnalité (réservé, observateur, loyal) ?
- Les personnages secondaires restent-ils dans leur rôle sans empiéter sur l'arc de Pierre ?
- La mort de Pierre au chapitre 5 est-elle préparée sans être annoncée ? (tension croissante, pas de révélation prématurée)
- L'objectif narratif global progresse-t-il : Pierre passe-t-il de l'arrivée innocente à une forme d'acceptation silencieuse de ce qu'il est devenu ?
- Les contraintes temporelles sont-elles respectées (8 jours, 6-13 juin 1944) ?

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