# 2026-08-06 - NEXT_ACT : éviter de couper un échange en cours sans rapport avec la condition

## Évolution demandée
L'utilisateur a signalé un risque de transition d'acte artificielle : dès que la condition
`[NEXT ACT]` littérale d'un acte est remplie, le modèle peut écrire le marqueur immédiatement même
si une interaction sans rapport (une conversation, une réparation d'équipement) est encore ouverte —
le tour suivant colle alors le bloc `[...]` du prochain acte juste après, ce qui coupe net.

Trois pistes discutées avec l'utilisateur avant de trancher :
1. Des tags d'acte (`[QUOTIDIEN]`/`[INTERRUPTION]`) pour faire varier le comportement du prompt selon
   le type d'acte — écarté : ajoute une branche de jugement supplémentaire pour le modèle et un champ
   sur `ScenarioAct`/le parser, pour un problème qui se résout sans toucher au code.
2. Répéter une clause de clôture dans chaque acte concerné du scénario (~24 actes camp/nuit) —
   commencé puis abandonné : viole la règle du projet contre la duplication d'une même consigne (voir
   CLAUDE.md, section PROMPT), et une même clause à maintenir en 24 endroits est fragile.
3. **Une seule règle générique dans le prompt fixe, appliquée automatiquement à tous les actes** —
   retenue.

Un premier libellé ("assure-toi qu'aucune conversation en cours n'est laissée en suspens") a été
rejeté par l'utilisateur : une négation absolue pousse le modèle à devoir prouver une absence, ce
qui est le genre de vérification floue qui, sur un petit modèle, peut déclencher une boucle de
réflexion ou un blocage (déjà documenté dans ce projet, `chatscenarios/rules.md` §4.2). Reformulé en
instruction positive et bornée à une seule réponse.

## Ce qui a été touché
`chat/src/main/java/storymagine/chat/coeur/domaine/session/ChatPromptBuilder.java`,
`NEXT_ACT_RULE_CONDITIONAL` — phrase ajoutée après la règle existante :

> "If wrapping up now would cut off an unrelated exchange still in progress (a conversation, an
> action someone is mid-way through), finish it in this same reply, then write "[NEXT ACT]" right
> after."

S'applique automatiquement à tous les actes qui définissent leur propre condition `[NEXT ACT]` (donc
aux 85 actes de temple-ilyria comme à tout autre scénario existant), sans rien dupliquer dans les
fichiers de scénario.

## Résultat
Filet de sécurité générique, pas une garantie : reconnaître qu'un échange est "en cours" reste un
jugement du modèle, et l'instruction ne corrige que la coupe pendant le tour du modèle lui-même — pas
le style "note brute" de l'ouverture du nouvel acte (bloc `[...]` injecté tel quel par
`ChatSession.advanceAct`, mécanisme volontaire et distinct — voir
`evols/2026-08-05-fix-transition-camp-premier-soir.md`). Reste à traiter au cas par cas dans le texte du
scénario si observé ailleurs. Suite de tests chat complète repassée verte après l'ajout.
