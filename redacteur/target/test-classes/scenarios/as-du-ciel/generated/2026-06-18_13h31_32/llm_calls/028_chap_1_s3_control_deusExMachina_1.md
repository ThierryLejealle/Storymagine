# chap_1_s3_control_deusExMachina — appel 1

## EN-TÊTE
- Démarré  : 2026-06-18 13:46:00
- Statut   : ✅ OK
- Sys      : ~1100 tok
- Usr      : ~1221 tok
- Réponse  : ~0 tok
- Durée    : 37,1s

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

## PROMPT UTILISATEUR

Contraintes de rédaction actives (pour référence) :
- Aucun discours patriotique. Ces hommes se battent parce qu'ils se battent.
  Les grandes causes ne s'énoncent pas — elles flottent en arrière-plan comme une évidence.
- Le Spitfire est nommé "Grey Ghost" par Jules, jamais "l'avion" ou "l'appareil"
  (sauf par des personnages formels comme Bertrand).
- Pas de résumé narratif là où une scène est possible.

Consigne de séquence (scénario) :
Jules le mécanicien présente le Spitfire à Pierre pour la première fois. Il lui explique ses habitudes, ses caprices, ses rituels de maintenance.


Plan de séquence :
Consigne : Jules le mécanicien présente le Spitfire à Pierre pour la première fois. Il lui explique ses habitudes, ses caprices, ses rituels de maintenance.

BEATS :
1. Jules Meca conduit Pierre vers le hangar où est stationné son Spitfire.
2. Le mécanicien commence une inspection minutieuse de la machine devant Pierre.
3. Jules explique en détail les caprices et les rituels de maintenance du Mk IX.
4. Il touche l'hélice de sa main droite avant d'insister sur le bon état des moteurs Merlin.
5. Pierre observe attentivement, écoutant chaque mot technique de Jules.
6. Jules désigne l'appareil en lui donnant son nom : 'Grey Ghost'.

SENSORIELS : L'odeur puissante et complexe du mélange huile/essence brûlée. Le bruit métallique des outils qui cliquent sur le moteur. La vue détaillée des rivets, de la peinture usée et des lignes aérodynamiques.

TON ET RYTHME : Pratique, méthodique, affectueux.

Texte à analyser :
Pierre traversa le bâtiment, laissant derrière lui la clarté forcée du bureau et l’atmosphère confinée de l'intérieur. Au sortir des portes doubles, il fut accueilli par un mélange puissant d'air frais et de cette odeur indéfinissable qui émane toujours des hangars : une synergie complexe entre le kérosène brûlé, la graisse chaude et l'huile moteur. Jules Meca attendait près d’une petite grille de service, son manteau taché de plusieurs couleurs de graisses différentes, sa moustache rigide contrastant avec le mouvement précis qu’il faisait en attendant que Pierre arrive. Le mécanicien ne dit pas bonjour ; il émit plutôt un grognement bref qui servit de confirmation mutuelle avant de se mettre en marche vers la piste principale.

« On y va », lança Jules, sa voix un peu rauque par le temps passé à crier au-dessus des moteurs. Il marchait d’un pas lourd et assuré, écartant légèrement les jambes pour maintenir son équilibre sur le bitume froid du tarmac. Après quelques minutes de marche, ils atteignirent la zone où se trouvait une file compacte d'avions stationnés. Jules s’arrêta devant un Spitfire, dont les ailes elliptiques semblaient capter chaque rayon de lumière matinale. Il ferma les yeux pendant une seconde, comme pour absorber l'immensité du ciel au-dessus des machines avant de se concentrer sur la tâche qui lui tombait à cœur.

Le mécanicien commença son inspection rituelle. Ses mains, grandes et puissantes, commencèrent à tapoter méthodiquement les ailes. Le bruit métallique des outils — un petit jeu de clés et une brosse à rivets — claqua régulièrement, créant une partition méthodique dans le silence du matin. Jules désigna l’avant de la machine, son doigt pointé vers le capot moteur. « Ce monstre est capricieux », expliqua-t-il sans aucune ironie, mais avec un sérieux absolu. Il détailla les exigences minimales : un certain couple précis pour le démarrage, des vérifications sur chaque joint d'huile qui ne toléraient aucun défaut, et une attention particulière aux lignes de refroidissement du Merlin 66. Le pilote devait comprendre que l’avion n'était pas seulement un assemblage de métal ; c'était un organisme exigeant, dont les humeurs devaient être respectées pour garantir la fiabilité.

Il s’agenouilla près des roues, puis passa à l’hélice. Il attrapa la pale droite entre son pouce et son index, ressentit sa texture lisse sous le vernis de protection, et fit pivoter légèrement l'hélice dans un geste expert. « Les moteurs Merlin », continua Jules en se redressant, les yeux brillants d’une fierté technique, « ils sont beaux, ces V12. Mais ils ne chantent pas pour n’importe qui. Ils exigent une écoute. Il faut entendre chaque changement de régime, le sifflement des soupapes à pleine charge. C'est la seule façon de savoir si tout est en ordre. » Pierre resta immobile, observant l'ensemble de ces gestes précis et cette description technique, assimilant les informations sans rien commenter.

Jules balaya les yeux sur les lignes aérodynamiques du Spitfire, s’attardant un instant sur le nez court et bombé. Il sourit alors légèrement, mais ce sourire ne toucha pas toute sa face — il restait dans la circonférence de son regard d'homme. « Tu vas connaître une belle chose », murmura-t-il en tapotant une fois l’aileron porteur. « On l'appellera Grey Ghost. »

Réponds FUITE (avec liste) ou OK.

---

## RÉPONSE

OK