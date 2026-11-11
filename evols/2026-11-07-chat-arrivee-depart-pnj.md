# 2026-11-07 - Chat : arrivée et départ de personnages (`npc-only`, `npc-arrives`, `npc-leaves`)

## Évolution demandée
Pouvoir faire arriver un personnage plus tard dans l'histoire (un PNJ majeur qui n'est pas là tout de suite, ou un
figurant : un marchand, deux gobelins hostiles), et le faire repartir. Différence avec `{npc-absent}` : le personnage
n'apparaît vraiment (vignette, fiche chez les autres, répliques) que lorsqu'on l'ajoute. Un figurant se crée à partir
d'une fiche modèle et d'un nom : `{npc-arrives gobelin Grik, Snak}`.

Décisions de l'auteur :
- Rétro-compatibilité : sans `{npc-only}`, tous les personnages sont là au départ. Avec `{npc-only noms}`, placée au
  plus tard dans la première scène (avant le premier `[NEXT ACT]`), seuls les personnages nommés le sont.
- L'historique n'est jamais effacé (les autres se souviennent d'un personnage parti) ; un arrivant connaît tout
  l'historique : limite documentée, pas de mémoire limitée à la présence. « Revenir » ne remet rien à zéro.
- Le joueur ne peut ni ajouter ni retirer de personnage : seul le scénario décide.

## Ce qui a été touché
- Domaine (`activity`) : `NpcActivity.OUTSIDE` (hors du cycle du clic), `ActivityDirective` `ARRIVES`/`LEAVES`,
  `ActivityDirectiveMarker` (deux lignes de commande), `ActivityResolver` (arrivées, départs, commandes larges qui
  ignorent les absents de l'histoire, erreur si un niveau est demandé avant l'arrivée), `CastActivity` (point de
  départ avec roster, `outsideIds`), `ArrivalSpec` (nouveau : `modèle Nom1, Nom2`).
- Domaine (`scenario`) : `StartingRoster` et `NpcOnlyMarker` (nouveaux), `CastInstantiator` (nouveau, pur : modèles et
  instances), `Cast` (ids des modèles), `ChatScenario` (composant `startingRoster`, validations, `{npc-starts}` sur un
  absent de l'histoire).
- Domaine (`session`) : `ChatSession` (un niveau forcé ne ramène jamais un hors-histoire, clic sans effet,
  `outsideNpcIds`). `ChatServiceImpl` : les hors-histoire ne sont pas des coéquipiers absents dans `Scene`.
- Validation : `ScenarioValidator` (les trois commandes, avertissement « n'apparaît jamais »).
- Infra : `ScenarioOutlineParser` (placement de `{npc-only}`), `ChatFileStorageAdapter` (modèles et instances lus avant
  les tirages).
- UI : `ChatWebServer.castView` ne montre pas les personnages hors de l'histoire.
- Documentation : `specs/chat.md`, guide de rédaction (§2.0, nouveau §2.13, §7.4), raccourci `SKILL.md`, démo
  `chatscenarios/demos/demo-arrivals`.
- Tests : `NpcArrivalsTest`, `NpcOutsideSessionTest`, ajouts à `ScenarioOutlineParserTest` et
  `ChatFileStorageAdapterTest`. Aucun prompt d'agent modifié.

## Résultat
617 tests verts depuis la racine du projet. La démo `demo-arrivals` est chargée par `DemoScenariosTest` ; elle a
révélé que `resolveNames` refusait les virgules (`{npc-leaves Grik, Snak}`), corrigé et testé.
**Non fait / à savoir** :
- Pas de `/lib/` pour les modèles (le modèle est un fichier de `characters/`).
- `{cast NOM}` sans liste peut tirer un figurant : donner une liste.
- Pas de portrait propre : la vignette n'existe que lorsque le personnage est dans l'histoire.
- **Non vérifié à la main** dans le navigateur : l'apparition et la disparition des vignettes d'un acte à l'autre.
