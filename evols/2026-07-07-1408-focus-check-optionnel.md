# 2026-07-07 14h08 - Focus check optionnel (extension du mécanisme Requirement)

## Évolution demandée

Suite à la fusion check/contrainte en `Requirement` (fiche précédente), question posée : un
focus n'est-il pas une forme de contrainte ? Réponse retenue après discussion : non — le
focus est un générateur-side "soft" (une invitation, "efforce-toi d'utiliser") sans équivalent
dur, mais il partage la même absence structurelle qu'avait `Requirement` avant sa fusion :
aucun côté vérification. Contrairement au lore (purement contextuel, jamais attendu dans le
texte), le focus a une intention de présence — un check a du sens pour lui, mais optionnel et
sans split plan/writer (contrairement aux contraintes dures).

Décision : ajouter un check optionnel au focus, en réutilisant le mécanisme bas niveau de
`Requirement` (syntaxe `|`) sans dupliquer sa logique de défaut ("pas de `|` = même texte pour
les deux"), qui ne convenait pas ici — pour un focus, l'absence de `|` doit signifier "pas de
check du tout", pas "check = même texte que le contenu".

## Ce qui a été touché

### `Requirement.java`
Extraction de la logique de split en `public static String[] splitOnPipe(String raw)`
(retourne `{avant, null}` si pas de `|` — laisse à l'appelant sa propre règle de défaut).
`Requirement.parse()` l'utilise et applique sa règle ("pas de `|` → même valeur des deux
côtés"). Mutualise le découpage sans imposer la sémantique de defaulting aux autres appelants.

### `FocusInline.java` (nouveau)
`record FocusInline(String text, String check)` + `FocusInline.parse(String raw)`, qui utilise
`Requirement.splitOnPipe()` mais applique sa propre règle : pas de `|` → `check = null`
(absent), jamais dupliqué depuis `text`.

### `TagElementParser.java` (mutualisé pour Focus/Lore/Personnage)
Ajout d'une 4e section `# CHECK` (à côté de `# COMMON`/`# PLAN`/`# WRITER`), reconnue dans
`parse()` et `parseSingleBlock()`. `TagBlock` gagne un champ `checkContent`, optionnel et
ignoré par les appelants qui n'en ont pas besoin (Lore, Personnage) — la boucle de parsing
(sections, marqueurs `[TAG]`, filtrage des commentaires HTML) reste unique pour les trois.

### `FocusElement.java`
Nouveau champ `checkContent` (5e paramètre du constructeur, en plus des 4 hérités de
`TagElement`) — `TagElement` lui-même n'est **pas** modifié pour ne pas imposer ce concept à
`LoreElement`/`Personnage` qui le partagent.

### `ScenarioFileAdapter.java`
- `loadFocus()` : passe désormais `b.checkContent()` au constructeur `FocusElement`.
- `resolveFocusItems()` : `new FocusInline(...)` → `FocusInline.parse(...)`.

### `ScenarioFormatters.java`
Nouvelle méthode `focusChecks(List<FocusItem>)` — symétrique de `focusText()` mais lit le côté
check (`FocusRef.resolved().checkContent()` ou `FocusInline.check()`), sans split plan/writer
(un seul check par item, conformément à la décision "check optionnel, pas de granularité par
phase"). **Non câblée dans un prompt d'agent** dans cette passe — reste une donnée exposée,
prête à être consommée par un futur `TextCoherenceCritic`/`PlanCoherenceCritic` (câblage
prompt = modification LLM, nécessite sa propre validation delta séparée).

### Exemples ajoutés dans `scenarios/as-du-ciel/` (scénario live, pas la fixture de test)
- `focus.md` : section `# CHECK` ajoutée aux tags `[CIEL]` et `[PEUR]`.
- `chapitres/chap_6.yaml` : exemple de focus inline avec `|` sur la dernière séquence
  ("La lettre de Pierre à Lucie... | La lettre à Lucie est-elle un objet physique... ?").

## Résultat

- BOM : vérifié absent sur les 8 fichiers touchés par cette extension.
- Compilation : `mvn compile` — OK, 0 erreur.
- Tests : `ScenarioLoadTest` — même échec préexistant qu'avant cette passe
  (`load_sequenceFocusRefsAreResolved`, sans rapport), aucune régression nouvelle.

## Points laissés en suspens

- `focusChecks()` n'est consommé par aucun agent pour l'instant — câblage dans
  `TextCoherenceCritic`/`PlanCoherenceCritic` à valider séparément (modification de prompt).
- Le côté check du focus n'a pas été ajouté à la fixture de test
  (`redacteur/src/test/resources/scenarios/as-du-ciel/`), volontairement laissée stable.
