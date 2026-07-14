# Module chat

Console de roleplay avec un LLM local (Ollama) : le joueur discute avec un personnage tenu par le
LLM, dans un décor fixé par un "chatscenario". Contrairement à `redacteur`, pas de plan ni de
chapitres — juste une conversation qui continue tant qu'on veut, avec compaction automatique de
l'historique pour ne jamais saturer le contexte du modèle.

# Structure d'un chatscenario

```
chatscenarios/{nom}/
  character.txt   — fiche du personnage tenu par le LLM, texte libre
  scenario.txt    — prémisse/décor de la conversation, texte libre
  history.md       — historique persisté (généré, ne pas éditer à la main pendant une session)
  summary.md        — résumé compacté courant (généré)
```

`character.txt` et `scenario.txt` sont statiques : jamais réécrits, jamais compactés. Ils sont
injectés en entier dans chaque appel au LLM. `history.md`/`summary.md` sont réécrits après chaque
tour et supprimés si l'utilisateur choisit de remettre la conversation à zéro au lancement.

Un chatscenario d'exemple est fourni : `chatscenarios/temple-noir/`.

# Conventions de formatage du chat

Ces 4 règles sont injectées dans chaque prompt (voir `ChatPromptBuilder`) — le joueur les utilise,
le LLM doit les respecter pour son personnage :

- `*texte*` — description d'une action physique ou d'un détail de scène, jamais une réplique
  parlée.
- Pensées privées écrites entre `*astérisques*` par un personnage — l'autre personnage n'est
  jamais censé les connaître (pas de metagaming), sauf si elles sont dites à voix haute ou
  visibles physiquement.
- `OOC: ...` — le joueur sort du jeu pour donner une consigne ou une note directe au LLM (sert
  aussi à négocier/planifier une action future avant de la jouer en scène).
- `DO: ...` — le joueur confie la plume du narrateur au LLM pour cette seule réponse : la scène
  peut avancer, d'autres personnages peuvent agir, avant de reprendre le roleplay normal au tour
  suivant.

# Appel LLM

Le tour de roleplay ET `ChatSummarizer` passent tous les deux par `ModelCallPort` (`/api/chat`,
rôles system + user) — même port, même instance `OllamaAdapter`, câblés une seule fois dans
`ChatModule`. `ChatPromptBuilder.build()` renvoie un `ChatPrompt(system, user)` : le system porte
le cadre fixe (règles, fiche personnage, scénario, résumé), le user porte l'échange récent et la
nouvelle ligne du joueur.

Le mode brut (`RawCompletionPort`, `/api/generate`, `raw:true`) a été essayé en premier puis
abandonné le 2026-07-11 : il cassait dès la première réponse (nom du personnage répété puis
dérive en répétition d'un seul mot) parce qu'il retire l'échafaudage de template de chat que le
modèle instruction-tuned a appris à l'entraînement — confirmé par un test manuel direct dans
Ollama (qui passe par `/api/chat`) qui fonctionnait parfaitement. `RawCompletionPort` reste
disponible dans `commun` comme capacité générale, simplement inutilisé par `chat` aujourd'hui.

Consigne de langue : le system prompt et `ChatSummarizer` demandent tous les deux de répondre
"dans la même langue que le personnage/scénario/les tours" plutôt que de forcer une langue fixe
dans le code — le contenu réel (fiche personnage, scénario) fait foi.

# Compaction de l'historique

Déclenchée dans `ChatServiceImpl` quand le nombre de mots du transcript non compacté dépasse
`SummaryBudget.wordBudget(contextWindow)` **et** qu'il reste plus de `KEEP_RECENT_TURNS` (4) tours
en mémoire. Les tours les plus anciens sont foldés dans `summary` par `ChatSummarizer`, les 4
derniers tours restent verbatim pour la continuité immédiate. Une seule passe de compaction — pas
de compression supplémentaire du résumé comme `SummaryCompressor` dans redacteur : le résumé du
chat reste une prose libre qui s'auto-régule tour après tour (les faits obsolètes sont remplacés,
pas accumulés).

# Interface web

`ChatCli` démarre un serveur HTTP local minimal (`ChatWebServer`, JDK `HttpServer`, pas de
framework) qui sert une page statique (`chat.html`, vanilla JS/CSS) et deux endpoints JSON :
`GET /history` et `POST /message` (corps = texte brut tapé par le joueur). Un seul thread traite
les requêtes — la session n'est pas conçue pour un accès concurrent.
