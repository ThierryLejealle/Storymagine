# 2026-08-06 - Réglage de taille de contexte (num_ctx) en session

## Évolution demandée
Ajouter une option pour fixer la taille de contexte du modèle depuis la session de chat (envoyée à
Ollama), en s'appuyant sur le mécanisme de seuils de compaction déjà dynamique (`ChatContextBudget`
recalcule déjà ses seuils à partir de `contextWindow()` à chaque tour, aucun code de seuil à ajouter).
Validé : réglage de session comme temperature/topK, dans `GenerationSettings` + panneau `chat.html`.

## Ce qui a été touché
- `commun/.../ports/GenerationOptions.java` : nouveau champ `contextWindow` (7e composant du record).
  Contrairement aux autres champs (réglages légers par appel), il est documenté comme adopté par
  l'adaptateur comme nouvelle taille de référence dès qu'il est fourni — changer `num_ctx` en cours
  de conversation recharge le modèle côté Ollama, ce n'est pas un réglage "un seul appel".
- `commun/.../infra/OllamaAdapter.java`, `generateInternal` : si `options.contextWindow()` est fourni,
  `contextWindowSize` (le champ interne, déjà mutable pour l'agrandissement automatique sur
  dépassement de contexte) est mis à jour AVANT la boucle d'envoi — `buildOllamaRequest` n'a pas eu
  besoin d'être touché, il lisait déjà ce champ. Les tentatives d'agrandissement automatique
  continuent de fonctionner normalement au-dessus de cette nouvelle base.
- `chat/.../session/GenerationSettings.java` : nouveau champ `contextWindow` (9e composant).
- `chat/.../agent/roleplaynarrator/RoleplayNarrator.java`,
  `agent/npcmindstate/NpcMindStateAnalyst.java`, `agent/nextactreadiness/NextActReadinessAnalyst.java` :
  passent désormais `settings.contextWindow()` à `GenerationOptions` — les trois agents du module
  partagent le même adaptateur, donc le réglage s'applique à toute la session, pas seulement au
  narrateur.
- `chat/.../ui/web/ChatWebServer.java` : `handleSettings` lit/écrit le champ `contextWindow` du
  formulaire, même convention que les autres réglages (vide = défaut serveur).
- `chat/src/main/resources/web/chat.html` : nouveau champ "Taille de contexte (tokens)" dans le
  panneau de réglages, préservé (comme maxTokens/interjectionChance) quand un préréglage
  Sérieux/Fun/Barré est appliqué — les préréglages ne concernent que la saveur d'échantillonnage, pas
  la taille de contexte.
- Tests : `RoleplayNarratorTest` (2 nouveaux tests, passthrough + valeur nulle par défaut) ;
  8 constructions positionnelles de `GenerationSettings` mises à jour dans `ChatServiceImplTest` et
  `RoleplayNarratorTest` (nouveau champ en fin de record).

## Résultat
Suite de tests complète du projet (racine, tous modules) repassée verte après le changement. Aucune
modification de prompt LLM dans cette évolution (uniquement du câblage de configuration).
