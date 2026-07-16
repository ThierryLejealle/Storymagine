# 2026-07-15 16h48 - modelInfoBlock() incoherent avec supportsThinking(), corrige

## 1. Demande

Suite au fix de 16h33 (masquage du quirk thinking gemma4 dans OllamaAdapter), l'utilisateur a
reformule le principe attendu : "Le coeur via l'adapteur doit pouvoir savoir si un modele est
think; et on lui cache le probleme : pour ollama on répond OUI et l'adaptateur se débrouille pour
que ça marche. C'est son role..." — verification demandee que ce principe tient partout, pas
seulement sur le chemin deja corrige.

## 2. Ce qui a ete trouve

`ModelLifecyclePort` expose DEUX façons d'interroger "ce modele pense-t-il ?", et elles n'etaient
pas coherentes entre elles :
- `supportsThinking()` — deja masque correctement depuis le 2026-07-09 (repond `true` pour la
  famille gemma4, quoi que declare `/api/show`).
- `modelInfoBlock()` → `OllamaModelInfo.formatDeclared()` — lisait directement le champ BRUT
  `OllamaModelInfo.supportsThinking` (celui d'`/api/show`, jamais corrige), donc aurait affiche
  "Thinking : non" pour un E2B/E4B alors que `supportsThinking()` repond `true` pour le meme modele.

Deux reponses contradictoires derriere le meme port selon la methode appelee — exactement le genre
d'incoherence que le principe de l'utilisateur interdit. Verifie qu'aucun appelant de production
n'affiche actuellement `modelInfoBlock()` (grep sur tout le projet — seuls l'interface, l'impl et
un stub de test le referencent), donc pas de bug visible aujourd'hui, mais un piege latent pour le
jour ou quelqu'un le branche.

## 3. Ce qui a ete touche

- `OllamaModelInfo.formatDeclared(...)` : nouveau parametre `boolean effectiveSupportsThinking`,
  utilise pour la ligne d'affichage au lieu du champ brut `supportsThinking`. Javadoc explique
  pourquoi (une seule source de verite doit sortir du port).
- `OllamaAdapter.modelInfoBlock()` : passe desormais `supportsThinking()` (la methode publique
  masquee) a `formatDeclared(...)`, au lieu de laisser `OllamaModelInfo` lire son propre champ brut.

## 4. Resultat

`mvn compile` + `mvn test` (racine) : propre, 162 (chat) + 57 (redacteur) + reste, zero regression.
Aucun test ne referençait `formatDeclared` directement (verifie par grep), changement de signature
sans impact sur les tests existants.
