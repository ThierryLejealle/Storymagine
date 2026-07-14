# 2026-07-14 23h03 - Chat : fix texte joueur perdu après un envoi interrompu/en échec

## 1. Évolution demandée
"Correct SOMETHING before : the STOP button should not erase what I said : I must be able to
edit it to relaunch it corrected." — l'utilisateur a observé la perte de son texte tapé après un
arrêt de génération.

## 2. Investigation
Traçage complet du chemin Stop → interruption de thread → `OllamaAdapter` →
`GenerationCancelledException` → `ChatServiceImpl.sendMessage` → `ChatWebServer.handleMessage`.
Le cas "interruption pendant l'attente d'un token" (`readLineWithTimeout`, la voie la plus
fréquente pour un vrai clic Stop) se convertit bien en `GenerationCancelledException` et remonte
correctement en `{stopped: true}`.

Cause trouvée côté client (`chat.html`) : `sendMessage()` ne restaurait `inputEl.value = text`
QUE dans la branche `data.stopped`. Deux autres chemins d'échec — `data.error` (toute autre
exception serveur, y compris un vrai TIMEOUT stream Ollama qui, lui, ne se convertit PAS en
`GenerationCancelledException` puisque ce n'est pas une interruption) et l'erreur réseau
(`catch (err)`) — effaçaient le texte du joueur sans le restaurer. Si le clic Stop tombe pile sur
un timeout en cours, ou sur toute autre erreur serveur, le texte disparaissait bien comme signalé.

## 3. Ce qui a été touché
- `chat.html`, fonction `sendMessage()` : `inputEl.value = text` restauré dans les 3 branches
  d'échec (`data.stopped`, `data.error`, erreur réseau `catch`), pas seulement `data.stopped`.
  Message de statut mis à jour pour les 2 nouveaux cas ("... — corrige et renvoie.").
- Aucun changement côté Java : le fix corrige un trou dans la logique JS, pas une régression du
  mécanisme d'annulation serveur (qui fonctionne correctement pour le cas testé).

## 4. Point non traité (à surveiller, pas bloquant)
En creusant `OllamaAdapter.readLineWithTimeout`, la lecture bloquante (`reader.readLine()`)
tourne sur un thread séparé (`STREAM_EXECUTOR`) pendant que le thread appelant attend sur
`future.get(timeout)`. Une interruption (Stop) débloque bien le thread appelant (qui lève
`GenerationCancelledException` correctement), mais le thread `STREAM_EXECUTOR` sous-jacent, lui,
n'est jamais annulé (`future.cancel(true)` n'est jamais appelé) — il reste bloqué sur la lecture
socket jusqu'à ce qu'Ollama ferme la connexion de son côté. Fuite mineure de thread/connexion, pas
un bug fonctionnel visible pour l'utilisateur, mais à corriger si ça devient gênant (pool de
threads qui grossit après des Stop répétés). Pas traité ici, hors scope de la demande.

## 5. Résultat
Fix JS uniquement, pas de nouveau test automatisé (comportement UI pur, pas de logique testable
côté cœur). À valider par l'utilisateur en usage réel : cliquer Stop à différents moments d'une
génération et vérifier que le texte revient systématiquement dans le champ de saisie.
