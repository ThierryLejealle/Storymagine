# 2026-06-22 11h00 — Modèle Story dans le domaine

## Evolution demandée
Créer un modèle domaine représentant l'histoire en cours d'écriture (`Story`), distinct du `Scenario` qui est la spec.
Package cible : `storymagine.redacteur.coeur.domaine.story`

## Ce qui a été touché

### Nouveau package `/domaine/story/` — 5 classes

**`ChapterId`** (record)
- Identifiant métier d'un chapitre ; valeur non nulle/vide garantie à la construction.

**`RepetitionMemory`**
- Blacklist anti-répétition en fenêtre glissante : phrases interdites + thèmes interdits.
- Taille de fenêtre calibrée sur le `contextWindow` du modèle (`calibrate(int)`).
- Basée sur la logique `StoryMemory.forbiddenPhrases/forbiddenThemes` du Redacteur.

**`WorldState`**
- État courant du monde : `entityStates` (par entité), `recentEvents` (4 derniers), `plotDirectives` (faits permanents d'auteur).
- `applyPlotDirectives()` : syntaxe `Entité → état` / `-Entité` (suppression).
- `snapshot()` / `restore()` : isolation pour les chapitres `dream` / `what_if`.
- Basée sur `StoryMemory.entityStates/recentEvents/plotDirectives` du Redacteur.

**`WrittenChapter`**
- Contenu produit d'un chapitre : `plan` (ChapterPlanner), `sequences` (textes écrits), `summary` (StoryCompressor).
- Mutable, construit de façon incrémentale.
- `fullText()` : concatenation des séquences séparées par une ligne vide.
- Basée sur `ChapterResult` du Redacteur + attributs manquants (`plan`).

**`Story`** — agrégat principal
- `startChapter(ChapterId)` : ouvre un nouveau chapitre, le retourne.
- `currentChapter()` : dernier chapitre ouvert (`Optional`).
- `storySoFar()` : résumés des chapitres complétés, prêts pour injection LLM.
- Compose `WorldState` + `RepetitionMemory`.

## Décisions techniques

- Loop journal (itérations de boucle) : laissé de côté, sera ajouté lors de la migration de l'orchestration.
- `plan` dans `WrittenChapter` : conservé (utile pour debug et relecture).
- Snapshot/restore uniquement sur `WorldState` (le cas isolation dream/what_if).
- Pure Java, aucune dépendance framework.

## Résultat
5 classes créées dans `/domaine/story/`. Non reliées à l'orchestration.
