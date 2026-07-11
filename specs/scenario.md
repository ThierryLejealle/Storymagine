# Fichier Scénario (scenario.md)

Fichier YAML à la racine du scénario.

- title: le titre de l'histoire
- language: la langue de rédaction. Ex: fr
- default_sequence_words: 300 — nombre de mots par défaut par séquence

# Structure du répertoire scénario

```
scenario.md          — config globale
goal.md              — objectif global de l'histoire
quality.md           — critères de qualité globaux (writer + correcteurs de style)
style.md             — style de rédaction (writer + correcteurs de style)
example.md           — exemple de rédaction pour inspirer le Writer
keep_phrases.md      — motifs récurrents autorisés (writer uniquement)
requirements.md       — règles globales (plan + rédaction) : contrainte pour le Planner/Writer, check pour les Critics

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
Injecté dans le prompt de StyleCorrector (guide de correction de style).

### STYLE : style.md
Style de rédaction. Injecté dans les prompts Writer et StyleCorrector.
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

### REQUIREMENTS : requirements.md
Une règle narrative unique, exprimée pour ses deux audiences possibles :
- la **contrainte**, transmise au Planner et au Writer (ce qu'il faut faire/respecter) ;
- le **check**, transmis aux Critics (ce qu'il faut vérifier dans le résultat).

Format de chaque ligne — voir « Syntaxe Requirement (contrainte \| check) » plus bas.
Sous `## PLAN` et/ou `## RÉDACTION`.
`## COMMON` (optionnel) : les éléments sous cette section s'appliquent aux deux phases.
Les éléments avant toute section sont aussi globaux aux deux phases.
Dans les chapitres YAML, le champ `requirements:` des séquences s'ajoute aux requirements globaux.

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

## requirements.md

Ce fichier utilise des sections h2 (`##`) :
- `## PLAN` : éléments pour la phase de planification
- `## RÉDACTION` : éléments pour la phase de rédaction
- `## COMMON` (optionnel) : éléments communs aux deux phases
- Éléments avant toute section : aussi globaux aux deux phases

## Syntaxe Requirement (contrainte | check)

Chaque ligne représente une règle, avec deux formulations séparées par `|` :

```
texte                    -> même texte utilisé comme contrainte ET comme check
contrainte | check       -> formulation différente pour chaque audience
contrainte |             -> contrainte seule (ignorée par les Critics)
| check                  -> check seul (ignoré par le Planner/Writer)
```

Le split se fait sur le premier `|` rencontré ; les espaces autour sont ignorés.
Convention de formulation : la contrainte est impérative ("ne pas...", "assure-toi que...
implicite dans le prompt"), le check est un fait déclaratif nu vérifiable ("il n'y a pas de
contact entre les personnages") — sans "Vérifie que" ni tournure interrogative, ce préfixe
est ajouté par le prompt de l'agent, pas par la donnée.

## Champ YAML requirements (chapitres et séquences)

Le champ `requirements:` dans les YAML de chapitre accepte trois formes :

```yaml
# 1. Texte simple → commun (plan ET writer)
requirements: "Pas de contact entre les personnages | Il n'y a pas de contact entre les personnages"

# 2. Liste plate → commun (plan ET writer)
requirements:
  - "Pas de contact entre les personnages | Il n'y a pas de contact entre les personnages"
  - "| La scène reste dans le compartiment"

# 3. Structuré → split explicite (les clés sont toutes optionnelles)
requirements:
  common:
    - "Pas de contact entre les personnages"
  plan:
    - "Le plan prévoit-il l'absence de contact ?"
  writer:
    - "Aucun contact, aucune discussion dans le texte rédigé"
```

Les fichiers goal, quality, style, example, keep_phrases n'ont pas de structure PLAN/WRITER :
ils sont soit globaux (goal, quality, style), soit writer-only (keep_phrases, example).

# Notation de référence dans les specs

Pour référencer le contenu combiné d'un fichier : NOM.GLOBAL+PLAN = somme de la partie
commune et de la partie spécifique au Plan.

# Fichiers Chapitre
Dans le répertoire `chapitres/`. Voir la spec détaillée chapitre.md
