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
1. Pierre arrive sur le tarmac de Thorney Island au petit matin du 6 juin.
2. Le grondement sourd des moteurs Merlin résonne à travers la brume matinale. L'air est saturé d'humidité et l'odeur âcre du kérosène froid s'élève.
3. Il marche vers les Spitfires alignés, le silence pesant malgré le bourdonnement lointain des machines en veille.
4. Les hélices sont immobiles dans le brouillard. Pierre observe la ligne de Spitfire Mk IX, reconnaissable à ses deux canons Hispano 20mm et quatre mitrailleuses Browning .303.

SENSORIELS : Le son sourd des moteurs au loin contrastant avec le silence oppressant du tarmac. L'odeur âcre de l'herbe mouillée et du kérosène froid. La vue d'un ciel bas, texturé par la brume.

TON ET RYTHME : Calme mais tendu. Phrases descriptives et lentes pour installer l'atmosphère.

Texte à analyser :
Le grondement sourd des moteurs Merlin résonna à travers la brume matinale, une vibration profonde qui semblait monter du sol lui-même et se perdre dans le ciel bas de Thorney Island. L'air, saturé d'humidité, portait l'odeur âcre du kérosène froid mêlée au parfum terreux de l'herbe mouillée — un mélange dense qui s’installait sur les narines comme une promesse invisible et pesante. Le ciel, à cette heure précise, n'était pas bleu ; il était d'un gris uniforme, texturé par la diffusion des premières lumières du jour à travers une épaisse couche de nuages bas, une mer cotonneuse où se confondent l'horizon et le plafond atmosphérique.

Le sac en toile fut posé sur l’aile biseautée du premier Spitfire Mk IX. Le silence qui suivit ce son mécanique lointain était oppressant, un calme tendu que seul le bourdonnement résiduel des machines en veille semblait briser. Les avions étaient alignés dans la grisaille, une rangée métallique immaculée et silencieuse, les hélices figées comme de grands insectes endormis sous le voile matinal. La lumière rasante du matin effleurait les radiateurs proéminents, faisant scintiller légèrement le métal sombre contre l'arrière des ailes elliptiques.

Pierre observa la ligne d'appareils, ses yeux gris-vert parcourant les formes familières du Grey Ghost. Chaque Spitfire était une entité complexe, chaque rivet un point de résistance face à ce ciel vaste et parfois hostile. Le silence s'étira, permettant au bruit lointain des turbines de devenir presque musical dans sa répétition monotone. Il sentit la froideur humide pénétrer ses vêtements alors qu’il scrutait le firmament, cette immense étendue qui lui servait déjà de refuge et de tombe potentielle. Les mains grandes, celles d'un homme habitué au labeur discret plutôt qu'à la précision des commandes, effleurèrent brièvement le tissu de son blouson avant de se poser sur l’épaule du sac posé près de l'appareil. Il était là, ancré dans cette attente immobile, entre la terre humide et l'infini gris qui s'étend au-dessus d'eux.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

FUITE
- "ses yeux gris-vert parcourant les formes familières du Grey Ghost." → type 2 — Le nom spécifique "Grey Ghost" est utilisé par Pierre, alors la consigne stipule que ce surnom doit être donné uniquement par Jules.