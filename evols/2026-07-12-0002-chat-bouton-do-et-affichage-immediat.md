# 2026-07-12 00h02 — Chat : bouton DO + affichage immédiat du tour du joueur

## 1. Demande

Deux demandes de l'utilisateur après le premier test réussi du chat (suite à
`evols/2026-07-11-2356-chat-fix-raw-completion-vers-api-chat.md`) :
- Un bouton "DO" dans la page pour prendre la main sur la scène sans avoir à taper le préfixe à la
  main.
- Signalé en test : le message tapé par le joueur n'apparaît qu'au retour de la réponse du LLM,
  pas immédiatement à l'envoi.

## 2. Ce qui a été touché

Uniquement `chat/src/main/resources/web/chat.html` (page statique, aucun changement Java/prompt).

- **Bouton DO** : nouveau bouton `#doBtn` à côté de "Répondre". Au clic, préfixe/retire `DO: ` en
  tête du champ de saisie (idempotent, toggle visuel `.active`) et rend la main au clavier — ne
  soumet pas directement, l'utilisateur peut encore compléter le texte de narration avant d'envoyer.
- **Affichage immédiat** : extraction d'une fonction `appendTurn(speaker, text)` réutilisée par
  `render()` (rendu complet depuis le serveur) et par le nouveau chemin optimiste. Le tour du
  joueur est maintenant ajouté à l'écran dès l'envoi (avant même la réponse HTTP), puis remplacé
  par le rendu complet et autoritaire du serveur (`render(data)`) une fois la réponse reçue. En cas
  d'erreur (réseau ou `data.error`), la bulle optimiste est retirée (`optimisticTurn.remove()`) —
  rien n'est persisté côté serveur si `service.sendMessage` échoue avant `storage.saveSession`,
  donc l'UI doit refléter cet échec plutôt que garder une bulle fantôme.

## 3. Résultat

`mvn compile test -pl chat -am` : OK, 22 tests toujours verts (changement purement front, aucun
test Java concerné). Non re-testé en usage réel dans le navigateur à la rédaction de cette fiche.
