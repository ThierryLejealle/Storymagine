# 2026-06-24 13h31 - Fix : contraintes de séquence ignorées par ChapterPlanner

## Evolution demandée
Les contraintes définies au niveau d'une séquence (champ `constraints` dans le YAML) n'étaient pas transmises au ChapterPlanner. Exemple : séquence 1 de `une-rencontre/chapitres/chap_1.yaml` avait `constraints: Pas de contact, pas de discussion, aucun regard direct entre les personnages.` — ignorée silencieusement.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/orchestrator/common/ScenarioFormatters.java`
  - Méthode `sequenceDescriptions` : ajout de la ligne `Contraintes : <texte>` dans la description assemblée par séquence, après focus/lore/characters.

## Vérification effectuée
- Characters (focus, lore, characters) étaient déjà présents dans `sequenceDescriptions` — confirmé en relecture.
- Seules les contraintes manquaient.

## Résultat
Le ChapterPlanner reçoit désormais les contraintes par séquence dans la section "Séquences à couvrir", au même titre que le focus, le lore et les personnages.
