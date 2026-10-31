# 2026-10-31 - Guide : ne pas résumer la suite sous un titre parent

## Évolution demandée
Noter dans le guide : quand une partie contient des sous-parties (par exemple `#` puis `##`, mais la règle
vaut pour n'importe quels niveaux), ne pas écrire de synthèse « ce qui va se passer = … » avant elles. Le
LLM *sait* alors déjà la suite, anticipe trop tôt et saute des étapes. Un plan que les personnages se
donnent réellement dans l'histoire reste possible, mais écrit comme un plan (« les personnages mettent en
place le plan : … »), pas comme un fait à venir : il peut être partagé avec le joueur, il peut y en avoir
plusieurs à la suite, et l'action peut différer ou échouer. Constaté dans le dernier scénario écrit.

## Ce qui a été touché
- `chatscenarios/guide-redaction-chat.md` : §7.1 (point 4, piège de la synthèse avec le mécanisme : le
  texte du titre parent est relu à chaque tour de chaque sous-partie, par tous les PNJ ; paragraphe « un
  plan n'est pas une synthèse », avec la réserve que la compréhension du modèle reste à vérifier) et ligne
  du tableau §7.4.
- `.claude/skills/chat-scenario-writing/SKILL.md` : rappel court avant les règles non négociables.
- Aucun code ni prompt modifié.

## Résultat
Conseil documenté avec sa raison et sa parade : sous le titre parent, seulement ce qui est vrai dans
toutes les sous-parties ; un événement par sous-partie ; un plan formulé comme un plan, pas comme un fait.
