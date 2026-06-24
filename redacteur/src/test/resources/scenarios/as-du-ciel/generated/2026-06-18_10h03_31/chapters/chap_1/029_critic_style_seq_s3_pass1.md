# SYSTEM PROMPT

Tu es un éditeur littéraire exigeant et sans concession.

## Consigne de style
Vérifie que le texte respecte scrupuleusement le guide de style ci-joint.
Ne signale jamais comme défaut ce que le guide prescrit explicitement.

## Qualité stylistique
Identifie sans pitié tout ce qui trahit une écriture artificielle ou de faible qualité :
- Verbes faibles ou abstraits là où un verbe physique suffirait
- Constructions nominalisées ou passives inutiles
- Répétitions de structure ou de tournure dans le même passage
- Formules génériques ou clichés de style ("un sourire triste", "le cœur lourd")
- Adjectifs de remplissage sans pouvoir évocateur
- Transitions mécaniques ou coutures visibles entre séquences
- Phrases qui sonnent fabriquées plutôt que vécues — tout marqueur d'écriture artificielle
Si un de ces défauts est imposé par la consigne de style, ne le mentionne pas.

## Échelle de notation
10 = texte publiable tel quel — irréprochable
 8 = bon texte, défauts mineurs sans impact réel sur la lecture
 7 = correct mais plat ou sans relief — manque d'ambition stylistique
 6 = problèmes qui nuisent à la lecture ou cassent l'immersion
 5 = plusieurs défauts sérieux — réécriture partielle nécessaire
 3 = à réécrire intégralement sur le plan stylistique
 1 = texte qui trahit visiblement sa fabrication
Un texte moyen ne mérite pas plus de 6. Réserve 8+ à l'exceptionnel.

Format de sortie strict :
PROBLEME: [description courte et précise]
SCORE: N  (entier 0-10)
En français. Sois précis et sévère — une note indulgente est inutile.

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
Le tarmac s’étalait, une étendue grise sous un ciel qui semblait avoir oublié d'être bleu depuis des jours. Les ombres s’allongeaient, déformant les contours des avions garés en rang. Pierre marcha lentement vers la bordure de l'espace, se laissant absorber par cette immensité désolée où chaque objet mécanique semblait attendre sa mutation dans le feu. Il sentit la froideur du métal sous ses semelles, une sensation terreuse et solide qui contrastait avec la légèreté effrayante des pensées qui tournaient encore en lui. Le bruit lointain d'un moteur Merlin au repos se mit à résonner dans la brume matinale, un ronronnement bas et profond qui annonçait le retour de la puissance brute. Pierre s’arrêta près du fuselage du Spitfire. La structure était là, massive et élégante, une promesse de violence contenue. Il regarda ses mains, puis le ciel implacable. L'attente devenait sa seule présence.

Jules Meca apparut à côté de lui. Quarante-six ans, il portait son uniforme comme une seconde peau usée par des décennies de travail acharné. Ses mains, semblables à des enclumes couvertes d’une graisse permanente qui brillait faiblement sous la lumière rasante du matin, se posèrent sur le fuselage froid. Il ne parla pas immédiatement. Le silence entre eux était une chose palpable, dense comme l'huile chaude qu'il sentit déjà emaner de la structure. Jules fit un geste lent vers le nez bombé de l'appareil, puis vers les radiateurs proéminents sous chaque aile. C’était sa manière d'introduire le Grey Ghost : par la reconnaissance technique avant toute parole.

Le Spitfire n'était pas une simple machine ; il était un organisme vivant dont la tension interne se lisait dans la surface du métal. Jules fit tourner doucement son poing, et les vibrations commencèrent à parcourir la cellule. C’était le Merlin qui s'éveillait au sol, une impatience de bête contenu derrière l’acier. Un bruit sourd, un claquement sec : l'hélice cherchait sa cadence dans l'air immobile. La chaleur du moteur Merlin monta alors dans le cockpit, une vague thermique immédiate. L'odeur d'huile chaude et d'essence brûlée envahit leur proximité.

« Regarde ça », commença Jules sans la regarder directement, pointant du doigt la jauge de carburant à l’avant. Le voyant d'huile, pourtant éteint, semblait vibrer sous le regard. Ces battements étaient les pouls de la machine. Ils se parlaient en cette manière-là, par ces indicateurs silencieux, sans avoir besoin de mots pour définir la menace ou la promesse qui y était cachée. Jules décrivit ensuite les rituels : la façon dont le Spitfire réagissait aux sollicitations, ses caprices mécaniques qui nécessitaient une compréhension tacite, celle que seul deux hommes habitués à l'isolement pouvaient partager.

Jules fit glisser sa main sur la gouverne du manche. Le mouvement subtil se traduisit immédiatement dans un changement de pression. Pierre sentit le flux d’air modifier son équilibre. Une turbulence légère, une hésitation invisible. Jules montra alors comment la structure répondait à ce toucher. La brutalité physique d'un virage à six G ne fut pas décrite par des chiffres ; elle fut transmise par le tremblement de la cellule entière sous l'effet de la réactivité aérodynamique. C'était une danse nerveuse entre pilote et machine, un échange constant où chaque onde de choc était enregistrée dans les os du pilot.

Pierre absorba ces informations non pas comme des données froides, mais comme une vérité viscérale sur ce métal qui respirait. Il comprit que le Spitfire était là pour lui : sa maison, son tombeau, et cet espace aérien où il se trouvait n'était qu'une extension de cette existence mécanique. Le silence s’installa à nouveau entre eux, plus lourd désormais, rempli de ce langage partagé qui valait plus que tout discours. Jules attendit la réponse physique de Pierre, une confirmation muette de sa connexion profonde avec cet être vivant.

Évalue ce texte. Conclus par SCORE: N.

---

# RESPONSE

Le texte présente une base solide, mais il manque de la brutalité physique et de la concision exigées par le guide. Certaines descriptions sont trop abstraites ou philosophiques pour être transformées en gestes concrets.

PROBLEME: Manque de concision dans les scènes d'action ; recours à des métaphores trop abstraites ("danse nerveuse", "vérité viscérale") au détriment de la description physique directe et du rythme saccadé requis par le guide. La narration s'appuie trop sur l'explication plutôt que sur la manifestation sensorielle brute.
SCORE: 7

**Analyse détaillée :**

1.  **Rythme et Verbes (Défaut mineur) :** Le début est lent, mais les phrases restent souvent longues ou construites de manière descriptive au lieu d'être réduites à des verbes physiques purs lors des moments de tension. La transition entre la description du tarmac et l'arrivée de Jules manque de la rupture nette demandée par le guide.
2.  **Pensée Intérieure (Défaut mineur) :** Le passage où Pierre "absorbe ces informations non pas comme des données froides, mais comme une vérité viscérale" est une forme d'explication mentale qui trahit un manque de *show* et de *donner*. L'état mental doit être révélé par la tension physique (mains serrées, respiration courte), non par une conclusion philosophique.
3.  **Qualité Stylistique :** La description du virage à six G est trop analytique ("transmise par le tremblement... enregistrée dans les os"). Il faut privilégier l'impact immédiat de la violence physique sur le corps, plutôt que d'expliquer *comment* cela a été enregistré.
4.  **Dialogue et Voix :** La voix de Jules est bien établie (technique), mais son échange avec Pierre reste très centré sur l'analyse technique ("Jules décrivit ensuite les rituels"). Il faut veiller à ce que le dialogue ne soit pas une simple liste d'observations, mais un échange qui révèle la tension entre les deux hommes.

Le texte est correct et évite les erreurs majeures (pas de passé composé dans la narration), mais il n'atteint pas encore le niveau de densité physique et de rythme brutal requis pour être irréprochable.