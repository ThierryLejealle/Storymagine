# 2026-07-15 20h21 - Max tokens releve pour laisser de la place a la reflexion

## 1. Demande

"attention : si on affiche le thinkin il faut augmenter le max token; ca ne marche plus..." — suite
aux fixs du jour (think toujours demande, affichage actif par defaut), le plafond `num_predict`
n'avait pas ete revu. Or ce plafond couvre la generation TOTALE d'Ollama (reflexion + reponse
visible combinees, pas juste la reponse) — avec des raisonnements observes autour de 300-600 tokens
dans les vrais logs de la journee, le defaut de 1200 tokens laissait trop peu de marge, au point de
tronquer ou casser la reponse.

Valeur cible demandee via question : **2500** (option recommandee, marge large pour un raisonnement
long + une reponse normale sans gaspiller un budget enorme a chaque tour).

## 2. Ce qui a ete touche

- `chat/src/main/resources/chat.properties` : `ollama.num-predict` passe de `1200` a `2500`,
  commentaire mis a jour avec la raison (think toujours demande desormais, le plafond couvre les
  deux).
- `chat.html` : placeholder de `maxTokensInput` (1200 → 2500) — doit rester synchronise avec le vrai
  defaut serveur, d'autant plus depuis le fix du jour qui materialise ce placeholder en valeur reelle
  au premier focus du champ (voir evol precedente, correctif des fleches +/-).
- `ChatWebServer.java` : commentaire d'exemple (`maxTokens=1200` → `maxTokens=2500`) mis a jour par
  coherence, purement illustratif (pas une valeur de comportement).

## 3. Resultat

`mvn -pl chat -am test` : propre, aucun test ne referençait l'ancienne valeur (verifie par grep).
Changement de configuration + commentaires uniquement, aucune logique modifiee.
