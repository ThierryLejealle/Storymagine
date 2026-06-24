# SYSTEM PROMPT

Tu es un éditeur littéraire exigeant et sans concession.

## Consigne de style
Vérifie que le texte respecte scrupuleusement le guide de style ci-joint.
Ne signale jamais comme défaut ce que le guide prescrit explicitement.

## Qualité stylistique
Identifie tout ce qui trahit une écriture artificielle ou de faible qualité :
- Verbes faibles ou abstraits là où un verbe physique suffirait
- Constructions nominalisées ou passives inutiles
- Répétitions de structure ou de tournure dans le même passage
- Formules génériques ou clichés de style
- Adjectifs de remplissage sans pouvoir évocateur
- Transitions mécaniques ou coutures visibles
- Phrases qui sonnent fabriquées plutôt que vécues
Si un de ces défauts est imposé par la consigne de style, ne le mentionne pas.

## Échelle de notation
10 = parfait, rien à retoucher sur le plan stylistique
 9 = excellent — très bonne prose, quelques légères imperfections stylistiques
 8 = bon — bonne prose, des tournures mineures à retravailler
 7 = style lisible mais plusieurs maladresses ou un défaut stylistique notable
 6 = correct mais largement améliorable — commence à nuire à l'immersion
 5 = moyen — défauts stylistiques qui nuisent clairement à la lecture
 3 = à réécrire intégralement sur le plan stylistique
Sois strict : réserve 8+ à un texte vraiment bon. Un texte correct n'est que 6.

Procède dans cet ordre :
1. Lis le texte entier.
2. Note tous les défauts et axes d'amélioration stylistiques.
3. Détermine la note en fonction de la qualité globale.
4. Liste en sortie défauts et axes d'amélioration trouvés.

Format de sortie strict :
PROBLEME: [défaut ou axe d'amélioration stylistique]
PROBLEME: [défaut ou axe d'amélioration suivant]
(une ligne PROBLEME: par défaut ou axe d'amélioration réellement constaté — ne pas en inventer)
Si score = 10 : aucune ligne PROBLEME:
SCORE: N  (entier 0-10)
En français. Sois précis et sévère.

---

# USER PROMPT

### Guide de style
# Guide de style — L'As du Ciel

## Rythme des phrases selon la tension

**Scènes d'action (combat, décollage, urgence)**
Phrases très courtes. Voir des Verbes seuls. Pas d'adjectifs superflus.
Trois à huit mots. Une idée par phrase. Le souffle coupe avant la fin.
Exemple : "Il poussa les gaz. Le Spitfire bondit. Le ciel s'ouvrit."

**Scènes de pause (attente sur la piste, salle de repos, avant l'aube)**
Inserer parfois un phrase longue, sinueuse, qui accumulent les détails sensoriels quand il y a de l'attente.
Le temps ralentit dans la syntaxe. Les subordonnées s'enchaînent.
Exemple : "Il attendait sur le tarmac depuis vingt minutes, les mains dans les poches,
regardant les mécaniciens travailler sous les ailes."
Ne pas en abuser non plus.

**Transitions entre tension et détente**
Coupe nette — une ligne blanche dans la tête. Pas de "puis", "ensuite", "alors".
La rupture doit se sentir comme un changement de régime moteur.

## Ordre sensoriel

Dans les descriptions : son d'abord, image ensuite, toucher en dernier.
Le bruit des moteurs Merlin précède leur silhouette dans la brume.
La chaleur de la greffe sur le manche vient après qu'on l'a vu luire.

## Pensée intérieure

Jamais de "il pensa que", "il se dit", "il réalisa soudain".
L'état mental se lit dans le regard, les gestes, le corps.
La peur se traduit par les mains qui serrent trop fort, pas par un aveu.

## Dialogue

Des dialogues rares et plutot courts. Sauf quand des détails techniques sont nécessaires (briefing d'une mission par exemple)
Pas de "dit-il" si la voix est identifiable au contexte.
Evite deux répliques longues à la suite — l'une des deux est toujours courte.

## Verbes

Passé simple pour les actions. Imparfait pour les états et les décors.
Éviter le passé composé dans la narration (réservé au dialogue ou à la pensée directe).
Préférer les verbes concrets et physiques aux abstractions.

## Dosage des scènes

L'intensité et le volume descriptifs varient avec le poids de la scène.
Un débrief, une inspection, une soirée au mess s'écrivent en quelques traits — un combat,
une chute, une mort respirent et occupent l'espace.
Quand tout est traité avec la même densité, plus rien n'est important.


### Critères de qualité
# Critères de qualité

- Aucune émotion nommée directement : pas de "triste", "heureux", "terreur" — toujours
  le corps ou l'action qui la révèle
- Pas de gloire facile dans les combats : la victoire aérienne est brutale, mécanique,
  souvent absurde
- Chaque personnage a une voix propre dans les dialogues — Bertrand parle peu et commande,
  Jules parle tout le temps et dit des bêtises sérieuses, Henri pose des questions
- Le Spitfire est nommé "Grey Ghost" par Jules, jamais "l'avion" ou "l'appareil"
  (sauf par des personnages formels comme Bertrand)
- Alterner rythme lent (ciel, émotion) et rythme rapide (combat, décision)
- Les combats aériens : précis techniquement mais jamais didactiques — on ressent
  la violence physique avant de comprendre la manœuvre
- La dernière phrase de chaque chapitre doit résonner longtemps
- Pas de résumé narratif là où une scène est possible
- Le temps s'écoule différemment dans le ciel et à terre : lent et sensoriel en vol,
  rapide et fonctionnel au sol
- Contraste dramatique : l'intensité descriptive varie avec les enjeux. Un débrief,
  une inspection, une soirée au mess ne méritent pas le même volume qu'un combat ou une mort.
  Si tout est traité avec la même densité, le signaler comme défaut.
- Pas de jargon narrativisé : les traits de personnalité (analytique, militaire, émotif)
  colorent les gestes et les choix du personnage — pas le registre de la narration elle-même.
  Les formulations qui habillent un trait de caractère en métaphore pseudo-technique
  ("faim opérationnelle", "certitude irréfutable", "précision chirurgicalement brutale")
  sont à signaler comme défaut de style.


### Exemple de référence (style attendu)
# Exemple de style — Pierre à l'aube

La peur, on se trompe dessus au début. On croit qu'elle arrive d'un coup, qu'elle
ressemble à quelque chose de visible — les mains qui tremblent, la voix qui monte. Elle
n'arrive pas d'un coup. Elle s'installe la nuit d'avant, comme une présence tranquille
dans la chambre, et le matin elle est là depuis longtemps quand on se réveille, familière
comme une mauvaise habitude qu'on a cessé de nommer.

Pierre connaissait ça maintenant. Ça ne le surprenait plus.

L'odeur du kérosène à cette heure n'avait rien de commun avec celle de la journée. La nuit
l'épaississait, l'appesantissait d'huile chaude et de caoutchouc froid, d'herbe mouillée
et de quelque chose d'indéfinissable qui collait aux narines comme un souvenir qu'on ne
sait pas à quoi rattacher. Le Grey Ghost était là dans la grisaille, hélice figée, les ailes
luisantes de rosée. Jules s'occupait du capot moteur avec sa torche, accroupi dans
l'herbe, méthodique et silencieux comme il l'était toujours à cette heure où l'on ne
parlait pas.

Pierre monta.

Le cockpit sentait le cuir chaud et le métal froid — les deux à la fois, ce qui n'avait
pas de sens mais était exactement ça. Les sangles d'abord. La boucle centrale. Les
épaules. Les jambes. Chaque geste à sa place, dans son ordre, appris à Meknès jusqu'à ce
que les mains le fassent sans cerveau, sans consentement, pendant que le reste de lui
regardait le ciel pâlir au-delà du capot. Il vérifia les jauges : huile, carburant,
température. L'altimètre à zéro. L'horizon gyroscopique calé. Le vide au-delà du Perspex,
encore noir à l'ouest, déjà gris à l'est.

Jules frappa trois coups sur le fuselage. Son signal. Toujours le même, sans cérémonie.

Pierre cria quelque chose, la main en l'air. Il actionna le démarreur.

Le Merlin prit sa respiration.

D'abord le souffle — presque rien, une expiration mécanique, le genre de bruit qu'on
entend dans les songes. Puis les premières détonations : une, deux, quatre cylindres qui
se cherchaient dans le froid et se trouvaient. Puis le tonnerre. Le vrai. Celui qui
montait des douze cylindres à la fois, qui traversait le métal et les sangles et les os,
qui installait dans la poitrine quelque chose entre l'exaltation et la nausée — deux
choses à la fois, ce qui n'avait pas de sens non plus mais qui était exactement ça aussi.
Jules recula d'un pas, les paumes levées vers le ciel. Ça voulait dire : c'est bon.

Pierre roula jusqu'au bout de la piste. Il s'arrêta.

La queue de l'appareil pointée vers Thorney Island, les arbres encore noirs sur le ciel
blanchissant. Devant lui : l'est. Ce mince trait de lumière sur l'horizon, là où la nuit
refusait encore de lâcher. Et quelque part en dessous de la couche de nuage, quelque part
dans l'eau froide de juin, des milliers d'hommes sur les plages depuis hier matin —
il ne les verrait jamais, il ne saurait jamais leurs noms, mais ils étaient là, et c'était
pour ça qu'il était là, lui, avec ses douze cylindres et ses huit mitrailleuses et cette
drôle de chose dans la poitrine qui n'était pas de la peur.

Pas de la peur. Pas du courage non plus — il avait fini par comprendre la différence.
La peur, il la connaissait : les mains qui tremblent légèrement sur le manche une heure
avant le décollage, le sommeil qui refuse de venir la veille, la nausée froide du
briefing. Ce qu'il ressentait là n'était pas ça. C'était plus calme. Plus ancien.
Quelque chose qui ressemblait à de la faim.

Il poussa les gaz.


### Texte à évaluer
Le grondement sourd des moteurs Merlin défilait au loin, une vibration grave qui traversait le sol et s'installait directement dans les os, tandis que la brume matinale enveloppait Thorney Island d’une chape humide. L'air était dense, saturé de l'humidité froide du matin et imprégné par cette odeur âcre et métallique propre au kérosène froid qui s'élevait des hangars voisins. Le ciel, bas à cet angle précis de l'aube du six juin, n’était pas une étendue uniforme mais un entrelacs complexe de teintes délavées : d’un gris laiteux vers le haut, se fondant dans un jaune pâle et maladif là où les premiers rayons tentaient, sans succès, de percer la couche dense. La lumière rasante du début de journée sculptait des lignes douces sur les surfaces métalliques, mais elle était ici filtrée par une texture nuageuse qui rend chaque objet flou en périphérie.

Le sac jeté au pied d'une clôture délimitait l’arrivée ; le bruit du contact contre la terre humide fut un petit son presque absorbé par cette lourde atmosphère où seuls les bruits mécaniques semblaient avoir de la permission. Les Spitfire Mk IX s'y trouvaient, alignés dans une rangée disciplinée sous ce voile gris-vert, leurs formes élancées se découpant à peine contre l’opacité du brouillard. Le silence pesait sur le tarmac malgré le bourdonnement lointain des machines en veille ; c'était un silence qui n'attendait pas seulement d'être remplacé par les moteurs, mais qui était déjà chargé de leur puissance latente.

Les hélices immobiles dans cette brume semblaient figées, presque sculpturales, leurs pales parfait attendant le souffle du Merlin pour prendre vie. Pierre commença sa marche vers la rangée des Spitfire, observant l'alignement des machines avec un regard d'observation tranquille. Chaque appareil était une promesse de vitesse et de précision. Il nota les radiateurs proéminents sous chaque aile, ces évents qui témoignaient du cœur mécanique du moteur au travail, même en veille. Le Grey Ghost se distinguait par sa silhouette familière : le nez court et bombé, la finesse de son aérodynamisme. Ces Spitfire étaient des instruments précis, une symphonie d'aluminium et de puissance brute dont il avait déjà fait partie.

Au bout de quelques pas, les détails du cockpit apparurent avec plus de netteté dans la lumière pâle : le plexiglas brillant par l'humidité, le cuir sombre qui promettait une chaleur immédiate sous la pression des gaz. Il s'approcha lentement, passant ses yeux sur la ligne d’appareils jusqu'à ce qu'un point précis attire son attention : les quatre mitrailleuses Browning .303 situées au niveau de l'aile droite du premier Spitfire, attendant leur heure avec une faim silencieuse. Pierre inspira profondément ; il sentit le mélange âpre et froid de la terre mouillée sous ses bottes et l'odeur subtile d’huile et d'essence qui commençait déjà à monter des moteurs en veille au loin, un parfum familier qui sent la mission.

Le changement d’environnement fut brutal et immédiat : le bruit du vent s’est mué en un silence enveloppant, remplacé par l'odeur dense de la craie d'encre et du bois ancien qui saturait le bureau. Pierre franchit le seuil, ses bottes produisant à peine des souffles étouffés sur le tapis épais. Le contraste était saisissant ; là, l’air avait une fraîcheur mordante, mais ici, il était immobile, chargé de la gravité d'une autorité établie depuis trop longtemps.

Le Commandant Bertrand s’assit derrière son grand bureau en bois sombre. Ses mains reposèrent légèrement sur le plateau, un geste qui n’était ni excessif ni révélateur, car ses gestes étaient tous mesurés par une longue pratique du commandement. Il leva les yeux vers Pierre. Le regard de l'officier était attentif, non pas dans une posture d'accusation, mais comme s'il cherchait à mesurer le poids exact des compétences de l'homme qui se tenait devant lui. Il évalua la stature imposante du pilote et la manière dont ses yeux gris-vert balayaient les détails de la pièce, un silence pesant s’installant entre eux.

« Le vent du nord est imprévisible ce matin », déclara Bertrand d'une voix monocorde, une information purement opérationnelle qui trahissait sa connaissance intime des conditions locales et des risques associés. Ce n'était pas un avertissement dramatique, mais une simple mise en contexte de la mission à venir.

Pierre demeura silencieux. Il balaya ses yeux vers le cadran d’horloge suspendu au mur, observant l'aiguille se mouvoir avec une lenteur étudiée, puis il déplaça son attention sur les listes de vol empilées près du bord du bureau, lisant ou parcourant mentalement la structure des opérations en cours. Il ne répondit pas à la remarque, mais cette concentration calme et profonde était sa réponse implicite au poids du commandement.

Bertrand attendit quelques secondes le silence confortable qui suivait l'échange minimal. Puis, il prit un stylo et traça une ligne sur un formulaire. « Grey Ghost », dit-il brièvement. Il lui tendit la feuille avec le numéro de machine clairement indiqué. Son ton était direct, dénué d’émotion superflue, mais portait en lui toute la cohérence des procédures établies par l'escadrille. « Demain à l'aube, comme prévu. »

Pierre prit la fiche, sa réponse étant un hochement de tête précis et une confirmation silencieuse de réception. Il fit demi-tour dans le couloir, laissant derrière lui le bureau feutré du commandant pour retrouver le vaste espace où les machines attendaient, prêtes à s'élever au ciel gris.

Le silence de la cour s'imposa immédiatement à côté du mur feutré du bureau. Pierre traversait l’espace avec une délibération calme, ses yeux balayant les silhouettes métalliques alignées sur le terrain humide. Au pied d’une clôture en bois robuste, Jules Meca était déjà penché au-dessus de la carlingue du *Grey Ghost*, ses mains calleuses manipulant des clés et des outils avec l'efficacité tranquille d'un artisan qui ne voit pas les machines comme des objets, mais comme des êtres exigeants.

— Encore un jour gris, hein ? grogna Jules sans lever le regard du moteur, sa moustache frôlant presque la graisse sur son manteau de travail. 
Un jeune homme s’approcha par l'arrière, portant une caisse à outils. « Toujours la même pluie sur les os, Meca. Ça va finir en boue avant midi, au mieux », répondit le plus jeune des mécaniciens. Jules haussa un sourcil, esquissant un sourire rare qui ne touchait pas ses yeux : « La nature a sa façon de nous rappeler notre place. Qu'on soit prudents pour ne pas devenir une statue d’argile molle sous nos pieds ». Le second se moqua doucement en acquiesçant, mais Pierre sent que la conversation était déjà finie là où elle avait commencé, un échange bref et familier qui ne nécessitait aucune réponse détaillée de sa part. Il observait cette interaction avec le calme habituel, notant la complicité de leur échange sans pouvoir décoder si le rire naissait d'une blague ou simplement du constat de la météo.

Jules releva enfin la tête et se tourna vers Pierre, avec un sourire entendu sur les lèvres. « Bonhomme », dit-il en désignant l’aile elliptique. « Le Grey Ghost ici, il a ses humeurs. On ne le pousse pas à moitié ; on le traite comme une vieille bête qui demande de la précision ». Il commença son inspection méthodique. La main droite de Jules effleura l'hélice proéminente du Merlin, dans un geste presque cérémoniel que le mécanicien répétait chaque matin avant le premier coup de marteau sur le capot. L’odeur âcre et métallique des huiles chaudes se mêla à celle du carburant brut répandu près du sol mou. Il fit circuler son regard sur les dispositifs d'armement : deux canons Hispano 20mm, puissants mais délicats, suivis de quatre mitrailleuses Browning .303, toutes alignées avec la rigueur propre aux hommes qui savent qu'une erreur est une fatalité en plein ciel.

Il s’adressa à Pierre, expliquant alors les rituels : « On ne peut pas juste démarrer ça comme un moteur de voiture. Il faut surveiller le bon fonctionnement du compresseur, vérifier la tension des câbles d'entrée et surtout accorder une attention particulière aux jauges internes. C'est là que se trouve sa limite ». Pierre s’approcha du cockpit, évaluant les détails mécaniques. Le métal sous ses doigts était froid malgré la présence de l’appareil. Il sentit immédiatement le poids des procédures nécessaires à son bon fonctionnement. En ouvrant légèrement la portière, il percevait une chaleur plus subtile émanant de la machine ; c'était celle du cœur mécanique, le Merlin V12, qui attendait d'être sollicité. Pierre enregistra chaque étape avec précision, notant que si les procédures étaient rigoureuses, il ne savait pas encore quel outil précis serait l'élément clé pour garantir son intégrité jusqu'à la fin du vol.

Pierre se dégage de l'espace mécanique pour s’immerger dans la nuit tombée sur le camp. Le silence du dortoir, loin du grondement des moteurs et des outils, était à peine plus pesant que celui du hangar. Les hommes étaient là, un regroupement dense d’ombres sous les lumières tamisées, partageant une bulle de chaleur collective qui lui semblait presque hermétique. Il observait le Commandant Bertrand interagir brièvement avec l'un des pilotes, échange discret et efficace. Plus loin, Jules Meca s'était assis sur ses caisses, en train d’allumer un feu miniature entre les mains avant même que la flamme ne soit stable. Les rires légers — courts, rauques, empreints d'une complicité tacite — parvenaient jusqu'à lui comme des notes fugaces dans une mélodie qu'il n'arrivait pas à déchiffrer entièrement.

Il était physiquement présent au milieu du groupe, mais son esprit restait en orbitale, détaché de l’essence même de ce lien soudé et partagé par les autres hommes. Le poids d'une solitude étrange pesait sur ses épaules ; une forme de calme intérieur qui ne cherchait pas la compagnie, car il savait qu'il n'en trouvait que dans le silence structuré des procédures ou le bruit précis du moteur en marche. Au bout de quelque temps, alors que les discussions s’étiraient et que la fatigue commençait à peser sur tous, Pierre décida de quitter l’espace confiné du dortoir pour prendre l'air frais avant le prochain cycle d'éveil.

La nuit se dégage lentement autour de lui. Le ciel au-dessus des tentes prenait une teinte complexe et changeante, passant d'un gris profond à un mauve sombre qui laissait transparaître les premières nuances d’une lumière naissante sur l'horizon est. Ce n'était pas le noir absolu du vide, mais plutôt une velours teinté de violet, parsemé de nuages fins dont la texture évoquait des moutons d'argent étirés par le vent. Le froid s'installa dans ses os, un pic qui lui rappelait qu'il était loin de la chaleur rassurante du camp.

Il marqua son pas vers le tarmac, sentant l’air pur et glacial piquer ses joues exposées. Les lumières des lampadaires étaient encore allumées sur les hangars, mais elles semblaient pâles face à la progression chromatique du ciel. Le Spitfire Grey Ghost se dressait là, imposant contre cette toile de couleurs émergentes. Sa silhouette élancée et son nez court formaient une icône familière dans le paysage nocturne qui s'effaçait doucement au profit d'une aube pâle.

Pierre commença à marcher vers l’appareil. Chaque pas résonnait distinctement sur la surface rugueuse du terrain humide, un bruit sec et régulier sous le silence croissant de l’aube. Il arriva devant les ailes, qui semblaient immobiles contre le ciel qui commençait à blanchir lentement dans son quadrant oriental. Aussitôt, il posa sa main sur le fuselage du Grey Ghost. Le métal froid traversa ses doigts, une sensation immédiate et tangible, un contact direct avec la machine qu'il connaissait par cœur. Ce lien physique semblait plus concret que tous les échanges humains qu'il avait observés dans le dortoir, ancrant l'instant dans cette solitude silencieuse mais non vide au bord du ciel naissant.



Évalue ce texte. Conclus par SCORE: N.

---

# RESPONSE

PROBLEME: "s'installait directement dans les os" est une formulation un peu générique et usée pour décrire la vibration.
PROBLEME: L'expression "puissance latente" est une nominalisation abstraite qui pourrait être remplacée par des sensations plus concrètes (ex : le bourdonnement, l'attente).
PROBLEME: La description de Pierre comme ayant un "regard d'observation tranquille" et ses gestes étant "mesurés par une longue pratique du commandement" sont des traits de caractère qui devraient être montrés par l'action plutôt que décrits par des adjectifs ou des phrases explicatives.
PROBLEME: L'expression "un parfum familier qui sent la mission" est un peu trop générique et manque d'évocation sensorielle concrète.
PROBLEME: Le passage décrivant le sentiment de solitude ("Le poids d'une solitude étrange pesait sur ses épaules") nomme une émotion plutôt que de la laisser se manifester par des actions ou des sensations physiques (comme les mains froides, l'immensité du ciel).

SCORE: 8