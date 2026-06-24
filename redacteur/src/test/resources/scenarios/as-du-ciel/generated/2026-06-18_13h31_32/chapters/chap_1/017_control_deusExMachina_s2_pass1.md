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

Texte à analyser :
La transition du grand air à l'intérieur fut immédiate et tranchante, comme le changement de vitesse d’un moteur au régime élevé. Pierre se permit de laisser son regard s'habituer aux murs feutrés du bâtiment. Le bureau était situé au fond d'un couloir où les pas lourds des officiers résonnaient sur le parquet. Une odeur âcre, mélange complexe d'encre séchée, de papier ancien et d'un tabac amer, s'accrocha à l'atmosphère confinée.

On ouvrit la porte du bureau du Commandant Bertrand sans prévenir. La pièce était baignée par une lumière filtrée qui sculptait les formes dans la pénombre austère des murs tapissés. Le commandant était déjà assis derrière un vaste bureau encombré de dossiers, ses mains posées sur le bois sombre et patiné.

L'échange fut bref. Les mots y étaient rares, presque chirurgicaux, chacun porteur d’un poids inouï. Bertrand ne regarda pas Pierre immédiatement ; il jeta un œil fugace à une carte militaire éparpillée sous son menton avant de lever les yeux. Le visage fermé du commandant ne trahissait aucune émotion, seulement la certitude implacable de celui qui porte le fardeau d'une responsabilité immense.

Le silence s’installa entre eux, un vide dense où chaque seconde semblait peser autant qu’un kilo de carburant. C'était ce poids silencieux du commandement : une attente non verbale qui demandait plus que la parole pour être comprise. Pierre resta immobile, laissant son regard se poser sur les mains du commandant avant de lever ses propres yeux gris-vert vers lui.

« Ton appareil est le numéro 321 » déclara Bertrand d'une voix basse et mesurée. Ce n'était pas une question, mais un fait énoncé avec la précision d'un ordre absolu. Il ne fit qu'une pause, laissant l’information pénétrer l'immensité silencieuse du bureau. Puis il ajouta, le regard fixe : « Demain, à l’aube. »

Le Commandant Bertrand se leva sans un mot de politesse supplémentaire, sa silhouette imposante s’éloignant dans la pièce austère pour laisser place au silence. Pierre ne resta pas longtemps debout ; il fit quelques pas mesurés vers la sortie et sortit du bureau avec une détermination feutrée, le poids de la mission déjà ancré plus profondément que l'odeur âcre du bureau.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK