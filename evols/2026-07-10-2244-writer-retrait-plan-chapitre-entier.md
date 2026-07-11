# 2026-07-10 22h44 — Writer : retrait du plan de chapitre entier (risque de spoiler)

## 1. Demande

Suite à une revue du prompt Writer (relecture + consultation Fable, voir aussi la fiche du même
jour sur `PlanCoherenceCritic`/`PlanGoalCritic`), plusieurs problèmes ont été identifiés dans
`Writer.java`, dont un jugé bug majeur par l'utilisateur : le Writer recevait `chapterPlan`
(`wc.plan()`, le JSON du **chapitre entier**, toutes séquences confondues) en plus de `sequencePlan`
(déjà scopé à la séquence en cours). Deux conséquences :

1. **Risque de spoiler** : le Writer voyait les beats des séquences futures alors qu'une règle du
   prompt lui interdit justement d'y faire allusion ("Tu n'écris que les événements de cette
   séquence"). Aucune trace dans les fiches d'évolution ne justifiait ce choix — probablement une
   simplicité d'implémentation (`wc.plan()` déjà disponible) plutôt qu'une décision délibérée,
   d'autant que `sequencePlan` (scopé) est déjà transmis séparément en priorité 1 ("Directives
   détaillées de l'auteur").
2. **Bug de troncature** : en mode réécriture, `"### Problèmes à corriger"` était concaténé **à la
   fin** de `chapterPlan`, mais `Writer.java` tronque ce champ via `TruncHelper.text()`, qui garde
   la **tête** et coupe la **fin** en cas de dépassement — risque de perdre silencieusement la
   section la plus importante en réécriture. Un mécanisme d'agrandissement automatique du contexte
   existe déjà côté `OllamaAdapter` (`ContextOverflowException` → grow ×1.3 jusqu'à
   `maxContextWindowSize`, puis relance), donc retirer la troncature sur ce point n'introduit pas
   de risque réel de dépassement silencieux.

Un audit plus large (Fable + vérification du code réel : `TruncHelper`, `PromptBuilder`,
`WriterStep`) a aussi relevé d'autres points sur le Writer, non traités dans cette session (redite
système/user sur le raccord de texte, ambiguïté "Sauf si le guide de style...", positionnement des
interdictions de répétition en tête du prompt malgré un commentaire disant l'inverse — à reprendre
plus tard si besoin).

## 2. Ce qui a été touché

### `WriterInput.java`
Champ `chapterPlan` retiré, remplacé par `rewriteProblems` (String, non-null seulement si
`isRewrite`).

### `WriterStep.java`
Suppression de la construction de `chapterPlan` (`wc.plan()` + concaténation manuelle de
`"### Problèmes à corriger"`). `rewriteProblems` passé tel quel à `WriterInput`.

### `Writer.java`
- Section "Éléments de la trame (dans l'ordre)" et son slot `sPlan` (1/8 du budget) supprimés.
- `"### Problèmes à corriger"` déplacé : ajouté à la suite du bloc "Directives détaillées de
  l'auteur" (nouveau code dans `buildUser()`) — même titre exact qu'avant (le préfixe RÉÉCRITURE du
  system prompt le cite mot pour mot, cohérence VITAL préservée), mais désormais dans la section la
  plus prioritaire et la plus petite (aucun risque de troncature).
- System prompt : toute référence à "la trame" comme concept séparé retirée (remplacée par "les
  directives détaillées de l'auteur"). Liste de priorité réduite de 8 à 7 niveaux ("Éléments de la
  trame" supprimé de la liste).
- Phrase vestige retirée : *"Ces directives ne portent que sur cette séquence : checks, contraintes,
  focus ou lore qu'elles mentionnent ne s'appliquent qu'à cette séquence."* — vérifié dans le code
  (`WriterStep.merge()`, `ScenarioFormatters.writerConstraints(scenario, chapter, sequence)`) que
  `focus`/`lore`/`contraintes` sont **déjà fusionnés par Java** (scénario + chapitre + séquence) en
  une seule section finale avant d'atteindre le prompt — le Writer n'a jamais eu de scoping à faire
  lui-même, contrairement à `PlanCoherenceCritic` qui, lui, reçoit un JSON multi-séquences avec des
  `points_a_verifier` par séquence et a réellement besoin de cette règle de portée.

### `Writer.md`
- Tableau des slots : ligne `chapterPlan` retirée.
- Liste de priorité mise à jour (7 niveaux).
- Nouveau paragraphe explicite sur le non-partage du plan des autres séquences (anti-spoiler) et la
  fusion Java déjà faite pour focus/lore/contraintes.
- Section "Mode réécriture" mise à jour (emplacement de "### Problèmes à corriger").

## 3. Résultat

Compilation vérifiée depuis la racine du projet (`mvn -q compile`, aucune erreur). Aucun test ne
référençait `WriterInput.chapterPlan()` (vérifié par recherche), donc aucun test cassé.

Non traité dans cette session (laissé en l'état, voir mémoire `project_deus_writer_leak_fix_queued`
pour un chantier séparé et en sommeil) : la fuite de raisonnement LLM dans le texte rédigé
("HORREUR ABSOLUE"), le renforcement de la consigne "aucun commentaire" avec exemple concret, le
nouveau type 6 de `DeusInMachinaCorrector`, et le sentinel `"[SUPPRESSION]"` de `CorrectionParser`.
Également non traités : les autres pistes de la revue Fable sur Writer (redite raccord système/user,
positionnement des interdictions de répétition, ambiguïté guide de style).
