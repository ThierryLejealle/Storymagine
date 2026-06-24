# Règles de retry — Storymagine

Source de référence : `../Redacteur/ChapterOrchestrator.java`
Paramètres configurables dans : `redacteur/engine.properties`

---

## 1. Retry du plan (`PlanWorkflow`)

**Déclenchement** : dès qu'au moins un agent critique émet une remarque
(non-vide dans AMELIORATION, DEFAUT_SIGNIFICATIF ou DEFAUT_MAJEUR).

**Nombre de passes** : `1 + critique.plan.max_retry` (défaut : 4 passes, soit 3 rewrites max).

**Sélection** : à la fin de toutes les passes, le plan avec la **meilleure note moyenne** est retenu —
pas nécessairement le dernier. L'historique des problèmes est passé au planificateur pour guider la correction.

**Agents concernés** : PlanNarrativeCritic, PlanCoherenceCritic, GoalPlanChecker.

---

## 2. Vérification par séquence (`WriteWorkflow.runSequenceCheckers`)

Trois vérificateurs indépendants, chacun avec sa propre boucle de réécriture.
Chaque boucle est limitée à `critique.sequence.max_retry` tentatives (défaut : 1).
Les boucles s'exécutent dans l'ordre : DeusInMachina → StyleChecker → SequenceChecker.

| Vérificateur          | Condition de réécriture                     | Nature  |
|-----------------------|---------------------------------------------|---------|
| DeusInMachinaChecker  | au moins une fuite mécanique détectée       | Binaire |
| SequenceStyleChecker  | score < 7 (STYLE_THRESHOLD)                 | Score   |
| SequenceChecker       | au moins un élément requis manquant         | Binaire |

Le SequenceChecker n'est activé que si le scénario déclare des `writerChecks`.

---

## 3. Critique du chapitre (`WriteWorkflow.runChapterCritique`)

Évalue le **texte complet du chapitre** (toutes séquences concaténées) après la phase d'écriture.

**Agents selon le type de chapitre** :

| Type       | Agents                                                        |
|------------|---------------------------------------------------------------|
| IMPERATIVE | TextNarrativeCritic, TextCoherenceCritic, GoalTextChecker    |
| DREAM      | TextDreamCritic, GoalTextChecker                              |
| WHAT_IF    | TextWhatIfCritic, TextCoherenceCritic, GoalTextChecker        |

**Déclenchement** : si la note moyenne de tous les agents ≤ `critique.chapitre.threshold` (défaut : 7,0).

**Nombre de passes** : `1 + critique.chapitre.max_retry` (défaut : 4 passes, soit 3 rewrites max).

**Comportement** :
- Les problèmes des critiques sont passés au Writer comme contraintes de réécriture.
- Toutes les séquences du chapitre sont réécrites (les vérificateurs per-séquence repassent aussi).
- La **version avec le meilleur score moyen** est retenue — pas nécessairement la dernière.
- La WorldState et la RepetitionMemory s'accumulent entre les passes (même comportement que Redacteur).

---

## Valeurs par défaut (`EngineConfig.defaults()`)

| Paramètre                          | Valeur |
|------------------------------------|--------|
| `critique.plan.max_retry`          | 3      |
| `critique.chapitre.threshold`      | 7.0    |
| `critique.chapitre.max_retry`      | 3      |
| `critique.sequence.max_retry`      | 1      |

Ces valeurs correspondent à celles de `engine.properties` et à `EngineConfig.defaults()`.
