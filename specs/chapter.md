# chapitres/chap_N.yaml — Format d'un chapitre

Les chapitres sont placés dans `story/<nom>/chapitres/`.
Nommage : `chap_1.yaml`, `chap_2.yaml`, `chap_11.yaml`… **Tri numérique** (chap_2 avant chap_11,
chap_02 et chap_2 sont identiques).

---

## Champs du chapitre

### `comment` *(string, optionnel)*
Note humaine. Aucun effet sur le moteur, non transmis aux agents.
Sert uniquement à documenter le fichier YAML.

---

### `title` *(string, obligatoire)*
Titre du chapitre.
- **Moteur :** `Chapter.title` — utilisé comme fallback dans les logs et le `SequenceChecker`
- **Agents :**
  - **Planner (`ScenarioPlannerContext`)** — injecté dans `### Chapitre à planifier / Titre :`

---

### `type` *(string, défaut : `imperative`)*
Type de chapitre. Conditionne le comportement du moteur.
- **Moteur uniquement** — non transmis aux agents
- Valeurs disponibles :
  - `imperative` — tout est explicitement décrit via des séquences ; c'est le seul type en scope actuellement

> Les types `loop`, `foreach`, `dream`, `what_if` existent dans le moteur mais ne font pas
> partie du format actuel. Voir `temp/reste_a_clarifier.md`.

---

### `description` *(string, optionnel)*
Synthèse narrative du chapitre.
- **Moteur :** `Chapter.description` — non utilisé directement dans la logique de séquençage
- **Agents :**
  - **ChapterPlanner** — injecté dans `Description :` du prompt de planification ; **omis si absent ou vide**

---

### `goal` *(string, optionnel)*
But dramatique du chapitre — ce que le chapitre doit accomplir narrativement sur le lecteur.
- **Moteur :** `Chapter.goal` — évalué par `NarrativeGoalContext` lors des passes de critique
- **Agents :**
  - **NarrativeGoalContext** — compare le plan ou le texte généré à ce but ; score le résultat sur 10

---

## Bloc `defaults:`

Propriétés héritées par toutes les séquences du chapitre via **merge additif** :
les valeurs de la séquence s'ajoutent à celles des defaults, elles ne les remplacent pas.

### `defaults.character_sheets` *(liste de strings, optionnel)*
Identifiants de fiches personnage à charger depuis `characters/<id>.md`.
- **Moteur :** merge avec `seq.characterSheets` dans `ChapterOrchestrator.writeOneSequence()`
- **Agents :**
  - **Planner (`ScenarioPlannerContext`)** — charge `## PLAN` + `## INTÉRIEUR` groupés par personnage → injecté dans le slot `chars`
  - **Writer (`WriterContext`)** — charge uniquement `## RÉDACTION` → injecté dans le slot `chars`
- **Sémantique :** personnages *possibles* dans les scènes du chapitre, pas nécessairement présents dans chaque séquence

> Syntaxe pour référencer une carte `.md` : `@nom_carte` (ex. `@checks_combat.md`).
> Fonctionnalité de référencement par @ : voir `temp/reste_a_clarifier.md`.

### `defaults.focus` *(liste de strings, optionnel)*
Tags de focus ou texte libre orientant la tonalité et les thèmes de la rédaction.
- **Moteur :** merge avec `seq.focus` dans `ChapterOrchestrator.writeOneSequence()`
- **Agents :**
  - **Planner (`ScenarioPlannerContext`)** — injecté via `FocusLoader` dans le slot focus du prompt
  - **Writer (`WriterContext`)** — même injection
- **Sémantique :** peut contenir des tags nommés (définis dans `focus.md`) ET/OU du texte libre
- **Guillemets optionnels :** le parser YAML les supprime automatiquement — `- "WAGON"` et `- WAGON` sont identiques

### `defaults.lore` *(string multi-ligne, optionnel)*
Lore passif hérité par toutes les séquences du chapitre (merge additif avec `lore.md` de l'histoire).
Accepte la même syntaxe que `lore.md` : texte libre, `[TAG]`, `#PLAN`, `#WRITER`.
- **Guillemets optionnels :** `["TAG"]` et `[TAG]` sont identiques — les guillemets autour du nom de tag sont ignorés.
  De même pour le texte inline : `"mon texte"` et `mon texte` sont équivalents (les guillemets de début/fin sont nettoyés).
- **Moteur :** `LoreLoader.parse(chapter.lore)` puis `LoreLoader.merge(storyLore, chapterLore)` dans `ChapterOrchestrator.orchestrate()`
- **Agents :**
  - **Planner (`ScenarioPlannerContext`)** — reçoit la section `#PLAN` (lore story + chapitre fusionnés) sous "Informations de référence"
  - **SequencePlanner (`SequencePlannerContext`)** — idem pour chaque séquence
  - **Writer (`WriterContext`)** — reçoit la section `#WRITER` (lore story + chapitre + séquence fusionnés) sous "Informations de référence"
- **Sémantique :** contexte de fond (historique, géographique, technique, atmosphérique) — jamais imposé, jamais vérifié par les critiques. Voir `docs/lore.md`.

### `defaults.checks` *(PlanWriterList, optionnel)*
Vérifications applicables à toutes les séquences du chapitre.
Voir **Format PlanWriterList** ci-dessous pour la syntaxe acceptée.
- **Moteur :** merge additif avec les checks de chaque séquence et les checks globaux du scénario
- **Agents :**
  - **PlanCoherenceCritic** — reçoit les planChecks fusionnés (scénario + chapitre)
  - **SequenceChecker** — reçoit les writerChecks fusionnés (scénario + chapitre + séquence)
  - **TextCoherenceCritic** — reçoit les writerChecks fusionnés (scénario + chapitre)

### `defaults.constraints` *(PlanWriterList, optionnel)*
Contraintes applicables à toutes les séquences du chapitre.
Voir **Format PlanWriterList** ci-dessous pour la syntaxe acceptée.
- **Moteur :** merge additif avec les contraintes globales du scénario
- **Agents :**
  - **ChapterPlanner** — reçoit les planConstraints fusionnées (scénario + chapitre)
  - **Writer** — reçoit les writerConstraints fusionnées (scénario + chapitre + séquence)
  - **TextCoherenceCritic, TextWhatIfCritic, DeusInMachinaChecker** — reçoivent les writerConstraints fusionnées

---

## Bloc `sequences:`

Liste des séquences à générer, dans l'ordre. **Si vide ou absent : aucune rédaction.**

### `sequences[].type` *(string, obligatoire)*
Type de séquence.
- **Moteur uniquement** — non transmis aux agents
- Valeurs en scope : `explicit` — la séquence est entièrement décrite par `directive`

### `sequences[].directive` *(string)*
Les directives de l'auteur pour cette séquence. C'est l'entrée **prioritaire** du Writer.
- **Moteur :** `SequenceConfig.directive` — résolu dans `ChapterOrchestrator.writeOneSequence()`
- **Agents :**
  - **Planner (`ScenarioPlannerContext`)** — toutes les directives sont listées dans `Séquences à couvrir` pour construire le plan global ; le Planner produit un plan structuré avec une section `## Séquence N` par séquence
  - **Writer (`WriterContext`)** — injecté sous `### Directives détaillées (prioritaires)` comme instruction de l'auteur ; reçoit **uniquement la tranche du plan correspondant à sa séquence** (pas les séquences suivantes)
  - **SequenceChecker (`SequenceCheckerContext`)** — utilisé comme contexte de référence pour évaluer le texte produit

**Hiérarchie Writer :** la directive est prioritaire sur la trame narrative (plan LLM) si les deux
divergent. La trame donne l'ordre et le rythme des beats ; la directive impose les contraintes
précises de l'auteur — faits impératifs, détails sensoriels, interdits absolus.

Exemple de contrainte impérative dans une directive :
```yaml
directive: >
  Pierre sort du mess. Il est midi — le soleil est au zénith,
  l'air vibre au-dessus du tarmac. Pas un nuage.
  Il pose la main sur le fuselage et la retire aussitôt : le métal brûle.
```
Le Writer traitera "soleil au zénith" et "métal brûlant" comme des faits non négociables,
même si le plan LLM n'en a pas tenu compte.

### `sequences[].character_sheets` *(liste de strings, optionnel)*
Fiches personnage additionnelles pour cette séquence uniquement (merge avec defaults).
Voir `defaults.character_sheets` pour le détail des agents.

### `sequences[].focus` *(liste de strings, optionnel)*
Focus additionnels pour cette séquence uniquement (merge avec defaults).
Voir `defaults.focus` pour le détail des agents.

### `sequences[].lore` *(string multi-ligne, optionnel)*
Lore passif additionnel pour cette séquence uniquement (merge additif avec le lore story + chapitre).
- **Moteur :** `LoreLoader.parse(seq.lore)` puis `LoreLoader.merge(combinedLore, seqLore)` dans `ChapterOrchestrator.writeOneSequence()`
- **Agents :** mêmes agents que `defaults.lore` — le lore de séquence s'additionne au lore déjà accumulé
- **Sémantique :** pour injecter un contexte très spécifique à une scène (une pièce particulière,
  un objet technique, une heure précise) sans polluer tout le chapitre. Voir `docs/lore.md`.

### `sequences[].checks` *(PlanWriterList, optionnel)*
Vérifications additionnelles pour cette séquence (merge avec defaults.checks et les checks globaux).
Voir `defaults.checks` pour le détail des agents.
Les planChecks de séquence sont transmis au PlanCoherenceCritic **par séquence** (numérotés).

### `sequences[].constraints` *(PlanWriterList, optionnel)*
Contraintes additionnelles pour cette séquence (merge avec defaults.constraints et les contraintes globales).
Voir `defaults.constraints` pour le détail des agents.

### `sequences[].min_words` *(int, défaut : -1)*
Nombre minimum de mots pour cette séquence. Écrase `default_sequence_words` de `scenario.md`.
- **Moteur :** `seqMinWords = seq.minWords >= 0 ? seq.minWords : book.defaultSequenceWords` dans `ChapterOrchestrator`
- **Agents :**
  - **Writer (`WriterContext`)** — contrainte de longueur injectée dans le prompt

### `stitch` *(string, optionnel — disponible au niveau scénario, chapitre et séquence)*
Règle de couture d'ouverture injectée dans le system prompt du Writer pour contrôler comment
chaque séquence commence par rapport à la précédente.

**Cascade de priorité :** `sequences[].stitch` → `chapter.stitch` → `scenario.stitch` → règle par défaut du moteur.

- **Moteur :** `ChapterOrchestrator.writeOneSequence()` — résout la cascade et passe la valeur dans `SequencePackage.stitch`
- **Agents :**
  - **Writer (`WriterContext`)** — remplace la règle d'ouverture codée en dur dans le system prompt

La règle par défaut (quand aucun `stitch` n'est déclaré) :
> La première phrase de chaque séquence utilise un point d'entrée différent de la séquence
> précédente : action physique, fragment de dialogue, détail sensoriel, pensée intérieure,
> objet ou geste isolé. Ne commence ni par le prénom d'un personnage seul ni par un pronom
> sujet nu ('Il', 'Elle', 'Ils') — montre d'abord ce qui se passe, le personnage en est le
> sujet implicite. Évite de reposer l'atmosphère générale du lieu au début de chaque séquence :
> le lecteur connaît déjà le décor.

Utilisation typique : surcharger la règle pour un genre ou un registre particulier, ou imposer
une contrainte spécifique à un chapitre (ex. toujours ouvrir sur du dialogue dans un chapitre
de tension, ou autoriser une ouverture atmosphérique justifiée).

```yaml
# Au niveau scénario (scenario.md) — s'applique à tous les chapitres
stitch: >
  Chaque séquence peut ouvrir librement : dialogue, action, sensoriel ou pensée.
  Varie le point d'entrée entre séquences adjacentes.

# Au niveau chapitre — surcharge le scénario pour ce chapitre uniquement
stitch: >
  Ce chapitre est tendu : ouvre chaque séquence sur un geste, un son ou une sensation
  physique brève — jamais sur une description d'ambiance.

# Au niveau séquence — surcharge tout le reste pour cette séquence uniquement
sequences:
  - directive: "Le décollage de nuit."
    stitch: >
      Cette séquence ouvre sur une sensation physique dans le cockpit
      avant même que le moteur soit lancé.
```

---

### `sequences[].no_transition` *(bool, défaut : false)*
Signale une rupture narrative intentionnelle — ellipse temporelle, changement de lieu brutal,
saut de point de vue. Supprime la règle de continuité et la queue de la séquence précédente.
- **Moteur :** `ChapterOrchestrator` — `prevText = seq.noTransition ? "" : budget.lastSentences(...)`
- **Agents :**
  - **Writer (`WriterContext`)** — quand `prevSentences` est vide, la règle de continuité n'est
    pas injectée dans le prompt ; le Writer démarre librement sans contrainte d'enchaînement

Par défaut (sans `no_transition`), le Writer reçoit la fin de la séquence précédente et l'instruction
de démarrer dans le fil du texte, sans rupture visible.

```yaml
sequences:
  - type: explicit
    directive: "La scène du mess — dîner animé."

  # Ellipse : on saute directement au lendemain matin
  - type: explicit
    no_transition: true
    directive: "Le lendemain à l'aube. Pierre est déjà sur le tarmac."
```

---

## Format PlanWriterList

Les champs `checks:` et `constraints:` (au niveau defaults et séquence) acceptent trois formes :

```yaml
# 1. Texte simple → commun (plan ET writer)
checks: "Aucun dialogue dans cette séquence"

# 2. Liste plate → commun (plan ET writer)
checks:
  - "Aucun dialogue dans cette séquence"
  - "Pierre reste passif"

# 3. Structuré — toutes les clés sont optionnelles
checks:
  common:
    - "Pierre reste passif"           # plan ET writer
  plan:
    - "Le plan prévoit-il le silence de Pierre ?"   # critic plan uniquement
  writer:
    - "Aucun dialogue de Pierre dans le texte rédigé"  # critic writer uniquement
```

La même syntaxe s'applique à `constraints:`.

**Règle dualité checks/constraints :**
- Une `constraint` est une directive pour le générateur (Planner, Writer) : ce qu'il doit respecter.
- Un `check` est une question de vérification pour le critic : ce qu'il doit contrôler.
- Ils sont duaux : chaque contrainte devrait idéalement avoir son check miroir.

---

## Plan structuré et anti-spoil

Le Planner génère un plan avec une section `## Séquence N` par séquence :

```
## Séquence 1
Maya entre. Eddie assis côté fenêtre. Elle pose ses affaires sans bruit...

## Séquence 2
Silence installé. Maya sort un roman. Eddie regarde défiler les champs...
```

Le Writer reçoit **uniquement la section correspondant à sa séquence** — il ne voit pas les
séquences suivantes et ne peut pas les anticiper dans sa prose.

**Fallback :** si le LLM ne respecte pas le format (nombre de sections incorrect), le moteur
utilise le plan complet comme avant et enregistre un avertissement dans `warnings.md` du
répertoire `generated/<run>/`.

---

## Fichier `warnings.md`

Créé automatiquement dans `generated/<run>/` quand un événement anormal survient pendant la
génération. Actuellement alimenté par :

- **Plan non structuré** — le LLM n'a pas produit le bon nombre de sections `## Séquence N`

Consulter ce fichier après chaque génération pour détecter les dégradations silencieuses.

---

## Exemple complet

```yaml
title: "Jour 1 — Arrivée"
type: imperative

description: "Pierre arrive à la base. L'escadrille est en alerte maximale."

goal: >
  Installer Pierre comme étranger dans un groupe soudé.
  Son isolement doit être senti, pas expliqué.

defaults:
  character_sheets: ["pierre_moreau", "commandant_bertrand"]
  focus: ["CIEL", "CAMARADERIE"]
  checks:
    - "Pierre reste observateur — il ne prend pas d'initiative"

sequences:

  - type: explicit
    directive: "Pierre arrive sur le tarmac à l'aube. Il pose son sac. Il regarde."
    checks:
      - "Aucun dialogue dans cette séquence"

  - type: explicit
    directive: "Bertrand reçoit Pierre. Deux minutes. Une phrase. Pierre repart."
    character_sheets: ["commandant_bertrand"]
    focus: ["PEUR"]
    min_words: 400
```
