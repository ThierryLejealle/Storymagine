# RepetitionFilter

## Rôle
Filtre les candidats à bannir (produits par RepetitionTracker) en retirant ceux qui sont
sémantiquement proches d'un leitmotiv intentionnellement récurrent (protégé dans `keep_phrases.md`).

## Politique de doute
En cas de doute, l'expression est **protégée** (non retournée dans les bannis).
On préfère autoriser quelques répétitions plutôt qu'interdire un leitmotiv par erreur.

## Bypass
Si `candidates` est vide ou `keepPhrasesContent` est vide, les candidats sont retournés inchangés
sans appel LLM.

## Format de sortie
```
- expression à bannir 1
- expression à bannir 2
```
(uniquement les expressions à bannir après exclusion des leitmotivs)

## Source Redacteur
`story.context.RepetitionFilterContext`
