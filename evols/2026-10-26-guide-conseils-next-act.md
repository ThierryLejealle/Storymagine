# 2026-10-26 - Guide de rédaction chat : spec complétée, section Conseils et avertissements, relecture

## 1. Évolution demandée

- Section de conseils utilisable par un petit LLM : bien gérer `[NEXT ACT]` (scénario bloqué, passage
  trop rapide, séquences qui se ressemblent comme plusieurs combats), écrire un long scénario avec un
  petit contexte (tableau d'actes temporaire, plusieurs `scenario-N.txt`).
- Documenter ce qui manquait : structure `#`/`##` et texte commun hérité, ce que voit chaque PNJ
  (anti-spoil, budget de contexte), le PNJ qui joue décide du `[NEXT ACT]`, `OOC:`/`DO:`, pièges de
  génération tirés des évolutions précédentes, principe « fiches et événements, pas un script ».
- Séparer la spec pure des conseils. Aucune référence à des scénarios existants ni aux fiches
  `evols/` dans le guide (il peut être partagé seul). Règle de maintenance dans le CLAUDE.md.
- Relecture : plus complet, plus précis, plus synthétique.

## 2. Ce qui a été touché

- `chatscenarios/guide-redaction-chat.md`, réécriture de §1 à §4.9 : tableaux à la place des
  paragraphes, historique des corrections retiré, règle de 3e personne regroupée en §2.2.
  Spec ajoutée : §2.3 (règles du parseur, texte hérité), §2.4 (blocs `[...]` multi-lignes),
  §2.5 (déclenchement du mode strict), §2.7 (ce que voit chaque PNJ), §4.5 (qui décide du
  `[NEXT ACT]`), §4.9 (`*…*`, `OOC:`, `DO:`). §5 et §6 inchangés.
- Même fichier, §7 « Conseils et avertissements » : 7.0 principe, 7.1 budget et anti-spoil, 7.2
  conditions `[NEXT ACT]`, 7.3 long scénario en petit contexte, 7.4 pièges connus.
- `.claude/skills/chat-scenario-writing/SKILL.md` : renvoi vers §2.3, §2.7, §7.
- `CLAUDE.md` du projet : règle de maintenance du guide.
- Faits vérifiés dans `ScenarioOutlineParser`, `ChatPromptBuilder`, `ChatFileStorageAdapter`.
  Aucun code ni prompt modifié.

## 3. Résultat

Documentation seule, aucune exécution. Spec et conseils sont séparés ; les conseils renvoient à la
spec par numéros de section.
