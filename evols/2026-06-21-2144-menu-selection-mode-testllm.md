# 2026-06-21 21h44 - Menu de sélection du mode dans test-llm

## Description de l'évolution

Ajout d'un menu interactif au lancement de `test-llm.bat` pour choisir entre les trois modes
de bench avant de lancer quoi que ce soit. Auparavant, l'absence d'argument lançait directement
la sélection d'un modèle spécifique.

## Ce qui a été touché

- `test-llm.bat` : valeur par défaut changée de `choisir` à `menu`
- `testllm/src/main/java/storymagine/testllm/ui/cli/TestLlmCli.java` :
  - Mode par défaut passé de `"choisir"` à `"menu"`
  - Ajout d'un bloc interactif (try-with-resources Scanner) qui :
    1. Affiche le menu 3 options si mode = `menu`
    2. Demande le modèle spécifique uniquement si le mode résolu = `choisir`
  - Le switch principal porte sur `modeEffectif` (résolu) plutôt que sur `mode` (brut)
  - Les modes directs `tout`, `favoris`, `choisir` restent utilisables en argument CLI

## Résultat

Au lancement sans argument :
```
Que voulez-vous tester ?
  1. Tous les modeles Ollama
  2. Favoris (configures dans testllm.properties)
  3. Choisir un modele specifique
Choix [1-3] :
```
La liste des modèles disponibles n'apparaît que si l'utilisateur choisit l'option 3.
