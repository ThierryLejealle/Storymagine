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
Un grondement sourd, régulier, portait à travers le voile matinal qui enveloppait Thorney Island. Le bruit des moteurs Merlin s’éveillait au loin, un bourdonnement mécanique et lointain qui semblait vibrer dans la terre humide sous les bottes de Pierre. L'air était d'une froideur mordante, saturé par une humidité épaisse qui avait le goût métallique du givre naissant sur l'herbe mouillée. Le transport militaire s’était arrêté quelques minutes plus tôt, laissant derrière lui un silence relatif que seule cette pulsation régulière osait rompre.

Sur le tarmac, les Spitfires s'étiraient dans la brume grise, une rangée de silhouettes familières dont les hélices restaient figées comme des insectes immobiles sous un ciel bas et uniformément voilé. Les avions formaient une mosaïque sombre de vert foncé et d’argent mat, leurs radiateurs proéminents semblant absorber le peu de lumière disponible. Pierre descendit du transport et commença à marcher lentement vers la zone d'alignement. Ses mains, larges et calleuses comme celles d'un pêcheur habitué aux glaces froides, s'habituèrent immédiatement au froid mordant qui s'insinuait sous les manteaux militaires.

Atteint de l’espace désigné, il posa son sac en toile sur le sol mou du tarmac avec un léger bruit de friction contre le bitume humide. Il ne fit aucun geste brusque ; Pierre était dans la contemplation tranquille des débutants qui observaient une scène complexe pour s'y intégrer. Levant enfin les yeux vers l’immensité supérieure, il observa le ciel : une couche dense et uniforme, où le gris se fondait dans un bleu pâle et éteint à peine par l'aube. La lumière rasante de ce premier matin peinait à percer la brume stratifiée qui recouvrait tout l’horizon, conférant au monde des teintes monochromes sublimes et mélancoliques. L’air, froid comme du verre, portait avec lui cette odeur indéfinissable d'essence fraîche et de métal mou exposé à l'humidité matinale.

Le passage du tarmac à l’intérieur fut un choc thermique discret, mais perceptible. La froideur mordante de l’aube céda à la chaleur statique et confinée d'un bâtiment ancien où le temps semblait s'être ralenti dans les murs épais. Pierre suivit son officiant jusqu'à une pièce aux proportions austères, éclairée par des fenêtres hautes dont le verre filtré donnait à l’intérieur un aspect sévère. L'odeur âcre du papier empilé et d'un tabac vieux imprègne l'air, se mêlant au relent ambré d’un café froid laissé trop longtemps sur une table de bois massif.

Il s’arrêta devant le bureau du Commandant Bertrand. Là, l'officier était déjà assis derrière un tas de dossiers militaires et des cartes froissées que les doigts avaient tachées de graphite. Le Commandant ne leva pas immédiatement les yeux. Ses mains se posèrent sur la surface en bois sombre, paumes ouvertes, avant qu'il ne reprenne sa contemplation du bureau. Pierre resta immobile, ses yeux gris-vert scrutant le visage fermé de l’homme, observant l'éclat sourd des cheveux gris qui contrastait avec la fermeté de sa mâchoire.

Le silence s'installa, dense et précis, pesant comme une chape sur les deux hommes. Ce n’était pas un silence vide ; c’était le poids d'un commandement transmis en quelques secondes, la gravité implicite des choix à venir. Le Commandant Bertrand enfin leva son regard, ne parlant qu'une fois pour évaluer Pierre de haut en bas : grande silhouette maigre, mains trop grandes. Puis il se pencha légèrement vers l'avant, le visage encore plus sombre dans l’ombre du bureau.

« Ton numéro est 14-B », annonça Bertrand d'une voix monocorde, dénuée de tout artifice ou chaleur superflu. Ce n'était pas une question ni un ordre en soi, mais une simple donnée factuelle qui venait ponctuer le silence. Pierre hocha la tête, absorbant l’information sans commentaire, ses yeux fixés sur les doigts du Commandant. L’officier continua son évaluation, balayant brièvement le jeune homme pour s'assurer qu'il n'y avait aucune hésitation dans sa posture.

« Demain, à l’aube », ajouta Bertrand, la phrase courte et définitive comme un coup de marteau sur une enclume. Il ne chercha pas à expliquer pourquoi ou comment ; il donnait simplement les paramètres du jeu. Le poids silencieux entre eux se résuma alors à cette unique directive : le vol est programmé pour demain.

Le Commandant renché la conversation, refermant ses dossiers avec un bruit sec et précis qui résonna dans la pièce calme. Pierre redressa son manteau et fit demi de tour. Il quitta le bureau sans hâte, laissant derrière lui l’odeur âcre du papier et le silence lourd que Bertrand avait laissé en place.

Pierre traversa le bâtiment, laissant derrière lui la clarté forcée du bureau et l’atmosphère confinée de l'intérieur. Au sortir des portes doubles, il fut accueilli par un mélange puissant d'air frais et de cette odeur indéfinissable qui émane toujours des hangars : une synergie complexe entre le kérosène brûlé, la graisse chaude et l'huile moteur. Jules Meca attendait près d’une petite grille de service, son manteau taché de plusieurs couleurs de graisses différentes, sa moustache rigide contrastant avec le mouvement précis qu’il faisait en attendant que Pierre arrive. Le mécanicien ne dit pas bonjour ; il émit plutôt un grognement bref qui servit de confirmation mutuelle avant de se mettre en marche vers la piste principale.

« On y va », lança Jules, sa voix un peu rauque par le temps passé à crier au-dessus des moteurs. Il marchait d’un pas lourd et assuré, écartant légèrement les jambes pour maintenir son équilibre sur le bitume froid du tarmac. Après quelques minutes de marche, ils atteignirent la zone où se trouvait une file compacte d'avions stationnés. Jules s’arrêta devant un Spitfire, dont les ailes elliptiques semblaient capter chaque rayon de lumière matinale. Il ferma les yeux pendant une seconde, comme pour absorber l'immensité du ciel au-dessus des machines avant de se concentrer sur la tâche qui lui tombait à cœur.

Le mécanicien commença son inspection rituelle. Ses mains, grandes et puissantes, commencèrent à tapoter méthodiquement les ailes. Le bruit métallique des outils — un petit jeu de clés et une brosse à rivets — claqua régulièrement, créant une partition méthodique dans le silence du matin. Jules désigna l’avant de la machine, son doigt pointé vers le capot moteur. « Ce monstre est capricieux », expliqua-t-il sans aucune ironie, mais avec un sérieux absolu. Il détailla les exigences minimales : un certain couple précis pour le démarrage, des vérifications sur chaque joint d'huile qui ne toléraient aucun défaut, et une attention particulière aux lignes de refroidissement du Merlin 66. Le pilote devait comprendre que l’avion n'était pas seulement un assemblage de métal ; c'était un organisme exigeant, dont les humeurs devaient être respectées pour garantir la fiabilité.

Il s’agenouilla près des roues, puis passa à l’hélice. Il attrapa la pale droite entre son pouce et son index, ressentit sa texture lisse sous le vernis de protection, et fit pivoter légèrement l'hélice dans un geste expert. « Les moteurs Merlin », continua Jules en se redressant, les yeux brillants d’une fierté technique, « ils sont beaux, ces V12. Mais ils ne chantent pas pour n’importe qui. Ils exigent une écoute. Il faut entendre chaque changement de régime, le sifflement des soupapes à pleine charge. C'est la seule façon de savoir si tout est en ordre. » Pierre resta immobile, observant l'ensemble de ces gestes précis et cette description technique, assimilant les informations sans rien commenter.

Jules balaya les yeux sur les lignes aérodynamiques du Spitfire, s’attardant un instant sur le nez court et bombé. Il sourit alors légèrement, mais ce sourire ne toucha pas toute sa face — il restait dans la circonférence de son regard d'homme. « Tu vas connaître une belle chose », murmura-t-il en tapotant une fois l’aileron porteur. « On l'appellera Grey Ghost. »

La nuit avait glissé sur la base avec une lenteur presque palpable. Pierre ne trouvait pas le sommeil ; il attendait plutôt que l'aube vienne, ou qu'elle passe simplement à côté de lui. Dans le dortoir, les autres pilotes dormaient dans un silence pesant, interrompu seulement par des grognements réguliers et monotones — le bruit du repos enfin trouvé après une journée de tension. Il ouvrit lentement les yeux, observant la silhouette sombre qui occupait son lit, se déplaçant légèrement au rythme de sa propre respiration calme. Au bout d’un moment où les ombres semblèrent s’immobiliser, Pierre quitta doucement son lit pour traverser le couloir étroit et faiblement éclairé par des ampoules jaunes au plafond. Il remarqua que dans la salle commune, même les figures habituelles étaient silencieuses ; un pilote lisait seul à une table, une autre se tenait près de la fenêtre en contemplant l'obscurité totale qui enveloppait le paysage militaire. Ce calme partagé était leur habitude, cette tranquillité collective où les mots devenaient inutiles, et tout ce qui comptait était le simple fait d'être là, ensemble, dans cette attente muette du jour suivant.

Après un court moment de contemplation silencieuse, Pierre décida de rejoindre l’extérieur. Il quitta le bâtiment et se mit à marcher vers la zone des hangars où les avions étaient stationnés. L’air frais mordit légèrement ses joues alors qu'il traversait les allées du terrain. Le vent nocturne, plus vif que prévu, s'engouffra dans l'allée longeant le hangar principal et balaya doucement la poussière sur l'asphalte. Il aperçut immédiatement le Grey Ghost sous le projecteur de service ; il y était garé, immobile et vigilant. Pierre s’approcha du Spitfire en marchant vers lui, laissant son regard parcourir les lignes élancées de l'aile elliptique. Le métal, malgré sa présence imposante, dégageait une froideur absolue à son toucher. Il posa sa main sur le fuselage lisse du Grey Ghost et sentit immédiatement cette sensation glacée qui passait à travers ses gants, une fraîcheur typique des objets exposés à l’air nocturne. Au-dessus de lui, le ciel commençait enfin à révéler son changement subtil ; la vaste étendue d'encre noire qu'il avait vue toute la nuit se transformait lentement en un dégradé étrange. À l’est, là où les premières lumières commençaient à poindre derrière les collines éloignées, le noir profond cédait progressivement à une teinte de gris pâle et blanchissant, comme si l'aube commençait timidement à déchirer la voilée obscurité.



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

AMELIORATION : Le personnage de Pierre est très bien décrit physiquement et par ses actions, mais il manque un élément d'introspection ou une réaction plus personnelle à l'environnement pour qu'il ne soit pas perçu uniquement comme un observateur passif.
DEFAUT_SIGNIFICATIF : L'absence totale de tension interne dans le chapitre rend Pierre très fonctionnel, mais cela freine la progression vers son arc narratif (l'homme qui sera brisé). Il manque une petite graine d'inquiétude ou de motivation personnelle pour que le lecteur s'y attache émotionnellement.
DEFAUT_MAJEUR : [RIEN]