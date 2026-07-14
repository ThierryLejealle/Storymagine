# 2026-07-12 18h02 — Chat : story beats "[...]" persistés comme tours NARRATOR

## 1. Demande

Suite à une discussion sur l'affichage d'une phrase depuis le scénario/les fiches lors d'un
changement de fiche (acte), l'utilisateur a fait évoluer le besoin en cours de conversation :
- Formalisme validé : une ligne `[...]`, seule sur sa ligne, dans le scénario ou un acte.
- Plusieurs lignes `[...]` possibles par fiche, toutes affichées (pas seulement la première).
- Elles doivent aller dans l'historique de la conversation (pas juste une bannière éphémère), pour
  survivre à la disparition de la fiche source du prompt une fois qu'on est passé à l'acte suivant
  (seul l'acte courant est envoyé au LLM). Confirmé explicitement : elles sont aussi compactées par
  `ChatSummarizer` comme n'importe quel tour, une fois hors fenêtre récente.
- Contrairement à `[NEXT ACT]` (marqueur émis par le LLM, jamais affiché ni mémorisé, retiré de la
  réponse), ces lignes sont écrites par l'auteur, jamais retirées du texte envoyé au LLM.

Demande complémentaire dans la même séance : peupler les deux scénarios de démo existants
(`temple-noir`, `temple-noir-actes`) avec ce formalisme.

## 2. Ce qui a été touché

### Domaine (`chat/coeur/domaine`)
- `Teaser` (nouveau, `chat/coeur/domaine/scenario`) — `extractAll(String text)` : regex
  `^[ \t]*\[([^\[\]\n]+)][ \t]*$` en mode MULTILINE, retourne toutes les lignes entre crochets, dans
  l'ordre, crochets retirés. Ne matche jamais `[NEXT ACT]` en pratique (pipelines disjoints : celui-
  ci lit les fichiers d'acte/scénario, `[NEXT ACT]` n'est cherché que dans la réponse du LLM).
- `ChatTurn.Speaker` — + `NARRATOR` (troisième valeur, à côté de `PLAYER`/`LLM`).
- `ChatSession` :
  - `fresh()` — extrait les story beats du premier acte (ou, scénario sans actes, de `premise()`)
    et les injecte comme tours `NARRATOR` dès l'ouverture.
  - `advanceAct()` — extrait les story beats du nouvel acte courant et les ajoute en fin
    d'historique au moment de la transition (bouton manuel ou marqueur `[NEXT ACT]` — les deux
    passent par cette méthode).
- `ChatPromptBuilder.transcript()` — + libellé `"Narration: "` pour les tours `NARRATOR` (partagé
  avec `ChatSummarizer`, comme `"Player: "`/`"Character: "`).

### Infra (`chat/infra/ChatFileStorageAdapter`)
- Nouveau header de persistance `### NARRATOR` dans `history.md`, lu/écrit symétriquement aux
  headers `### PLAYER`/`### LLM` existants.

### Web (`chat/ui/web`)
- `chat.html` — `appendTurn()` rend un tour `NARRATOR` comme une ligne centrée en italique (classe
  `.narrator-note`, réutilise le style de l'ancienne bannière de transition éphémère, abandonnée).
- Fix scaling indépendant demandé dans la même séance : la rangée de boutons d'actes
  (`.acts`/`.acts button`) passe de largeur fixe centrée à `overflow-x:auto` + boutons `min-width`
  (au lieu d'un cercle `width` fixe) pour ne pas casser visuellement si un scénario a beaucoup
  d'actes (anticipé jusqu'à ~100) — préventif, le mécanisme reste linéaire pour l'instant (voir
  mémoire `project_chat_future_ideas_queued`).

### Décision de design abandonnée en cours de route
Une première implémentation (bannière `ChatHistoryView.teaser` calculée à la volée, jamais
persistée, une seule ligne affichée) a été écrite puis entièrement retirée après que l'utilisateur
a précisé que plusieurs lignes doivent être supportées et doivent survivre dans l'historique/le
résumé — le tour `NARRATOR` persisté est la seule option qui satisfait cette contrainte sans
duplication de mécanisme.

### Contenu des scénarios de démo
- `temple-noir-actes/act-1.txt` à `act-8.txt` — une ligne `[...]` par acte (deux pour `act-3.txt`),
  décrivant la situation telle qu'elle est vraie au moment où l'acte commence (jamais un
  développement censé émerger pendant la scène, qui serait pré-raconté à tort puisque toutes les
  lignes d'un acte s'affichent ensemble, d'un coup, à l'entrée dans l'acte).
- `temple-noir/scenario.txt` — une ligne `[...]` (ce scénario n'a pas d'actes, la ligne s'affiche
  donc à l'ouverture de la session).

## 3. Fonctionnalité complémentaire ajoutée dans la même séance : titre de fiche (affichage seul)

Demande distincte de l'utilisateur : un titre par acte, visible dans l'écran chat, mais **jamais**
transmis au LLM ni jamais persisté dans l'historique (contrainte inverse des `[...]` story beats
ci-dessus). Formalisme retenu : une ligne `# Titre` (façon markdown) tout en haut du fichier d'acte.

- `ScenarioAct` — + champ `title`, `record ScenarioAct(int number, String title, String text)`.
- `ChatFileStorageAdapter.parseAct()` (nouveau) — si la première ligne du fichier commence par `# `,
  l'extrait comme `title` et la retire de `text` (donc structurellement absente du prompt et de
  toute narration persistée) ; sinon `title` est `""` et `text` reste inchangé (rétrocompatible).
- `ChatHistoryView`/`ChatWebServer` — + `actTitle`, toujours celui du **seul acte courant** (jamais
  la liste complète) : afficher tous les titres d'avance spoilerait la suite, même en restant
  linéaire (pas d'embranchements, voir [[project_chat_future_ideas_queued]]).
- `chat.html` — le numéro d'acte reste affiché (utile pour déboguer, décision explicite de
  l'utilisateur) ; le titre s'ajoute en suffixe (`"Acte 2 : La maison fouillée"`) dans la bannière
  de transition et dans une nouvelle légende permanente sous la rangée de boutons.
- Les 8 fichiers `act-N.txt` de `temple-noir-actes` : première ligne convertie de `"Acte N — Titre"`
  (texte brut, envoyé au LLM jusqu'ici) vers `"# Titre"` (désormais retirée du prompt).

### Complément demandé dans la même séance : favicon
Ajout d'un favicon inline (`<link rel="icon" href="data:image/svg+xml,...">`, emoji 🎭) dans
`chat.html` — aucune requête externe, cohérent avec le serveur web local sans dépendance.

## 4. Résultat

`mvn -pl chat -am test` : tous les tests passent (existants + nouveaux : `TeaserTest`,
`ChatSessionTest` nouveau, + tests ajoutés à `ChatPromptBuilderTest` (dont
`actTitleIsNeverSentToTheLlm`), `ChatFileStorageAdapterTest` (parsing du titre), `ChatServiceImplTest`).
Non testé en usage réel dans le navigateur depuis cette session — à valider par l'utilisateur avec
`chat.bat`.
