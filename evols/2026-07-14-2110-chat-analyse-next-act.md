# 2026-07-14 21h10 - Chat : bouton d'analyse de la condition NEXT ACT

## 1. Évolution demandée
L'utilisateur analysait déjà où en était la condition de passage à l'acte suivant en tapant
"OOC: ..." au personnage, mais ça polluait l'historique et le chat. Demande : un petit bouton à
côté de "Suivant" qui interroge le LLM (mêmes scénario/fiche/résumé/tours récents que d'habitude)
avec un prompt dédié centré sur l'analyse de la condition, affiché dans une pop-up, jamais écrit
dans l'historique.

Plan présenté et validé avant écriture (agent séparé, prompt dédié, endpoint en lecture seule).
Un point a été révisé après la première proposition : la température devait initialement être
basse (0.3, mode "analytique", comme les agents `Reviewer`) — l'utilisateur a fait remarquer que
le but est de voir ce que *ce* modèle avec *ces* réglages ferait réellement en jeu, pas un
jugement froid et plus "raisonnable" qui risquerait de masquer une vraie confusion du modèle.
Revu pour reprendre exactement les réglages de `RoleplayNarrator` (température 1.0 et les 4 autres
défauts), lus depuis `GenerationSettings` de la session comme le fait le tour de roleplay normal.

## 2. Ce qui a été touché
- Nouveau package `chat/coeur/domaine/agent/nextactreadiness/` :
  `NextActReadinessAnalyst` (agent, mêmes 5 défauts de sampling que `RoleplayNarrator`),
  `NextActReadinessAnalystInput`/`Output`, `NextActReadinessPromptBuilder` (prompt système dédié,
  voix auteur pas personnage, format `CONDITION:`/`STATE:`/`MISSING:` avec sentinelle `[RIEN]`,
  parsing inline — pas `ReviewOutputParser`, format différent), `NextActReadinessAnalyst.md`.
- `ChatService`/`ChatServiceImpl` : `analyzeNextActReadiness(...)`, lecture seule (aucun tour
  ajouté, rien persisté), lève `IllegalStateException` s'il n'y a pas d'acte suivant.
- `ChatModule` : câblage du nouvel agent (même `ModelCallPort` que le reste).
- `ChatWebServer` : `POST /analyze-next-act`.
- `chat.html` : bouton 🔍 à côté de "Suivant" (même condition d'activation), pop-up modale avec
  3 sections, jamais ajoutée à l'historique.
- Tests : `NextActReadinessAnalystTest` (parsing, sentinelle vide, contenu du prompt envoyé),
  `ChatServiceImplTest` (délégation + garde-fou pas d'acte suivant). 106 tests verts après coup.

## 3. Résultat
`mvn -pl chat -am test` vert. Aperçu visuel de la pop-up vérifié dans le navigateur (fichier mocké
dans le scratchpad, jamais commité). À valider par l'utilisateur en session réelle avec gemma4 —
en particulier si le format `CONDITION:`/`STATE:`/`MISSING:` est bien suivi par un petit modèle en
usage réel (les agents `Reviewer` existants suivent un format proche avec succès, bon signe).
