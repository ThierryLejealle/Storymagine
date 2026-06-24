# Proofreader

## Rôle
Détecte les fautes de langue dans une séquence et retourne des paires (phrase erronée → phrase corrigée).
Les corrections sont appliquées en Java par la couche service via `String.replace()` — pas de second appel LLM.

## Fautes détectées
- Grammaire, orthographe, accord, conjugaison
- Mots manquants ou sémantiquement inappropriés
- Phrases bancales : calques, pléonasmes, syntaxe confuse

## Format de sortie
```
CORRECTIONS:
- FAUX: "phrase exacte contenant le problème"
  JUSTE: "phrase corrigée"
```
Ou `PAS D'ERREUR` si aucune faute.

## Contraintes
- Une seule phrase corrigée par JUSTE: (pas de variantes)
- Le texte est tronqué à `contextWindow/3` chars

## Application des corrections
Le service applique chaque correction avec `text.replace(wrongSentence, correctSentence)`.
Si la phrase n'est plus trouvée dans le texte (LLM a paraphrasé), la correction est ignorée silencieusement.

## Source Redacteur
`story.context.ProofreaderContext`
