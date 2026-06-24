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
- [Narratif] Le passage où Pierre réfléchit à son "vide dans son existence personnelle" est très introspectif et philosophique ; l'ancrer davantage dans une expérience sensorielle ou un événement concret de la guerre pourrait renforcer le lien entre sa quête personnelle et la réalité du conflit.

### Objectif narratif du chapitre non atteint :
- Les interactions avec les autres personnages (Jules Meca) sont très fonctionnelles et efficaces, mais pourraient gagner en subtilité pour accentuer le sentiment de "stranger" plutôt que d'être simplement un membre reconnu du groupe.

Texte à analyser :
Le bruit sec des bottes sur le parquet du bâtiment claquait dans la pénombre, une sonnerie presque métallique qui contrastait avec la douceur relative de l'air intérieur. Pierre entra dans le bureau du Commandant Bertrand, un espace où le temps semblait s’être figé entre les piles de dossiers militaires et les cartes froissées. L’atmosphère y était lourde d’un mélange âcre — celui du papier ancien mouillé par l'humidité ambiante, mêlé au relent amer d'un café froid qui attendait depuis des heures et à une trace imperceptible de tabac rassis.

Le Commandant Bertrand s'était déjà assis derrière son bureau massif, un homme dont le visage fermé et la mâchoire serrée trahissaient le poids constant du commandement. Il observa Pierre avec ce regard d’acier habituel, scrutant chaque micro-expression sans rien dire, ne faisant qu'attendre l'entrée dans une formalité qui dépassait largement les mots. Les minutes s'écoulèrent dans un silence dense, ponctué uniquement par la respiration régulière de Bertrand et le bruit ténu des doigts du commandant tapotant nerveusement sur le bois usé.

Ce vide que Pierre ressentait habituellement — ce manque d’ancrage personnel face à l'immensité de cette guerre qui engloutissait tout — ne pouvait être comblé par une pensée abstraite ici ; il devait se manifester dans la réalité matérielle du conflit. Il regarda les montagnes de documents, ces preuves froides et officielles que des vies s'entremêlaient au bord d'un conflit global. Ce n’était pas l'idée de la lutte qui le troublait, mais cette nécessité implacable, ce devoir constant qu'il devait assumer pour que ses actions aient un sens concret, une raison valable autre que sa propre survie.

« Le Grey Ghost », dit Bertrand finalement, sa voix grave et dépourvue d’émotion, brisant la quiétude du bureau. « Ton numéro est le 102. »

Pierre acquiesça lentement. L'information était précise, factuelle, dénuée de toute fioriture ou d'adjectif superflu — exactement comme il l'avait imaginé. Il sentit son corps se détendre légèrement face à cette concision, une sorte de relief dans la lourdeur des échanges.

Le commandant continua sans pause, ses yeux ne quittant jamais le pilote : « Demain, à l’aube. »

La directive était courte et définitive. Pierre redressa les épaules, sentit le poids léger du sac sur son épaule, puis quitta la pièce. En sortant, il croisa brièvement Jules Meca près d'un hangar de service ; le mécanicien ne s'arrêta pas pour un signe reconnaissable, mais lui lança juste un petit hochement de tête en passant devant les piles de pneus et les trappes du tarmac. Ce geste, si bref et dépourvu d'artifice particulier, était une reconnaissance silencieuse, sans le besoin de la camaraderie bruyante ou des mots inutiles que beaucoup confèrent à l’amitié.

Pierre se dirige vers le hangar où attendait son appareil, déjà plus certain de ce qui l'attendait demain. L'odeur du kérosène et du métal froid devint soudain plus forte dans ses narines, lui rappelant qu'il était bien sur la piste d'un autre engagement, un engagement dont il n’avait besoin que de comprendre le fond pour se lancer dans les détails. Il atteignit son spot sous la lumière blafarde du hangar et commença à inspecter le fuselage avec une attention immédiate, ses mains, grandes et robustes comme celles d'un pêcheur, caressant le métal froid de la carlingue du Grey Ghost.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK