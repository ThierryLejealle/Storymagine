# 2026-06-23 22h18 — Focus/lore spécifiques à une séquence dans le texte de la directive

## Évolution demandée

Les éléments focus, lore et contraintes propres à une séquence individuelle
doivent apparaître dans le texte de la directive que voit le LLM (ChapterPlanner, Writer).
Le mécanisme existant dans `sequenceDescriptions()` (tags `[Focus:]`/`[Lore:]`) réalise déjà cela ;
il est conservé et rétabli après une erreur de suppression.

## Ce qui a été touché

### YAML scénario
- `scenarios/une-rencontre/chapitres/chap_1.yaml` : suppression des champs `focus`, `lore`,
  `constraint` de la séquence 1 ; leur contenu (wagon, gare de province, contrainte de non-contact)
  est intégré directement en texte libre dans la directive. Approche manuelle équivalente au mécanisme
  automatique, choisie pour ce scénario où le contenu est simple et non réutilisé.

### Domaine
- `SequenceOverrides.java` : commentaire javadoc corrigé — "additions" au lieu de "overrides"
  pour refléter la sémantique additive (les éléments séquence s'ajoutent aux defaults chapitre).

## Résultat

- La compilation est propre (zéro erreur).
- `as-du-ciel`, `1998`, `modele` : leurs `focus`/`lore` par séquence sont résolus et embarqués
  dans le texte de la directive via `[Focus:]`/`[Lore:]`, comme attendu.
- `une-rencontre` seq 1 : contenu embarqué manuellement en texte libre dans la directive.
