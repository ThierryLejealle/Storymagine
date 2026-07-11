# 2026-07-10 18h04 — Révision des prompts DeusInMachinaCorrector / DeusInMachinaCritic

## 1. Demande

Suite à une relecture du corrector suivant dans la chaîne (`DeusInMachinaCorrector`), plusieurs
écarts ont été identifiés par rapport à son agent jumeau `DeusInMachinaCritic` (censé détecter les
mêmes 5 types de fuites) :
- Le Corrector n'avait pas les garde-fous anti-faux-positifs du Critic sur les types 2 (fiche
  personnage) et 3 (artefact de scénario) — problématique car le Corrector patche le texte
  AVANT que le Critic ne l'évalue : un faux positif du Corrector n'est jamais rattrapé.
- Le PRINCIPE annonçait "deux fuites principales" alors que la taxonomie en développe cinq —
  incohérence entre les deux prompts (Corrector et Critic).
- Absence de consigne demandant à JUSTE de préserver le sens/l'intention du passage et du
  scénario (demande explicite de l'utilisateur en cours de session).

Une consultation externe (modèle Fable) a été utilisée pour proposer une première version du
delta, en présentant le contexte complet (objectif de l'agent, les deux prompts, les problèmes
identifiés). Fable a découvert que l'écart Corrector/Critic était partiellement volontaire
(durcissement du 2026-06-26, voir `2026-06-26-1500-deusinmachinacorrector-renforcement.md`) et a
adapté sa proposition pour ne pas défaire ce durcissement.

Le delta complet (prompt Corrector, prompt Critic, clause JUSTE, stratégie de relance) a été
présenté à l'utilisateur avant écriture, avec l'ancien texte, le nouveau texte, la raison et
l'objectif de chaque changement — validation explicite obtenue avant toute modification.

## 2. Ce qui a été touché

### `DeusInMachinaCorrector.java`
- PRINCIPE fusionné avec le TEST : suppression de la liste "deux fuites principales" (les deux
  exemples concrets sont déplacés dans la description du type 3, leur vraie place taxonomique).
- Type 2 : ajout de la règle "les descriptions physiques ponctuelles ne sont PAS des fuites" +
  exemple OK ("Ses yeux noisette, légèrement myopes...") — repris du Critic.
- Type 3 : description enrichie des deux cas concrets (fragment recopié / confirmation suivie) +
  ajout de la règle "observer le rythme ou la dynamique d'une scène n'est pas un artefact" +
  exemple OK ("...marquant une pause nette...") — repris partiellement du Critic (la phrase sur
  "impossible sous la plume d'un romancier humain" n'a pas été reprise pour ne pas dupliquer le
  TEST et ne pas affaiblir le durcissement du 26/06).
- Nouvelle clause dans FORMAT DE RÉPONSE : "JUSTE ne change que ce qui constitue la fuite :
  conserve le sens, l'intention et les faits du passage et du scénario — ne réécris rien d'autre."
- `retryStrategy()` : `RATIO_THRESHOLD` → `DECREASING_AND_RATIO_THRESHOLD` (les 5 types de fuites
  forment un ensemble fermé et énumérable, comme la grammaire — convergence réelle attendue,
  cohérent avec le choix fait pour `GrammarCorrector`).

### `DeusInMachinaCritic.java`
- PRINCIPE fusionné avec la même logique que le Corrector (suppression "deux fuites principales",
  phrase de transition vers les cinq formes). Le TEST du Critic (formulation différente de celui
  du Corrector) reste inchangé.
- Type 3 : description enrichie des deux cas concrets, comme le Corrector. La RÈGLE du Critic
  ("impossible sous la plume d'un romancier humain... observer le rythme...") reste inchangée,
  déjà complète.

### Non modifié
- RÈGLE PRÉALABLE (les deux agents, y compris l'Exception du Corrector).
- Types 1, 4, 5 (les deux agents).
- FORMAT DE RÉPONSE du Critic (FUITE/OK) et bloc FORMAT DE RÉPONSE du Corrector hors nouvelle
  clause JUSTE (CORRECTIONS/FAUX/JUSTE/PAS DE CORRECTION — contrat parsé par `CorrectionParser`).

## 3. Résultat

Décision explicite de ne pas ajouter de contrôle runtime supplémentaire sur la qualité sémantique
des paires FAUX/JUSTE (au-delà du contrôle générique déjà existant — `TextPatcher.apply()` /
`WriteWorkflow#applyCorrections`, qui vérifie que FAUX est bien trouvé dans le texte et logue un
avertissement sinon). La préservation du sens est désormais demandée au niveau du prompt
(clause JUSTE ci-dessus) plutôt que vérifiée a posteriori par un second appel LLM — jugé
disproportionné tant qu'aucune dérive réelle n'est observée en pratique.

Compilation vérifiée depuis la racine du projet (`mvn -q compile`, aucune erreur) après le lot
complet de modifications.

Point annexe relevé mais hors périmètre de cette session : ni `DeusInMachinaCorrector.md` ni
`DeusInMachinaCritic.md` n'existent, alors que la règle du projet impose un `.md` par agent —
constat partagé par la plupart des agents du module (Grammar/Naturality/Style/Check/
PlanFidelity), pas spécifique à ceux-ci.
