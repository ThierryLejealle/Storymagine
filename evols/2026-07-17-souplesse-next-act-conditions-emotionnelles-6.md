# 2026-07-17 - Souplesse sur les conditions [NEXT ACT] de nature émotionnelle/interne

## 1. Demande

Signalé sur un scénario personnel (`chatscenarios/temp/`) : la condition de transition d'acte
"au bout je vais peut-être te saouler à te demander pardon, alors tu vas écrire [NEXT_ACT]" ne se
déclenchait jamais, même après plusieurs échanges où l'agacement du personnage était clairement
palpable. Objectif donné par l'utilisateur : "être plus souple sur les conditions émotionnelles.
Ne pas rusher, ne pas être tatillon sur le mot exact."

Investigation initiale mal ciblée : le premier chantier a porté sur `NextActReadinessAnalyst`
(l'outil "loupe" 🔍, une analyse à la demande, en lecture seule, qui ne pilote rien). L'utilisateur
a fait remarquer que ce n'est pas ce prompt qui compte pour le déroulement réel de la partie, mais
`ChatPromptBuilder.NEXT_ACT_RULE_CONDITIONAL` — la règle donnée à chaque tour au PNJ qui JOUE, qui
décide lui-même d'écrire "[NEXT ACT]" dans sa réponse. Les deux prompts ont finalement été corrigés,
avec des textes adaptés à chacun (voir §3).

Deux consultations Fable (voir mémoire `feedback_fable_token_cost_confirm`) ont affiné la
formulation :
- 1re consultation (sur le prompt de l'analyste) : a suggéré de distinguer condition factuelle vs
  condition interne, avec un exemple. L'exemple initial listait des comportements précis (soupirs,
  coupe la parole...) — rejeté par l'utilisateur car ça recrée le même défaut de littéralité, juste
  déplacé sur d'autres mots-clés recherchés textuellement plutôt que sur le sentiment lui-même.
  Reformulé en termes génériques ("anger, annoyance, or irritation") sur la suggestion directe de
  l'utilisateur.
- 2e consultation (sur la vraie règle qui pilote le jeu, `NEXT_ACT_RULE_CONDITIONAL`) : a identifié
  un risque spécifique à ce prompt-ci (absent pour un simple analyste après coup) — le modèle qui
  JOUE le personnage génère lui-même la "preuve" de l'état émotionnel ; il pourrait sur-jouer la
  colère qu'il interprète pour se donner un prétexte à déclencher [NEXT ACT] plus tôt. Parade
  retenue : ancrer la tendance sur le comportement du JOUEUR (observable dans le résumé et les
  derniers échanges fournis), pas sur les émotions que le personnage vient lui-même de jouer.

## 2. Ce qui a été touché

`ChatPromptBuilder.NEXT_ACT_RULE_CONDITIONAL` — règle envoyée au PNJ qui joue, à chaque tour où
l'acte en cours précise sa propre condition de transition.

`NextActReadinessPromptBuilder` (SYSTEM) — prompt de l'outil d'analyse séparé (bouton 🔍) : même
principe appliqué (bullet STATE enrichi + second exemple), pour rester cohérent même si ce n'est
pas ce prompt qui pilote le jeu.

## 3. Résultat

`NEXT_ACT_RULE_CONDITIONAL` distingue désormais : condition externe/factuelle (attendre qu'elle se
soit réellement produite, inchangé) vs condition d'état interne du personnage (juger sur le
comportement du joueur sur plusieurs échanges récents, sans exiger que le sentiment soit énoncé
mot pour mot, et sans compter les propres émotions jouées par le personnage comme preuve).

`NextActReadinessPromptBuilder` gagne un second exemple (condition d'état interne) montrant un
jugement par tendance générale plutôt que par liste de signes précis à retrouver verbatim.

`mvn -pl chat -am clean test` : 209 tests, tous verts (pas de test ajouté — ces prompts n'ont pas
d'assertion verbatim sur leur contenu exact). `mvn compile` (racine) : aucune régression
cross-module.

Playtest réel (Ollama + modèle local) non fait par Claude — à valider par l'utilisateur sur le
scénario `chatscenarios/temp/`.
