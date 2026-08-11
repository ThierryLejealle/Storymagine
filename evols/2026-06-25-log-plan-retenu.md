# 2026-06-25 - Log du plan retenu apres retries

## Evolution demandee
Dans le workflow plan, indiquer dans les logs/console quel plan a ete retenu (le meilleur selon la moyenne des critiques) apres plusieurs tentatives. Verifier aussi que :
- sur les retries suivants, c'est bien le plan precedent (et non le premier) qui est passe
- seules les critiques de CE plan sont passees (pas la somme de toutes les critiques)

## Resultat de l'analyse
- **Plan passe au retry suivant** : CORRECT -- wc.setPlan() est mis a jour a chaque tentative (ligne 61), donc la tentative N+1 recoit toujours le plan de la tentative N.
- **Critiques passees** : CORRECT -- wc.setCoherence() remplace (ne cumule pas) les problemes a chaque iteration.
- **Log du plan retenu** : MANQUANT -- rien n'etait logue pour indiquer quelle tentative avait ete retenue.

## Ce qui a ete touche

### LogPort.java (commun)
- Ajout de la methode default void planRetained(int bestAttempt, int totalAttempts, double bestScore) (no-op par defaut)

### ConsoleLogAdapter.java (commun/infra)
- Surcharge de planRetained() : affiche '-> plan retenu : tentative X/Y  moy Z.ZZ'

### FileLogAdapter.java (commun/infra)
- Surcharge de planRetained() : ecrit la meme ligne dans master-log.txt

### TeeLogAdapter.java (commun/infra)
- Surcharge de planRetained() : forward aux delegates

### PlanWorkflow.java (redacteur)
- Ajout de bestAttempt (1-based, mis a jour quand un meilleur score est trouve)
- Ajout de finalAttempt (nombre de tentatives effectivement executees)
- Appel de log.planRetained(bestAttempt, finalAttempt, bestScore) apres la boucle, uniquement si finalAttempt > 1
