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
Le grondement sourd des douze cylindres résonnait dans l'air minéral, une fréquence si basse qu'elle semblait vibrer sous le crâne plutôt que d'être entendue par les oreilles. Le silence du matin à six mille mètres était absolu, jusqu'à ce que la mécanique s'éveille.

Le ciel, à cette altitude précise et à cette heure charnière de l'aube, n’était pas bleu ; il était une teinte maladive de gris perle, un blanc voilé par la condensation des nuages bas qui semblaient lourds comme du coton mouillé. La lumière rasante venait d'une source invisible, teintant les bords des formes métalliques dans un orange prématuré, une promesse éphémère avant le crépuscule. L’horizon se courbait là, une ligne indistincte où la terre cédait au vide, donnant l'impression que Pierre ne regardait pas seulement vers l'extérieur, mais qu'il habitait la limite entre ce qui est solide et ce qui s'efface.

Pierre déposa le sac à dos près de la piste. Les mains trop grandes pour son corps, héritées d'un père jamais vu, touchèrent le cuir froid du sac. L'odeur était là avant même l'approche : un mélange dense d’huile chaude, d’essence brûlée et de métal ayant retenu une chaleur insoutenable. C’était la signature de la machine vivante qui attendait sa cadence.

Le Spitfire Grey Ghost reposait, une silhouette elliptique caractéristique sous le ciel blafard. Ses radiateurs proéminents captaient les premières lueurs fantomatiques du soleil levant, transformant les surfaces froides en miroirs huileux. La hélice semblait figée, pourtant, dans le silence de la station, on imaginait déjà sa vibration latente, cherchant nerveusement une cadence oubliée sous le béton. Chaque pièce de fer blanc possédait une chaleur résiduelle, comme un cœur mécanique qui bat doucement avant l'impulsion brutale du moteur Merlin.

Pierre observa les machines avec une précision clinique et détachée. Les yeux gris-vert fixaient la ligne du nez court et bombé de l'appareil. La jauge de carburant était vide ; le voyant d'huile, pourtant, signalait une tension interne palpable. Ce n’était pas un assemblage inerte, mais un organisme en sommeil, dont les battements étaient sa seule preuve de vie.

Un mouvement brusque sur la piste provoqua une résonance dans la cellule. Le Merlin s’anima, et le grondement bas monta en puissance, passant d'un cliquetis à un rugissement profond qui faisait vibrer les dents. La chaleur du moteur commença à se diffuser, emprisonnant l'air autour de Pierre comme un poumon surchargé. Une odeur âcre de combustible brûlé envahit le cockpit minuscule.

Le Spitfire n'était pas seulement une machine ; il était la traduction directe des ordres envoyés par le manche. Le moindre effleurement du gouvernail se traduisait immédiatement dans une dérive, une réponse violente et immédiate qui faisait trembler toute la cellule entière. Pierre sentit cette brutalité physique de la réactivité aérodynamique monter en lui, une sensation brute qui contrastait avec son calme intérieur habituel.

Jules apparut à côté, ses mains couvertes de graisse permanente se déplaçant avec une familiarité maladroite vers le capot moteur. Il s'arrêta, sans rien dire, laissant la vapeur chaude et l'odeur d'essence brûlée submerger momentanément les odeurs du métal froid. Le regard de Jules était celui d'une blague retenue, un silence partagé qui parlait plus fort que n’importe quel discours sur le combat à venir.

Pierre observa ce geste bref, cette main posée sur l'épaule, avant que tout ne s'éteigne dans une absence soudaine. Le contact était passé sans mots. Ce fut la seule forme de communication acceptée entre eux. Le silence qui suivit n'était pas un vide, mais une densité partagée, une compréhension tacite de ce qui se déroulerait bientôt en plein ciel gris et brûlant. Pierre vit dans cette lumière incertaine non seulement sa maison, mais aussi l’immensité de la tâche à venir.

Une odeur âcre de combustible brûlé envahit le cockpit minuscule. Le Spitfire n'était pas seulement une machine ; il était la traduction directe des ordres envoyés par le manche. Le moindre effleurement du gouvernail se traduisait immédiatement dans une dérive, une réponse violente et immédiate qui faisait trembler toute la cellule entière. Pierre sentit cette brutalité physique de la réactivité aérodynamique monter en lui, une sensation brute qui contrastait avec son calme intérieur habituel.

Jules apparut à côté, ses mains couvertes de graisse permanente se déplaçant avec une familiarité maladroite vers le capot moteur. Il s'arrêta, sans rien dire, laissant la vapeur chaude et l'odeur d'essence brûlée submerger momentanément les odeurs du métal froid. Le regard de Jules était celui d'une blague retenue, un silence partagé qui parlait plus fort que n’importe quel discours sur le combat à venir.

Pierre observa ce geste bref, cette main posée sur l'épaule, avant que tout ne s'éteigne dans une absence soudaine. Le contact était passé sans mots. Ce fut la seule forme de communication acceptée entre eux. Le silence qui suivit n'était pas un vide, mais une densité partagée, une compréhension tacite de ce qui se déroulerait bientôt en plein ciel gris et brûlant. Pierre vit dans cette lumière incertaine non seulement sa maison, mais aussi l’immensité de la tâche à venir.

Le silence du bureau était plus épais que celui du cockpit. Commandant Bertrand se tenait derrière le bois sombre du fauteuil. Ses cheveux noirs devenus gris en deux ans encadraient un visage fermé, mâchoire serrée sous une lumière faible qui n'était pas celle des projecteurs de hangar mais d'une lampe de bureau oubliée. Il observait Pierre sans bouger. La tension dans la pièce était palpable, lourde comme l’air avant le premier coup de vent.

Pierre entra. Deux minutes. L’échange fut une affaire de présence. Bertrand ne prononça aucun mot sur les ordres qui sommeillaient en lui ; il fit simplement un mouvement de tête vers la porte du bureau. Pierre commença à parler, sa voix grave et atténuée par l'habitude des silences. Il lui donna le numéro de son appareil, une simple séquence de chiffres froides et définitives. Bertrand hocha légèrement la tête, une reconnaissance sans émotion apparente. Puis il sortit, laissant Pierre seul dans la pénombre du bureau.

Pierre resta là, les pieds ancrés sur le tapis usé. L'air dans l'office était sec, chargé d'un parfum de vieux papier et de poussière. Il attendit que le bruit des pas de Bertrand se soit évanoui complètement. Le temps s’étira, chaque seconde s’accrochant au poids du commandement. Les mains de Pierre, grandes et maladroites comme celles d'un pêcheur, tournaient lentement autour d’une cigarette qu’il n’allait jamais allumer sous ces yeux. La guerre ne se résumait pas à la mécanique ou aux manœuvres ; elle était dans cette attente muette, ce poids que seul le silence pouvait porter.

Il resta immobile jusqu'à ce que l'obscurité du bureau soit totale. Il prit son numéro de machine et murmura une seule phrase : "Demain, à l'aube." Ce fut tout. La communication achevée. Pierre quitta la pièce par la porte, laissant derrière lui le poids invisible du regard de Bertrand qui avait jugé sans dire.

Le tarmac était vaste. Une étendue grise sous un ciel qui semblait avoir oublié d'être bleu depuis des jours. Les ombres s’allongent, déformant les contours des avions garés en rang. Pierre marcha lentement vers la bordure de l'espace, se laissant absorber par cette immensité désolée où chaque objet mécanique semblait attendre sa mutation dans le feu. Il sentit la froideur du métal sous ses semelles, une sensation terreuse et solide qui contrastait avec la légèreté effrayante des pensées qui tournaient encore en lui. Le bruit lointain d'un moteur Merlin au repos se mit à résonner dans la brume matinale, un ronronnement bas et profond qui annonçait le retour de la puissance brute. Pierre s’arrêta près du fuselage du Spitfire. La structure était là, massive et élégante, une promesse de violence contenue. Il regarda ses mains, puis le ciel implacable. L'attente devenait sa seule présence.

Le tarmac s’étalait, une étendue grise sous un ciel qui semblait avoir oublié d'être bleu depuis des jours. Les ombres s’allongeaient, déformant les contours des avions garés en rang. Pierre marcha lentement vers la bordure de l'espace, se laissant absorber par cette immensité désolée où chaque objet mécanique semblait attendre sa mutation dans le feu. Il sentit la froideur du métal sous ses semelles, une sensation terreuse et solide qui contrastait avec la légèreté effrayante des pensées qui tournaient encore en lui. Le bruit lointain d'un moteur Merlin au repos se mit à résonner dans la brume matinale, un ronronnement bas et profond qui annonçait le retour de la puissance brute. Pierre s’arrêta près du fuselage du Spitfire. La structure était là, massive et élégante, une promesse de violence contenue. Il regarda ses mains, puis le ciel implacable. L'attente devenait sa seule présence.

Jules Meca apparut à côté de lui. Quarante-six ans, il portait son uniforme comme une seconde peau usée par des décennies de travail acharné. Ses mains, semblables à des enclumes couvertes d’une graisse permanente qui brillait faiblement sous la lumière rasante du matin, se posèrent sur le fuselage froid. Il ne parla pas immédiatement. Le silence entre eux était une chose palpable, dense comme l'huile chaude qu'il sentit déjà emaner de la structure. Jules fit un geste lent vers le nez bombé de l'appareil, puis vers les radiateurs proéminents sous chaque aile. C’était sa manière d'introduire le Grey Ghost : par la reconnaissance technique avant toute parole.

Le Spitfire n'était pas une simple machine ; il était un organisme vivant dont la tension interne se lisait dans la surface du métal. Jules fit tourner doucement son poing, et les vibrations commencèrent à parcourir la cellule. C’était le Merlin qui s'éveillait au sol, une impatience de bête contenu derrière l’acier. Un bruit sourd, un claquement sec : l'hélice cherchait sa cadence dans l'air immobile. La chaleur du moteur Merlin monta alors dans le cockpit, une vague thermique immédiate. L'odeur d'huile chaude et d'essence brûlée envahit leur proximité.

« Regarde ça », commença Jules sans la regarder directement, pointant du doigt la jauge de carburant à l’avant. Le voyant d'huile, pourtant éteint, semblait vibrer sous le regard. Ces battements étaient les pouls de la machine. Ils se parlaient en cette manière-là, par ces indicateurs silencieux, sans avoir besoin de mots pour définir la menace ou la promesse qui y était cachée. Jules décrivit ensuite les rituels : la façon dont le Spitfire réagissait aux sollicitations, ses caprices mécaniques qui nécessitaient une compréhension tacite, celle que seul deux hommes habitués à l'isolement pouvaient partager.

Jules fit glisser sa main sur la gouverne du manche. Le mouvement subtil se traduisit immédiatement dans un changement de pression. Pierre sentit le flux d’air modifier son équilibre. Une turbulence légère, une hésitation invisible. Jules montra alors comment la structure répondait à ce toucher. La brutalité physique d'un virage à six G ne fut pas décrite par des chiffres ; elle fut transmise par le tremblement de la cellule entière sous l'effet de la réactivité aérodynamique. C'était une danse nerveuse entre pilote et machine, un échange constant où chaque onde de choc était enregistrée dans les os du pilot.

Pierre absorba ces informations non pas comme des données froides, mais comme une vérité viscérale sur ce métal qui respirait. Il comprit que le Spitfire était là pour lui : sa maison, son tombeau, et cet espace aérien où il se trouvait n'était qu'une extension de cette existence mécanique. Le silence s’installa à nouveau entre eux, plus lourd désormais, rempli de ce langage partagé qui valait plus que tout discours. Jules attendit la réponse physique de Pierre, une confirmation muette de sa connexion profonde avec cet être vivant.

Jules décrivit ensuite les rituels : la façon dont le Spitfire réagissait aux sollicitations, ses caprices mécaniques qui nécessitaient une compréhension tacite, celle que seul deux hommes habitués à l'isolement pouvaient partager. Jules fit glisser sa main sur la gouverne du manche. Le mouvement subtil se traduisit immédiatement dans un changement de pression. Pierre sentit le flux d’air modifier son équilibre. Une turbulence légère, une hésitation invisible. Jules montra alors comment la structure répondait à ce toucher. La brutalité physique d'un virage à six G ne fut pas décrite par des chiffres ; elle fut transmise par le tremblement de la cellule entière sous l'effet de la réactivité aérodynamique. C'était une danse nerveuse entre pilote et machine, un échange constant où chaque onde de choc était enregistrée dans les os du pilot.

Pierre absorba ces informations non pas comme des données froides, mais comme une vérité viscérale sur ce métal qui respirait. Il comprit que le Spitfire était là pour lui : sa maison, son tombeau, et cet espace aérien où il se trouvait n'était qu'une extension de cette existence mécanique. Le silence s’installa à nouveau entre eux, plus lourd désormais, rempli de ce langage partagé qui valait plus que tout discours. Jules attendit la réponse physique de Pierre, une confirmation muette de sa connexion profonde avec cet être vivant.

Puis, le silence se rompit par un changement de régime. Le cockpit sentit soudain la montée en puissance du moteur. Ce n'était pas l'odeur habituelle de l'huile chaude et du cuir ; c'était une surchauffe métallique, âcre, celle d'une machine poussée à ses limites. Jules tira sa cigarette dans la bouche. Il la fixa, fumant lentement, un geste qui marquait la fin de la routine et le début de quelque chose de plus urgent.

Pierre sortit du cockpit. Le sol était sec sous les bottes. La nuit tomba enfin sur la base, laissant place à cette transition étrange où le ciel devenait le décor dominant. Jules se tenait près du fuselage froid du Grey Ghost, scrutant l'horizon là où il s’y étirait une lumière rasante et mourante. Le Spitfire reposait, immense et silencieux sous les lumières tamisées de la base. La texture des nuages était celle d'une matière épaisse, presque palpable, une promesse de mouvement ou de catastrophe. Pierre regardait ce spectacle avec ses yeux gris-vert, ceux qui ne cherchaient pas le danger mais l’immensité désolée où chaque objet mécanique semblait attendre sa mutation dans le feu.

Le ciel se blanchissait doucement à l'est. Cette couleur n'était pas celle du jour imminent ; c'était une teinte irréelle, presque spectrale, qui effaçait les contours de la terre sous eux. L’horizon se courbait légèrement sur lui-même comme un vieux drap déchiqueté. Pierre vit dans ce ciel son lieu de vie et sa tombe, cette vaste étendue où rien ne s’arrêtait jamais. La peur n'arriva pas soudain ; elle s'installa comme une pression sourde dans la poitrine, le corps se raidissant avant même que le premier souffle ne soit aspiré. Gorge sèche. Mains qui serrent le manche trop fort sur la console du Spitfire. Vision tunnel lors d'une attaque adverse – l'image n'était plus claire qu'un flou mouvant et implacable. Le temps se dilatait dans les deux secondes avant que l'action ne soit forcée par l'appui sur la gâchette. L’adrénaline qui suivait le combat : les mains tremblaient, les genoux étaient en coton.

Il y avait alors un autre geste. Jules s'approcha de Pierre, sa trapu silhouette se découpant contre le métal sombre du Spitfire. Il ne dit rien. La main de Jules vint reposer sur l'épaule de Pierre pendant deux secondes. C’était cela : la camaraderie silencieuse. Une présence sans mot. Puis il recula, et tout s'effaça à nouveau dans la contemplation du ciel qui se consumait en une aube sanglante. Le silence revint entre eux, plus ancien que le métal, plus solide qu’un ordre de combat. Pierre resta là, absorbant l'immensité désolée au-delà des lignes tracées, son corps figé dans cette tension physique qu'il connaissait mieux que ses propres pensées.



### Questions de coherence
- Le ton reste-t-il sobre et retenu — aucune envolée lyrique gratuite ?
- Les éléments de focus demandés ([CIEL], [MACHINE], [PEUR], [CAMARADERIE], [MORT]) sont-ils présents et développés avec soin là où ils sont demandés ?
- Les émotions sont-elles montrées via le corps ou les gestes, jamais nommées directement ?
- Le temps s'écoule-t-il différemment dans le ciel (lent, sensoriel) et à terre (rapide, fonctionnel) ?
- Les dialogues respectent-ils la voix propre de chaque personnage ?

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

AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : [RIEN]
DEFAUT_MAJEUR : [RIEN]