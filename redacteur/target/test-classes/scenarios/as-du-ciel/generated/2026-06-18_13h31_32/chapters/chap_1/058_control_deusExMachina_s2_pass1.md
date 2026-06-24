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
Le commandant Bertrand reçoit Pierre dans son bureau. Deux minutes. Peu de mots. Le poids du commandement dans chaque silence. Pierre repart avec son numéro de machine et une seule phrase : demain, à l'aube.


Plan de séquence :
Consigne : Le commandant Bertrand reçoit Pierre dans son bureau. Deux minutes. Peu de mots. Le poids du commandement dans chaque silence. Pierre repart avec son numéro de machine et une seule phrase : demain, à l'aube.

BEATS :
1. Pierre est conduit dans le bureau du Commandant Bertrand.
2. Bertrand s'assied derrière un bureau encombré de cartes et de dossiers militaires.
3. Un échange très bref a lieu où les mots sont mesurés et précis.
4. Le poids silencieux du commandement se fait sentir entre les deux hommes.
5. Pierre reçoit son numéro d'appareil et une directive concise concernant le vol de demain.
6. Il quitte la pièce avec une détermination feutrée.

SENSORIELS : L'odeur âcre du papier ancien, du café froid et du tabac dans le bureau. Le bruit des bottes sur le parquet est sec. La lumière filtrée à travers les fenêtres donne un aspect austère aux murs.

TON ET RYTHME : Formel, concis, lourd.

### Problèmes à corriger :
- [Narratif] Le personnage de Pierre est très bien décrit physiquement et par ses actions, mais il manque un élément d'introspection ou une réaction plus personnelle à l'environnement pour qu'il ne soit pas perçu uniquement comme un observateur passif.
- [Narratif] L'absence totale de tension interne dans le chapitre rend Pierre très fonctionnel, mais cela freine la progression vers son arc narratif (l'homme qui sera brisé). Il manque une petite graine d'inquiétude ou de motivation personnelle pour que le lecteur s'y attache émotionnellement.

### Objectif narratif du chapitre non atteint :
- Bien que le groupe soit décrit comme soudé par une routine silencieuse (le dortoir), il manque un élément de chaleur ou d'interaction sociale minimale entre les pilotes existants pour bien contraster avec l'isolement de Pierre, même si son isolement est très clair.

Texte à analyser :
Le ciel semblait enfin se résoudre à laisser passer la grisaille pour une clarté plus stable, mais le chemin vers l'intérieur de la base imposait un changement d'atmosphère immédiat. Pierre quitta les hangars et traversa le tarmac en direction des bureaux administratifs. Le bruit sec de ses bottes sur le béton, auparavant étouffé par le bourdonnement des moteurs au loin, devint une résonance nette dans le silence matinal. Il sentit la pression monter, non pas celle du danger à venir, mais l'urgence silencieuse d'une nécessité personnelle : il devait se rendre là où les décisions étaient prises, car c’était là que se jouait son utilité au-delà de sa propre survie. À mi-chemin, près d'un groupe de mécaniciens rassemblés autour du nez d'un autre Spitfire, il aperçut Jules et deux autres pilotes échangeant un bref sourire en regardant une tâche mécanique compliquée. Un geste simple — une main posée quelques secondes sur l'épaule de son voisin avant qu'il ne se retire pour reprendre sa position, ou un échange rapide de blagues lancées à voix basse— confirma ce lien invisible et solide qui maintenait le groupe malgré leur isolement fonctionnel. Pierre acquiesça légèrement en retour, le regard balayant brièvement la zone sans s’y attarder; il était là pour une raison spécifique, un rendez-vous avec l'autorité.

Le bureau du Commandant Bertrand se trouvait au fond d'un couloir dont les murs de béton semblaient absorber toute chaleur. En franchissant le seuil, Pierre fut immédiatement frappé par la concentration des odeurs : cette alliance âcre entre le papier ancien et le relent ambré d’un café froid laissé depuis longtemps. Le Commandant Bertrand était assis derrière un amas de dossiers militaires, une montagne de cartes froissées qui témoignait du poids colossal de son commandement. Il ne leva pas les yeux immédiatement. Pierre s'approcha, adoptant la posture habituelle des hommes qui attendent qu'une décision soit prise avant d’être jugés dignes de l’action. Bertrand finit par lever lentement le regard. Le visage était fermé, marqué par une fatigue dont il ne parlait jamais.

« Asseyez-vous, Moreau », ordonna l'officier sans aucune chaleur dans la voix. Il fit un signe vers une chaise en cuir usé. Pierre s'assit et se contenta de regarder ses mains pendant quelques instants. Le temps semblait ralentir, étiré par le silence pesant qui emplissait la pièce, chaque seconde étant chargée d'une implication non verbale. L'atmosphère était formelle, lourde du poids des responsabilités militaires accumulées au fil des années. Bertrand déplaça un dossier et désigna une feuille de papier avec son index. « Le Grey Ghost », dit-il, sa voix grave. Il ne donna aucune explication sur la nécessité ou l’urgence de la mission ; il énonça simplement les faits. Pierre se redressa, ses yeux gris-vert fixés sur le commandant. L'autorité silencieuse de Bertrand était palpable; elle exigeait une réponse précise et sans hésitation.

Le Commandant désigna un emplacement sur son bureau encombré où reposait une note manuscrite. « Machine numéro 243. Vol demain, à l’aube », trancha-t-il. C'était tout : le chiffre, la date, l'heure. Il ne donna pas plus de détails tactiques que nécessaire pour cette première consultation. Pierre hocha la tête une seule fois. Le poids du commandement s'était résorbé dans l'efficacité pure des mots échangés. S’il y avait un sentiment d'inquiétude sous le calme apparent, il était juste assez subtil pour être une graine de détermination plutôt qu'une peur paralysante; ce n'était pas une simple obligation, c'était une nécessité personnelle enveloppée dans l'habit formel du devoir militaire. Il se leva et fit demi-tour avant même que Bertrand ne puisse prononcer un mot supplémentaire. Il quittait le bureau avec la certitude feutrée de savoir où il devait être quand les premières lueurs effleureront l’horizon.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK