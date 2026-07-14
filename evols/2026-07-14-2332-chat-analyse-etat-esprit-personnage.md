# 2026-07-14 23h32 - Chat : bouton d'analyse de l'état d'esprit du personnage

## 1. Évolution demandée
Avant de démarrer la grosse évolution multi-PNJ (planifiée mais pas encore codée), l'utilisateur
a demandé un bouton supplémentaire à côté de 🔍 (NEXT ACT) : un bouton 🧠 qui interroge le LLM
sur l'état d'esprit du personnage (situation actuelle vue de son côté, pensées privées, plans),
dans une pop-up, sans polluer l'historique — "Comme tu as fait la loop pour NEXT ACT" (même
mécanique que `NextActReadinessAnalyst`).

Delta de prompt présenté et validé avant écriture (comme d'habitude). Mêmes décisions de
conception que l'agent précédent, reprises sans re-débattre : voix auteur (pas le personnage),
mêmes réglages de génération que `RoleplayNarrator` (température 1.0, pas de mode analytique
froid), format à sections fixes avec sentinelle `[RIEN]`.

## 2. Ce qui a été touché
- Nouveau package `chat/coeur/domaine/agent/npcmindstate/` : `NpcMindStateAnalyst`,
  `NpcMindStateAnalystInput`/`Output`, `NpcMindStatePromptBuilder` (prompt système dédié, format
  `SITUATION:`/`THOUGHTS:`/`PLANS:`), `NpcMindStateAnalyst.md` (renvoie vers le `.md` de
  `NextActReadinessAnalyst` pour la partie du raisonnement commune, plutôt que de la dupliquer).
- `ChatService`/`ChatServiceImpl` : `analyzeMindState(...)`, lecture seule, aucune précondition
  (contrairement à `analyzeNextActReadiness`, toujours disponible).
- `ChatModule` : câblage du nouvel agent.
- `ChatWebServer` : `POST /analyze-mind-state`.
- `chat.html` : bouton 🧠 à côté de 🔍 dans la barre d'actes (toujours actif). La pop-up modale
  existante a été généralisée (`runAnalysis(title, endpoint, sections)`) pour être partagée entre
  les deux analyses au lieu de dupliquer le HTML/CSS d'une seconde modale — titre dynamique
  (`#analysisTitle`), sections paramétrées par tableau `[label, champ JSON, texte de repli]`.
- Constructeur `ChatServiceImpl` : nouveau paramètre `NpcMindStateAnalyst`, tous les sites
  d'appel mis à jour (module + 7 sites de test).
- Tests : `NpcMindStateAnalystTest` (parsing, sentinelle vide sur `PLANS`, contenu du prompt),
  `ChatServiceImplTest` (délégation, lecture seule). 123 tests verts après coup.

## 3. Résultat
`mvn clean test` vert sur tout le projet. Aperçu visuel de la pop-up vérifié dans le navigateur
(fichier mocké dans le scratchpad, jamais commité). À valider par l'utilisateur en session réelle
avec gemma4.

## 4. Suite
La grosse évolution multi-PNJ planifiée juste avant (voir échange précédent, pas encore de fiche
dédiée) reste à démarrer : `Npc`/`Cast` prédéfinis (un fichier `.txt` par personnage dans le
dossier scénario, `scenario.txt` excepté, rétrocompatible avec `character.txt`), `SpeakerSelector`
(détection de mention + repli en ordre alphabétique dérivé de l'historique), sans ajout/suppression
de PNJ en session (décidé simplifié). Séance 1 = mécanique cœur sans IHM, séance 2 = vignettes.
