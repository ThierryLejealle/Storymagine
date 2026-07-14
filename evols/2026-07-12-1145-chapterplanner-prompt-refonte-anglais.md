# 2026-07-12 11h45 — ChapterPlanner : refonte du prompt JSON en anglais

## 1. Demande

Analyse du log `003_PlanGoalCritic_1.md` puis `002_ChapterPlanner_1.md` (scénario 1998, run
2026-07-12_10h32) : le plan généré pour le chapitre 1 avait deux vrais défauts —
1. L'accord "se retrouver pour le repas puis profiter de la plage" n'était que partiellement
   couvert (repas seul, plage omise).
2. La consigne "Ni Corinne ni Catherine dans cette scène" était respectée en substance (aucune
   des deux n'agit) mais Corinne était quand même nommée dans un beat ("avec Corinne qui reste
   au lit").

Demande explicite de l'utilisateur : faire réécrire le prompt système `ChapterPlanner` (mode
JSON) par Fable, en anglais (alignement sur la convention des agents récents type
`PlanGoalCritic`), avec des exemples strictement génériques (aucune référence aux
personnages/lieux de ce scénario), et justification de chaque changement. Deux consultations
Fable :
1. Un premier passage a proposé 4 corrections ciblées, mais avec des exemples recopiés du
   scénario en cours (repas/plage, Corinne) — rejeté par l'utilisateur ("DANGER : prompt sur
   mesure pour cette histoire").
2. Un second passage, recadré explicitement (exemples génériques obligatoires, réécriture
   complète en anglais), a produit la version retenue.

Une revue critique manuelle a suivi (longueur du prompt +40-50%, cohérence avec les libellés de
section français du prompt utilisateur, risque de bundler traduction + fix dans un seul commit,
clarification noms de champs JSON vs langue des valeurs, portée réelle de la checklist finale).
Un point de mon analyse s'est avéré faux (affirmation que `Think: n/a` impliquerait l'absence de
toute délibération du modèle — corrigé par l'utilisateur : `Think: n/a` reflète seulement la
neutralisation du flag API par `resolveThink()`, pas l'absence de raisonnement réel du modèle).

## 2. Ce qui a été touché

### `ChapterPlanner.java`
- `JSON_PLANNER_SYSTEM` — entièrement réécrit en anglais. Ajouts : section "EXCLUDED
  CHARACTERS" (inexistante avant — aucune règle ne couvrait l'exclusion de personnage), règle 1
  explicitée pour les consignes à plusieurs éléments (exemple générique "lunch + walk by the
  river", noms/lieu inventés), table d'exemples relabellisée "NEVER / INSTEAD" (au lieu de la
  colonne neutre "abstrait → observable" qui pouvait être lue comme du vocabulaire disponible)
  + règle anti-copie explicite. Clarification ajoutée sous OUTPUT FORMAT : les noms de champs
  JSON restent inchangés, seules les valeurs sont en français (résout une ambiguïté réelle de
  l'ancien prompt, où "En français." flottait au milieu sans portée claire).
- Nouvelle constante `JSON_PLANNER_FINAL_CHECK` — checklist finale à 2 points (couverture
  multi-éléments, absence totale des personnages exclus), extraite du texte de Fable et
  **volontairement séparée** de `JSON_PLANNER_SYSTEM` : `INNER_STATE_NOTE` et `FOCUS_LORE_NOTE`
  restent des constantes françaises partagées avec `FREE_PLANNER_SYSTEM` (non touché, hors
  périmètre), concaténées après `mainSystem` dans `buildSystem()` — la checklist devait rester
  la toute dernière chose lue par le modèle (effet de récence), donc câblée en toute fin de
  `buildSystem()`, après `buildForbiddenPhrases(in)`, uniquement en mode JSON non-correction.
- `FREE_PLANNER_SYSTEM` et `CORRECTION_SYSTEM` : **non modifiés**, hors périmètre de cette
  demande (pas analysés/validés avec Fable).

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec (aucun test
n'était couplé au texte français exact de `JSON_PLANNER_SYSTEM`). Non encore observé en usage
réel sur un nouveau run de planification.

## Note — risque assumé

Ce changement bundle deux choses dans un seul commit : le fix des 2 défauts réels, et la
traduction FR→EN du reste du prompt (demandée explicitement, alignement avec `PlanGoalCritic`).
Si un comportement inattendu apparaît sur `ChapterPlanner` après ce changement, il faudra garder
à l'esprit que deux variables ont changé en même temps — pas de moyen de distinguer laquelle est
en cause sans un test A/B séparé, qu'on n'a pas fait.
