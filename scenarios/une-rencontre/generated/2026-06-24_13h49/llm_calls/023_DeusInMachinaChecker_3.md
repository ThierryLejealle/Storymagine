# DeusInMachinaChecker — appel 3

## EN-TÊTE
- Démarré  : 2026-06-24 13:53:06
- Statut   : ✅ OK
- Sys      : ~578 tok
- Usr      : ~233 tok
- Réponse  : ~0 tok
- Durée    : 7,0s

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
Le train s’arrête sur une voie secondaire. Le métal émet un sifflement métallique strident qui déchire le calme ambiant. Une lumière rasante, dorée, inonde la vitre avec un éclat spectaculaire, transformant l'intérieur du wagon en une scène presque éphémère. Maya lève les yeux vers ce paysage sauvage et verdoyant qui s’étire à perte de vue, ses traits se figent dans une immobilité soudaine. Elle lance un commentaire court, sans attendre de réponse immédiate, adressé au vide : « Regarde ça ». Eddie tourne enfin sa tête, surpris par la voix douce et posée de Maya. Il répond brièvement, ses mots sortant naturellement, presque murmurés contre le bruit sourd du freinage.

Réponds FUITE (avec liste) ou OK.

---

## RÉPONSE

OK