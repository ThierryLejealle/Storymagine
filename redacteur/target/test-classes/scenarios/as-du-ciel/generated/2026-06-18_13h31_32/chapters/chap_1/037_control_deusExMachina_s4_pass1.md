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

Texte à analyser :
La nuit avait glissé sur la base avec une lenteur presque palpable. Pierre ne trouvait pas le sommeil ; il attendait plutôt que l'aube vienne, ou qu'elle passe simplement à côté de lui. Dans le dortoir, les autres pilotes dormaient dans un silence pesant, interrompu seulement par des grognements réguliers et monotones — le bruit du repos enfin trouvé après une journée de tension. Il ouvrit lentement les yeux, observant la silhouette sombre qui occupait son lit, se déplaçant légèrement au rythme de sa propre respiration calme. Au bout d’un moment où les ombres semblèrent s’immobiliser, Pierre quitta doucement son lit pour traverser le couloir étroit et faiblement éclairé par des ampoules jaunes au plafond. Il remarqua que dans la salle commune, même les figures habituelles étaient silencieuses ; un pilote lisait seul à une table, une autre se tenait près de la fenêtre en contemplant l'obscurité totale qui enveloppait le paysage militaire. Ce calme partagé était leur habitude, cette tranquillité collective où les mots devenaient inutiles, et tout ce qui comptait était le simple fait d'être là, ensemble, dans cette attente muette du jour suivant.

Après un court moment de contemplation silencieuse, Pierre décida de rejoindre l’extérieur. Il quitta le bâtiment et se mit à marcher vers la zone des hangars où les avions étaient stationnés. L’air frais mordit légèrement ses joues alors qu'il traversait les allées du terrain. Le vent nocturne, plus vif que prévu, s'engouffra dans l'allée longeant le hangar principal et balaya doucement la poussière sur l'asphalte. Il aperçut immédiatement le Grey Ghost sous le projecteur de service ; il y était garé, immobile et vigilant. Pierre s’approcha du Spitfire en marchant vers lui, laissant son regard parcourir les lignes élancées de l'aile elliptique. Le métal, malgré sa présence imposante, dégageait une froideur absolue à son toucher. Il posa sa main sur le fuselage lisse du Grey Ghost et sentit immédiatement cette sensation glacée qui passait à travers ses gants, une fraîcheur typique des objets exposés à l’air nocturne. Au-dessus de lui, le ciel commençait enfin à révéler son changement subtil ; la vaste étendue d'encre noire qu'il avait vue toute la nuit se transformait lentement en un dégradé étrange. À l’est, là où les premières lumières commençaient à poindre derrière les collines éloignées, le noir profond cédait progressivement à une teinte de gris pâle et blanchissant, comme si l'aube commençait timidement à déchirer la voilée obscurité.

Réponds FUITE (avec liste) ou OK.

---

# RESPONSE

OK