# DeusInMachinaChecker — appel 1

## EN-TÊTE
- Démarré  : 2026-06-24 13:34:17
- Statut   : ✅ OK
- Sys      : ~578 tok
- Usr      : ~570 tok
- Réponse  : ~0 tok
- Durée    : 9,3s

---

## PROMPT SYSTÈME

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
Une consigne interdit X → le texte mentionne l'absence de X au lieu de simplement ne pas en parler.
  RÈGLE : ne signaler que si X correspond à une contrainte listée dans les consignes fournies.

2. FICHE PERSONNAGE DANS LA BOUCHE DU PERSONNAGE
Un trait de personnage réapparaît dans le texte comme étiquette permanente plutôt qu'observation vivante.

3. ARTEFACT DE SCÉNARIO
Mots ou tournures qui appartiennent au script de fabrication, pas à la fiction.
  FUITE : "Dans cette scène, Pierre comprend que..."
  OK    : Tout ce qu'un roman publié en librairie pourrait contenir.

4. LISTE NARRATIVISÉE
Plusieurs phrases SÉPARÉES dont chacune coche une case — aucune n'a de poids propre.
  RÈGLE ABSOLUE : une liste introduite par deux-points (:) dans UNE seule phrase n'est JAMAIS type 4.
  Au minimum 4 phrases SÉPARÉES sont requises.

5. ABSENCE JUSTIFIÉE
Le texte explique pourquoi quelque chose n'arrive pas — justification qui ne s'explique que par une contrainte reçue.

────────────────────────────────────────────────────────────
FORMAT DE RÉPONSE
────────────────────────────────────────────────────────────

Si tu détectes des fuites :
FUITE
- "[citation exacte, courte]" → type [1-5] — [une ligne d'explication]

Si le texte est propre :
OK

Sois précis et sélectif. En français.

---

## PROMPT UTILISATEUR

Contraintes de rédaction actives (pour référence) :
Interdiction d'écrire "il sentit que" ou "elle comprit que" — montrer par l'action.
Pas de cliché romantique lourd. La retenue vaut mieux que l'effusion.

Texte à analyser :
Le sifflement lointain de la locomotive s'est éteint, laissant derrière lui un silence épais et étrange, une présence lourde dans le vieux wagon d'autrefois. Maya franchit le seuil étroit du compartiment, sentant immédiatement l'odeur âcre mêlée à celle du cuir vieilli et à la poussière accumulée au fil des décennies. Le métal froid du siège en cuir défraîchi sous ses fesses était une sensation sèche contre sa tenue.

Un homme était déjà assis près de la fenêtre, immobile, le dos droit comme un pilier figé. Ses épaules larges masquaient presque entièrement la silhouette dans l'ombre tamisée qui filtrait à travers les rideaux épais des compartiments isolés. La lumière d'un après-midi d'été, voilée par le tissu sombre, sculptait des zones de chaleur et d'obscurité sur le bois sombre du wagon.

Maya fit un mouvement économique, abaissant son sac à dos lourd pour poser son poids avec une précision chirurgicale sur le siège adjacent au sien. Ses yeux balayèrent brièvement la pièce, s'arrêtant une seconde de trop sur le profil impassible de l'homme assis près de la vitre avant de se diriger vers les affaires qu'elle devait ranger. L’attention distante d'Eddie suivait chaque infime déplacement du corps, un examen analytique et dénué de toute émotion apparente. Les mains posées à plat sur ses genoux ne bougeaient pas, mais le regard qui s'orientait fixait toujours la même ligne lointaine.

Le rideau épais servit alors de frontière invisible entre ce petit espace clos et le bruit sourd du couloir du wagon, où les rouages des trains semblaient encore tourner dans une nuit oubliée. Maya termina son installation, rangeant ses derniers objets avec des gestes précis, mesurés comme si elle calculait la place exacte qu'elle occupait. Un espace se forma entre eux, un vide respectueux qui contrastait avec l'intensité contenue de leur présence respective. Le silence dans le compartiment ne fut pas une absence de bruit, mais une substance palpable, chargée d’observations muettes et d’attentes non exprimées.

Réponds FUITE (avec liste) ou OK.

---

## RÉPONSE

OK