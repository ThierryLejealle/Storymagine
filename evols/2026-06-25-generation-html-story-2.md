# 2026-06-25 - Generation HTML de l'histoire en cours de generation

## Evolution demandee
Reprendre la feature de generation HTML presente dans l'ancien projet Redacteur.
En cours de generation de la story, generer un fichier story.html contenant l'histoire sequence par sequence (chapitre par chapitre).

## Ce qui a ete touche

### Nouveau : redacteur/coeur/ports/HtmlExportPort.java
Port de sortie avec methode exportHtml(String bookTitle, Story story).
Inclut un NOOP statique pour les contextes sans export fichier (tests, surcharges sans FileLog).

### Nouveau : redacteur/infra/HtmlExporter.java
Builder HTML pur (aucun I/O). Reprend le CSS et la structure de l'ancien HtmlExporter.java du projet Redacteur.
Adapte pour travailler directement sur List<WrittenChapter> plutot que sur du markdown.
Genere : titre, sommaire cliquable, sections par chapitre, lien retour en haut.

### Nouveau : redacteur/infra/HtmlFileExportAdapter.java
Implemente HtmlExportPort. Prend un Supplier<Path> (runDir de FileLogAdapter) pour ecrire story.html.
Silencieux en cas d'erreur I/O pour ne pas interrompre la generation.

### Modifie : redacteur/coeur/domaine/orchestrator/StoryOrchestrator.java
Ajout de HtmlExportPort en dependance constructor.
Appel exportHtml() a la fin de chaque chapitre (apres evaluateWorkflow), pour une mise a jour progressive.

### Modifie : redacteur/RedacteurModule.java
Surcharge assemble(OllamaConfig, String, LogPort) supprimee.
Nouvelle surcharge assemble(OllamaConfig, String, LogPort, HtmlExportPort).
Toutes les autres surcharges utilisent HtmlExportPort.NOOP.
StoryOrchestrator construit avec le port HTML en parametre.

### Modifie : redacteur/ui/cli/RedacteurCli.java
Creation d'un HtmlFileExportAdapter(fileLog::runDir) avant l'assemblage.
Passage a la nouvelle surcharge assemble(..., htmlExport).

## Resultat
Pendant la generation, story.html est ecrit/ecrase dans le run directory apres chaque chapitre complete.
Le fichier est autonome (CSS inline), lisible directement dans un navigateur sans serveur.
En cas de crash mid-generation, les chapitres deja generes sont dans le HTML.
