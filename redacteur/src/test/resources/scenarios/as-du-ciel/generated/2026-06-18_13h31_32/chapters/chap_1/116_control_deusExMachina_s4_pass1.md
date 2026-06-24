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
La première nuit à la base. Pierre ne dort pas. Un moment de vie ordinaire parmi les pilotes dans le dortoir ou la salle commune. Avant l'aube, il retourne sur le tarmac et pose la main sur le fuselage du Grey Ghost. Le métal est froid. Le ciel commence à blanchir à l'est.


Plan de séquence :
Consigne : La première nuit à la base. Pierre ne dort pas. Un moment de vie ordinaire parmi les pilotes dans le dortoir ou la salle commune. Avant l'aube, il retourne sur le tarmac et pose la main sur le fuselage du Grey Ghost. Le métal est froid. Le ciel commence à blanchir à l'est.

BEATS :
1. Pierre se réveille au milieu de la nuit sans avoir trouvé le sommeil.
2. Il observe les activités matinales et silencieuses des autres pilotes dans la salle commune.
3. Après un moment d'attente, il quitte son dortoir pour aller vers le tarmac.
4. Le vent frais balaye l'allée du hangar où se trouve le Grey Ghost.
5. Pierre s'approche de l'appareil et pose sa main sur le fuselage froid en métal.
6. Il remarque que les couleurs du ciel ont commencé à passer du noir profond au blanc pâle à l'est.

SENSORIELS : Le silence enveloppant, seulement troublé par des bruits mécaniques distants. Le toucher du métal glacé sous la paume de Pierre. L'odeur subtile et propre de l'aube qui chasse le froid nocturne.

TON ET RYTHME : Contemplatif, silencieux, anticipatif.

### Problèmes à corriger :
- [Narratif] Le passage où Pierre réfléchit à son "vide dans son existence personnelle" est très introspectif et philosophique ; l'ancrer davantage dans une expérience sensorielle ou un événement concret de la guerre pourrait renforcer le lien entre sa quête personnelle et la réalité du conflit.

### Objectif narratif du chapitre non atteint :
- Les interactions avec les autres personnages (Jules Meca) sont très fonctionnelles et efficaces, mais pourraient gagner en subtilité pour accentuer le sentiment de "stranger" plutôt que d'être simplement un membre reconnu du groupe.

Texte à analyser :
Le grondement du Rolls-Royce Merlin, qui venait de troubler le silence matinal, s’est apaisé en un bourdonnement régulier et puissant, une promesse mécanique que l’appareil était prêt à endurer la journée. Pierre haussa légèrement les épaules, acceptant ce rythme imposé par la machine. Il n'avait pas besoin d'une réponse pour valider le travail de Jules ; son silence était suffisant pour témoigner de cette compréhension mutuelle qui dépasse les mots. Le mécanicien, lui, continua son inspection des roues et des freins, ses mains calleuses travaillant avec une familiarité qui donnait l’impression qu’il avait passé toute sa vie à connaître chaque centimètre du Grey Ghost.

Le froid commençait à s'installer dans le hangar, un froid précis, celui qui vient quand la nuit est dense et lourde. Pierre se dégage de l'amas d'hommes pour marcher vers l'extérieur, quittant les abords des hangars où l’activité de réparation avait mis en mouvement une partie du groupe. Le ciel au-dessus de Thorney Island commençait à basculer dans des nuances plus claires ; le noir profond s'était déjà fissuré par une bande pâle et froide sur la ligne d'horizon à l'est, annonçant l’éveil sans encore offrir la chaleur du soleil.

Il fit un pas hors de l'allée ombragée pour rejoindre le tarmac dégagé où se tenait l'escadrille. L'air frais fouça autour de lui, portant avec lui une odeur plus pure que celle des hangars : celle de l'aube qui lutte contre la nuit. Il sentit cette impression de vide intérieur qu’il portait toujours, ce sentiment d'être un observateur solitaire au milieu du tumulte de la guerre. Mais ici, dans le silence pré-matinal, ce vide ne se faisait pas sentir comme une absence, mais plutôt comme une immense capacité à absorber tout ce qui l'entourait. Le ciel semblait être sa propre toile, et chaque changement de couleur sur cette vaste étendue reflétait un état d'esprit plus que n'importe quelle émotion passagère.

Après quelques minutes de marche lente, il arriva au bord du Grey Ghost. L’appareil se dressait là, immobile, une silhouette élégante sculptée dans le métal sombre. Pierre s’approcha, et le vent frais balaya doucement la surface plane des ailes. Il tendit sa main vers l'avant, posant ses paumes sur le fuselage. Le contact fut immédiat : un froid sec et pénétrant, contrasté par la texture lisse du revêtement de l’appareil. Au-dessus de lui, les premiers rayons timides de l'est commençaient à transformer le ciel d'un bleu délavé vers un rose pâle extrêmement délicat, une couleur qui ne durerait qu’une heure avant que le soleil ne domine la scène.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK