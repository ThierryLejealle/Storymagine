# 2026-06-27 16h00 - ProofreaderCorrector : connecteur logique contradictoire

## Evolution demandée
Ajouter la détection des connecteurs logiques contradictoires : une phrase dont les deux
parties sont liées par "bien que", "parce que", "donc", "pourtant"… mais dont le connecteur
contredit le sens réel.

Exemple déclencheur : "Il se sentait exposé dans son intimité, bien qu'ils fussent en public."
→ être en public renforce l'exposition, il ne la contredit pas. "bien que" est faux.

## Ce qui a été touché
- `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/writer/proofreadercorrector/ProofreaderCorrector.java`
  - Ajout d'un bullet "connecteur logique contradictoire" avec exemple
  - Ajout d'un exemple correspondant dans le bloc CORRECTIONS du format

## Résultat
Le modèle est guidé pour détecter les incohérences de connecteur logique intra-phrase.
Risque : raisonnement sémantique fin, potentiellement difficile pour les petits modèles.
A observer sur les prochaines générations.
