# 2026-11-03 - Bibliothèques communes à plusieurs scénarios : `/lib/nom`

## Évolution demandée
Pouvoir mutualiser des fichiers de tirage entre scénarios : au lieu de `{pick random evenements}` (fichier du
`picks/` du scénario), écrire `{pick random /lib/evenements}` pour remonter sur une bibliothèque commune.
Décisions prises avec l'auteur :
- `/lib/` est le dossier `lib/` **dans la racine de scénarios choisie** (à côté des dossiers de scénarios), donc
  `chatscenarios/demos/lib/` pour les démos.
- Sujet indépendant des variables de casting (fiche précédente). Variables dans les paragraphes d'une bibliothèque :
  pas un objectif en soi ; elles marchent pourtant déjà, par construction (les casts s'exécutent après les `pick`).

## Ce qui a été touché
- `PickTag` : accepte `nom` ou `/lib/chemin` (lettres, chiffres, `_`, `-` par partie : jamais de `.` ni de `..`),
  avec `isShared()`, `sharedPath()` et `file()` (« picks/x.txt » ou « lib/x.txt », pour les messages).
  `PickResolver` et `RepeatExpander` utilisent `file()` dans leurs messages (textes inchangés pour `picks/`).
- `PickFileLibrary` : lit `lib/{chemin}.txt` à côté des dossiers de scénarios pour un tag partagé ; message
  d'absence qui explique où chercher.
- Rien d'autre : `unique` (clé = le tag complet), tirages 🎲, `picks.txt`, figeage dans `resolved/` et validation
  marchent sans changement. `lib/` n'est pas listé comme scénario (pas de `scenario.txt`).
- Démo : `demo-lib` + `chatscenarios/demos/lib/evenements.txt`.
- Tests : `PickTagTest` (chemins refusés), `SharedLibraryTest` (lecture, deux scénarios, sous-dossier, `repeat`,
  bibliothèque absente, `unique`, tirage figé malgré une modification, nom dans le tableau 🎲),
  `ScenarioValidatorTest` (nom pouvant sortir de `lib/`), `DemoScenariosTest` (14 démos).
- Documentation : guide §2.6 (bloc « Bibliothèque commune ») et §2.9 ; skill `chat-scenario-writing` ;
  `specs/chat.md` ; `specs/todo.md`.
- Aucun prompt d'agent modifié.

## Résultat
Tests verts. Un `{pick random /lib/evenements}` marche dans tous les scénarios d'une même racine. Un scénario qui
utilise `/lib/` n'est plus autonome : le copier à la main demande aussi `lib/` ; sans lui le chargement échoue en
nommant `lib/x.txt`. Les sauvegardes et archives de partie restent jouables sans `lib/` (elles emportent le
scénario déjà tiré).
**Non vérifié à la main** : `demo-lib` dans l'interface, et le tableau 🎲 avec une ligne `/lib/…`.
