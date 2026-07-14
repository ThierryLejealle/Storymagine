# 2026-07-14 10h15 - Chat : paramètre min-P + défauts plus créatifs + annuler sur plusieurs échanges

## Demande

1. "Et min-p ?" puis confirmation que c'est un paramètre pertinent pour du RP — l'utilisateur
   demande son ajout au panneau de réglages, et "regardes une bonne valeur par défaut... plus de
   créativité serait un gros +".
2. "Je peux avoir un bouton pour revenir en arrière sur plusieurs conversations ?" — clarifié via
   questions : annuler plusieurs échanges d'affilée dans la session en cours (pas restaurer une
   session antérieure), avec un petit champ nombre à côté du bouton Annuler.

## Ce qui a été touché

### min-P + défauts créatifs (RoleplayNarrator)
- `commun/coeur/ports/GenerationOptions.java` : ajout du champ `minP` (5e composant du record) ;
  jamais défaulté par l'adaptateur (omis de la requête si absent, laissant Ollama appliquer son
  propre défaut) — seul `RoleplayNarrator` en fixe un.
- `commun/infra/OllamaAdapter.java` : `buildOllamaRequest` ajoute `min_p` aux options Ollama
  quand fourni.
- `chat/coeur/domaine/session/GenerationSettings.java` : ajout du champ `minP` (nullable, non
  persisté, comme les autres réglages de session).
- `chat/coeur/domaine/agent/roleplaynarrator/RoleplayNarrator.java` : nouveaux défauts de
  génération, pensés pour du RP créatif plutôt que les défauts génériques d'Ollama/du reste du
  projet — température **0.9 → 1.0**, top-K **→ 100** (quasi désactivé), top-P **→ 0.98** (quasi
  désactivé, non exposé dans le panneau), min-P **→ 0.05** (fait le vrai travail de troncature,
  seuil relatif à la confiance du modèle plutôt qu'une coupure fixe — cf. discussion : coupe la
  queue incohérente sans brider les cas où le modèle hésite vraiment entre plusieurs suites
  plausibles, contrairement à top_p seul).
- `chat/ui/web/ChatWebServer.java` : `/settings` (POST) lit et applique `minP`.
- `chat.html` : nouveau champ "Min-P" dans le panneau de réglages (placeholder 0.05) ; valeurs
  par défaut affichées côté JS alignées sur les nouveaux défauts (température 1.0).

### Annuler sur plusieurs échanges
- `ChatService.undo` / `ChatServiceImpl.undo` : nouveau paramètre `int steps` — recule du Nième
  tour joueur en partant de la fin (au lieu du seul dernier) ; si moins de `steps` échanges
  existent, recule jusqu'au tout premier tour joueur disponible (clampé, pas d'erreur).
- `ChatWebServer.handleUndo` : lit `steps` en query string (`POST /undo?steps=3`), défaut 1.
- `chat.html` : petit champ nombre à côté du bouton "↩ Annuler" (défaut 1, min 1, max 50).
- Tests `ChatServiceImplTest` : 2 nouveaux cas (recul de N échanges exact ; clamp quand `steps`
  dépasse le nombre d'échanges réellement disponibles).

## Résultat

`mvn clean test` (4 modules) au vert, 96 tests côté chat (+2 pour l'undo multi-pas). Les défauts
de génération du roleplay sont maintenant réglés spécifiquement pour la créativité RP (et non plus
hérités tels quels des réglages génériques d'Ollama), ajustables par session via le panneau ;
l'utilisateur peut reculer de plusieurs échanges en un clic.
