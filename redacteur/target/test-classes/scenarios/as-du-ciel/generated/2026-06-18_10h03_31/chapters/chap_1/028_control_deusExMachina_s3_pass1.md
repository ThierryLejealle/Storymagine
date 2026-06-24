# SYSTEM PROMPT

Tu lis un texte de roman et détectes les passages qui brisent l'immersion narrative —
les endroits où la mécanique de fabrication est devenue visible dans la prose.

RÈGLE PRÉALABLE — COHÉRENCE AVEC LE SCÉNARIO
Si une "Consigne de séquence" ou un "Plan de séquence" sont fournis dans les données,
lis-les en premier. Un passage qui réalise fidèlement ce qui y est demandé n'est PAS
une fuite : c'est le moteur narratif qui fonctionne. La fuite n'existe que si la
mécanique de fabrication devient visible au-delà ou indépendamment de la consigne reçue.

PRINCIPE
Un lecteur n'a pas accès aux instructions qui ont créé ce texte. Toute phrase qui ne
s'explique que si l'on connaît ces instructions est une fuite.

TEST : pour chaque phrase suspecte, demande-toi — cette phrase existerait-elle si
aucune consigne ne l'avait provoquée ? Si non, c'est une fuite.

────────────────────────────────────────────────────────────
CINQ FORMES DE FUITES
────────────────────────────────────────────────────────────

1. NÉGATION VERBALISÉE
Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement
ne pas en parler.
  FUITE : "Il n'y avait pas de nuage ce jour-là."  (si la consigne interdit les nuages)
  FUITE : "Pierre ne ressentit aucune douleur à la jambe."
  OK    : "Le ciel était vide."  /  [la jambe n'est tout simplement pas mentionnée]
  OK    : Contraste stylistique normal ("il ne fit pas X, il fit Y") — pas de type 1.
  RÈGLE : ne signaler que si X correspond à une contrainte listée dans les consignes fournies.

2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
Un trait de personnage réapparaît dans le texte comme si la narration se citait
elle-même — étiquette permanente plutôt qu'observation vivante.
  FUITE : "Je suis par nature machiavélique — c'est ainsi."
  FUITE : "Bertrand, taciturne comme toujours, garda le silence."
          ("comme toujours" transforme une observation en label permanent)
  OK    : "Bertrand ne dit rien."  (le trait est montré, il n'est pas nommé)

3. ARTEFACT DE SCÉNARIO
Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction.
  FUITE : "Dans cette scène, Pierre comprend que..."
  FUITE : "Ce passage montre le lien entre Pierre et Henri."
  FUITE : "Comme prévu, l'escadrille décolla."
  FUITE : "Cette séquence illustre le thème de..."
  OK    : Tout ce qu'un roman publié en librairie pourrait contenir.

4. LISTE NARRATIVISÉE
Plusieurs phrases SÉPARÉES dont chacune coche une case — aucune n'a de poids propre.
  FUITE : "Pierre arriva. Il observa le tarmac. Il déposa son sac. Il chercha Jules.
           Jules était absent."  (5 micro-phrases séparées = 5 cases cochées)
  OK    : "Pierre fit son inspection : vérifier les vibrations, noter la température,
           contrôler le carburant."  (liste dans UNE seule phrase = écriture normale)
  OK    : "Ces sept jours : le décollage à l'aube, les patrouilles, le retour épuisé."
           (montage en une phrase)
  RÈGLE ABSOLUE : une liste introduite par deux-points (:) dans UNE seule phrase
  n'est JAMAIS type 4. Au minimum 4 phrases SÉPARÉES sont requises.

5. ABSENCE JUSTIFIÉE
Le texte explique pourquoi quelque chose n'arrive pas — justification qui ne
s'explique que par une contrainte reçue.
  FUITE : "Bertrand ne dit rien, car ce n'était pas le moment des mots."
          (la justification trahit qu'on a évité le dialogue sur consigne)
  FUITE : "Il n'y eut pas de combat ce jour-là — le ciel resta vide, comme si la
           guerre avait décidé de souffler."
  OK    : "Bertrand ne dit rien."  (l'absence est là, sans justification)
  OK    : Psychologie ou état physique du personnage qui tient dans la logique interne
           du récit, sans rapport avec les contraintes reçues.
  OK    : Justifications causales (mécanique, physique, émotionnelle) indépendantes
           de toute consigne de rédaction.

────────────────────────────────────────────────────────────
FORMAT DE RÉPONSE
────────────────────────────────────────────────────────────

Si tu détectes des fuites :
FUITE
- "[citation exacte, courte]" → type [1-5] — [une ligne d'explication]

Si le texte est propre :
OK

Sois précis et sélectif. Les absences légitimes, le style elliptique, les transitions
courtes et les métaphores ne sont pas des fuites. Ne signale que ce qui est réellement
causé par une instruction externe devenue visible dans la prose.
En français.

---

# USER PROMPT

Contraintes de rédaction actives (pour référence) :
- Aucun discours patriotique. Ces hommes se battent parce qu'ils se battent.
  Les grandes causes ne s'énoncent pas — elles flottent en arrière-plan comme une évidence.
- Le Spitfire est nommé "Grey Ghost" par Jules, jamais "l'avion" ou "l'appareil"
  (sauf par des personnages formels comme Bertrand).
- Pas de résumé narratif là où une scène est possible.

Consigne de séquence (scénario) :
Jules le mécanicien présente le Spitfire à Pierre pour la première fois. Il lui explique ses habitudes, ses caprices, ses rituels de maintenance.


Plan de séquence :
Jules Meca présente le Spitfire Mk IX à Pierre sur le terrain. Jules parle pour deux, utilisant des gestes et des observations techniques pour communiquer ce que les mots ne peuvent pas dire. Il explique de manière décontractée les habitudes et les rituels de maintenance du Spitfire : la façon dont il réagit aux sollicitations, ses caprices mécaniques. Pierre écoute attentivement, absorbant non seulement les informations techniques, mais aussi l'atmosphère particulière qu'émane de Jules – une forme de connexion silencieuse entre deux hommes habitués à la solitude et au travail acharné.

Texte à analyser :
Le tarmac s’étalait, une étendue grise sous un ciel qui semblait avoir oublié d'être bleu depuis des jours. Les ombres s’allongeaient, déformant les contours des avions garés en rang. Pierre marcha lentement vers la bordure de l'espace, se laissant absorber par cette immensité désolée où chaque objet mécanique semblait attendre sa mutation dans le feu. Il sentit la froideur du métal sous ses semelles, une sensation terreuse et solide qui contrastait avec la légèreté effrayante des pensées qui tournaient encore en lui. Le bruit lointain d'un moteur Merlin au repos se mit à résonner dans la brume matinale, un ronronnement bas et profond qui annonçait le retour de la puissance brute. Pierre s’arrêta près du fuselage du Spitfire. La structure était là, massive et élégante, une promesse de violence contenue. Il regarda ses mains, puis le ciel implacable. L'attente devenait sa seule présence.

Jules Meca apparut à côté de lui. Quarante-six ans, il portait son uniforme comme une seconde peau usée par des décennies de travail acharné. Ses mains, semblables à des enclumes couvertes d’une graisse permanente qui brillait faiblement sous la lumière rasante du matin, se posèrent sur le fuselage froid. Il ne parla pas immédiatement. Le silence entre eux était une chose palpable, dense comme l'huile chaude qu'il sentit déjà emaner de la structure. Jules fit un geste lent vers le nez bombé de l'appareil, puis vers les radiateurs proéminents sous chaque aile. C’était sa manière d'introduire le Grey Ghost : par la reconnaissance technique avant toute parole.

Le Spitfire n'était pas une simple machine ; il était un organisme vivant dont la tension interne se lisait dans la surface du métal. Jules fit tourner doucement son poing, et les vibrations commencèrent à parcourir la cellule. C’était le Merlin qui s'éveillait au sol, une impatience de bête contenu derrière l’acier. Un bruit sourd, un claquement sec : l'hélice cherchait sa cadence dans l'air immobile. La chaleur du moteur Merlin monta alors dans le cockpit, une vague thermique immédiate. L'odeur d'huile chaude et d'essence brûlée envahit leur proximité.

« Regarde ça », commença Jules sans la regarder directement, pointant du doigt la jauge de carburant à l’avant. Le voyant d'huile, pourtant éteint, semblait vibrer sous le regard. Ces battements étaient les pouls de la machine. Ils se parlaient en cette manière-là, par ces indicateurs silencieux, sans avoir besoin de mots pour définir la menace ou la promesse qui y était cachée. Jules décrivit ensuite les rituels : la façon dont le Spitfire réagissait aux sollicitations, ses caprices mécaniques qui nécessitaient une compréhension tacite, celle que seul deux hommes habitués à l'isolement pouvaient partager.

Jules fit glisser sa main sur la gouverne du manche. Le mouvement subtil se traduisit immédiatement dans un changement de pression. Pierre sentit le flux d’air modifier son équilibre. Une turbulence légère, une hésitation invisible. Jules montra alors comment la structure répondait à ce toucher. La brutalité physique d'un virage à six G ne fut pas décrite par des chiffres ; elle fut transmise par le tremblement de la cellule entière sous l'effet de la réactivité aérodynamique. C'était une danse nerveuse entre pilote et machine, un échange constant où chaque onde de choc était enregistrée dans les os du pilot.

Pierre absorba ces informations non pas comme des données froides, mais comme une vérité viscérale sur ce métal qui respirait. Il comprit que le Spitfire était là pour lui : sa maison, son tombeau, et cet espace aérien où il se trouvait n'était qu'une extension de cette existence mécanique. Le silence s’installa à nouveau entre eux, plus lourd désormais, rempli de ce langage partagé qui valait plus que tout discours. Jules attendit la réponse physique de Pierre, une confirmation muette de sa connexion profonde avec cet être vivant.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK