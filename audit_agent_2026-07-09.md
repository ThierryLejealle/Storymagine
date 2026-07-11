# Audit des entrées agents — Storymagine

Pour chaque agent LLM de la chaîne de génération : ce qu'il reçoit réellement (pas ce qu'il
devrait recevoir), et ce qui manque, est en trop, ou ne correspond pas à ce que son propre
prompt annonce.

- Module analysé : `redacteur`
- Périmètre : 24 agents actifs + 3 agents orphelins (non branchés)
- Méthode : analyse statique du code (Step + Input + Agent + `.md`) — **aucune modification appliquée**
- Date : 2026-07-09

**Note (2026-07-09, en cours de correction)** : les noms de classes citées dans ce document ont
été mis à jour au fil des corrections pour refléter le renommage classe ↔ `agentName()` (ex.
`TextNarrativeCritic` → `ChapterNarrativeCritic`, `GoalPlanCritic` → `PlanGoalCritic`). Ce document
n'est plus un instantané figé de l'état initial — c'est le suivi vivant des corrections.

---

## Constats transversaux

Les problèmes ne sont pas répartis au hasard : ils se regroupent en quelques schémas qui
traversent plusieurs agents. À lire avant le détail par agent.

### 🔴 Anomalies (code/doc incohérent avec le prompt réel)

1. ~~**`ChapterCoherenceCritic` — le focus calculé n'est jamais inséré dans le prompt.**~~
   ✅ **FAIT (2026-07-09)** — après analyse, le focus n'était pas seulement mal câblé mais
   conceptuellement hors périmètre (qualité d'exécution, pas cohérence factuelle) : champ
   retiré plutôt que branché. Voir fiche agent ci-dessous.

2. ~~**`ChapterPlanner` — le même texte est injecté deux fois sous deux titres différents.**~~
   ✅ **FAIT (2026-07-09)** — voir détail dans la fiche `ChapterPlanner` ci-dessous.

3. ~~**`DeusInMachinaCritic` / `DeusInMachinaCorrector` — le prompt réclame des données que le
   code ne fournit jamais.**~~
   ✅ **FAIT (2026-07-09)** — voir fiches agents ci-dessous.

4. **`ChapterDreamCritic` — double défaut : libellé trompeur + valeur codée en dur inopérante.**
   Le prompt affiche une section "Psychologie du personnage (objectif du livre)" mais ne reçoit
   en réalité que `ScenarioFormatters.bookGoal(scenario)` — jamais une fiche personnage. Par
   ailleurs `ChapterDreamCriticStep` transmet toujours `"moyen"` comme niveau de réalisme ; cette
   valeur ne correspond à aucune des branches du `switch` (`"realistic"`, `"surreal"`), donc
   l'agent retombe systématiquement sur le mode `"symbolic"` par défaut tout en affichant
   "moyen" dans le prompt envoyé au LLM.

5. ~~**`ChapterNarrativeCritic` — la section "Objectif du chapitre" contient en fait l'objectif du
   livre.**~~
   ✅ **FAIT (2026-07-09)** — voir fiche agent ci-dessous. Confirmé utilisé en usage réel
   (chapitres IMPERATIVE, log `NNN_ChapterNarrativeCritic_N.md` — nom de log = `agentName()`,
   pas le nom de classe ; harmonisation classe/agentName() prévue en fin d'audit, tous agents).

### 🟠 Données mortes (chargées mais jamais consommées)

6. **`style.md` et `quality.md` ne parviennent à aucun agent actif.**
   `Scenario.writingStyle()` et `Scenario.quality()` sont chargés depuis le disque mais n'ont
   plus aucun appelant côté orchestrateur. `StyleCorrectorStep` instancie explicitement
   `new StyleCorrectorInput(text, null, null, ...)` — les deux champs sont câblés à `null` alors
   que `StyleCorrector` contient une branche dédiée (`hasStyle`) pour les exploiter.
   `WriterStep` fait de même pour `styleGuide`.
   ✅ **FAIT (2026-07-09)** — `Writer` → `styleGuide` et `StyleCorrector` → `styleGuide` +
   `qualityCriteria` tous branchés, voir fiches agents.

7. ~~**`Writer` — trois champs de `WriterInput` toujours nuls, un toujours vide.**~~
   ✅ **FAIT (2026-07-09)** — `loopJournal`, `sequenceContext`, `actionsText` retirés (aucun
   producteur trouvé nulle part dans le code, y compris parmi les agents orphelins — voir fiche
   agent). `styleGuide` câblé (cf. constat 6).

8. **`ScenarioFormatters.focusChecks()` n'est appelé nulle part.**
   Cette méthode existe spécifiquement pour qu'un Critic vérifie qu'un élément de focus déclaré
   a bien été utilisé (le côté « check » d'un focus, symétrique au côté « check » d'un
   `Requirement`). Aucun Step ne l'invoque : le côté vérification du focus est mort, seul le
   côté « à utiliser » (Writer/Planner) est branché.

### 🟡 Trous structurels / périmètre incomplet

9. **`PlanNarrativeCritic`, `PlanCoherenceCritic`, `PlanGoalCritic` — aucun ne voit la consigne
   de l'auteur (`chapter.description()`), seulement son `goal` court.**
   Seul `StoryFidelityCritic` (niveau livre, actif en FULL uniquement) reçoit consigne + plan
   ensemble. Un plan qui respecte scrupuleusement la consigne mais s'écarte légèrement du
   `goal` peut être signalé à tort par 3 critics sur 4 ; à l'inverse, un plan qui trahit un
   point précis de la consigne sans toucher au `goal` peut passer inaperçu jusqu'à la critique
   livre — qui ne tourne pas en dessous du niveau FULL. Voir tableau ci-dessous.
   ✅ **`PlanNarrativeCritic` FAIT (2026-07-09)** — voir fiche agent ci-dessous.
   ⏸️ **`PlanCoherenceCritic` — décision volontaire de NE PAS ajouter la consigne** (2026-07-09) :
   son rôle est la cohérence factuelle, pas la fidélité à l'intention de l'auteur (déjà couvert
   par `PlanNarrativeCritic`/`PlanGoalCritic`/`StoryFidelityCritic`) — l'ajouter aurait fait
   sanctionner deux fois le même écart par des critics différents. `entityState` ajouté à la
   place (trou plus pertinent pour son rôle réel — voir fiche agent).
   ✅ **`PlanGoalCritic` FAIT (2026-07-09)** — voir fiche agent ci-dessous. Cas réel observé en
   plus de la simple absence de consigne : le critic a produit "Incohérence logique : ..." sur
   un plan, alors que son SYSTEM interdit explicitement de juger la cohérence — la restriction
   de périmètre n'était posée qu'en préambule, jamais rappelée à l'étape "note tous les défauts"
   ni dans l'instruction finale (les deux points de plus forte attention). Corrigé en même temps.

10. ~~**`DeusInMachinaCritic` / `DeusInMachinaCorrector` — checks au niveau chapitre seulement,
    pas séquence.**~~
    ✅ **FAIT (2026-07-09)** — corrigé en même temps que le constat 3 (même cause : le Step ne
    recevait pas l'objet `Sequence`). Voir fiches agents ci-dessous.

### Qui voit la consigne complète du chapitre ?

| Agent | `chapter.description()` (consigne) | `chapter.goal()` (objectif) | Remarque |
|---|:---:|:---:|---|
| ChapterPlanner | ✅ | ✅ | logique — c'est lui qui doit la traduire en plan |
| StoryFidelityCritic / Narrative / Causal | ✅ | ✅ | via `chaptersBlock` — actif en FULL uniquement |
| PlanNarrativeCritic | ✅ (2026-07-09) | ✅ | corrigé — voir fiche agent |
| PlanCoherenceCritic | ❌ (volontaire) | ✅ | rôle = cohérence factuelle, pas fidélité consigne — `entityState` ajouté à la place |
| PlanGoalCritic | ✅ (2026-07-09) | ✅ | corrigé — voir fiche agent |
| Writer | ❌* | ❌ | *reçoit `chapterPlan` (dérivé de la consigne) + `sequence.directive()`, pas la consigne brute |
| ChapterNarrativeCritic | ✅ (2026-07-09) | ✅ (2026-07-09) | corrigé — voir fiche agent |
| ChapterCoherenceCritic | ❌ | ❌ | reçoit `Chapter` mais n'en lit ni goal ni description |
| ChapterGoalCritic | ❌ | ✅ | seul critic de texte correctement branché sur le goal |

---

## Phase 1 — Plan livre

Critique globale sur l'ensemble des plans de chapitres concaténés — active uniquement en
qualité FULL, avant toute écriture.

### StoryFidelityCritic — ✅ RAS
Vérifie que chaque plan de chapitre réalise fidèlement la consigne de l'auteur (description +
objectif), à l'échelle du livre entier.

- **Reçoit** : `bookGoal`, `chaptersBlock` (titre + consigne + objectif + plan, par chapitre)
- Le seul agent de toute la chaîne à voir consigne et plan ensemble, à l'échelle du livre.
  Rappel : inactif hors qualité FULL — c'est un choix de coût documenté, pas un manque de
  câblage.

### StoryNarrativeCritic — ✅ RAS
Repère les incohérences d'arc de personnage entre les ajouts (au-delà de la consigne) de deux
chapitres différents.

- **Reçoit** : `bookGoal`, `chaptersBlock`
- Entrées cohérentes avec un rôle explicitement borné aux « ajouts de l'IA » — ne juge jamais
  la consigne elle-même.

### StoryCausalCritic — ✅ RAS
Repère les contradictions factuelles entre les ajouts (au-delà de la consigne) de deux
chapitres différents.

- **Reçoit** : `bookGoal`, `chaptersBlock`
- Même remarque que StoryNarrativeCritic. Les trois critics livre ne comparent les chapitres
  qu'entre eux, jamais contre les fiches lore/personnages — cohérent avec leur définition
  (« ajout de l'IA contredit un autre ajout de l'IA »), pas un manque.

---

## Phase 2 — Plan chapitre

Génération puis critique du plan d'un chapitre — beats par séquence, cohérence, arc narratif,
atteinte de l'objectif.

### ChapterPlanner — ✅ FAIT (2026-07-09)
Produit le plan d'un chapitre — libre ou JSON structuré par séquence (beats, sensoriels,
intention de scène).

- **Reçoit** : `chapterTitle`, `chapterDescription`, `chapterSetting`, `sequenceDescriptions`
  (+ beats hint), `bookGoal`, `storySoFar`, `entityState`, `characters` (chapitre),
  `constraints`, `focusText`/`loreText` (chapitre), `forbiddenPhrases`,
  `isRewrite`/`previousPlan`, `problemsToFix`
- **Constat initial** : `coherence` et `problemsToFix` recevaient la même chaîne
  (`wc.coherence()`), affichée sous deux titres différents ("État de cohérence" / "Problèmes à
  corriger impérativement"). Vérification : `WrittenChapter.coherence()` n'est alimenté que par
  du feedback de critiques fusionné (`ChapterPlanWorkflow.setCoherence` /
  `StoryPlanWorkflow.setCoherence`) — pas de concept distinct d'« état de cohérence ».
- **Correctif appliqué** :
  - suppression de la section "État de cohérence" et du champ `coherence` de
    `ChapterPlannerInput` (doublon exact, aucune perte d'information) ;
  - réordonnancement du prompt : "Plan précédent (à corriger)" est maintenant présenté **avant**
    "Problèmes à corriger impérativement" (au lieu de l'inverse), pour rapprocher l'instruction
    actionnable du point de génération — effet de récence, important pour les petits modèles
    ciblés par le projet ;
  - l'instruction finale ("Corrige chacun des points ci-dessus...") est désormais accolée à la
    liste de problèmes plutôt qu'au plan précédent.
- Fichiers modifiés : `ChapterPlanner.java`, `ChapterPlannerInput.java`, `ChapterPlannerStep.java`.
  Fiche d'évolution : `evols/2026-07-09-1900-chapterplanner-dedoublonnage-coherence.md`.

### PlanNarrativeCritic — ✅ FAIT (2026-07-09)
Évalue la progression de l'arc narratif d'un plan de chapitre.

- **Reçoit** : `plan`, `bookGoal`, `chapterDescription`, `chapterGoal`
- **Constat initial** : sans la consigne (`chapterDescription`), l'agent pouvait qualifier de
  "défaut d'arc" un choix que l'auteur avait explicitement demandé mais qui n'apparaissait pas
  dans le `goal` court.
- **Correctif appliqué** :
  - ajout de `chapterDescription` (section "Consigne de l'auteur (ce chapitre)") ;
  - réordonnancement du prompt : `bookGoal → chapterDescription → chapterGoal → plan` (large
    vers précis, puis l'objet jugé au plus près de l'instruction "Evalue le plan.") — même
    principe que `ChapterPlanner`, cohérent avec l'ordre déjà utilisé par `StoryFidelityCritic` ;
  - SYSTEM : "Si l'objectif de ce chapitre est fourni..." → "Si la consigne ou l'objectif de ce
    chapitre sont fournis...", pour que la garde anti-faux-positif couvre aussi la consigne.
- Fichiers modifiés : `PlanNarrativeCritic.java`, `PlanNarrativeCriticInput.java`,
  `PlanNarrativeCriticStep.java`.

### PlanCoherenceCritic — ✅ FAIT (2026-07-09, partiel — décision volontaire)
Vérifie la cohérence factuelle d'un plan (fiches personnages, contraintes, continuité
intra-plan **et** avec les faits déjà établis dans l'histoire).

- **Reçoit** : `enrichedPlan` (+ `points_a_verifier`/focus/lore par séquence), `chapterGoal`,
  `checks` (scénario + chapitre), `characters` (fusion chapitre + séquences), `entityState`
- **Constat initial** : le prompt savait déjà chercher des contradictions type « fiche dit 1943,
  le plan dit 1942 » — mais seulement entre séquences du même plan, faute d'accès à
  `entityState` (faits déjà établis par les chapitres précédents).
- **Décision** : `chapterDescription` volontairement **non ajoutée** — le rôle de cet agent est
  la cohérence factuelle, pas la fidélité à l'intention de l'auteur (déjà couvert par
  `PlanNarrativeCritic`/`PlanGoalCritic`/`StoryFidelityCritic`) ; l'ajouter aurait fait juger le
  même écart deux fois par des critics différents (score moyenné faussé).
- **Correctif appliqué** :
  - ajout de `entityState` (section "État des entités (faits déjà établis)") ;
  - SYSTEM : la continuité factuelle couvre maintenant explicitement "les séquences du plan et
    les faits déjà établis dans l'histoire" (au lieu de seulement "entre les séquences du
    plan") ; ajout d'un exemple DEFAUT_MAJEUR dédié à ce cas.
- Fichiers modifiés : `PlanCoherenceCritic.java`, `PlanCoherenceCriticInput.java`,
  `PlanCoherenceCriticStep.java`.

### PlanGoalCritic — ✅ FAIT (2026-07-09)
Vérifie que le plan fait avancer concrètement l'objectif narratif du chapitre.

- **Reçoit** : `plan`, `chapterDescription`, `chapterGoal`, `bookGoal`
- **Constat initial** : même remarque que `PlanNarrativeCritic` (goal seul potentiellement trop
  court). Cas réel observé en plus : le critic a produit `PROBLEME: "Incohérence logique : ..."`
  alors que son propre SYSTEM interdit explicitement de juger la cohérence — la restriction de
  périmètre n'était posée qu'en préambule, jamais rappelée à l'étape "note tous les défauts" ni
  dans l'instruction finale (les deux points de plus forte attention dans un prompt).
- **Correctif appliqué** :
  - ajout de `chapterDescription` (section "Consigne de l'auteur (ce chapitre)", en tête) ;
  - SYSTEM : "Ne juge pas... la cohérence globale du roman" complété par "— c'est le rôle d'un
    autre agent, jamais le tien" ; étape 2 reformulée en "PAR RAPPORT A LA CONSIGNE ET A
    L'OBJECTIF DE CE CHAPITRE (jamais la cohérence générale, jamais la qualité littéraire)" ;
    ajout de la garde anti-faux-positif partagée avec `PlanNarrativeCritic`/`PlanCoherenceCritic` ;
  - instruction finale reformulée pour citer consigne + objectif au lieu du seul objectif.
- Fichiers modifiés : `PlanGoalCritic.java`, `PlanGoalCriticInput.java`, `PlanGoalCriticStep.java`.

---

## Phase 3 — Write séquence : Writer & correcteurs

Rédaction d'une séquence, puis quatre passes de correction inline (patches FAUX→JUSTE
appliqués en Java, jamais de réécriture directe par le LLM).

### Writer — ✅ FAIT (2026-07-09)
Rédige la prose d'une séquence à partir de la trame, des directives et du contexte narratif
accumulé.

- **Reçoit** : `sequenceDescription`/`sequencePlan`, `minWords`, `isRewrite`, `charactersText`
  (chapitre+séquence), `focusText`/`loreText` (chapitre+séquence), `chapterPlan` (+ problèmes
  si réécriture), `prevSentences`, `entityState`, `storySoFar`, `forbiddenPhrases`/
  `forbiddenThemes`, `redactionConstraints`, `styleGuide`, `writingExample`, `setting`, `stitch`
- **Constat initial** : 4 champs de `WriterInput` jamais alimentés par `WriterStep` —
  `loopJournal` (null), `styleGuide` (null), `sequenceContext` (null), `actionsText` ("").
  Recherché un producteur possible pour chacun (y compris parmi les agents orphelins,
  notamment `FocusActionFilter` comme candidat pour `actionsText`) : aucun ne convient —
  `FocusActionFilter` filtre des *noms* de catégories, pas du texte narratif à injecter tel
  quel. Aucune trace d'un ancien mécanisme de "boucle" pour `loopJournal` ailleurs dans le code.
- **Bug annexe découvert et corrigé** : `writingExample` (`example.md`) était lu par
  `readOptionalFile` (brut) au lieu de `readOptionalDirective` (qui filtre les blocs
  `<!-- -->` et traite un template non rempli comme absent) — seul fichier "directive" du
  scénario à ne pas passer par ce filtre. Conséquence concrète observée en usage réel : le
  commentaire-template d'`example.md` ("Collez ici un extrait représentatif...") était envoyé
  tel quel au LLM comme si c'était le vrai exemple de style, dès qu'un scénario ne l'avait pas
  rempli. Corrige aussi `StyleCorrector` et l'orphelin `ChapterStyleCritic` (ex-`ChapterStyleChecker`),
  qui consomment le même champ.
- **Incohérence de nommage découverte et corrigée** (règle VITAL de `agent/CLAUDE.md`) : la
  liste de priorité en cas de conflit du SYSTEM prompt citait des libellés ("Contraintes de
  rédaction", "Fiches personnages", "Style et exemple de rédaction") qui ne correspondaient à
  **aucun titre de section réel** du prompt utilisateur ("Règles de rédaction", "Personnages
  présents", "Guide de style" fusionné avec l'exemple). Toutes les données étaient bien
  transmises — seuls les noms divergeaient.
- **Correctifs appliqués** :
  - câblage de `styleGuide` → `scenario.writingStyle()` (`style.md`, jusqu'ici jamais appelé
    nulle part dans le projet) ;
  - retrait de `loopJournal`, `sequenceContext`, `actionsText` (champ `WriterInput`, paramètre
    `WriterStep`/`WriteWorkflow`, sections et slots correspondants) ;
  - fix loader : `writingExample` lu via `readOptionalDirective` ;
  - liste de priorité éclatée en 8 points (Style / Exemple séparés) et alignée mot pour mot sur
    les titres de section réels ;
  - **réordonnancement complet** du prompt utilisateur : les sections suivent désormais l'ordre
    **inverse** de la liste de priorité (la moins prioritaire — Lore — en tête, la plus
    prioritaire — Directives de l'auteur — juste avant l'instruction finale). Raison : effet de
    récence pour petit LLM, même principe déjà appliqué à `ChapterPlanner`/`PlanGoalCritic`/
    `PlanNarrativeCritic` — voir `Writer.md` pour le détail.
- Fichiers modifiés : `Writer.java`, `WriterInput.java`, `WriterStep.java`, `WriteWorkflow.java`
  (retrait du paramètre `actionsText` des deux call sites), `ScenarioFileAdapter.java`,
  `Writer.md`.

### DeusInMachinaCorrector — ✅ FAIT (2026-07-09)
Corrige en ligne les passages où la mécanique de fabrication devient visible (fuites
d'instructions, étiquettes de personnage, artefacts de scénario…).

- **Reçoit** : `text`, `checks` (scénario + chapitre + **séquence**), `sequenceDirective`,
  `sequencePlan`
- **Constat initial** : le SYSTEM prompt dit explicitement de lire en priorité la "Consigne de
  séquence"/le "Plan de séquence" pour ne pas corriger à tort un passage voulu — mais ni l'un ni
  l'autre n'existait dans `DeusInMachinaCorrectorInput`, et le Step ne recevait même pas l'objet
  `Sequence` (donc structurellement incapable de les fournir). Les checks manquaient aussi le
  niveau séquence (variante 2 arguments au lieu de 3).
- **Correctif appliqué** : `DeusInMachinaCorrectorStep.run()` reçoit désormais `Sequence` et
  `sequencePlan` ; sections "Consigne de séquence"/"Plan de séquence" ajoutées **en tête** du
  prompt (le SYSTEM demande explicitement de les lire en premier — ordre physique aligné sur
  l'instruction littérale, pas sur l'effet de récence général) ; `writerChecks` passé en
  variante 3 arguments.
- Fichiers modifiés : `DeusInMachinaCorrector.java`, `DeusInMachinaCorrectorInput.java`,
  `DeusInMachinaCorrectorStep.java`, `WriteWorkflow.java` (`applyCorrectorsPhase` + 2 call
  sites).

### NaturalityCorrector — ✅ RAS
Corrige les formulations artificielles, surconstruites ou analytiques ("ambiance expliquée",
"conclusions narratorielles"…).

- **Reçoit** : `text`
- Tâche purement stylistique et indépendante du contenu — aucun contexte scénario nécessaire
  pour la remplir correctement.

### StyleCorrector — ✅ FAIT (2026-07-09)
Corrige les faiblesses de style (verbes faibles, répétitions de structure, clichés,
transitions mécaniques).

- **Reçoit** : `text`, `writingExample`, `styleGuide`, `qualityCriteria`
- **Constat initial** : `StyleCorrectorStep` instanciait
  `new StyleCorrectorInput(text, null, null, scenario.writingExample())`. Pourtant
  `StyleCorrector.buildSystem()`/`buildUser()` contiennent déjà tout le nécessaire (branche
  `hasStyle`, section "Critères de qualité") pour exploiter ces deux champs.
- **Vérification faite avant de brancher** : contenu réel de `quality.md` inspecté sur
  plusieurs scénarios (`as-du-ciel`, `1998`) — pas des templates vides, du contenu concret et
  spécifique non couvert ailleurs dans le pipeline (voix propre par personnage, conventions de
  nommage, registre interdit). `specs/scenario.md` disait "Injecté dans les prompts Critic" et
  "style.md — writer uniquement" — décalage avec l'architecture actuelle dû à la fusion
  `StyleCritic` → `StyleCorrector` (evol `2026-06-26-1430`) ; doc mise à jour en même temps.
- **Correctif appliqué** : `StyleCorrectorStep` passe désormais `scenario.writingStyle()` et
  `scenario.quality()` au lieu de `null, null`.
- Fichiers modifiés : `StyleCorrectorStep.java`, `specs/scenario.md`.

### ProofreaderCorrector — ✅ RAS
Corrige grammaire, orthographe, accords, syntaxe.

- **Reçoit** : `text`
- Tâche indépendante du contenu narratif — périmètre minimal correct.

---

## Phase 3 (suite) — Write séquence : critiques

Critique globale lancée après les correcteurs — score moyen, retour au Writer si sous le
seuil.

### DeusInMachinaCritic — ✅ FAIT (2026-07-09)
Détecte (sans corriger) les mêmes cinq types de fuites que son homologue Corrector.

- **Reçoit** : `text`, `checks` (scénario + chapitre + **séquence**), `sequenceDirective`,
  `sequencePlan`
- **Constat initial** : même anomalie exacte que `DeusInMachinaCorrector` — même SYSTEM en tête
  de prompt, même trou de données. Corrigé ensemble (même Step-level fix : `Sequence` désormais
  transmis).
- Fichiers modifiés : `DeusInMachinaCritic.java`, `DeusInMachinaCriticInput.java`,
  `DeusInMachinaCriticStep.java`, `WriteWorkflow.java` (call site dans `runSequenceCritique`).

### PlanFidelityCritic — ✅ RAS
Vérifie que chaque beat planifié pour la séquence est bien développé dans le texte rédigé.

- **Reçoit** : `beats` (extraits du plan de séquence), `sequenceText`
- Périmètre minimal et exact pour sa tâche — rien à ajouter, rien à enlever.

### CheckCritic — ✅ RAS
Vérifie que chaque point de contrôle déclaré dans le scénario est respecté dans le texte.

- **Reçoit** : `checks` (scénario + chapitre + séquence), `sequenceText`
- Seul des trois critics de séquence à utiliser la variante `writerChecks` à 3 arguments
  (inclut la séquence) — le bon niveau de détail. Pour mémoire, ses échecs ne remontent pas au
  Writer (TODO déjà signalé dans le code : hors périmètre de cet audit, qui porte sur les
  entrées).

---

## Phase 4 — Write séquence : post-traitement

Toujours exécuté, même en BROUILLON — alimente la mémoire de l'histoire pour les séquences
suivantes.

### StateExtractor — ✅ RAS
Extrait les changements d'état des entités physiques (personnages, objets, lieux) après une
séquence.

- **Reçoit** : `sequenceText`, `previousState` (entityState)

### RepetitionTracker — ✅ RAS
Extrait expressions et schémas narratifs distinctifs à éviter dans les séquences suivantes.

- **Reçoit** : `text`, `alreadyPhrases`, `alreadyThemes`

### RepetitionFilter — ✅ RAS
Retire de la liste noire les candidats qui recoupent en fait un leitmotiv protégé
(`keep_phrases.md`).

- **Reçoit** : `candidates`, `keepPhrasesContent`

---

## Phase 5 — Critique de chapitre

Après l'écriture de toutes les séquences — critique sur le texte complet, sélection des
critics selon le type de chapitre (IMPERATIVE / DREAM / WHAT_IF).

### ChapterNarrativeCritic — ✅ FAIT (2026-07-09)
Évalue la progression de l'arc narratif du texte final (chapitres IMPERATIVE).

- **Reçoit** : `text`, `chapterDescription`, `chapterGoal`, `bookGoal`
- **Constat initial** : `ChapterNarrativeCriticStep.run(text, scenario)` ne recevait même pas le
  `Chapter` — impossible d'accéder à son `goal`. Le prompt affichait la valeur reçue (en réalité
  le `bookGoal`) sous le titre `### Objectif du chapitre` — étiquette trompeuse. L'agent ne
  passait pas non plus par `PromptBuilder` (concaténation manuelle), contrairement au reste du
  projet. Même raisonnement que `PlanNarrativeCritic` : le `goal` court peut être insuffisant
  pour ne pas signaler à tort un choix narratif explicitement voulu par la consigne (ex. réel
  observé : deux personnages qui ne se croisent pas avant une séquence donnée — explicite dans
  `chap_1.yaml.description`, absent du `goal` court).
- **Correctif appliqué** : `ChapterNarrativeCriticStep` reçoit désormais `Chapter` et transmet
  `chapter.description()` + `chapter.goal()` ; `ChapterNarrativeCritic.call()` réécrit avec
  `PromptBuilder`, sections et ordre alignés sur `ChapterGoalCritic` (même phase, même texte
  évalué, cité par l'audit comme référence) avec la consigne ajoutée en tête : "Consigne de
  l'auteur (ce chapitre)" → "Objectif narratif de ce chapitre" → "Objectif global du roman
  (contexte)" → "Texte à évaluer" → instruction finale. SYSTEM : garde anti-faux-positif étendue
  à la consigne (même formulation que `PlanNarrativeCritic`/`PlanGoalCritic`).
- Fichiers modifiés : `ChapterNarrativeCritic.java`, `ChapterNarrativeCriticInput.java`,
  `ChapterNarrativeCriticStep.java`, `WriteWorkflow.java` (call site).

### ChapterCoherenceCritic — ✅ FAIT (2026-07-09)
Vérifie la cohérence factuelle du texte final (checks, fiches personnages, continuité).

- **Reçoit** : `text`, `checks` (scénario + chapitre)
- **Constat initial** : `focusText` était calculé par le Step (avec en plus le mauvais côté —
  `focusText(..., forWriter=true)` au lieu du côté check), porté par l'Input, documenté dans le
  `.md` de l'agent — et absent du prompt réellement construit par `ChapterCoherenceCritic.call()`.
- **Analyse approfondie avant de corriger** : vérification du contenu réel des `# CHECK` de
  `focus.md` (`scenarios/as-du-ciel/focus.md`) — ce sont des questions de **qualité
  d'exécution** ("Le ciel est-il décrit avec un détail sensoriel concret... ?"), pas de
  cohérence factuelle. Le SYSTEM de `ChapterCoherenceCritic` s'interdit lui-même explicitement de
  juger la qualité littéraire/le style — y injecter le focus aurait été une erreur de
  catégorie (même nature que l'incident "Incohérence logique" sur `PlanGoalCritic`).
- **Décision** : le focus n'a pas sa place dans cet agent. Champ `focusText` retiré (record,
  Step, `.md`) plutôt que branché. Le côté check du focus (`ScenarioFormatters.focusChecks()`,
  constat 8 ci-dessus) reste délibérément non branché nulle part — question ouverte sur *où* il
  devrait aller, pas résolue aujourd'hui.
- Fichiers modifiés : `ChapterCoherenceCriticInput.java`, `ChapterCoherenceCriticStep.java`,
  `ChapterCoherenceCritic.md`.

### ChapterDreamCritic — 🔴 Anomalie
Évalue la qualité onirique d'un chapitre RÊVE (résonance symbolique, cohérence interne du
rêve).

- **Reçoit** : `text`, `bookGoal` (étiqueté "Psychologie du personnage"), `realismLevel = "moyen"`
  (codé en dur)
- **Absent** : fiche du personnage qui rêve, source réelle pour `realismLevel`
- **Constat** : le libellé "Psychologie du personnage" promet une fiche personnage ; seul le
  `bookGoal` arrive. Et `realismLevel` ne correspond à aucune branche du `switch`
  ("realistic"/"surreal") : l'agent utilise toujours les critères "symbolic" par défaut quel
  que soit le réglage affiché.
- **Proposition** : créer une source réelle pour `realismLevel` si la distinction
  symbolic/realistic/surreal doit vraiment être pilotable, et brancher une vraie fiche
  personnage si "psychologie du personnage" doit rester un critère réel.

### ChapterWhatIfCritic — ✅ RAS
Évalue la plausibilité physique et causale d'un chapitre scénario-alternatif (WHAT_IF).

- **Reçoit** : `text`, `checks` (scénario + chapitre)
- Le prompt exclut explicitement et à raison la cohérence avec la trame principale (divergence
  voulue). Entrées cohérentes avec ce périmètre.

### ChapterGoalCritic — ✅ RAS
Vérifie que le texte final atteint l'objectif narratif propre à ce chapitre (tous types de
chapitre).

- **Reçoit** : `text`, `chapterGoal` (correctement `chapter.goal()`), `bookGoal`
- Le seul critic de texte final correctement câblé sur l'objectif propre du chapitre, avec un
  étiquetage fidèle à la donnée reçue — sert de référence pour corriger ChapterNarrativeCritic
  ci-dessus.

---

## Phase 6 — Post-production

Après validation d'un chapitre entier — condense l'histoire pour les chapitres suivants.

### StoryCompressor — ✅ RAS
Produit un résumé factuel croissant de l'histoire, intégré au contexte des chapitres suivants
(`storySoFar`).

- **Reçoit** : `existingSummary`, `newChapterText`, `chapterIndex`, budgets mots (base + par
  chapitre)
- Périmètre correct — consignes INCLURE/EXCLURE claires et exploitables par un petit modèle.

---

## Agents orphelins (hors pipeline actif)

Présents dans `agent.temp`, non référencés dans `RedacteurModule` ni dans aucun `Workflow` —
n'appellent jamais de LLM en usage réel actuellement.

- **FocusActionFilter** — sélectionnerait les groupes de focus/catégories d'action pertinents
  pour une séquence donnée. Aucun appelant.
- **ChapterStyleCritic** *(renommé le 2026-07-09, ex-`ChapterStyleChecker`)* — équivalent
  chapitre de l'ancien contrôle de style, remplacé en pratique par `StyleCorrector` au niveau
  séquence. Aucun appelant.
- **CharacterCritic** *(renommé le 2026-07-09, ex-`CharacterChecker`)* — vérifierait la
  cohérence personnage vs fiche + ruptures d'état. Aucun appelant. Fichier source toujours en
  encodage corrompu (mojibake — `"Ã©diteur"`, `"cohÃ©rence"` au lieu de `"éditeur"`,
  `"cohérence"`) — préservé tel quel lors du renommage (correction du contenu = hors périmètre
  du renommage, à traiter séparément si l'agent est un jour réactivé).

---

*Analyse strictement statique — aucun fichier n'a été modifié. Chaque constat renvoie à un
Step, un Input, un Agent ou un `.md` lu dans le module `redacteur`.*
