# 2026-06-27 15h45 - ProofreaderCorrector : correction pronom de genre incohérent

## Evolution demandée
Le ProofreaderCorrector ne détectait pas les erreurs de genre de pronom déductibles
du passage lui-même. Exemple : "Il se figea… l'odeur la frappa" — le personnage est
désigné par "il" puis par "la" dans la même séquence, l'un des deux est faux.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/writer/proofreadercorrector/ProofreaderCorrector.java`
  Ajout d'une précision sur le bullet "fautes d'accord" pour couvrir explicitement
  le genre d'un pronom incohérent avec le reste du passage, avec un exemple concret.

## Résultat
Le modèle est maintenant guidé pour détecter les incohérences de genre intra-passage.
Aucun ajout d'input (fiches personnages) n'est nécessaire : le genre est déductible
du texte reçu par l'agent.
