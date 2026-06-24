# 2026-06-23 19h00 — InitStoryService + template story/modele

## Évolution demandée
Migrer depuis ../Redacteur la fonctionnalité d'initialisation d'une nouvelle histoire :
créer un `InitStoryService` dans le coeur et un `init-story.bat` qui copie le répertoire
modèle vers un nouveau répertoire histoire.

## Ce qui a été créé

### Coeur — service
- `redacteur/coeur/service/InitStoryCommand.java` — record : storyRoot, bookName, overwrite
- `redacteur/coeur/service/InitStoryResult.java` — record : bookDir
- `redacteur/coeur/service/InitStoryService.java` — interface
- `redacteur/coeur/service/InitStoryServiceImpl.java` — validation + délégation au port

### Coeur — port
- `redacteur/coeur/ports/StoryTemplatePort.java` — directoryExists + copyTemplate

### Infra
- `redacteur/infra/StoryTemplateFileAdapter.java` — copie récursive de story/modele/ avec Files.walk

### CLI
- `redacteur/ui/cli/InitStoryCli.java` — toute l'interaction interactive (Scanner)
  - Demande le répertoire racine (défaut C:\dev\llm\story)
  - Demande le nom de l'histoire
  - Vérifie l'existence + demande confirmation écrasement
  - Appelle le service et affiche le résultat

### Template
- `story/modele/` — 13 fichiers copiés/adaptés depuis ../Redacteur/story/modele/
  - scenario.md, goal.md, style.md, constraints.md, lore.md, focus.md,
    checks.md, quality.md, example.md, keep_phrases.md, context_events.md
  - characters/README.md, characters/personnage_exemple.md
  - chapitres/chap_1.yaml (imperative), chapitres/chap_2.yaml (interlude)
  - actions/actions_exemple.md
  - lore.md : exemple spécifique à Redacteur retiré, structure documentaire conservée

### Bat
- `init-story.bat` — stupide : résout le chemin du template (`%~dp0story\modele`),
  appelle mvn exec:java avec `-Dexec.mainClass=InitStoryCli` et `-Dinit.template.dir`

## Résultat
- Compilation OK, BUILD SUCCESS.
- `RedacteurModule` non modifié (InitStoryService s'assemble sans Ollama directement dans la CLI).
