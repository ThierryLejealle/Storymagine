# 2026-09-14 - Fix : llama-server rejetait tous les appels (reasoning_content:null)

## Demande

Retour utilisateur apres premier test reel contre une instance llama-server : tous les appels
echouaient en boucle (HTTP 500, "Failed to parse messages: [json.exception.type_error.302] type
must be string, but is null", retry x6 puis echec).

## Cause

`LlamaServerMessage.reasoningContent` (mappe sur le champ JSON `reasoning_content`) n'avait pas
d'annotation `@JsonInclude(NON_NULL)`. Ce POJO sert a la fois aux messages de reponse (ou ce champ
est rempli si le modele a reflechi) et aux messages de requete (system/user), ou il vaut toujours
null puisqu'on ne l'envoie jamais explicitement. Sans l'annotation, Jackson serialisait quand meme
`"reasoning_content":null` dans les deux messages de chaque requete `/v1/chat/completions` —
un champ que llama-server n'attend pas en entree et rejette avec une erreur de parsing JSON.

## Ce qui a ete touche

- `commun/infra/LlamaServerMessage` : ajout de `@JsonInclude(JsonInclude.Include.NON_NULL)` sur
  `reasoningContent`, alignee sur les autres champs optionnels du meme fichier
  (`LlamaServerChatRequest.minP/nPredict/stop`).
- `commun/test/infra/LlamaServerAdapterTest.generateSyncParsesTextAndReasoning` : capture
  desormais le corps de la requete envoyee au faux serveur et verifie qu'il ne contient jamais
  "reasoning_content" — non-regression sur cet incident precis. Verifie manuellement que ce test
  echouait bien avant le correctif (retrait temporaire de l'annotation, test rouge avec le corps de
  requete fautif affiche dans le message d'echec, puis remise en place).

## Resultat

`mvn clean test` OK sur l'ensemble du projet, 5/5 sur `LlamaServerAdapterTest`. Corrige la seule
anomalie remontee lors du premier test reel contre une instance llama-server.
