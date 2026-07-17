# 2026-07-17 08h33 - Les PNJ absents restent connus + fix mute pendant un round en cours

## 1. Demande

Suite du partage de fiche publique (voir `evols/2026-07-16-2142-...`) : "Hum j'ai un doute : si les
persos sont muets, ils existent quand même non ? Il faudrait juste ajouter dans le prompt qui est
présent ou absent. Pas les faire disparaître de la mémoire du LLM." Confirmé après proposition de
delta ("oui c'est ça !").

Bug signalé en cours de route : "je crois que j'ai un perso qui a parlé alors que j'avais coché le
haut-parleur sur son icône pour dire absent" — confirmé ensuite par observation du thinking d'un
autre PNJ, qui se demandait ce que les personnages "absents" allaient dire/faire, preuve qu'ils
n'étaient pas correctement signalés comme absents dans son prompt.

## 2. Diagnostic du bug

Deux causes distinctes trouvées :
1. Le prompt ne disait RIEN sur les PNJ absents — ni leur existence, ni leur statut. Un PNJ pouvait
   donc halluciner sur ce que fait un coéquipier absent, faute d'info.
2. Bug réel de timing : le panneau de personnages (🔊/🔇) n'était jamais désactivé pendant une
   génération. La liste des PNJ qui vont parler ce tour est calculée UNE FOIS au début du round
   (`resolveSpeakers`) ; si le joueur mute quelqu'un PENDANT que la génération d'un round à
   plusieurs PNJ est en cours, le PNJ déjà programmé pour répondre le fait quand même — son statut
   n'était jamais revérifié entre le calcul du round et l'exécution de son propre tour.

## 3. Ce qui a été touché

`Scene.java` : nouveau champ `absentTeammates` (liste de `Npc`), à côté de `otherPresent`.

`ChatServiceImpl.sceneFor`/`representativeScene` : calculent aussi la liste des coéquipiers
absents (cast entier moins les présents).

`ChatPromptBuilder.java` — delta de prompt validé avant écriture (règle CLAUDE.md) :
- Nouveau bloc `NOT IN THE SCENE RIGHT NOW` (fiche publique de chaque absent, jamais leur secret),
  même principe que `ALSO PRESENT` mais avec une consigne différente : ne jamais les faire
  parler/agir, seulement les référencer.
- `OTHER_NPCS_RULE` étendu pour couvrir ce cas ("never have them speak, act, or walk in"),
  condition d'ajout élargie à `!otherPresent().isEmpty() || !absentTeammates().isEmpty()`.

`ChatServiceImpl.generateRepliesAndFinish` (fix du bug de timing) : le statut de présence d'un PNJ
est revérifié juste avant SON PROPRE tour dans la boucle du round (pas seulement une fois au début)
— si muté entre-temps, son tour déjà programmé est simplement sauté.

`chat.html` (défense en profondeur côté client) : nouveau flag `generating`, boutons du panneau
personnages (🔊/🔇 et 💬) désactivés pendant une génération — empêche la course qui a causé le bug
plutôt que de compter uniquement sur le rattrapage serveur.

## 4. Tests

`ChatPromptBuilderTest` : `absentTeammatesShareTheirPublicSheetButNeverTheirSecretAndAreNeverMadeToSpeak`,
`notInTheSceneBlockIsOmittedWhenNobodyIsAbsent`. Tous les sites d'appel `new Scene(...)` mis à jour
(4e paramètre `absentTeammates`).

`ChatServiceImplTest.mutingAnNpcMidRoundStillSkipsTheirAlreadyQueuedTurn` — mute marcus via le
listener de streaming juste après le tour d'elena (simule un clic 🔇 pendant un round en cours),
vérifie que son tour déjà programmé est sauté.

`mvn -pl chat -am clean test` : 205 tests (+3), tous verts. `mvn compile` (racine) : aucune
régression cross-module. Syntaxe JS vérifiée (`node --check`).

## 5. Bug annexe corrigé au passage (même échange)

Le formulaire d'envoi acceptait la touche Entrée pendant une génération en cours (seul le bouton
Envoyer était désactivé, pas la soumission du formulaire) — deux requêtes `/message` en parallèle
pouvaient muter la même `ChatSession`. Fix distinct, voir
`evols/2026-07-16-2326-fix-envoi-pendant-generation-et-precision-prompt.md`.

Non testé en conditions réelles (nécessite Ollama + modèle chargé) — à vérifier par l'utilisateur.
