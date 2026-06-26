# 2026-06-26 14h30 - Synthese critique Writer dans les logs

## Evolution demandee
Le workflow Plan affichait deja une jolie synthese des critiques (moyenne, RETRY/PASS, tentative retenue).
Le workflow Writer n'avait rien de tout ca apres ses critiques de Phase 3 (sequence) ni apres les critiques chapitre.

## Ce qui a ete touche

### commun/coeur/ports/LogPort.java
- +2 methodes default : sequenceRetained et chapterRetained

### commun/infra/ConsoleLogAdapter.java
- Override sequenceRetained : affiche -> sequence retenue : passe X/N  moy Y.YY
- Override chapterRetained  : affiche -> chapitre retenu : passe X/N  moy Y.YY

### commun/infra/FileLogAdapter.java
- Meme chose pour le log fichier.

### redacteur/.../orchestrator/write/WriteWorkflow.java
- runSequenceCritique : ajout scoresSummary apres avg, tracking bestPass/finalPasses, appel sequenceRetained si > 1 passe.
- run (chapitre) : tracking bestChapPass/finalChapPasses, appel chapterRetained si > 1 passe.

## Resultat
Les logs Writer affichent maintenant la meme synthese que Plan apres chaque groupe de critiques.
