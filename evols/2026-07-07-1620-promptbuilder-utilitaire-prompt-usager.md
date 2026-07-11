# 2026-07-07 16h20 - PromptBuilder (utilitaire de construction du prompt Usager)

## Évolution demandée

Demande initiale : un utilitaire "PromptUser" que chaque agent pourrait utiliser pour construire
son prompt Usager, capable de piocher dans le scénario et la story (sections conditionnelles,
requirements par niveau, consignes de séquence, etc.).

Avant d'écrire du code, analyse du besoin réel (agent Explore) : confirmation qu'un pattern
"section optionnelle titrée" est ré-implémenté 4 fois différemment dans le module `agent`
(`Writer.appendSection`, `ChapterPlanner.appendSection` avec un paramètre `budget` mort,
`GoalTextCritic`/`GoalPlanChecker.section` qui renvoie une `String`, et des ternaires inline dans
4 autres agents), plus `trunc()` dupliqué à l'identique dans 20 fichiers. En revanche
l'extraction de données Scenario/Story vers du texte est déjà bien centralisée dans
`ScenarioFormatters` — pas de duplication à ce niveau.

Discussion avec l'utilisateur → décisions actées avant implémentation :
1. `PromptBuilder` reste **agnostique du domaine** (aucun accès à `Scenario`/`Story`/`Chapter`/
   `Sequence`) — un pur "StringBuilder de sections titrées". L'agent continue d'extraire son
   contenu via `ScenarioFormatters` puis le passe en `String` à `PromptBuilder`.
2. `trunc()` (troncature) reste **hors périmètre** de cette passe — traité séparément si besoin.
3. Les phrases d'instruction dupliquées avec de légères variantes ("Assure-toi que..." vs
   "Vérifie que...") sont **préservées telles quelles par agent** — aucune harmonisation de texte
   de prompt dans cette passe (aurait nécessité la procédure de validation delta/raison/objectif).
4. Migration de tous les agents concernés dans la foulée (portée validée explicitement).

## Ce qui a été touché

### `commun/coeur/domaine/prompt/PromptBuilder.java` (nouveau)
Classe agnostique du projet, dans le module `commun` (réutilisable par tout module fonctionnel).
API : `section(title, content)` (rien si `content` est blank), `sectionOrElse(title, content,
fallback)` (texte de repli si blank), `raw(text)` (texte libre sans titre), `build()`. Le
séparateur `"\n\n"` n'est inséré qu'entre deux blocs non vides (pas de ligne(s) vide(s) en tête
de prompt si le premier bloc est vide) — seule différence de sortie par rapport aux implémentations
maison remplacées, purement cosmétique (espaces), sans impact sur le contenu envoyé au LLM.

### `commun/pom.xml`
Ajout de la dépendance test `junit-jupiter` (absente jusqu'ici, le module n'avait pas de tests).

### `commun/src/test/java/.../prompt/PromptBuilderTest.java` (nouveau)
7 tests : sections vides ignorées, jointure par ligne vide, pas de trou quand une section est
sautée, `sectionOrElse` (repli et contenu présent), `raw` (ajout et no-op si vide).

### Agents migrés (buildUser uniquement — le prompt Système n'est pas touché)
`Writer`, `ChapterPlanner`, `PlanCoherenceCritic`, `PlanNarrativeCritic`, `TextCoherenceCritic`,
`TextWhatIfCritic`, `GoalTextCritic`, `GoalPlanChecker`, `CheckCritic`, `PlanFidelityCritic`,
`DeusInMachinaCorrector`, `DeusInMachinaCritic`, `NaturalityCorrector`, `StyleCorrector`,
`ProofreaderCorrector`. Suppression des méthodes `appendSection`/`section` maison devenues
inutiles (dont le paramètre `budget` mort de `ChapterPlanner.appendSection`). Aucun changement
de texte de prompt : les phrases d'instruction, titres de section et contenus sont strictement
repris tels quels, seule la plomberie d'assemblage change.

## Résultat

- Compilation (`mvn -pl commun,redacteur -am test`) : OK, 0 erreur.
- `PromptBuilderTest` : 7/7 passent.
- 3 échecs préexistants sans rapport (confirmés par `git status` : chantier en cours de fusion
  Check/Constraint → Requirement déjà présent dans l'arbre avant cette session, notamment
  `ScenarioFileAdapter`, `FocusInline`, `Scenario.java`) :
  - `WorkflowLogTest.writeWorkflow_draft_logsAllWriterSteps`
  - `WorkflowLogTest.planOnly_doesNotLogWriteOrEval`
  - `ScenarioLoadTest.load_sequenceFocusRefsAreResolved` (déjà signalé comme préexistant dans
    la fiche `2026-07-07-1408-focus-check-optionnel.md`)
  `MockModelCallPort` ne matche que sur le prompt Système (jamais modifié ici), ce qui exclut
  tout lien avec cette évolution.

## Points laissés en suspens

- `trunc()` (dupliqué dans ~20 fichiers) : dédoublonnage non traité, à décider séparément.
- Harmonisation des phrases d'instruction requirements ("Assure-toi..." / "Vérifie...") : non
  faite, nécessiterait une validation delta/raison/objectif dédiée si souhaitée un jour.
- `DeusInMachinaCorrector`/`DeusInMachinaCritic` utilisent `PromptBuilder.raw()` (pas `section()`)
  car leur bloc "Points de vérification actifs" ne suit pas la convention `### Titre`.
