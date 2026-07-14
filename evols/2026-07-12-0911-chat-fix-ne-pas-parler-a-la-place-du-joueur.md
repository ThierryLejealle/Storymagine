# 2026-07-12 09h11 — Chat : remettre "ne pas parler à la place du joueur", sauf via DO:

## 1. Demande

En usage réel, le LLM a répondu/agi à la place du personnage du joueur sans que celui-ci n'utilise
`DO:`. C'est la règle que Fable avait proposée initialement ("Never speak or act for the player's
character") et que j'avais retirée sur demande de l'utilisateur (`evols/2026-07-11-2356-...`),
son test manuel dans Ollama n'en ayant pas eu besoin. En usage réel dans l'app, sans cette règle,
le modèle a fini par dépasser cette limite.

## 2. Ce qui a été touché

### `ChatPromptBuilder.java` — `SYSTEM_INTRO`
Ligne réintroduite, mais correctement scopée cette fois (contrairement à la version Fable
d'origine, qui était une interdiction absolue en contradiction avec `DO:`) :

Avant :
```
Let's roleplay. You are the character described below. Stay in character in every reply:
speak, act and think as they would. Keep replies vivid but a natural length — ...
```

Après :
```
Let's roleplay. You are the character described below. Stay in character in every reply:
speak, act and think as they would. Never speak or act for the player's character, unless
they hand you the narrator's pen with "DO: " (see below). Keep replies vivid but a natural
length — ...
```

La règle renvoie vers le bloc `DO:` de `SYSTEM_FORMATTING` ("see below") plutôt que de
redupliquer sa mécanique — cohérent avec la règle projet anti-duplication de consignes.

## 3. Résultat

`mvn test -pl chat -am` : OK, tests toujours verts (aucun test ne verrouille le texte exact du
system prompt, seulement sa structure). Non encore re-testé en usage réel à la rédaction de cette
fiche.
