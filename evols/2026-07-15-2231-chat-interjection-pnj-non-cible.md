# 2026-07-15 22h31 - Interjections : les PNJ non cibles peuvent reagir

## 1. Demande

"Si un seul personnage est ciblé mais que plusieurs sont présents, on peut avoir un % de chance
pour chacun des autres de parler aussi. Avec peut-être une consigne de donner un message court ?
[...] en option avec une icone aussi sur le personnage" — reprend un principe explicitement ecarte
lors de la conception initiale du multi-PNJ ("Pas de bavardage pondere par PNJ"), mais demande a
nouveau, avec des mecanismes precises via 3 questions posees et validees :
1. % global (un seul reglage de session), pas un % par PNJ.
2. Chaque PNJ present eligible tire independamment (pas de plafond a 1 interjecteur max).
3. L'interjecteur repond APRES le PNJ vise, voit deja sa reponse.
4. Icone par PNJ pour activer/desactiver son eligibilite — **defaut ACTIF** (decision explicite de
   l'utilisateur, opt-out plutot qu'opt-in).
5. % dans le panneau reglages, avec les autres curseurs (temperature, topK...) — defaut 50%.

## 2. Delta de prompt (valide par l'utilisateur ET revu par Fable avant ecriture)

Premier jet propose puis critique par Fable (~33K tokens, cout estime au prealable et confirme) :
4 problemes releves — repetition de la regle de longueur (3 formulations du meme rappel, contraire
a la convention du projet), l'exemple "grave" faisait commander toute la piece par le PNJ (apprend
exactement le comportement a interdire, un petit modele imite les exemples plus qu'il n'obeit aux
regles abstraites), exemple 2 hors-mecanique et sans paire Wrong/Right sur la meme situation,
confusion de personne (le PNJ ecrit a la 3e personne alors que le reste du prompt utilise "I").
Version revisee, retenue apres correction manuelle supplementaire ("eyes on the player" → "watching
you closely", repere par l'utilisateur comme peu immersif — un terme de jeu video dans une ligne de
prose in-character) :

```
This is an interjection, not your main turn: the player addressed someone else, and you are
reacting from where you are to the exchange you just witnessed. Default to one short line or
gesture. Only if that exchange itself just turned serious — a threat, violence, a shocking
reveal — give your reaction the weight it deserves. Either way the scene still belongs to the
player and the character they addressed: react, then leave them the next move.
Example — the player asked Elena a question and she just answered calmly:
Wrong (turns it into a full turn): *I set down my mug and lean forward, launching into a long
story of my own about the last time someone asked me that...*
Right: *I grunt from my corner.* "Don't believe a word of it."
Example — the player just drew a blade on Elena:
Wrong (token remark, ignores the danger): *I raise an eyebrow.* "Trouble again?"
Right (full weight, still only a reaction): *I am on my feet before the blade clears its
sheath, hand on my dagger, watching you closely.* "Put it away. Now."
```

## 3. Ce qui a ete touche

Mecanique complete, miroir de la presence partout ou c'est pertinent :

- **`Scene`** : nouveau champ `interjecting` (boolean).
- **`ChatPromptBuilder`** : `INTERJECTION_RULE` (delta ci-dessus), injectee apres `OTHER_NPCS_RULE`
  uniquement quand `scene.interjecting()`.
- **`SpeakerSelector.rollInterjectors(...)`** : nouvelle methode pure, separee de `select()` — ne
  s'applique que si le "primaire" vient d'une mention unique (jamais sur mention multiple, jamais
  sur repli aleatoire). Chaque PNJ present, eligible, different de la cible tire independamment
  contre `chance`.
- **`ChatSession`** : `interjectingNpcIds` (nouveau `Set<String>`, defaut = tout le cast — meme
  logique que `allPresent`), `setInterjecting(id, bool)` (pas de garde "au moins un", contrairement
  a `setPresent`), reconciliation dans `reloadScenario` (meme motif que la presence).
- **`GenerationSettings.interjectionChance`** (Double, defaut null → `RoleplayNarrator
  .INTERJECTION_CHANCE_DEFAULT = 0.5`, constante publique partagee avec `ChatServiceImpl`).
- **`ChatFileStorageAdapter`** : nouveau fichier `interject.txt`, miroir exact de `present.txt`
  (lecture/ecriture/archivage sur reset).
- **`ChatService`/`ChatServiceImpl`** : `setNpcInterjecting(...)`, `resolveSpeakers(...)` (combine
  `select()` + `rollInterjectors()` en une liste ordonnee primaire-puis-interjecteurs), branche dans
  `sendMessage`/`retry`.
- **`ChatWebServer`** : endpoint `POST /set-interjecting`, `NpcView.interjecting`, parsing
  `interjectionChance` dans `/settings`.
- **`chat.html`** : icone 💬 par PNJ (ligne separee de la vignette de presence, meme motif que
  `.save-row`/`.save-delete-btn`) — bleu quand actif, gris quand desactive (defaut actif) ; champ
  "Chance d'interjection" dans le panneau reglages (decimal 0-1, meme convention que minP/repeat
  penalty, pas de conversion pourcentage).

## 4. Bug trouve et corrige en testant (pas juste en relisant)

`reloadScenario()` videit `presentNpcIds`/`interjectingNpcIds` (`.clear()`) AVANT d'appeler
`reconcile(presentNpcIds, ...)` — comme `reconcile()` recoit le meme objet `Set` que celui qu'on
vient de vider, il reconciliait depuis un set deja vide. Detecte par 3 tests existants qui ont
echoue apres l'ajout de la 2e reconciliation (`ChatSessionTest` x2, `ChatServiceImplTest` x1) —
corrige en calculant les deux reconciliations dans des variables locales AVANT de vider les champs.

## 5. Regression evitee dans un test existant

`mentioningOneNpcByNameMakesOnlyThatOneReply` (deja existant) serait devenu **intermittent** :
avec l'interjection a 50% par defaut et un `Random` non-seede (`newService`), le 2e PNJ present
aurait eu 1 chance sur 2 d'interjecter, cassant l'assertion "une seule reponse". Corrige en
desactivant explicitement l'eligibilite du 2e PNJ dans ce test — son intention reste de tester
`SpeakerSelector.select()` seul, pas l'interaction avec `rollInterjectors` (testee separement).

## 6. Resultat

`mvn clean test` (racine, 4 modules) : 179 tests cote chat (+16 nouveaux), tous verts, zero
regression ailleurs. Pas encore teste avec un vrai LLM — le scenario d'aventure a 3 PNJ (amazones)
demande juste apres est un candidat naturel pour ca.
