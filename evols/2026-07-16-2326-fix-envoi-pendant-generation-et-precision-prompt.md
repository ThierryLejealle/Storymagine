# 2026-07-16 23h26 - Fix : envoi possible pendant une génération + précision "depuis le début" du prompt

## 1. Demande

Deux points distincts dans le même échange :
1. Bug : "je peux tenter de répondre alors que tous les persos n'ont pas fini de répondre. Il
   faudrait un mécanisme qui prévient l'IHM que des persos vont parler et qu'il faut quand même
   attendre avant d'envoyer la réponse."
2. Suite validée du delta précédent (partage de fiche publique, voir
   `evols/2026-07-16-2142-...`) : préciser que la fiche publique partagée entre PNJ présents est
   une base ("ce que tu savais depuis le début"), pas l'intégralité de ce qu'ils savent désormais
   — l'histoire qui suit dans le prompt (résumé + acte en cours + échange récent) peut avoir
   enrichi cette connaissance depuis.

## 2. Diagnostic (bug d'envoi concurrent)

`setControlsDisabled(true)` désactive bien `sendEl`/`doBtnEl`/`undoBtnEl`/`retryBtnEl`, mais
`inputEl` (le champ de texte) n'est jamais désactivé — volontairement, pour rester tapable pendant
la génération. Or `formEl`'s submit handler (déclenché par la touche Entrée sur `inputEl`, pas
seulement par un clic sur `sendEl`) n'avait aucune vérification d'un état "génération en cours" :
il suffisait d'appuyer sur Entrée pendant qu'un round à plusieurs PNJ tournait encore pour
déclencher un second appel `sendMessage()` en parallèle — deux requêtes `/message` simultanées
(le pool de threads du serveur en autorise 2, voir `ChatWebServer`) mutant la même `ChatSession`,
un simple POJO non pensé pour la concurrence.

## 3. Ce qui a été touché

`chat.html` : nouveau flag `generating` (suit `setControlsDisabled`), vérifié en garde au tout
début du handler `submit` du formulaire et du clic sur `doBtnEl` (défense en profondeur, le bouton
désactivé bloque déjà nativement le clic, mais pas l'entrée clavier sur le formulaire).

`ChatPromptBuilder.java` : bloc "ALSO PRESENT" — *"what you know about them"* devient *"what you
knew about them from the start [...] the story below may have taught you more since"*.
`OTHER_NPCS_RULE` — *"that's the only thing you know about them, nothing more"* devient *"the
story may have taught you more since, but never anything from a private note that isn't yours"* —
mis en cohérence avec le premier changement (avant, les deux phrases se contredisaient : l'une
promettait plus d'informations via l'histoire, l'autre les excluait explicitement).

## 4. Résultat

`mvn -pl chat -am clean test` : 202 tests, tous verts (aucun test ne dépendait du texte exact
modifié). Syntaxe JS vérifiée (`node --check`). Non testé en conditions réelles (nécessite Ollama +
modèle chargé, et pour le bug d'envoi concurrent, un vrai essai d'appui sur Entrée pendant un round
multi-PNJ) — à vérifier par l'utilisateur.
