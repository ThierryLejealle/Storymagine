# 2026-06-26 12h00 - Refonte critics/correctors : faux positifs, sévérité, split ElementCritic

## Évolution demandée

Réduire les faux positifs dans plusieurs agents, aligner les formats de sortie des Correctors,
introduire la sévérité graduée (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR) dans StyleCritic,
et remplacer ElementCritic par deux agents mieux ciblés.

---

## Ce qui a été touché

### 1. NaturalityCorrector — alignement format

- `NaturalityFinding` : suppression du champ `probleme` inutilisé ; renommage `extrait`/`suggestion` → `wrongPhrase`/`correctedPhrase`
- Prompt entièrement réécrit : format FAUX/JUSTE (identique à ProofreaderCorrector), sentinel PAS DE CORRECTION
- Nouvelles catégories : "ambiance expliquée" (La tension était palpable…)
- Règle anti-faux-positif : exemple "Elle sentit le monde se fissurer sous ses pieds." → correction refusée
- Parser réécrit en logique FAUX/JUSTE ligne par ligne

### 2. Warning replace miss — tous les Correctors

- `WriteWorkflow` : les trois méthodes `applyDeusCorrections`, `applyNaturalityFixes`, `applyProofCorrections` sont passées en instance (non static) et ajoutent un `log.warn()` si `String.replace()` ne trouve pas la phrase
- Règle documentée dans `agent/CLAUDE.md` (section "Corrector — Replace Warning Rule")

### 3. DeusInMachinaCritic — faux positifs

- TEST reformulé : "Un romancier humain qui écrirait librement pourrait-il écrire exactement cela ?"
- Type 2 : ajout RÈGLE excluant les descriptions physiques ponctuelles + exemple OK (yeux noisette)
- Type 3 : ajout RÈGLE excluant l'observation du rythme/dynamique + exemple OK (pause nette)

### 4. StyleCritic — score toujours à 1

- Migration de `ProblemScoreParser` → `CriticOutputParser` (AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR)
- Définitions calibrées (proposées par utilisateur) :
  - AMELIORATION : retouche possible, la plupart des lecteurs ne remarquent rien
  - DEFAUT_SIGNIFICATIF : faiblesse perceptible par un lecteur attentif
  - DEFAUT_MAJEUR : rompt l'immersion, donne une impression de texte artificiel
- Deux exemples dans le prompt (un avec problèmes, un vide avec [RIEN])
- Reformulation du header qualité pour éviter le biais sur le registre stylistique

### 5. Split ElementCritic → PlanFidelityCritic + CheckCritic

**Diagnostic** : ElementCritic mélange deux responsabilités distinctes —
vérifier que les beats du plan sont développés (contenu narratif obligatoire)
vs vérifier des points de contrôle du scénario (règles à respecter).
De plus, il ne recevait pas la liste des beats extraite du plan.

**Nouveaux agents créés** :

- `planfidelitycritic/PlanFidelityCritic.java` — vérifie que chaque beat est développé (MANQUANT:, TOUT_OK)
- `planfidelitycritic/PlanFidelityCriticInput.java` — `(List<String> beats, String sequenceText)`
- `planfidelitycritic/PlanFidelityCriticOutput.java` — `(List<String> failures, int score)`
- `checkcritic/CheckCritic.java` — vérifie les points de contrôle du scénario (ECHEC:, TOUT_OK)
- `checkcritic/CheckCriticInput.java` — `(List<String> checks, String sequenceText)`
- `checkcritic/CheckCriticOutput.java` — `(List<String> failures, int score)`

**Formule score** : `Math.max(1, 10 - failures.size() * 3)` pour les deux.

**Steps créés** :

- `PlanFidelityCriticStep.java` — inclut `extractBeats(String sequencePlan)` qui parse la section "BEATS :" du plan
- `CheckCriticStep.java`

**Comportement Writer** :
- PlanFidelityCritic : échecs remontés au Writer avec préfixe "Beat du plan non développé : "
- CheckCritic : échecs NON remontés au Writer (TODO : dual representation)

**Supprimés** :
- `elementcritic/ElementCritic.java`
- `elementcritic/ElementCriticInput.java`
- `elementcritic/ElementCriticOutput.java`
- `orchestrator/write/ElementCriticStep.java`

**Mis à jour** :
- `WriteWorkflow.java` — remplacement du bloc ElementCritic par PlanFidelityCritic + CheckCritic
- `RedacteurModule.java` — câblage des deux nouveaux agents/steps
- `orchestrator/CLAUDE.md` — diagramme mis à jour
- `agent/CLAUDE.md` — format Corrector canonique FAUX/JUSTE + règle replace warning

---

## Résultat

Compilation OK sur l'ensemble du projet.
Les faux positifs des trois critics concernés ont été ciblés par des règles et exemples explicites.
ElementCritic est remplacé par deux agents à responsabilité unique, recevant les bonnes données.
