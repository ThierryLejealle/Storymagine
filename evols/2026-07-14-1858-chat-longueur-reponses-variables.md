# 2026-07-14 18h58 - Chat : consigne de longueur de réponse variable selon le poids du moment

## 1. Évolution demandée
L'utilisateur observait que le personnage joué par le LLM répond toujours avec une longueur très
similaire, ce qui manque de naturel : une conversation réelle varie beaucoup en longueur selon le
poids émotionnel du moment (un simple "Oui." peut avoir plus d'impact qu'un paragraphe). Demande
de consulter Fable pour des pistes, puis validation explicite du delta avant modification du
prompt (règle CLAUDE.md sur les modifications de prompt LLM).

Fable a diagnostiqué que la consigne existante ("natural length") ne donne aucun critère
décisionnel et que le modèle s'ancre sur la taille de ses propres répliques précédentes. Il a
proposé une règle à critère explicite (poids du moment, 3 paliers) plus deux exemples wrong/right
(un cas court à fort impact, un cas long de nouvelle scène), en évitant toute duplication
positif/négatif de la même consigne. Delta présenté à l'utilisateur (texte exact avant/après,
raison, objectif) et validé ("GO. Excellent") avant écriture.

## 2. Ce qui a été touché
- `chat/src/main/java/storymagine/chat/coeur/domaine/session/ChatPromptBuilder.java`,
  constante `SYSTEM_INTRO` :
  - Retrait de la phrase "Keep replies vivid but a natural length — this is a back-and-forth
    conversation, not a novel."
  - Ajout d'un bloc "Length: match each reply to the weight of the moment..." (3 paliers courts/
    longs/entre-deux + rappel anti-padding) suivi de deux exemples concrets (domaine générique,
    sans noms de scénario réel, pour éviter toute copie littérale en jeu), insérés juste après le
    paragraphe d'ouverture et avant la note sur la fiche personnage.
- Compilation vérifiée (`mvn -pl chat -am compile`) : OK.

## 3. Résultat
Changement de prompt uniquement (pas de code de logique). Risques identifiés par Fable à
surveiller à l'usage (non traités ici, à observer en session réelle) :
- dérive possible vers des réponses systématiquement ultra-courtes,
- troncature du mode long si `maxTokens` est calé trop bas,
- interaction avec le mirroring déjà combattu dans ce projet (le modèle pourrait se mettre à
  imiter la longueur du message du *joueur* plutôt que la sienne — si observé, étendre la clause
  en "...or the player's message").

Pas de test unitaire dédié (contenu de prompt, pas de logique testable unitairement) ; à valider
par l'utilisateur en session de chat réelle.

## 4. Copie de l'ancien SYSTEM_INTRO (avant toute modification du 2026-07-14)
Fichiers non versionnés dans git (`ChatPromptBuilder.java` était `??`, jamais committé) : pas
d'historique git disponible pour revenir en arrière. Copie intégrale du texte tel qu'il était
avant cette fiche et avant la fiche suivante (`2026-07-14-1937-...`), à titre de sauvegarde :

```java
private static final String SYSTEM_INTRO = """
    Let's roleplay. You are the character described below. Stay in character in every reply:
    speak, act and think as they would. Never speak or act for the player's character, unless
    they hand you the narrator's pen with "DO: " (see below). Keep replies vivid but a natural
    length — this is a back-and-forth conversation, not a novel. Reply in the same language as
    the character and scenario below.

    The character sheet below is a private author's note — your character has never
    read it. Show its traits and motives through behavior only; never quote, name or
    explain them. Example, for a character described as shy — Wrong: "Since I'm shy,
    maybe we could stay here." Right: *she hesitates, eyes dropping to the floor*
    "Maybe... we could stay here?\"""";
```
