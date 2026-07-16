# 2026-07-15 13h42 - Chat : multi-PNJ, séance 1b (branchement) + IHM — implémentation complète

## 1. Évolution demandée
Suite de la séance 1a (fondations isolées, voir `evols/2026-07-15-0009-...`). L'utilisateur a
donné le feu vert pour tout implémenter d'une traite, sans repasser par une validation à chaque
étape ("Fait tout sans me demander"), avec des précisions de comportement données en une seule
fois :
- Détection de mention : **tous** les PNJ nommés dans le message du joueur répondent (pas un seul
  choisi arbitrairement) ; si personne n'est nommé, **2 PNJ pris au hasard** parmi les présents
  répondent (pas de rotation déterministe comme envisagé en séance 1a).
- "Muter" des PNJ pour les rendre absents (vignettes, déjà planifié).
- Simplification volontaire : pas de gestion de ce que chaque PNJ "sait" de l'histoire — tout le
  monde connaît toute l'histoire (résumé/transcript partagés), mais chaque PNJ ne connaît QUE sa
  propre fiche, jamais celle des autres.
- Nouvelle demande apparue en cours de route : scinder chaque fiche de personnage en deux parties
  — ce qui est public (connu de tous) et ce qui est secret (connu du PNJ seul), avec une
  séparation simple dans le fichier.

Contrairement aux autres évolutions de la soirée précédente, **aucun delta de prompt n'a été
présenté avant écriture** pour la nouvelle règle anti-usurpation (voir §3) — l'utilisateur avait
explicitement levé cette exigence pour toute cette séance ("sans me demander"). Documenté ici a
posteriori pour traçabilité, conformément à l'esprit de la règle même si la lettre n'a pas été
suivie à la demande explicite de l'utilisateur.

## 2. Décisions d'implémentation prises en cours de route (non re-débattues)
- `SpeakerSelector.select(...)` ne prend plus `lastSpeakerId` (rotation abandonnée au profit du
  hasard) mais un `java.util.Random` injecté (déterminisme testable). Retourne `List<Npc>`
  (jamais vide), pas un `Npc` unique.
- Le "public/secret" ne change PAS le mécanisme Scene/otherPresent (toujours juste des noms pour
  les autres PNJ présents, jamais leur fiche même publique) — le split sert uniquement à
  structurer la fiche du PNJ qui PARLE (il reçoit toujours `publicInfo + secretInfo` en entier,
  puisqu'il se connaît lui-même complètement). Convention de fichier : une ligne `# SECRET` seule
  sépare les deux parties ; absente = tout est public.
- **Règle du Cast, simplifiée sans cas particulier** : `Cast` = tous les fichiers `.txt` du
  dossier scénario sauf `scenario.txt`/`act.txt`/`present.txt` (fichiers de plomberie). Un
  scénario existant avec juste `character.txt` devient un `Cast` d'un seul PNJ automatiquement.
- Plusieurs PNJ peuvent répondre au même tour joueur : `ChatServiceImpl` boucle sur les PNJ
  sélectionnés, CHAQUE appel relit `session.turns()` à jour — le 2e PNJ voit donc la réplique du
  1er, pas seulement le message du joueur (conversation cohérente, pas des monologues parallèles).
- `ChatPromptBuilder.build(...)` perd son paramètre `PlayerMessage` : `recentTurns` se termine
  désormais toujours par la ligne courante (déjà ajoutée à la session avant l'appel), le "trailer"
  séparé `"Player: ..."` disparaît — simplification qui a émergé naturellement en réfléchissant à
  comment un 2e PNJ du même tour doit voir la réplique du 1er sans dupliquer le message du joueur.
- `NextActReadinessAnalyst`/`NpcMindStateAnalyst`/`ScenarioTesterServiceImpl` : adaptés au Cast en
  utilisant le premier PNJ présent (`representativeScene`) comme PNJ par défaut — choix simple,
  documenté comme tel, pas de notion de "PNJ actuellement actif" pour ces outils d'analyse.
- `retry()` régénère tout le bloc de répliques depuis le dernier tour joueur (pas juste UNE
  réplique) et relance `SpeakerSelector` à neuf — peut donc changer de PNJ répondant par rapport
  au tour original (simplification assumée, documentée dans le code).
- `Npc.label()` (nouveau) retombe sur l'`id` (nom de fichier) si aucun `# Nom` n'est déclaré, PAS
  sur un "Character" générique fixe comme avant — un id de fichier réel est plus utile qu'un
  placeholder, maintenant que chaque PNJ a une identité de fichier propre.
- Constructeur `ChatServiceImpl` : nouveau paramètre `Random` en dernière position (nouvelle
  surcharge à 7 arguments), les surcharges à 5/6 arguments existantes restent compatibles (elles
  délèguent avec `new Random()` par défaut) — zéro rupture pour les appelants existants qui ne se
  soucient pas de déterminisme.

## 3. Nouvelle règle de prompt (non validée au préalable, voir §1)
Ajout dans `ChatPromptBuilder` (`OTHER_NPCS_RULE`), seulement injecté quand `Scene.otherPresent()`
n'est pas vide :
```
Other characters may be present in the scene (named above, under ALSO PRESENT) — you know
only their names, nothing else about them. Speak and act only for your own character;
never write dialogue or actions for another present character, even briefly.
Example — Marcus is present:
Wrong: *Marcus nods and steps back.*
Right: *I glance at Marcus, unsure if he noticed.*
```
Raison : généralisation de la règle anti-mirroring déjà existante ("jamais à la place du joueur")
à un nouveau risque introduit par le multi-PNJ (usurper un AUTRE PNJ présent) — même technique
pédagogique (exemple concret faux/juste) que les règles voisines du prompt.

## 4. Ce qui a été touché (exhaustif)
**Domaine (nouveau)** : `Npc` (id/name/publicInfo/secretInfo/fullSheet()/label()), `Cast`
(liste-objet, `find`/`npcs`/`size`), `Scene` (speaker + otherPresent), `SpeakerSelector` (mention
en mots entiers sur n'importe quel mot d'un nom composé + repli aléatoire).

**Domaine (modifié)** : `ChatScenario` (characterSheet/characterName → `Cast cast`), `ChatTurn`
(nouveau champ `npcId`, nullable), `ChatSession` (`presentNpcIds`, `setPresent` avec garde "au
moins un présent", `restore` étendu), `ChatPromptBuilder` (Scene au lieu de PlayerMessage, labels
multi-locuteurs via `Cast`, section ALSO PRESENT, règle anti-usurpation), `RoleplayNarrator`/
`RoleplayNarratorInput` (Scene, stop-sequences dynamiques par PNJ présent).

**Infra** : `ChatFileStorageAdapter` — chargement du Cast multi-fichiers, split public/secret sur
marqueur `# SECRET`, format d'historique `### LLM: {npcId}`, persistance `present.txt`,
`resetSession`/`archive` étendus.

**Service** : `ChatServiceImpl` — boucle multi-répliques, `retry()` régénère tout le bloc,
`setNpcPresent(...)` (nouveau), agents d'analyse adaptés au Cast. `ChatTurnResult.replyTurn` →
`replyTurns: List<ChatTurn>`.

**Web/IHM** : `NpcView` (nouveau DTO), `ChatHistoryView.cast` (la liste de PNJ voyage sur chaque
réponse), endpoint `POST /set-present`. `chat.html` : colonne de vignettes à gauche (`#castPanel`,
clic = mute/unmute), labels de locuteur dynamiques par tour (`.speaker-label`, JS-rendu, remplace
les anciens labels CSS figés "Personnage"/"PERSONNAGE" des styles Immersif/Script sans toucher au
côté joueur de ces styles).

**Tests** : `NpcTest`, `CastTest`, `SpeakerSelectorTest` (réécrit pour l'API multi-réponses),
tous les tests existants cassés par les changements de forme (`ChatScenario`, `ChatTurn`,
`ChatPromptBuilder`, `ChatTurnResult`) mis à jour, plus des tests dédiés au nouveau comportement :
chargement Cast multi-fichiers, split public/secret, round-trip npcId/présence,
`ChatServiceImplTest` (mention → 1 seul répond, pas de mention → 2 au hasard avec `Random` seedé,
mute exclut de la sélection, persistance de la présence, garde du dernier présent).

## 5. Résultat
`mvn clean test` vert sur tout le projet (153 tests côté chat, contre 135 en fin de séance 1a —
18 nouveaux, zéro régression ailleurs). Aperçu visuel des vignettes + labels dynamiques vérifié
dans le navigateur (fichier mocké dans le scratchpad avec un cast de 2 PNJ, jamais commis) — pas
de test end-to-end avec un vrai LLM (l'app n'a pas été relancée avec `chat.bat`).

## 6. Limites connues, non traitées ici
- Les styles Immersif/Script ne réservent toujours pas l'espace du nouveau panneau gauche
  (`--cast-w`) dans leurs overrides de largeur — même limitation préexistante déjà documentée pour
  `--saves-w`/`--gauge-w`, pas aggravée par cette séance.
- Pas d'ajout/suppression de PNJ en session (décidé simplifié dès le plan initial) — le Cast est
  fixé par les fichiers présents dans le dossier scénario, modifiables seulement à la main.
- Aucun scénario de démonstration multi-PNJ n'a été créé dans `chatscenarios/` — à faire pour un
  vrai test de bout en bout.
- `retry()` peut changer les PNJ répondants d'un tour à l'autre (voir §2) — potentiellement
  surprenant à l'usage, à observer.
