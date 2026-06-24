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
Pierre arrive à la base à l'aube du 6 juin. Le bruit des moteurs Merlin résonne au loin. Sur le tarmac, les Spitfires sont alignés dans la brume, hélices immobiles. Il pose son sac et regarde le ciel.


Plan de séquence :
Consigne : Pierre arrive à la base à l'aube du 6 juin. Le bruit des moteurs Merlin résonne au loin. Sur le tarmac, les Spitfires sont alignés dans la brume, hélices immobiles. Il pose son sac et regarde le ciel.

BEATS :
1. Pierre arrive à Thorney Island et descend du transport militaire.
2. Le son lointain des moteurs Merlin résonne sur le terrain humide.
3. Il marche vers la zone d'alignement où les Spitfires sont stationnés dans la brume matinale.
4. Les hélices immobiles des avions créent une mosaïque de métal gris et vert sous un ciel bas.
5. Pierre pose son sac en toile sur le sol mou du tarmac.
6. Il lève les yeux pour observer la texture uniforme du ciel qui peine à se dégager.

SENSORIELS : L'air est froid, humide et saturé de brume. Le bruit des moteurs Merlin s'entend comme un grondement sourd et régulier au loin. La vue est dominée par les teintes grises et bleues du ciel et de l'herbe mouillée.

TON ET RYTHME : Calme, contemplatif, lent.

### Problèmes à corriger :
- [Narratif] Le personnage de Pierre est très bien décrit physiquement et par ses actions, mais il manque un élément d'introspection ou une réaction plus personnelle à l'environnement pour qu'il ne soit pas perçu uniquement comme un observateur passif.
- [Narratif] L'absence totale de tension interne dans le chapitre rend Pierre très fonctionnel, mais cela freine la progression vers son arc narratif (l'homme qui sera brisé). Il manque une petite graine d'inquiétude ou de motivation personnelle pour que le lecteur s'y attache émotionnellement.

### Objectif narratif du chapitre non atteint :
- Bien que le groupe soit décrit comme soudé par une routine silencieuse (le dortoir), il manque un élément de chaleur ou d'interaction sociale minimale entre les pilotes existants pour bien contraster avec l'isolement de Pierre, même si son isolement est très clair.

Texte à analyser :
Le grondement sourd des moteurs Merlin s’élevait de loin, une pulsation grave et régulière qui troublait le calme matinal et vibrait dans la terre humide sous les pieds de Pierre. L'air était un mélange dense d'humidité saturée et du sel marin, enveloppant la zone comme une couverture lourde avant que la lumière ne parvienne à déchirer les strates nuageuses. La file des transports militaires s’arrêta finalement sur le tarmac mou, laissant derrière elle l’odeur âcre du carburant frais et la promesse d'un jour nouveau et incertain.

Une série de pas mesurés traversèrent le terrain gras vers la zone où les Spitfires étaient alignés. Le ciel, à cette altitude et à cette heure précise, n'était pas simplement gris ; il était une masse cotonneuse, un mélange complexe de bleu très pâle et d’argent brumeux, dont la texture semblait mouillée par l’humidité ambiante. Ce n'était plus le voile matinal habituel ; c'était une couche dense qui pesait sur tout, un plafond bas qui suggérait que le monde s'étendait bien au-delà de ce périmètre immédiat et visible.

Les hélices immobiles des Spitfires formaient une mosaïque silencieuse de métal vert et gris sous cette voûte céleste. Pierre avança lentement vers l’un d’eux, notant la silhouette élancée du Grey Ghost, dont les radiateurs proéminents semblaient attendre le premier souffle ardent. S'arrêtant à proximité, il posa son sac en toile sur le sol spongieux et sentit immédiatement le froid de l'acier sous ses doigts.

Un petit mouvement près des ailes attira son regard ; Jules Meca était déjà là, les mains engluées dans la graisse travaillant méticuleusement sur un carénage, sa moustache imposante contrastant avec l'uniformité du paysage brumeux. Il leva un œil fatigué vers Pierre et fit un signe de tête bref et silencieux – non pas une salutation formelle, mais une reconnaissance tacite que le travail commençait, ou qu'une présence nouvelle s’était jointe à la rotation du cycle quotidien. Un simple contact visuel qui signifiait : "Tu es là maintenant."

Pierre sentit un léger réchauffement dans sa poitrine – cette petite chaleur de camaraderie non verbale, ce lien ténu mais essentiel entre les hommes qui partageaient ces heures précises d'attente. Il regarda le ciel une fois de plus, observant la manière dont la lumière rasante tentait vainement de déborder des bords du nuage bas. Ce ciel était son décor habituel, sa maison invisible et sa tombe potentielle ; c’était un espace immense où l'on se perd si on ne sait pas où chercher le sol sous ses pieds.

Dans ce silence partagé entre lui et les silhouettes mécaniques autour de lui, une pensée plus profonde émergea sans être formulée : il n'était pas seulement là par obligation ou par devoir militaire ; il était venu parce que ce vide dans son existence personnelle — cet espace laissé vacant depuis trop longtemps — exigeait un remplissage digne. Il cherchait quelque chose qui ne pouvait se trouver au chaud, loin de l'odeur du kérosène et des nuages bas : une raison valable pour monter si haut, où chaque minute comptait autant que le souffle lui-même.

Il leva les yeux vers la ligne d’horizon, là où la masse grise semblait enfin laisser place à une mince bande plus claire, annonçant l'éveil du jour sur ce continent lointain. Pierre inspira profondément l'air froid et humide. Il savait que le Grey Ghost attendait son pilote pour commencer sa vie dans cette altitude exigeante.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK