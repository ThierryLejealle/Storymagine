# 2026-07-11 19h30 — NaturalityCorrector : refonte des exemples et clause anti-compression

## 1. Demande

Chantier queued depuis la session précédente (2026-07-10) : appliquer à `NaturalityCorrector.java`
la révision de ses 8 signatures et de sa clause de préservation du sens, jamais écrite (le fil avait
été interrompu par un "STOP" avant le dernier feu vert). Repris ce jour, avec relecture du delta
exact via l'historique de conversation (les décisions individuelles par signature n'avaient jamais
été perdues, seule la validation finale groupée manquait).

Deux consultations Fable supplémentaires cette session :
1. Revue générale du prompt + les 8 exemples — a suggéré de revoir les signatures 1 et 2 (jugées
   "limite, pourrait être légitime dans un texte normal" par l'utilisateur), de déplacer la
   signature 8 vers GrammarCorrector, d'ajouter un contre-exemple anti-compression et un garde-fou
   côté Java (`applyCorrections`/`TextPatcher`).
2. Suite à l'inquiétude explicite de l'utilisateur ("je ne veux pas que l'agent devienne un
   compresseur de texte") : demande à Fable de formuler précisément la règle "une seule suppression
   autorisée : le commentaire de paraphrase d'intention accroché à une action — jamais une licence
   de compression générale".

Décisions de l'utilisateur : garder la signature 8 dans Naturality (ce n'est pas une faute de
français mais une confusion de personnage/pronom du LLM — un bug de suivi de contexte, pas de
grammaire) ; accepter les nouveaux exemples 1 & 2 de Fable ; ne pas (pour l'instant) ajouter le
garde-fou côté Java ni le contre-exemple anti-compression suggérés par Fable — seule la clause
verbale ci-dessous a été retenue.

## 2. Ce qui a été touché

### `NaturalityCorrector.java`
- Liste des signatures : passage de 7 catégories en prose libre (avec "conclusions
  narratorielles" comme catégorie séparée, fusionnée ici dans PHRASE-BILAN) à **8 signatures
  numérotées et nommées**, chacune avec un exemple FAUX/JUSTE explicite :
  1. GESTE SUR-INTERPRÉTÉ — nouvel exemple ("signalant ainsi son refus...")
  2. PHRASE-BILAN — nouvel exemple ("preuve que la glace était enfin brisée")
  3. CONCEPT À LA PLACE DU CONCRET — inchangé
  4. ACTION NOMINALISÉE — inchangé
  5. PRÉNOM AUTO-RÉFÉRENTIEL — inchangé
  6. VOCABULAIRE DE MACHINE — nouvel exemple (tasses/rangement, remplace l'exemple de combat jugé
     "pourrait passer")
  7. AMBIANCE EXPLIQUÉE — inchangé
  8. RÉFÉRENCE DE PERSONNAGE INCOHÉRENTE — nouvelle signature (pronom/article incohérent pour un
     personnage dans le même passage — confusion de contexte du LLM, explicitement pas une faute
     de grammaire)
- Exemple du bloc FORMAT STRICT aligné sur le nouvel exemple de la signature 1.
- Bloc final remplacé : l'ancienne ligne "ATTENTION : conserve le sens, le rythme et les
  informations..." devient une clause structurée en deux temps (proposition de Fable) :
  - règle générale posée en positif ("corrige par remplacement", jamais de résumé/compression) ;
  - unique exception explicitée : suppression d'un commentaire de paraphrase d'intention accroché
    à une action ("signalant ainsi...", "preuve que...") — jamais une reformulation du reste du
    texte, verrouillé par la dernière phrase de la clause ("ne permet jamais de raccourcir le
    reste du texte").
  Emplacement inchangé (après FORMAT STRICT, avant le sentinel "PAS DE CORRECTION").

Non modifiés dans ce passage : `retryStrategy()` (reste `RATIO_THRESHOLD`, tâche ouverte/créative
au sens de `agent/CLAUDE.md`), le reste de la structure du fichier (REGLES, questions du
"romancier", format CORRECTIONS/FAUX/JUSTE, sentinelle).

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**. Aucune régression.

Non retenu (proposé par Fable, laissé de côté à ce stade) : déplacement de la signature 8 vers
GrammarCorrector, contre-exemple anti-compression dans le prompt, garde-fou de longueur côté Java
dans `applyCorrections`/`TextPatcher` (rejet si JUSTE < ~40% de la longueur de FAUX sur un FAUX
long). À reconsidérer si la compression réapparaît malgré la nouvelle clause.
