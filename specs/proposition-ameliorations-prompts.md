# Analyse et propositions d'amélioration — Prompts des agents

> Analyse réalisée le 2026-06-25.  
> Périmètre : 23 agents dans `redacteur/.../domaine/agent/`.  
> Objectif : identifier les redites, contradictions, ambiguïtés et faiblesses vis-à-vis des petits modèles (contexte limité, tendance à externaliser le raisonnement, sensibilité aux formulations négatives complexes).

---

## Tableau de synthèse

| Priorité | Agent(s) | Problème |
|----------|----------|----------|
| **Haute** | GoalPlanChecker | Risque de fuite de raisonnement dans la sortie |
| **Haute** | GoalPlanChecker + GoalTextChecker | Convention cas-vide incohérente entre agents miroirs |
| **Haute** | PlanCoherenceCritic, TextCoherenceCritic, TextNarrativeCritic, PlanNarrativeCritic | "ces trois lignes" ambigu → "ces trois sections" |
| **Haute** | TextCoherenceCritic | Focus présent dans le user mais absent de la liste du system |
| **Moyenne** | ChapterStyleChecker + SequenceStyleChecker | Double instruction style guide (redite) |
| **Moyenne** | ChapterStyleChecker + SequenceStyleChecker | Échelles de notation inconsistantes entre agents frères |
| **Moyenne** | ChapterPlanner | Règles beats 1 et 2 identiques dans JSON_PLANNER_SYSTEM |
| **Moyenne** | GoalTextChecker, TextNarrativeCritic, Proofreader | Manque d'exemples de sortie (agents frères en ont) |
| **Faible** | DeusInMachinaChecker | Caractères unicode U+2500 (séparateurs ────) |
| **Faible** | SequenceChecker | Arithmétique "-1 pt par élément" trop complexe pour petits modèles |
| **Faible** | FocusActionFilter | Critère de "pertinence" non défini |
| **Faible** | RepetitionFilter | Logique inversée potentiellement confuse |
| **Faible** | CausalAnalyzer, NarrativeArcAnalyzer | Questions redondantes + manque d'exemple de sortie |
| **Faible** | StoryCompressor | Limite de mots répétée dans system ET user |
| **Info** | Writer | Répétition de la règle de raccord (system + user) — intentionnelle, OK |

---

## Analyse agent par agent

---

### CausalAnalyzer

**Prompt system actuel :**
```
Tu es un analyste de cohérence narrative. Tu examines les PLANS de tous les chapitres d'un roman.
Tu vérifies UNIQUEMENT la cohérence causale entre chapitres :
chaque événement important a-t-il une cause ? Chaque cause a-t-elle une conséquence ?
Signale : événements sans cause, contradictions factuelles entre chapitres,
conséquences importantes jamais exploitées.
Si rien à signaler, écris SCORE: 10 sans PROBLEME.

Format de sortie strict :
PROBLEME: [description courte d'un problème réel]
SCORE: N  (entier 0-10)
En français.
```

**Problème 1 — Questions redondantes**
> `chaque événement important a-t-il une cause ? Chaque cause a-t-elle une conséquence ?`

Les deux questions expriment la même règle : la causalité est vérifiée dans les deux sens. La seconde n'ajoute rien.

Suggestion :
```
Vérifie que chaque événement important a une cause et que chaque cause a une conséquence dans le récit.
```

**Problème 2 — Pas d'exemple de sortie pour le cas non-vide**

Le cas vide est traité (`SCORE: 10 sans PROBLEME`) mais le cas avec problèmes n'a pas d'exemple. Un petit modèle peut produire du texte libre au lieu du format attendu.

Suggestion — ajouter après le format :
```
Exemple :
PROBLEME: La trahison de Martin (chapitre 4) n'a aucune cause établie dans les chapitres précédents.
PROBLEME: La mort de Claire (chapitre 2) n'est jamais exploitée dans la suite.
SCORE: 6
```

---

### NarrativeArcAnalyzer

Mêmes problèmes que CausalAnalyzer (questions redondantes, manque d'exemple). Même suggestion applicable.

---

### ChapterStyleChecker

**Prompt system actuel (sections clés) :**
```
[Section optionnelle style guide]
"Vérifie que le texte respecte scrupuleusement le guide de style ci-joint."
"Ne signale jamais comme défaut ce que le guide prescrit explicitement."   ← (A)

[Section qualité stylistique]
liste de 7 critères...

[Note optionnelle styleException]
"Si un de ces défauts est imposé par la consigne de style, ne le mentionne pas."  ← (B)
```

**Problème 1 — Double instruction style guide (T2)**

(A) et (B) sont la même règle avec deux formulations différentes, séparées par toute la liste des critères. Un petit modèle les traite comme deux règles distinctes, ou oublie (A) quand il lit (B).

Suggestion — supprimer (A), garder uniquement (B) car elle est contextualisée juste après les critères de qualité qu'elle nuance :
```
[Supprimer la phrase (A) dans styleGuideSection]
[Conserver la phrase (B) dans styleException]
```

**Problème 2 — Échelle incohérente avec SequenceStyleChecker (T3)**

ChapterStyleChecker :  `10, 9, 8, 7, 6, 5, 3`  
SequenceStyleChecker : `10, 8, 7, 6, 5, 3, 1`

Les deux agents évaluent la même dimension stylistique à des granularités différentes. Les "trous" poussent les modèles à utiliser des notes non prévues (ex. 4, 2 pour Chapter ; 9, 4, 2 pour Sequence).

Suggestion — aligner sur une échelle commune, par exemple :
```
10 = irréprochable, publiable tel quel
 8 = bon, défauts mineurs sans impact réel
 7 = correct mais plat ou maladroit par endroits
 6 = problèmes qui nuisent à la lecture ou cassent l'immersion
 5 = plusieurs défauts sérieux — réécriture partielle nécessaire
 3 = à réécrire intégralement sur le plan stylistique
 1 = texte qui trahit visiblement sa fabrication
Réserve 8+ à l'exceptionnel. Un texte moyen ne dépasse pas 6.
```

---

### SequenceStyleChecker

Mêmes problèmes que ChapterStyleChecker (double instruction style guide, échelle inconsistante). Même suggestion applicable.

---

### ChapterPlanner

**Prompt JSON_PLANNER_SYSTEM (section beats) :**
```
Règles pour les beats, par ordre de priorité :
1. Ne perds jamais une action explicitement présente dans la consigne — la couverture est absolue.
2. Couvre tous les éléments de la consigne sans en omettre aucun.
3. Enrichis librement au-delà de la consigne — rends la séquence riche et intéressante.
```

**Problème 1 — Règles 1 et 2 identiques**

Règle 1 = "ne perds jamais une action explicitement présente dans la consigne".  
Règle 2 = "couvre tous les éléments de la consigne sans en omettre aucun".  
C'est exactement la même instruction, dite deux fois avec un vocabulaire légèrement différent.

Suggestion — fusionner en une seule règle :
```
Règles pour les beats, par ordre de priorité :
1. Couvre TOUS les éléments de la consigne sans exception — la couverture est absolue.
2. Enrichis librement au-delà de la consigne — rends la séquence riche et intéressante.
```

**Prompt FOCUS_LORE_NOTE :**
```
Focus et lore : la section « Éléments à utiliser (focus) — toutes les séquences »
s'applique à l'ensemble du chapitre — efforce-toi de les intégrer dans chaque séquence concernée.
```

**Problème 2 — "toutes les séquences" vs "chaque séquence concernée"**

"S'applique à l'ensemble du chapitre" puis "chaque séquence concernée" : si c'est pour tout le chapitre, toutes les séquences sont concernées. "Concernée" introduit une ambiguïté non définie.

Suggestion :
```
s'applique à l'ensemble du chapitre — intègre ces éléments dans chaque séquence où ils s'y prêtent naturellement.
```

---

### GoalPlanChecker

**Prompt system actuel (section procédure) :**
```
Procède dans cet ordre :
1. Analyse le plan entier.
2. Note tous les défauts et axes d'amélioration.
3. Détermine la note en fonction de la qualité globale.
4. Liste en sortie défauts et axes d'amélioration trouvés.

FORMAT STRICT :
PROBLEME: une ligne par problème, ou [RIEN] si aucun problème.
SCORE: la note que tu as déterminée  (entier 1-10)
Rien d'autre : ni texte avant ni texte apres ces trois lignes.
```

**Problème 1 — Risque de fuite de raisonnement (haute priorité)**

Les étapes 1, 2, 3 sont de la réflexion interne. Rien n'interdit au modèle de les écrire en sortie avant le format strict. Les petits modèles externalisent souvent leur chaîne de pensée ("Analyse : ...", "Défauts trouvés : ...").

Suggestion — ajouter explicitement après la procédure :
```
Écris UNIQUEMENT le FORMAT STRICT ci-dessous — ne produis pas les étapes intermédiaires 1, 2 et 3 dans ta réponse.
```

**Problème 2 — Convention cas-vide incohérente avec GoalTextChecker (T4)**

GoalPlanChecker : `PROBLEME: [RIEN]` si aucun problème (avec exemple).  
GoalTextChecker : `Si score = 10 : aucune ligne PROBLEME:` (sans exemple).

Deux agents miroirs, deux conventions. Le parseur doit gérer les deux cas.

Suggestion — aligner GoalTextChecker sur la convention de GoalPlanChecker :
```
[Dans GoalTextChecker] Remplacer "Si score = 10 : aucune ligne PROBLEME:" par
"PROBLEME: [RIEN] si aucun problème"
Et ajouter les mêmes exemples que GoalPlanChecker.
```

**Problème 3 — "ces trois lignes" ambigu (T1)**

`Rien d'autre : ni texte avant ni texte après ces trois lignes.`

Il peut y avoir plusieurs lignes PROBLEME — "ces trois lignes" est faux.

Suggestion :
```
Rien d'autre : ni texte avant ni texte après ces deux ou trois sections.
```
(ou plus précisément : `Uniquement les lignes PROBLEME: et la ligne SCORE: — rien d'autre.`)

---

### GoalTextChecker

**Problème 1 — Manque d'exemples de sortie**

GoalPlanChecker, son agent miroir, a deux exemples complets. GoalTextChecker n'en a aucun.

Suggestion — ajouter les mêmes exemples :
```
Exemple 1 — deux problèmes, note 7 :
PROBLEME: Le passage central ne produit pas l'effet d'isolement requis par l'objectif.
PROBLEME: La scène finale amène une résolution prématurée qui court-circuite la tension.
SCORE: 7

Exemple 2 — aucun problème :
PROBLEME: [RIEN]
SCORE: 10
```

**Problème 2 — Convention cas-vide** : voir GoalPlanChecker Problème 2.

**Problème 3 — Échelle incomplète**

L'échelle saute : `10, 9, 8, 7, 6, 5, 3`. Les valeurs 4, 2, 1 sont absentes mais le modèle peut les utiliser.

Suggestion :
```
Ajouter "(entier parmi : 3, 5, 6, 7, 8, 9, 10)" après SCORE: N
```
ou compléter l'échelle avec les valeurs manquantes.

---

### PlanCoherenceCritic

**Problème — "ces trois lignes" ambigu (T1)**

```
Rien d'autre : ni texte avant ni texte apres ces trois lignes.
```

Il y a 3 *sections* pouvant contenir chacune plusieurs lignes (AMELIORATION, DEFAUT_SIGNIFICATIF, DEFAUT_MAJEUR). "Ces trois lignes" est inexact — un modèle produisant 3 DEFAUT_SIGNIFICATIF peut penser avoir violé la règle.

Suggestion :
```
Rien d'autre : ni texte avant ni texte après ces trois sections.
```

*Note : ce prompt est par ailleurs l'un des mieux construits de l'ensemble — les exemples contextualisés (pilotes, Biggin Hill) sont exactement ce dont les petits modèles ont besoin.*

---

### PlanNarrativeCritic

Même problème T1 ("ces trois lignes"). Même suggestion.

---

### DeusInMachinaChecker

**Problème 1 — Caractères unicode dans les séparateurs**

```
────────────────────────────────────────────────────────────
CINQ FORMES DE FUITES
────────────────────────────────────────────────────────────
```

Les caractères `─` (U+2500) peuvent être tokenisés bizarrement par certains petits modèles quantisés ou générer du bruit dans le contexte.

Suggestion :
```
=== CINQ FORMES DE FUITES ===
```
ou simplement `## CINQ FORMES DE FUITES` (markdown).

**Problème 2 — PRINCIPE et TEST redondants**

```
PRINCIPE
Un lecteur n'a pas accès aux instructions qui ont créé ce texte. Toute phrase qui ne
s'explique que si l'on connaît ces instructions est une fuite.

TEST : pour chaque phrase suspecte, demande-toi — cette phrase existerait-elle si
aucune consigne ne l'avait provoquée ? Si non, c'est une fuite.
```

PRINCIPE et TEST expriment la même règle. Le TEST est plus opérationnel ; le PRINCIPE est redondant.

Suggestion — supprimer le PRINCIPE, garder seulement le TEST reformulé :
```
RÈGLE : pour chaque passage suspect, demande-toi — cette phrase existerait-elle
dans un roman publié, sans aucune consigne de fabrication ? Si non, c'est une fuite.
```

---

### TextCoherenceCritic

**Problème 1 — "ces trois lignes" ambigu (T1)**

Même problème que PlanCoherenceCritic. Même suggestion.

**Problème 2 — Focus dans le user absent du system (haute priorité)**

Le user envoie une section `### Éléments à utiliser (focus)` mais le system définit :
```
Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks),
fiches personnage (faits et psychologie des personnages), continuite factuelle.
```
Le focus n'est pas dans cette liste. Le modèle reçoit un focus mais n'a pas de consigne sur quoi en faire avec ce focus.

**Option A** — Ajouter le focus à la liste du system :
```
Tu verifies UNIQUEMENT : faits etablis, contraintes explicites (checks),
fiches personnage, continuite factuelle, et utilisation des éléments focus requis.
```

**Option B** — Retirer la section focus du user si la vérification de présence du focus n'est pas l'objectif de cet agent (le SequenceChecker s'en charge déjà via les `checks`).

**Problème 3 — Pas d'exemples (contrairement à PlanCoherenceCritic)**

PlanCoherenceCritic a des exemples riches (Biggin Hill, etc.). TextCoherenceCritic n'en a aucun. Pour des agents frères, cette asymétrie fragilise la cohérence des résultats.

Suggestion — ajouter 2 exemples brefs au format AMELIORATION/DEFAUT_SIGNIFICATIF/DEFAUT_MAJEUR.

---

### TextNarrativeCritic

**Problème 1 — "ces trois lignes" ambigu (T1)**

Même suggestion que PlanNarrativeCritic.

**Problème 2 — Pas d'exemples (contrairement à PlanNarrativeCritic)**

PlanNarrativeCritic a deux exemples complets. TextNarrativeCritic n'en a aucun. Asymétrie dans des agents frères.

Suggestion — reprendre les mêmes exemples en les adaptant au texte (au lieu du plan) :
```
Exemple 1 — deux défauts significatifs :
AMELIORATION : [RIEN]
DEFAUT_SIGNIFICATIF : Le lien émotionnel entre la séquence d'ouverture et la résolution finale n'est pas perceptible dans le texte.
DEFAUT_SIGNIFICATIF : La montée en tension promise par l'objectif s'effondre à mi-chapitre sans retournement.
DEFAUT_MAJEUR : [RIEN]
```

---

### TextWhatIfCritic

**Problème — Échelle incomplète**

L'échelle saute : `10, 9, 8, 7, 6, 5, 3`. Les valeurs 4, 2, 1 sont absentes.

Suggestion :
```
Ajouter "(entier parmi : 3, 5, 6, 7, 8, 9, 10)" après SCORE: N
```
ou compléter avec : `4 = incohérences sérieuses`, `2 = prémisse invalidée par ses propres conséquences`, `1 = inutilisable`.

---

### Proofreader

**Problème — Manque d'exemple concret FAUX/JUSTE**

Le format est bien défini mais sans exemple. Les petits modèles ont tendance soit à ne citer qu'un mot (au lieu de la phrase entière), soit à ajouter des explications après JUSTE:.

Suggestion — ajouter un exemple minimal dans le system :
```
Exemple :
CORRECTIONS:
- FAUX: "Il a été allé au marché hier soir."
  JUSTE: "Il est allé au marché hier soir."
```

---

### RepetitionFilter

**Problème — Logique inversée potentiellement confuse**

Le modèle doit retourner les candidats qui NE sont PAS des leitmotivs. C'est une double négation sémantique. Les petits modèles ont tendance à inverser les listes (retourner les leitmotivs au lieu des candidats à bannir).

Suggestion — reformuler positivement :
```
Retourne UNIQUEMENT les expressions de la liste "candidates à bannir" qui ne correspondent
à aucun leitmotiv protégé. Les expressions proches d'un leitmotiv sont exclues de ta réponse.
```

---

### SequenceChecker

**Problème — Arithmétique trop complexe pour petits modèles**

```
SCORE: N  (entier 0-10 ; 10 = tous présents et développés ; -1 pt par élément manquant ou insuffisant)
```

Les petits modèles font souvent des erreurs de comptage et d'arithmétique. La règle `-1 pt par élément` est trop mécanique pour être fiable.

Suggestion — remplacer par une échelle qualitative :
```
SCORE: N  (entier 0-10)
10 = tous les éléments présents et développés
7-9 = 1 ou 2 éléments insuffisamment développés
4-6 = plusieurs éléments absents ou superficiels
0-3 = majorité des éléments absents
```

---

### StateExtractor

Rien à signaler. Court, format strict, limites bien posées, cas vide explicite (`AUCUN`). Très bon agent.

---

### StoryCompressor

**Problème — Limite de mots répétée dans system ET user**

System : `Maximum X mots. Prose factuelle. En français.`  
User : `Produis le résumé mis à jour en X mots maximum.`

Redite légère. Pas bloquant mais consomme des tokens inutilement.

Suggestion — garder uniquement dans le user (plus proche de la demande concrète), supprimer du system.

---

### RepetitionTracker

Rien à signaler. Limites de taille bien posées, format clair avec deux sections, mécanisme anti-doublon dans le user. Très bon agent.

---

### TextDreamCritic

Rien à signaler. Calibration par niveau de réalisme bien construite, critères ciblés, format clair.

---

### Writer

**Point d'attention — Répétition de la règle de raccord (intentionnelle)**

La règle "raccorde au texte précédent" est dans le system (`openingRule`) et répétée à la fin du user. Cette répétition est intentionnelle et utile : le system est "loin" quand le modèle lit la fin du user. Pas de changement nécessaire.

**Point d'attention — Longueur des listes d'interdictions**

Les listes `EXPRESSIONS À NE PAS RÉPÉTER` et `SCHÉMAS NARRATIFS USÉS` sont en fin de system prompt. Pour les petits modèles avec contexte réduit, les instructions en fin de system prompt sont souvent ignorées quand le user est long.

Suggestion — envisager de déplacer ces listes au début du user (avant les sections de contexte), plus proches du moment où le modèle génère le texte.

---

### FocusActionFilter

**Problème — Critère de pertinence non défini**

Le modèle doit choisir ce qui est "pertinent" pour une séquence mais le system ne définit pas ce que "pertinent" signifie. Les petits modèles peuvent tout sélectionner (pour ne rien manquer) ou sélectionner au hasard.

Suggestion — ajouter une règle de sélection explicite :
```
Un groupe focus est pertinent si ses items concernent directement une action, un lieu,
un objet ou un personnage impliqué dans la séquence décrite.
Un groupe focus qui ne peut pas apparaître naturellement dans cette séquence est exclu.
```

Suggestion 2 — ajouter un exemple de sortie minimal :
```
Exemple :
FOCUS: ARMES_MEDIEVALES, CHEVAL
ACTIONS: combat, déplacement
```

---

## Notes de méthode

- Agents sans problème notable : **RepetitionTracker**, **StateExtractor**, **TextDreamCritic**
- Les agents critics (PlanCoherenceCritic, PlanNarrativeCritic) sont les mieux construits de l'ensemble grâce à leurs exemples contextualisés — ce pattern devrait être étendu à leurs miroirs TextCoherenceCritic et TextNarrativeCritic
- La cohérence entre agents miroirs (Plan/Text) est un axe d'amélioration systématique : exemples, conventions de cas vide, échelles de notation
