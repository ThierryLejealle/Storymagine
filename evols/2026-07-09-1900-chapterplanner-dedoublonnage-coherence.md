# 2026-07-09 19h00 - ChapterPlanner : suppression du doublon coherence/problemsToFix

## 1. Évolution demandée

Suite à l'audit `audit_agent_2026-07-09.md`, un doublon avait été identifié dans le prompt de
`ChapterPlanner` : le champ `wc.coherence()` était injecté deux fois dans le prompt sous deux
titres différents ("État de cohérence" et "Problèmes à corriger impérativement"), avec
exactement le même contenu.

Delta validé par l'utilisateur :
- suppression de la section "État de cohérence" (le champ `coherence` ne porte aucun concept
  distinct — vérifié : `WrittenChapter.coherence()` n'est alimenté que par du feedback de
  critiques fusionné, jamais par autre chose) ;
- réordonnancement du prompt : "Plan précédent (à corriger)" présenté **avant** "Problèmes à
  corriger impérativement" (au lieu de l'inverse), pour rapprocher l'instruction actionnable du
  point de génération (effet de récence, pertinent pour les petits modèles ciblés par le
  projet) ;
- déplacement de l'instruction finale ("Corrige chacun des points ci-dessus. Produis le plan
  corrigé au format JSON.") de la section Plan vers la section Problèmes, qui reste toujours la
  dernière avant génération.

## 2. Ce qui a été touché

- `redacteur/.../agent/plan/chapterplanner/ChapterPlanner.java` — `buildUser()` : suppression de
  la section "État de cohérence", réordonnancement Plan précédent → Problèmes à corriger,
  instruction finale déplacée et conditionnée à `isCorrection` (comportement identique à avant
  sur ce point : l'instruction "format JSON" n'apparaît qu'en mode correction).
- `redacteur/.../agent/plan/chapterplanner/ChapterPlannerInput.java` — suppression du champ
  `coherence` (record), javadoc `isRewrite` ajustée.
- `redacteur/.../orchestrator/plan/ChapterPlannerStep.java` — suppression du passage en double
  de `wc.coherence()` (ne reste que le paramètre `problemsToFix`).
- `audit_agent_2026-07-09.md` — constat n°2 et fiche agent `ChapterPlanner` marqués FAIT.

## 3. Résultat

Compilation du module `redacteur` (et dépendances) vérifiée : `mvn -pl redacteur -am compile` →
succès. Aucun autre appelant de `ChapterPlannerInput` dans le projet (vérifié par grep), donc
pas d'impact ailleurs. Comportement fonctionnel inchangé hors suppression du doublon et
réordonnancement — pas de nouveau champ, pas de nouvelle donnée.
