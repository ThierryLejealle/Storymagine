# StateExtractor

## Rôle
Extrait les changements d'état des entités physiques après une séquence.
Appelé après chaque écriture de séquence. Sa sortie est accumulée dans la mémoire narrative
et injectée comme snapshot "état actuel" dans la séquence suivante.

## Format de sortie
```
ETAT: [entité] → [état actuel]
EVENT: [événement important pour la continuité]
```
Ou exactement `AUCUN` si aucun changement notable.

## Contraintes
- Maximum 5 lignes
- Entités ciblées : personnages, véhicules, objets clés
- Texte tronqué à 1 200 caractères
- Pas de commentaires

## `previousState` (entrée optionnelle)
Si fourni, injecté avant le texte de séquence pour éviter que le LLM re-détecte de faux changements.

## Source Redacteur
`story.context.StateExtractorContext.extract`
Note : `StateExtractorContext.summarizeIteration` (résumé pour journal de boucle) est exclu —
uniquement utilisé par LoopOrchestrator, non implémenté dans Storymagine.
