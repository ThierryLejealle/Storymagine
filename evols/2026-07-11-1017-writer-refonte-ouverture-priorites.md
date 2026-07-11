# 2026-07-11 10h17 — Writer : refonte de la règle d'ouverture, priorités, harmonisation

## 1. Demande

Suite de la revue Writer entamée le 2026-07-10 (points A-K identifiés avec Fable). Reprise le
2026-07-11 avec les directives suivantes de l'utilisateur, point par point :

- **A** (interdictions mal placées) : laisser tel quel, pas de changement.
- **B** ("Tu ne prends aucune décision narrative") : supprimer — redondant avec le respect strict
  des directives déjà exigé ailleurs.
- **C** (triple synonyme "allusion/indice/anticipation") : simplifier.
- **D** (bloc final surchargé, portée ambiguë du "Sauf si le guide de style...") : améliorer.
- **E** (branche "nouvelle séquence" : comparaison invérifiable à une séquence précédente non
  fournie) : discuté en détail — décision de toujours passer la séquence précédente quand elle
  existe (voir ci-dessous), donc ce cas ne survient plus que pour la vraie première séquence d'un
  chapitre, où on retire la clause de comparaison invérifiable.
- **F** (doublon système/user sur le raccord) : corriger.
- **G** (branche `stitch` : deux maîtres pour la première phrase, garde-fou anti-Il/Elle perdu) :
  refonte demandée par l'utilisateur — toujours montrer la séquence précédente ; si un `stitch`
  existe, l'encadrer explicitement comme consigne de gestion de la transition plutôt que de
  l'injecter brut, et ne plus imposer la continuité stricte ("DOIT en être la suite directe") dans
  ce cas, puisque le stitch peut légitimement vouloir autre chose (ellipse, saut temporel).
- **H** (consigne qualitative de longueur perdue selon le seuil chiffré) : bug confirmé, à corriger.
- **I** (liste de priorité incomplète) : proposer un classement.
- **J** (trois conventions de formatage différentes) : harmoniser.
- **K** ("ces directives" ambigu en fin de prompt) : améliorer.

Discussion additionnelle sur le bug de budget de `storySoFar` (`sHistory * 4` au lieu de
`sHistory`, découvert en creusant le point E/l'idée de donner la séquence précédente complète) :
corrigé, mais le seuil 1/8 lui-même est laissé inchangé — étude du "budget trop timide en général"
reportée à plus tard (voir mémoire `project_writer_budget_timide_todo`).

## 2. Ce qui a été touché

### `WriterInput.java`
Champ `prevSentences` renommé `previousSequenceText` — le contenu a changé de nature (3 dernières
phrases → texte intégral de la séquence précédente), donc l'ancien nom ne décrivait plus le champ.

### `WriterStep.java`
`lastSentences(wc, 3)` remplacé par `previousSequenceText(wc)` : renvoie le texte **intégral** de
la dernière séquence écrite du chapitre (plus de découpage en phrases), ou `""` s'il n'y en a pas.

### `Writer.java`
**`buildSystem()`** :
- Point B : phrase "Tu ne prends aucune décision narrative..." supprimée.
- Point H : `lengthConstraint` restructuré — la consigne qualitative ("développe... profondeur et
  précision sensorielles") est désormais toujours présente ; seul le seuil chiffré varie selon
  `minWords`.
- Points E/G : règle d'ouverture réécrite en 3 cas mutuellement exclusifs, dans cet ordre :
  1. `stitch` fourni → encadré explicitement ("Applique cette consigne pour gérer le passage de la
     séquence précédente à celle que tu vas écrire : {stitch}") au lieu d'être injecté brut.
  2. Pas de stitch, séquence précédente existante → règle par défaut inchangée ("raccorde sans
     résumer, poursuis l'action").
  3. Ni l'un ni l'autre (première séquence du chapitre) → "point d'entrée varié", sans comparaison
     à une séquence précédente absente (clause invérifiable retirée).
- Point I : liste de priorité réduite à ce qui peut vraiment entrer en conflit (7 niveaux inchangés
  dans leur contenu), avec une phrase séparée déclarant les interdictions et les faits déjà établis
  (état des entités, texte précédent) comme absolus et hors de cet arbitrage — plutôt que de les
  intégrer en les numérotant (aurait allongé une liste déjà jugée longue).
- Point C : "aucune allusion aux séquences suivantes, aucun indice sur la suite, aucune
  anticipation des événements à venir" → "aucune anticipation des événements des séquences
  suivantes" (une seule formulation).
- Point D : bloc final réorganisé en 3 paragraphes clairement séparés (événements de la séquence ;
  fiche personnage/traits non négociables ; naturel du texte), et la portée de l'exception "guide de
  style" clarifiée explicitement : *"Cette règle de naturel s'efface uniquement devant une consigne
  contraire explicite du guide de style"* — ne couvre plus que le paragraphe naturel/anti-IA, sans
  ambiguïté sur le reste.

**`buildUser()`** :
- Bug budget : `t.tailText(in.storySoFar(), sHistory * 4, "storySoFar")` → `sHistory` (fraction
  réellement 1/8 comme documenté, au lieu de ~1/2 du contexte par accident). TODO inline + mémoire
  ajoutés pour étudier si 1/8 reste le bon seuil une fois le vrai comportement observé.
- Point J : `bannedPhrases`/`bannedThemes` et `[Cadre de la scène]` passés au même mécanisme
  `PromptBuilder.section("Titre", contenu)` que toutes les autres sections — suppression des
  en-têtes MAJUSCULES et des crochets bruts, une seule convention de formatage partout.
- Point F/G : la section "Texte précédent" n'impose plus systématiquement "DOIT en être la suite
  directe" — seulement quand il n'y a pas de `stitch` ; avec un `stitch`, note neutre ("Ce texte est
  déjà écrit — ne le reproduis pas") laissant le stitch gouverner la transition.
- Séquence précédente donnée **en entier, sans troncature** (comme `chapterPlan`/`rewriteProblems`
  précédemment) — s'appuie sur l'agrandissement automatique de contexte d'`OllamaAdapter` en cas de
  dépassement, plutôt que de risquer de couper le texte que le Writer doit directement continuer.
- Point K : "Écris le texte de cette séquence en respectant intégralement **ces directives**." →
  "...**l'ensemble des éléments ci-dessus**." — ne peut plus être compris comme ne visant que la
  section "Directives détaillées de l'auteur" juste au-dessus.

### `Writer.md`
Mis à jour en profondeur : tableau des slots (retrait de la troncature sur `previousSequenceText`,
TODO sur `storySoFar`), section priorité (interdictions/faits établis hors liste, formatage
harmonisé), section "Logique de la directive" enrichie (mode réécriture), section "Règles
d'ouverture" entièrement réécrite pour documenter les 3 cas stitch/continuité/point d'entrée varié
et le renommage `prevSentences` → `previousSequenceText`.

## 3. Résultat

Compilation vérifiée depuis la racine (`mvn -q test-compile`, aucune erreur). Suite de tests
(redacteur + commun) : 19 tests, 1 seul échec — le même échec pré-existant et sans rapport déjà
documenté le 2026-07-10 (`WorkflowLogTest.planOnly_doesNotLogWriteOrEval`, chantier PLAN_ONLY en
cours ailleurs). Aucune régression introduite.

Chantiers explicitement reportés (voir mémoire) : Naturality (8 exemples validés en discussion,
jamais écrits), Deus/Writer sur la fuite de raisonnement LLM (3 deltas prêts), étude du budget de
contexte Writer en général.
