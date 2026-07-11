# 2026-07-07 17h13 - Fiabilisation du parsing et de l'application des corrections FAUX/JUSTE

## Description de l'évolution demandée

Suite à l'analyse de deux appels LLM réels (`007_SequenceDeusInMachinaCorrector_2.md` et
`008_SequenceDeusInMachinaCorrector_3.md`), diagnostic vérifié par script (pas à l'œil) :

- Fichier 007 : 8 corrections proposées par le LLM, **6 silencieusement ignorées**
  (`replace miss`).
- Fichier 008 : 6 corrections, **5 ignorées**, et **1 qui s'applique mais corrompt le
  texte** en injectant littéralement le commentaire du LLM
  (`" (Note : Cette phrase était déjà bien formulée...)"`) dans la prose finale.

Cause racine identique dans les 11 cas manqués : le LLM recopie une citation imbriquée
(déjà entre guillemets doubles dans le texte source) en utilisant des guillemets
**simples** `'...'` au lieu des guillemets doubles `"..."` du texte réel — contenu
identique, glyphe différent, donc `String.replace()` ne trouve jamais la citation.
Cause du cas corrompu : le JUSTE se terminait par un commentaire parenthésé inventé par
le LLM, non fermé par un guillemet, donc la fonction `unquote()` de l'époque
(qui exigeait un guillemet en tête ET en queue) laissait la chaîne brute — guillemet
parasite en tête + commentaire compris.

Demande : mutualiser le parsing (dupliqué 4 fois) et l'application des corrections
(dupliquée 4 fois dans `WriteWorkflow`), et fiabiliser les deux pour éliminer ces
deux classes de bug, en nettoyant FAUX et JUSTE de façon symétrique.

## Ce qui a été touché

- **`redacteur/coeur/domaine/agent/commun/CorrectionParser.java`** (nouveau) : parsing
  FAUX:/JUSTE: mutualisé pour les 4 correcteurs. Nettoie un nombre quelconque de
  guillemets parasites (`"`, `'`, `“`, `”`, `‘`, `’`) en tête et en queue, appliqué
  identiquement aux deux côtés de chaque paire ; strip en plus un commentaire
  parenthésé final sur le JUSTE uniquement (jamais sur le FAUX, qui peut légitimement
  devoir inclure une annotation du texte source pour la supprimer avec le reste).
- **`commun/coeur/domaine/text/TextPatcher.java`** (nouveau) : remplace le
  `String.replace()` littéral. Recherche tolérante aux guillemets — traite `"`, `'`,
  `“`, `”`, `‘`, `’` comme équivalents entre eux (rien d'autre n'est toléré), ce qui
  résout la cause racine des 11 miss observés. Retourne un `Result(text, applied)`
  explicite au lieu du test fragile `patched.equals(result)`.
- **`WriteWorkflow.java`** : les 4 méthodes `applyXxxCorrections` dupliquées
  remplacées par un seul `applyCorrections` générique (utilise `TextPatcher` +
  `LogPort.warn` sur `!applied()`), chacune réduite à un adaptateur d'une ligne.
- **`DeusInMachinaCorrector`, `NaturalityCorrector`, `ProofreaderCorrector`,
  `StyleCorrector`** : `parseFindings`/`parseCorrections` + `unquote()` dupliqués
  supprimés, remplacés par un appel à `CorrectionParser.parse(...)`. Corrige au
  passage une incohérence mineure de `StyleCorrector` (n'acceptait pas le préfixe
  `"- FAUX:"`, seulement `"FAUX:"`).
- **`agent/CLAUDE.md`** : nouvelle section "Corrector — Parsing is Mutualized",
  section "Replace Warning Rule" réécrite pour documenter `TextPatcher` et
  interdire tout `String.replace()` littéral dans un futur correcteur.

## Résultat

Correctif validé par simulation Python rejouant la logique exacte sur les deux fichiers
réels avant d'écrire le moindre Java : **13/14 corrections désormais appliquées**
(contre 3/14 avant), zéro injection de commentaire résiduelle. Le seul cas restant
(008, correction #2) est un défaut de transcription du LLM — un guillemet carrément
omis au milieu d'une citation dupliquée, pas une simple substitution de glyphe — qui
nécessiterait une correspondance floue (Levenshtein) pour être rattrapé ; hors
périmètre validé, le miss reste correctement loggé plutôt que de risquer un remplacement
au mauvais endroit.

Compilation complète (`commun`, `redacteur`) et tests unitaires validés
(`mvn -pl commun,redacteur -am test`). Le module `testllm` a une erreur de compilation
de test préexistante, sans rapport avec ce changement (confirmée par `git stash`).
