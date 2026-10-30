# 2026-10-29 - Chat : serveur web non bloqué par les appels longs, icône ❓, erreurs visibles du debug

## 1. Évolution demandée

Le bouton 🐞 (scénario envoyé) ouvrait sa pop-up sans aucun contenu, alors qu'il ne fait aucun appel au LLM.
L'icône du bouton d'origine d'une réponse devait être un point d'interrogation.

## 2. Ce qui a été touché

- Cause trouvée par un test : `ChatWebServer` utilisait `newFixedThreadPool(2)`. Un appel LLM long (réponse,
  analyses 🔍 🧠 ❓) occupe un fil pendant des minutes ; avec deux fils pris, le clic suivant, même un simple
  affichage (🐞, 🎲, `/stop`), restait en attente sur « Chargement… ». Remplacé par `newCachedThreadPool()`.
- `/scenario-prompt` et `/picks` renvoient une erreur JSON lisible au lieu de fermer la connexion
  (la page n'affichait qu'un échec muet).
- Icône ❓ à la place de 🧭 (page, guide, specs, skill, fiche de l'agent, fiche 2026-10-28-...-7).
- Tests : `ChatWebServerResponsivenessTest` (avec trois appels longs en cours, un clic de debug est
  servi ; échouait avant la correction), `ChatWebServerEndpointsTest` (🐞, 🎲, ❓ en vrai HTTP).
- Vérifié hors test : la page chargée dans un vrai Chrome sans fenêtre et un clic sur 🐞 affichent bien la
  prémisse, l'acte et les avertissements, sans erreur JavaScript. Ces sondes temporaires sont supprimées.

## 3. Résultat

`mvn clean test` : tous les tests passent. La cause chez toi reste une hypothèse (forte, car le test la
reproduit) : si 🐞 reste vide après relance, la pop-up affichera maintenant l'erreur.
