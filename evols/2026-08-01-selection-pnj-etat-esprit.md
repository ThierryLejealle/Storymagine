# 2026-08-01 - Sélection du PNJ pour l'état d'esprit

## Évolution demandée
En multi-PNJ, le bouton 🧠 "État d'esprit" appelait toujours `representativeScene(session).speaker()`
côté service — le premier PNJ présent, choisi arbitrairement, sans que l'utilisateur puisse
choisir lequel analyser. La pop-up n'affichait pas non plus le nom du personnage analysé.

## Ce qui a été touché
- `ChatService`/`ChatServiceImpl.analyzeMindState` : prend désormais un `npcId` en paramètre,
  résout le `Npc` par id dans `session.scenario().cast()` (au lieu de `representativeScene`), et
  lève `IllegalStateException` si le PNJ est inconnu ou absent de la scène.
- `NpcMindStateAnalystInput` : javadoc mise à jour (ne référence plus `representativeScene`).
- `ChatWebServer` : `/analyze-mind-state` lit `npcId` en query string (même pattern que `/retry`).
- `chat.html` : le bouton 🧠 ouvre désormais un petit menu listant les PNJ présents (`#npcPicker`) ;
  le choix ferme le menu et lance l'analyse pour ce PNJ. Le titre de la pop-up devient
  "État d'esprit de {nom}".
- `ChatServiceImplTest` : test existant adapté au nouveau paramètre, nouveau test pour le
  garde-fou PNJ inconnu/absent.

## Résultat
Tests du module `chat` verts (`mvn -pl chat -am test`). En multi-PNJ, l'utilisateur choisit
explicitement quel personnage analyser et voit son nom dans la pop-up.
