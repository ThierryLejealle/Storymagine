# Migration Redacteur → Storymagine

## Statut
Phase 1 terminee : package Scenario code et teste (2026-06-21).
Phase 2 a venir : agents LLM et orchestrateur.

---

## Phase 1 — Package Scenario

### Structure des packages

```
storymagine.redacteur.coeur.domaine.scenario
  Scenario.java
  ScenarioConfig.java
  Personnage.java
  TagElement.java          (abstract — parent de FocusElement et LoreElement)
  NarrativeType.java       (enum : IMPERATIVE / DREAM / WHAT_IF)
  Chapter.java
  ChapterDefaults.java
  Sequence.java
  SequenceOverrides.java   (additifs characters/focus/lore/checks d'une sequence)
  CheckList.java           (checks PLAN + checks WRITER)
  Check.java               (record)
  ConstraintList.java      (contraintes PLAN + contraintes WRITER)
  Constraint.java          (record)

  focus/
    FocusPool.java          (= contenu de focus.md)
    FocusElement.java       (extends TagElement — tags referençables dans les chapitres)
    FocusItem.java          (parent abstrait pour les items focus d'un chapitre)
    FocusRef.java           (extends FocusItem — tag qui pointe sur FocusPool)
    FocusInline.java        (extends FocusItem — texte inline, hors pool)

  lore/
    LorePool.java           (= contenu de lore.md)
    LoreElement.java        (extends TagElement)
    LoreItem.java           (parent abstrait pour les items lore d'un chapitre)
    LoreRef.java            (extends LoreItem — tag qui pointe sur LorePool)
    LoreInline.java         (extends LoreItem — texte inline, hors pool)
```

---

### Decisions de conception

#### TagElement (abstract)
Parent commun de FocusElement et LoreElement. Meme structure, roles differents.

```
String tag            // [CIEL], [MERLIN], etc.
String globalContent  // contenu pour tous les agents (TOUS)
String planContent    // nullable — contenu specifique PLAN
String writerContent  // nullable — contenu specifique WRITER
```

Les elements TOUS (globalContent seulement) apparaissent en premier dans les fichiers,
avant les sections #PLAN et #WRITER.

#### FocusPool vs focus dans un chapitre
- FocusPool = le reservoir defini dans focus.md
- Un Chapter/Sequence ne REFERENCE pas directement FocusElement —
  il contient une List<FocusItem> composee de FocusRef (tag vers pool) ou FocusInline (texte direct)
- FocusRef pointe UNIQUEMENT sur un FocusElement, jamais sur un FocusGroup (voir note ci-dessous)

#### DDD — Lore et Focus sont des classes, pas des listes
FocusPool et LorePool ont des comportements propres : filtrer par section, fusionner
(heritage chapter defaults -> sequence). Ce ne sont pas de simples List<>.

#### Sequence
Sequence.SequenceOverrides regroupe les champs additifs (characters, focus, lore, checks)
pour rester sous la limite de 4 parametres (regle CLAUDE.md).

---

### NOTE IMPORTANTE — FocusGroup exclu du modele de base

Source : commentaire de `Redacteur/story/modele/focus.md` :
> "GROUPE DE FOCUS (chapitres loop/foreach uniquement)"

Les FocusGroup (ex: TIR_MITRAILLEUSE dans as-du-ciel) ne sont utilises que dans des
chapitres de type loop ou foreach. Storymagine exclut deliberement ces types
(seuls IMPERATIVE / DREAM / WHAT_IF sont supportes).

**Decision actee : FocusGroup est exclu du modele. Ne pas implementer.**

Si un scenario futur necessite des groupes de formulations dans un chapitre imperatif,
ouvrir une session de conception avant d'ajouter quoi que ce soit.

---

## Phase 2 — Adapteur ScenarioFileAdapter

Lire et valider un repertoire scenario depuis le filesystem. Va dans `/infra`.

| Fichier source        | Objet cible       | Notes                              |
|-----------------------|-------------------|------------------------------------|
| scenario.md           | ScenarioConfig    |                                    |
| lore.md               | LorePool          | Parser sections #PLAN / #WRITER    |
| focus.md              | FocusPool         | Parser tags [NOM] et groupes       |
| checks.md             | CheckList         | Sections PLAN / REDACTION          |
| constraints.md        | ConstraintList    |                                    |
| example.md            | String (dans ScenarioConfig) |                         |
| characters/*.md       | Personnage        | Id = nom du fichier sans .md       |
| chapitres/chap_N.yaml | Chapter + Sequence | Ordonnes par numero               |
| character_pools.yaml  | ignore (phase 1)  |                                    |
| context_events.md     | ignore (phase 1)  |                                    |
| actions/*.md          | ignore (phase 1)  |                                    |

Port dans `/coeur/ports/` :
```java
interface ScenarioReaderPort {
    List<ScenarioError> validate(Path scenarioRoot);
    Scenario load(Path scenarioRoot);
}
```

---

## Phase 3 — Agents LLM

18 agents a migrer depuis `Redacteur/src/.../context/` vers `/coeur/domaine/`.
Chaque agent = package dedie avec :
- Classe Input
- Classe Output
- Classe PromptBuilder
- Fichier NomAgent.md (documentation)

Agents principaux :
- ScenarioPlanner
- SequencePlanner
- Writer
- Critic (3 variantes)
- NarrativeGoal
- Proofreader
- RepetitionTracker
- StateExtractor
- Compressor
- DeusInMachina
- StyleChecker
- CharacterChecker

---

## Phase 4 — Orchestrateur

- ChapterOrchestrator (imperative uniquement)
- Pas de LoopOrchestrator ni ForeachOrchestrator (exclus du modele)

---

## Phase 5 — Memoire et Logging

- StoryMemory (memoire persistante par chapitre)
- SessionLogger / LlmCsvLogger

---

## Scenario as-du-ciel

Copier depuis `C:\dev\llm-scriot\Redacteur\story\as-du-ciel\`
vers `scenarios\as-du-ciel\` a la racine de Storymagine.

Adaptation a faire lors de la copie :
- Retirer toute reference loop/foreach dans les YAML de chapitres
- Verifier que les FocusGroup de focus.md ne sont references nulle part
  dans des sequences imperatives (cf. note FocusGroup ci-dessus)
