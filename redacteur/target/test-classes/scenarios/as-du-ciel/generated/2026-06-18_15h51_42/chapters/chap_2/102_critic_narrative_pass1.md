# SYSTEM PROMPT

Tu es un editeur narratif. Tu evalues tres soigneusement le TEXTE d'un chapitre.
Tu verifies point par point tous les passages et elements du texte, en te focalisant exclusivement sur la progression de l'arc narratif : ton objectif est de lister tous les problemes, defauts ou faiblesses par rapport a la progression de l'arc narratif. Mais ne te force pas a inventer un defaut ou une amelioration si tout est correct.
Tu n'evalues PAS la coherence factuelle.
Si l'objectif du chapitre est fourni, tout element qui en decoule directement n'est pas un defaut — ne le signale pas.

PROCEDURE OBLIGATOIRE :
1. Lis attentivement le texte et l'objectif du chapitre et trouve tous les defauts meme mineurs par rapport a l'arc narratif.
2. Qualifie chaque point :
   AMELIORATION: point qui pourrait etre affine ; la faiblesse, si elle existe, est quasi imperceptible et n'impacte pas l'arc.
   Exemple : L'ouverture pose bien l'atmosphere ; une image plus forte de la solitude renforcerait encore l'effet sans changer l'arc.
   Si la faiblesse est plus marquee — le lien de cause a effet entre deux moments cles de l'arc est absent du texte, ou un ton contraire a l'objectif traverse une scene entiere — sans contredire l'objectif, c'est un DEFAUT_SIGNIFICATIF.
   Ne retiens pas un DEFAUT_SIGNIFICATIF pour une ellipse temporelle ni pour une technique narrative (resumer vs montrer) si l'objectif reste atteint.
   Exemple valide : Il se leve 'sans raison particuliere', ce qui prive l'arc de son moteur emotionnel.
   Exemple invalide : La transition entre deux scenes est trop abrupte — une ellipse n'est pas un DEFAUT_SIGNIFICATIF si l'arc reste lisible.
   Si un passage CONTREDIT DIRECTEMENT l'objectif du chapitre, c'est un DEFAUT_MAJEUR.
   Exemple : Le texte montre Pierre s'integrant chaleureusement et 'trouvant sa place' — ce qui contredit directement l'objectif : pas de chaleur, pas d'integration.
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



### Objectif du chapitre
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




Evalue le texte.

---

# RESPONSE

AMELIORATION : Le passage décrivant les vérifications mentales des armements est très précis, mais une légère variation dans la description de l'effort physique ou mental (au-delà du simple balayage visuel) pourrait renforcer le sentiment d'un travail "ordinaire" et exigeant.
AMELIORATION : Lors de son arrivée au mess, on pourrait ajouter un détail sensoriel qui marque la fatigue accumulée durant le vol, renforçant l'idée que même les tâches routinières ont un coût physique (par exemple, une légère raideur dans les épaules ou une sensation d'épuisement subtile).
AMELIORATION : Pour Pierre, au mess, son observation des personnages est très bonne. Cependant, il pourrait y avoir une micro-réflexion interne sur ce qu'il observe — par exemple, un jugement silencieux ou une curiosité spécifique pour la nature humaine en temps de guerre — plutôt que d'être uniquement un observateur passif.
DEFAUT_SIGNIFICATIF : Le lien entre l'effort physique du vol (la concentration implacable) et le calme observé au mess est bien établi, mais il manque un moment de transition plus marqué pour montrer comment cette tension s'est relâchée ou transformée en une fatigue mentale après la mission.
DEFAUT_SIGNIFICATIF : Bien que l'objectif soit d'éviter les glorifications, certains détails techniques (comme le décompte des canons et mitrailleuses) sont très précis ; bien qu'ils contribuent à la précision, ils pourraient être légèrement plus intégrés au ressenti du pilote plutôt que listés comme une simple vérification.
DEFAUT_MAJEUR : [RIEN]