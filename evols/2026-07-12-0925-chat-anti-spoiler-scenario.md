# 2026-07-12 09h25 — Chat : consigne anti-spoiler du scénario

## 1. Demande

Le prompt système du chat ne disait rien sur le fait que le SCENARIO fourni contient des
événements scriptés à venir. Risque : le personnage "sait" à l'avance ce qui va se passer et
anticipe (émotions, réactions) avant que l'événement n'arrive réellement dans la conversation —
ça casse l'immersion.

Intention précisée par l'utilisateur : ne jamais utiliser de contenu du scénario qui n'est pas
encore arrivé, ne jamais anticiper un événement ou une émotion scriptée (ex : une colère prévue
plus tard dans le scénario), mais garder toute liberté d'improvisation et d'invention dans le
présent de la scène — l'interdit porte uniquement sur l'anticipation/le spoil, pas sur la
créativité.

Consigne demandée : soumettre la question à Fable (prompt système + explication uniquement,
aucune lecture de fichier), présenter son avis, puis valider le delta avant écriture (règle
CLAUDE.md sur la modification de prompt LLM).

## 2. Ce qui a été touché

### Fable — consultation
Appel `Agent` (model: fable, run_in_background, aucun outil d'exploration autorisé), coût
~23,4k tokens. Fable propose de formuler la contrainte comme un état de connaissance du
personnage plutôt qu'une liste d'interdits, avec un exemple unique couvrant à la fois
l'anticipation d'un événement et d'une émotion scriptée.

### `ChatPromptBuilder.java` — bloc SCENARIO

Avant :
```
SCENARIO (your background knowledge — reveal it gradually as the story unfolds, never all at
once or ahead of what has actually happened):
{scenario}
```

Après :
```
SCENARIO (this may describe events scripted to happen later, including how you will react
to them — for your character, that future does not exist yet. You learn of a scripted event
only when it actually happens in the chat: until then, never hint at it, prepare for it, or
feel anything about it. Example: if the scenario says you will get angry when the letter
arrives, you are calm and unsuspecting right up until the letter actually arrives. You are
free to invent new details and improvise in the present moment — just never reach ahead of
it):
{scenario}
```

Validé explicitement par l'utilisateur avant écriture.

## 3. Résultat

`mvn -q -pl chat -am test` : OK, tests toujours verts (aucun test ne verrouille le texte exact
du system prompt, seulement sa structure). Non encore re-testé en usage réel à la rédaction de
cette fiche.
