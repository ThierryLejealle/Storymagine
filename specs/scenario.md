# Fichier Scénario (scenario.md)

Fichier YAML à la racine du scénario.

- title: le titre de l'histoire
- language: la langue de rédaction. Ex: fr
- default_sequence_words: 300 — nombre de mots par défaut par séquence

# Structure du répertoire scénario

```
scenario.md          — config globale
goal.md              — objectif global de l'histoire
quality.md           — critères de qualité globaux
style.md             — style de rédaction (writer uniquement)
example.md           — exemple de rédaction pour inspirer le Writer
keep_phrases.md      — motifs récurrents autorisés (writer uniquement)
constraints.md       — contraintes globales (plan + rédaction)
checks.md            — vérifications globales (plan + rédaction)

lore.md              — base de connaissances par TAG
focus.md             — points de focalisation narrative par TAG

characters/          — une fiche par personnage
chapitres/           — un fichier YAML par chapitre
```

# Catégories de fichiers

## Fichiers globaux (applicables à toute l'histoire)

Ces fichiers s'appliquent à l'ensemble du scénario. Ils ne sont pas déclarés dans les chapitres.

### GOAL : goal.md
Objectif global du livre. Décrit le genre, le protagoniste, l'arc narratif, le ton, la longueur cible.
Texte libre. Injecté dans les prompts Planner et Critic comme contexte de référence.

### QUALITY : quality.md
Critères de qualité à vérifier dans le texte généré.
Format : liste de tirets, un critère vérifiable par ligne.
Injecté dans les prompts Critic.

### STYLE : style.md
Style de rédaction. Usage : rédaction uniquement (Writer).
Format : texte libre ou liste de tirets.

### EXAMPLE : example.md
Exemple de rédaction pour calibrer le ton et le style du Writer.
Texte libre. Pas de structure COMMUN/PLAN/WRITER.

### KEEP_PHRASES : keep_phrases.md
Motifs récurrents intentionnels (leitmotivs) que le correcteur de répétitions ne doit pas signaler.
Format : liste de tirets, un motif par ligne.
Usage : rédaction uniquement (RepetitionFilter).

## Fichiers à accumulation (globaux + chapitre + séquence)

Ces fichiers définissent un niveau de base global. Les chapitres et séquences peuvent ajouter
leurs propres éléments — qui s'accumulent avec le global, sans l'écraser.

Ordre d'accumulation : global (fichier .md) → defaults du chapitre → séquence

### CONSTRAINTS : constraints.md
Contraintes à respecter pendant le plan et la rédaction.
Format : liste de tirets sous `## PLAN` et/ou `## RÉDACTION`.
`## COMMON` (optionnel) : les éléments sous cette section s'appliquent aux deux phases.
Les éléments avant toute section sont aussi globaux aux deux phases.

### CHECKS : checks.md
Éléments à vérifier à chaque séquence.
Format : liste de tirets sous `## PLAN` et/ou `## RÉDACTION`.
`## COMMON` (optionnel) : les éléments sous cette section s'appliquent aux deux phases.
Les éléments avant toute section sont aussi globaux aux deux phases.
Dans les chapitres YAML, le champ `checks:` des séquences s'ajoute aux checks globaux.

## Fichiers à TAGS (pools de contenu référencés par les chapitres)

Ces fichiers contiennent des éléments nommés par un TAG. Les chapitres et séquences
les référencent par leur TAG dans les champs `focus:` et `lore:`.

### LORE : lore.md
Base de connaissances narrative. Chaque entrée est un TAG avec du contenu descriptif.
Exemple : un TAG par type d'avion dans une histoire d'aviation.
Structure : `[TAG]` / `# PLAN` / `# WRITER`

### FOCUS : focus.md
Points de focalisation narrative. Chaque entrée oriente l'attention du LLM sur un aspect.
Exemple : le bruit des mitrailleuses dans un combat aérien.
Structure : `[TAG]` / `# PLAN` / `# WRITER`

## Fichiers personnage

### PERSONNAGES : characters/{id}.md
Une fiche par personnage dans le répertoire `characters/`.
Référencés par leur id dans les champs `character_sheets:` des chapitres/séquences.
Structure : contenu global / `# PLAN` / `# WRITER`

Les sous-sections `##` à l'intérieur de `# PLAN` ou `# WRITER` sont des aides au LLM
(ex: `## PRIVE`, `## INCONSCIENT`) — leur titre est transmis avec leur contenu.

# Structure COMMUN/PLAN/WRITER

## Fichiers .md à sections (lore.md, focus.md, fiches personnage)

Chaque élément est structuré en trois parties :
- Contenu avant `# PLAN` : commun aux deux phases
- `# COMMON` (optionnel, utilisable à n'importe quel endroit) : commun aux deux phases
- `# PLAN` : spécifique à la planification (optionnel)
- `# WRITER` : spécifique à la rédaction (optionnel)

`# COMMON` est optionnel et peut apparaître avant ou après `# PLAN` / `# WRITER`.
Il ne perturbe pas le parsing des autres sections.

## checks.md et constraints.md

Ces fichiers utilisent des sections h2 (`##`) :
- `## PLAN` : éléments pour la phase de planification
- `## RÉDACTION` : éléments pour la phase de rédaction
- `## COMMON` (optionnel) : éléments communs aux deux phases
- Éléments avant toute section : aussi globaux aux deux phases

## Champs YAML checks et constraints (chapitres et séquences)

Les champs `checks:` et `constraints:` dans les YAML de chapitre acceptent trois formes :

```yaml
# 1. Texte simple → commun (plan ET writer)
checks: "Vérifie qu'il n'y a pas de contact"

# 2. Liste plate → commun (plan ET writer)
checks:
  - "Vérifie qu'il n'y a pas de contact"
  - "Vérifie que la scène reste dans le compartiment"

# 3. Structuré → split explicite (les clés sont toutes optionnelles)
checks:
  common:
    - "Pas de contact entre les personnages"
  plan:
    - "Le plan prévoit-il l'absence de contact ?"
  writer:
    - "Aucun contact, aucune discussion dans le texte rédigé"
```

La même syntaxe s'applique à `constraints:`.

Les fichiers goal, quality, style, example, keep_phrases n'ont pas de structure PLAN/WRITER :
ils sont soit globaux, soit writer-only (style, keep_phrases, example).

# Notation de référence dans les specs

Pour référencer le contenu combiné d'un fichier : NOM.GLOBAL+PLAN = somme de la partie
commune et de la partie spécifique au Plan.

# Fichiers Chapitre
Dans le répertoire `chapitres/`. Voir la spec détaillée chapitre.md
