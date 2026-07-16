# 2026-07-16 09h00 - Budget de generation +30% (thinking souvent volumineux)

## 1. Demande

"Je pense aussi qu'il faudra augmenter un peu le budget token : le thinking est parfois gros.
Prévoir d'augmenter de 30%."

## 2. Ce qui a été touché

`chat/src/main/resources/chat.properties` : `ollama.num-predict` releve de 2500 a 3250 (+30%,
arrondi). C'est le plafond de generation par tour (reflexion + reponse visible), lu par
`OllamaConfig`/`OllamaAdapter` (module commun) comme valeur par defaut quand aucun `maxTokens`
n'est precise par session (`GenerationSettings.maxTokens() == null`, voir
`OllamaAdapter.java:617`). Reglable par session dans le panneau reglages sans toucher ce defaut.

`chat/src/main/resources/web/chat.html` : placeholder du champ "Max tokens" du panneau reglages
aligne sur la nouvelle valeur par defaut (2500 -> 3250), purement cosmetique (indicatif pour
l'utilisateur, ne pilote aucun comportement).

## 3. Resultat

Aucun test unitaire ne dependait de la valeur 2500 (verifie par recherche). Pas de recompilation
necessaire (fichier de proprietes). A observer au prochain playtest : le thinking prend-il
toujours une part disproportionnee du budget, ou est-ce que 3250 laisse une marge suffisante a la
reponse visible sur les tours les plus reflechis.
