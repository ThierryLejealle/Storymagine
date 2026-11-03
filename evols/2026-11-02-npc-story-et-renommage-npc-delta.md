# 2026-11-02 - Sections de fiche `## tag` avec `{npc-story}`, et `{delta-pnj}` devenu `{npc-delta}`

## Évolution demandée
1. **Sections de fiche activables** : dans une fiche personnage, des sections `## tag` dont le texte n'entre
   dans la fiche qu'après un `{npc-story on nom tag}` et en sort avec `off`. Décisions prises avec l'auteur :
   - Le **premier mot** du titre `##` est le tag, le reste de la ligne est un commentaire non envoyé.
   - La visibilité vient de la **place** : un `##` avant `# SECRET` est public une fois actif, après il est secret.
   - Le texte hors tag, dans chaque zone, est **permanent** : `# SECRET` n'est pas un tag, on n'y touche pas.
     Les fiches existantes se lisent à l'identique (aucune ne contenait de `##`).
   - Un secret qui devient public = deux tags (un `off`, un `on`), au texte différent.
   - **L'état se prolonge** : une commande vaut pour tous les actes suivants, même hors du titre qui la porte,
     jusqu'à un autre `on` ou `off` (durée de `{npc-delta}`, pas celle de `{npc-mode}`). Au départ, aucun tag actif.
   - Le tag est le dernier mot de la commande (le nom du personnage peut avoir plusieurs mots).
2. **Nom homogène** : `{delta-pnj}` est renommé `{npc-delta}` (préfixe `npc-` pour toute commande sur un
   personnage). L'ancien nom est refusé avec un message qui donne le nouveau.
3. **Documentation du petit delta** : ce qu'il sait faire (ajouter, jamais retirer, texte paramétrable par un
   tirage, fiche partagée complétée sans modification) et quand préférer un tag.

## Ce qui a été touché
- Domaine (`scenario`) : `StoryTag`, `StoryDirective`, `StoryDirectiveMarker`, `NpcSheetParser`, `StoryResolver`
  (nouveaux) ; `Npc` (+ `tags`, `hasTag`, `withActiveTags`, les deltas conservent les tags), `ScenarioAct`
  (+ `stories`), `ChatScenario` (`sheetAt` applique les tags puis les deltas ; tag ou personnage inconnu refusé),
  `LegacyCommandSyntax` (refus de `{delta-pnj}`), `CharacterDeltaMarker` (nouveau nom).
- Infra : `ChatFileStorageAdapter` (la fiche est lue par `NpcSheetParser` ; une fiche invalide est rapportée par
  `validateScenario` au lieu de lever), `ScenarioOutlineParser` (extrait les `{npc-story}`, refusés dans la
  prémisse), `ScenarioScaffolder` (gabarit de fiche : mention des tags).
- Validation : `ScenarioValidator` (`npc-story`, ancien nom, avertissements « tag jamais activé » et « off sans effet »).
- Démos : `demo-npc-story` (le voisin louche) ; `demo-delta-pnj` renommé `demo-npc-delta`.
- Tests : `NpcSheetParserTest`, `StoryDirectiveMarkerTest`, `NpcStoryScenarioLoadingTest` (fichiers, durée,
  retour arrière, tag public lu par les autres et tag secret jamais), `NpcTest`, `ScenarioValidatorTest`,
  `DemoScenariosTest` (12 démos) et les tests de `npc-delta` renommés.
- Documentation : guide §2.8 (petit delta), §2.8 bis (nouveau), §2.9, §3, tableau des titres ; skill
  `chat-scenario-writing` ; `specs/chat.md` (section « Sections de fiche ») ; `specs/todo.md` ; index des démos.
- Aucun prompt d'agent modifié : le texte d'un tag actif s'ajoute à la fiche comme si l'auteur l'y avait écrit.

## Résultat
533 tests verts (508 + 25). Sauvegarde, retour arrière, « Précédent » et ⟳ n'ont rien à stocker : l'état des tags
est recalculé depuis le numéro d'acte.
**Non vérifié à la main** : le comportement d'un vrai LLM sur un changement de ton quand un tag est remplacé par
un autre (`demo-npc-story`), et la lisibilité des messages d'erreur dans l'interface.
**Limite connue** : la visibilité d'un tag dépend de sa place par rapport à `# SECRET`, sans signal si l'auteur
déplace un bloc de l'autre côté. Le récapitulatif « tags publics / tags secrets » par fiche prévu au plan n'est
pas fait (le rapport de validation n'a pas de niveau d'information) ; noté dans `specs/todo.md`.
**Changement de comportement** : un scénario qui contient `{delta-pnj …}` est refusé au chargement tant que la
commande n'est pas renommée en `{npc-delta …}`.
