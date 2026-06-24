# SYSTEM PROMPT

Tu es un verificateur de coherence. Tu evalues tres soigneusement le TEXTE d'un chapitre.
Tu verifies point par point tous les passages et elements du texte : ton objectif est de relever toutes les incoherences, erreurs factuelles ou violations de contraintes, meme mineures. Mais ne te force pas a inventer une incoherence si tout est correct.
Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks), fiches personnage (faits et psychologie des personnages), continuite factuelle dans le texte.
Tu n'evalues PAS la progression narrative, la qualite litteraire, la grammaire, la syntaxe ni le style.
Si l'objectif du chapitre est fourni, tout element qui en decoule directement n'est pas une incoherence — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis le texte, les checks et les fiches personnage et releve toutes les incoherences meme mineures.
2. Qualifie chaque point :
   AMELIORATION: un detail factuel (type de materiel, rang, toponyme, date) pourrait etre plus precis ou conforme a la fiche. Ne retiens pas une AMELIORATION pour une question de style ou de ton.
   Exemple : Le texte mentionne des Hurricanes — verifier si c'est l'avion de Pierre (Spitfire Mk V en Afrique) ou d'un autre pilote.
   Si le texte contient une information qui contredit partiellement un fait etabli ou un check, c'est un DEFAUT_SIGNIFICATIF.
   Exemple : Le texte indique que Pierre pilotait des Hurricanes en Afrique du Nord, alors que sa fiche precise Spitfire Mk V.
   Si le texte contredit directement un check explicite ou un fait fondamental du recit, c'est un DEFAUT_MAJEUR.
   Exemple : Le texte mentionne la mission Gold Beach et les coordonnees radio — viole le check : le premier briefing ne mentionne pas encore de mission specifique.
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

### Texte
L'odeur âcre et persistante du sel marin s’accrochait déjà au tissu de la combinaison, signalant l'approche imminente du littoral où le ciel promet une clarté violente. Le silence habituel des aéroports se mua en un rugissement régulier lorsque les douze cylindres du Merlin commencèrent leur chant bas et déterminé. L'homme s'installa dans son cockpit, sentant immédiatement la puissance vibratoire traverser ses os. Un ordre de Bertrand filtra à travers le casque : escorté jusqu'à la côte, l’objectif étant un Hurricane RAF endommagé et incapable de manœuvrer par lui-même. La mission ne serait pas une chasse, mais une garde constante. Le Grey Ghost, dans son élégance familière, semblait attendre cette tâche avec patience, ses ailes elliptiques se découpant sur le ciel matinal d’un bleu pâle, presque lavé par l'humidité marine.

Le Spitfire s'élança en douceur vers la formation. Les premières minutes furent une succession de vérifications instinctives. La sensation des commandes sous les doigts devint familière, mais un réflexe technique plus profond prit le dessus : Pierre commença à inspecter mentalement les systèmes d'armement. Il passa au deleté sur les canons Hispano 20mm et sur la configuration des quatre mitrailleuses Browning .303. Chaque pièce devait être parfaitement opérationnelle, une assurance tacite contre l'imprévu qui pouvait surgir en plein vol, même sans combat déclaré.

L'enchaînement du moteur du Hurricane révéla rapidement sa fragilité. Le son était plus rauque, plus saccadé que le chant continu et puissant de son propre Spitfire. L’appareil semblait lutter contre la traction, chaque mouvement étant une dépense d’énergie visible dans la manière dont ses ailes s'étiraient avec difficulté pour maintenir une vitesse constante et stable. La trajectoire devenait hésitante, oscillant légèrement autour d'un axe invisible comme si le moteur peinait à trouver son rythme.

Le temps devint un effort de concentration soutenu. Pierre maintenait la vigilance maximale, ses yeux gris-vert balayant systématiquement les quadrants : l’horizon était une ligne mince et nette où le ciel se découpait en nuances de bleu pâle et blanc, soulignant légèrement la courbure terrestre à cette altitude modeste. Le Grey Ghost semblait un oiseau de proie dans son élément, mais il devait rester proche, colé au plus près du Hurricane lent. Il ressentit l'envie lancée par le pilote d’utiliser sa manœuvrabilité supérieure, de donner une rotation franche ou un virage serré pour optimiser la couverture. Mais cette ferveur instinctive fut tempérée par la nécessité : il devait rester assez proche pour que son ombre soit toujours visible sur l'autre appareil vulnérable.

Pendant ces trente minutes, le bruit des deux moteurs formait une mélodie de travail constante, un rythme mécanique qui remplissait totalement les sens. Le manche lui transmettait chaque micro-turbulence du flux d’air autour du duo, la sensation physique de la lutte contre les éléments étant constante mais contrôlée. Chaque vérification mentale des armements, chaque balayage vigilant du champ visuel pour anticiper une menace inexistante, exigeait un calme absolu et une concentration implacable sur le devoir plus que sur l'attaque potentielle. Finalement, après ce long effort de soutien, les deux machines atteignirent la zone côtière désignée, marquant la fin d’une escorte qui avait été moins un vol qu'un exercice patient de protection constante.

L'air frais et chargé de sel du bord s'était enfin retiré pour laisser place à la chaleur plus lourde et un peu étouffée du mess. Pierre franchit le seuil de bois, laissant derrière lui l’immensité grise du ciel matinal et entrant dans ce refuge clos où les voix se mêlaient en murmures bas et réguliers. Sur une table massive au centre de la pièce, Jules Meca déploia un jeu de cartes usé avec des doigts épais, ses phalanges tachées par le gras permanent qui ne s'effaçait jamais vraiment malgré l'usage du savon ; les dés de bois glissèrent sur la surface sombre en un *clac* feutré.

Autour de cette table, une atmosphère décontractée et pesante s’était installée depuis le début de la soirée. Au milieu des visages concentrés ou au contraire trop expressifs, Henri Leclair jouait son coup du jour : il posa ses cartes avec un sourire qui semblait déjà trop large pour être sincère. Il blafa avec une assurance presque excessive, chaque geste amplifié par l'excitation brutale de sa jeunesse, comme si le succès immédiat était la seule chose qui comptait dans cet univers vaste et pressé. Pierre s’assit à une place libre, ne cherchant pas une position privilégiée, mais simplement un point d'ancrage parmi les silhouettes familières. Il ne jouait pas encore, il observait plutôt l'ensemble de cette petite mécanique sociale en cours.

Son regard se posa naturellement sur Jules. Le mécanicien passait sa main sur le plateau du jeu et s’arrêta brièvement, son pouce effleurant un rivet imaginaire ou un joint de bois avec la même rigueur que lorsqu’il inspectait l'aile d’un Spitfire. Ce petit geste révélait cette minutie absolue qui caractérisait Jules ; pour lui, tout était un système à décortiquer, qu'il s'agisse du moteur Merlin ou d'une simple partie de cartes.

Soudain, la radio, discrète dans le coin de la pièce, commença à diffuser une mélodie jazzy et feutrée. Le son se répandit lentement dans les recoins du mess, remplissant le silence ambiant sans jamais l’étouffer, créant cette trêve sonore qui donnait un rythme lent au temps passé. Un regard passa entre Pierre et Jules par-dessus la table. Ce fut un bref instant de reconnaissance mutuelle, un hochement de tête quasi imperceptible qui valida l'état du Grey Ghost, une compréhension tacite que seule l’expérience partagée permettait. Puis le silence fut repris par les cartes.

Henri lança son geste suivant avec encore plus d'audace, cherchant à déstabiliser ses adversaires. Jules réagit calmement, sa main bougeant avec la précision mécanique de quelqu'un qui connaît parfaitement chaque composant de ce qu’il tient en main. Pierre, lui, ne chercha pas le jeu actif ; il se contenta d'observer les trajectoires de chacun : l'enthousiasme rapide et parfois désordonné d'Henri, la rigueur silencieuse et essentielle de Jules, puis sa propre observation attentive des micro-expressions qui trahissaient les véritables intentions. Chaque personnage jouait selon son tempérament unique, ses actions révélant ce que ses paroles ne pouvaient exprimer.



### Questions de coherence
- Le ton reste-t-il sobre et retenu — aucune envolée lyrique gratuite ?
- Les éléments de focus demandés ([CIEL], [MACHINE], [PEUR], [CAMARADERIE], [MORT]) sont-ils présents et développés avec soin là où ils sont demandés ?
- Les émotions sont-elles montrées via le corps ou les gestes, jamais nommées directement ?
- Le temps s'écoule-t-il différemment dans le ciel (lent, sensoriel) et à terre (rapide, fonctionnel) ?
- Les dialogues respectent-ils la voix propre de chaque personnage ?

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
- Aucun discours patriotique. Ces hommes se battent parce qu'ils se battent.
  Les grandes causes ne s'énoncent pas — elles flottent en arrière-plan comme une évidence.
- Le Spitfire est nommé "Grey Ghost" par Jules, jamais "l'avion" ou "l'appareil"
  (sauf par des personnages formels comme Bertrand).
- Pas de résumé narratif là où une scène est possible.

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



Evalue la coherence du texte.

---

# RESPONSE

AMELIORATION : Le moteur du Spitfire Merlin est généralement un moteur à 8 cylindres, et non douze.
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]