# 2026-07-11 23h30 — Refonte des critics de plan de chapitre : 3 mandats flous → 5 axes orthogonaux

## 1. Demande

Chantier "Critics review (queued)" repris en profondeur : les 3 critics de plan de chapitre
(`PlanCoherenceCritic`, `PlanNarrativeCritic`, `PlanGoalCritic`) avaient des mandats qui se
chevauchaient (incident réel : `PlanCoherenceCritic` a fait un jugement relevant en fait de
`PlanNarrativeCritic`), et aucun ne vérifiait littéralement, élément par élément, que la
consigne de l'auteur était respectée par les beats du plan.

Conception menée en conversation avec l'utilisateur + 2 consultations Fable conceptuelles
(sans lecture de code, coût 26 476 puis 37 700 tokens) aboutissant à **5 agents sur 5 axes
orthogonaux** :
- **A — PlanGoalCritic** (élargi) : fidélité littérale à la consigne (but, description,
  directive de CHAQUE séquence). Seul agent sans clause de primauté — comparer à la consigne
  EST son objet.
- **B — PlanFactsCritic** (nouveau) : respect des faits déjà établis (fiches perso, checks,
  état des entités), avec clause de primauté (un changement voulu par la consigne n'est jamais
  un défaut) — évite de reproduire le faux positif déjà vécu (opération médicale signalée à
  tort comme incohérence).
- **C — PlanCoherenceCritic** (recentré) : cohérence purement interne du plan (contradictions/
  ruptures entre ses propres séquences), avec clause de primauté sur les directives de séquence.
  Nom enfin fidèle au mandat (l'ancien faisait bien plus : fiches, checks, continuité factuelle).
- **D — PlanContinuityCritic** (nouveau) : continuité narrative avec l'historique du livre
  (résumé des chapitres précédents) — recentré sur le narratif (pas factuel, ça c'est B) sur
  conseil de Fable, qui avait signalé un risque de chevauchement B/D sinon. Rien ne vérifiait
  ce point avant le niveau de qualité FULL.
- **E — PlanDramaCritic** (nouveau, suggéré par Fable) : effort dramaturgique — le plan
  s'efforce-t-il d'être vivant, à l'intensité que LA CONSIGNE appelle (jamais un niveau absolu).

`PlanNarrativeCritic` (rythme/transitions, aucun ancrage factuel, jugement le plus flou) est
retiré — son utilité réelle est absorbée par C (rupture = incohérence) et E (plat = manque
d'effort).

Décision utilisateur additionnelle : paramétrer la langue de sortie dynamiquement pour ces 5
agents seulement (`ScenarioConfig.language()` existait déjà mais n'était utilisé nulle part),
plutôt que le "En francais." en dur des ~20 autres agents — généralisation explicitement
reportée (TODO).

## 2. Ce qui a été touché

### Nouveau — `commun/.../text/LanguageNames.java`
Code ISO → nom anglais lisible (`"fr"` → `"French"`), défaut `"French"`.

### `orchestrator/common/ScenarioFormatters.java`
Nouvelle méthode `sequenceDirectivesBlock(List<Sequence>)` — directives brutes par séquence,
numérotées ("Sequence 1: ...", "Sequence 2: ..."), pour les critics vérifiant la fidélité
beat-par-beat (jamais câblé à un critic auparavant).

### Agent A — `agent/plan/goalcritic/` (réécrit)
Input : `+sequenceDirectives`, `+language`. Format de sortie passé de `PROBLEME:`/
`ProblemScoreParser` (unique parmi les critics de plan) à `AMELIORATION/DEFAUT_SIGNIFICATIF/
DEFAUT_MAJEUR`/`CriticOutputParser` — harmonisé avec les autres.

### Agent B — `agent/plan/factscritic/` (nouveau)
`PlanFactsCritic`/`Input`/`Output`/`.md`. Reprend le volet "faits" de l'ancien
`PlanCoherenceCritic` (fiches, checks, entityState), plan enrichi via
`ScenarioFormatters.enrichPlanJson` (comme avant), + clause de primauté sur la consigne.

### Agent C — `agent/plan/coherencecritic/` (réécrit, recentré)
Input réduit à `plan` + `sequenceDirectives` (primauté) + `language` — retrait de
`description`/`chapterGoal`/`checks`/`characters`/`entityState`.

### Agent D — `agent/plan/continuitycritic/` (nouveau)
`PlanContinuityCritic`/`Input`/`Output`/`.md`. Entrée : consigne (primauté) + `Story.summary()`
+ plan. **Court-circuite l'appel LLM** si `summary` est vide (premier chapitre du livre) —
renvoie directement un résultat vide/parfait, rien à comparer. Le `writingExample` du scénario
n'est jamais traité comme un "chapitre 0" (documenté comme pur exemple de style, sans lien avec
l'intrigue réelle — vérifié sur le scénario 1998).

### Agent E — `agent/plan/dramacritic/` (nouveau)
`PlanDramaCritic`/`Input`/`Output`/`.md`. Jugement toujours relatif à l'intensité annoncée par
la consigne, jamais absolu.

### Retiré — `agent/plan/narrativecritic/` (agent + `.md` + step) supprimé entièrement.

### Steps
`PlanGoalCriticStep` (mis à jour), `PlanFactsCriticStep`/`PlanContinuityCriticStep`/
`PlanDramaCriticStep` (nouveaux), `PlanCoherenceCriticStep` (mis à jour, entrée réduite),
`PlanNarrativeCriticStep` (supprimé).

### `orchestrator/plan/ChapterPlanWorkflow.java`
Pool de critics passé de 3 à 5 (goal/facts/coherence/continuity/drama) ; moyenne, seuil
éliminatoire et fusion de feedback recalculés sur 5 scores au lieu de 3. Même mécanisme de
scoring/retry qu'avant, juste généralisé — pas de nouveau système d'agrégation nécessaire
(confirmé par Fable).

### `RedacteurModule.java`
Wiring des 5 nouveaux agents/steps, retrait de `PlanNarrativeCritic`.

### Nettoyage doc
Références obsolètes à `PlanNarrativeCritic` corrigées dans `CriticOutputParser.java`,
`StoryNarrativeCritic.java/.md`, `StoryCausalCritic.md`, `StoryFidelityCritic.md`.

### Tests
`WorkflowLogTest.planWorkflow_standard_logsAllPlanAgents` et `planOnly_doesNotLogWriteOrEval` :
mocks nettoyés (les anciens fragments de prompt français ne matchent plus les nouveaux prompts
anglais — repli sur `UNIVERSAL_PASS`, déjà compatible avec le format `CriticOutputParser` utilisé
par les 5 nouveaux critics) ; assertions mises à jour pour les 5 nouveaux noms de critics.

## 3. Résultat

`mvn compile` : OK. `mvn test` (tous modules) : **17 (commun) + 5 (testllm) + 19 (redacteur)
tests, 0 échec**.

Non fait (reporté explicitement par l'utilisateur) : généraliser le paramétrage de langue
dynamique aux ~20 autres agents du projet (voir mémoire `project_critics_review_queued`).
