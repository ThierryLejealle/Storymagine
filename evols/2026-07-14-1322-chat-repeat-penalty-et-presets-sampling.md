# 2026-07-14 13h22 - Chat : repeat_penalty réglable + 3 préréglages de sampling (Fable)

## Demande

L'utilisateur a proposé un jeu de valeurs trouvé ailleurs (temp 1.25, top_p 0.95, top_k 64,
min_p 0.02, repeat_penalty 1.02) et a demandé l'avis de Fable : ces paramètres sont-ils tous
utiles pour Gemma, lesquels exposer, et trois préréglages nommés "Sérieux"/"Fun"/"Complètement
barré" (utilisables, sans dérive qui casse l'histoire). Suite validée : "met tout et propose les
trois règles... Passe en Fun par défaut".

## Consultation Fable (~30K tokens, confirmé avant envoi)

Verdict sur le jeu proposé : `min_p 0.02` + `repeat_penalty 1.02` combinés sont trop laxistes pour
un petit Gemma en RP long (dérives de noms propres, boucles/mirroring après 20-30 tours) ;
`top_k 64`/`top_p 0.95` sont les valeurs officielles Gemma 3, sans reproche ; `temperature 1.25`
acceptable seulement si `min_p` reste ≥ 0.04. `top_k`, `top_p` et `min_p` font tous les trois le
même travail (couper la queue de distribution) — redondants entre eux, c'est le plus strict qui
l'emporte ; les leviers qui comptent vraiment : température, min_p, repeat_penalty. Recommandation
UI : exposer `repeat_penalty` (seul vrai levier contre les boucles), ne pas exposer `top_p`
(redondant avec min_p, source de confusion).

## Ce qui a été touché

- `commun/coeur/ports/GenerationOptions.java` : ajout du champ `repeatPenalty` (6e champ).
- `commun/infra/OllamaAdapter.java` : `buildOllamaRequest` applique
  `options.repeatPenalty() != null ? options.repeatPenalty() : repeatPenalty` (au lieu de
  toujours utiliser le défaut de l'adaptateur, jamais surchargeable au préalable).
- `chat/coeur/domaine/session/GenerationSettings.java` : ajout de `topP` et `repeatPenalty`.
  `topP` n'est jamais un réglage manuel direct dans l'UI (redondant avec min_p) — seul un
  préréglage le fait varier.
- `chat/coeur/domaine/agent/roleplaynarrator/RoleplayNarrator.java` : nouveau
  `REPEAT_PENALTY_DEFAULT = 1.08` ; les défauts actuels (temp 1.0, top_k 100, top_p 0.98,
  min_p 0.05) correspondent déjà exactement au préréglage "Fun" validé par Fable — confirmés
  comme le comportement par défaut de session, comme demandé ("Passe en Fun par défaut").
- `chat/ui/web/ChatWebServer.java` : `/settings` lit/écrit désormais `topP` et `repeatPenalty`.
- `chat.html` : nouveau champ "Repeat penalty" dans le panneau ; nouvelle rangée de 3 boutons
  préréglages ("Sérieux", "Fun", "Complètement barré") qui appliquent en un clic les 5 valeurs de
  la table Fable (température/top_k/top_p/min_p/repeat_penalty), y compris `top_p` bien que non
  affiché comme champ manuel. Placeholder top-K corrigé (40 → 100, était resté périmé depuis le
  relèvement du défaut la fois précédente).

| Paramètre | Sérieux | Fun (défaut) | Complètement barré |
|---|---|---|---|
| temperature | 0.7 | 1.0 | 1.3 |
| top_k | 64 | 100 | 100 |
| top_p | 0.95 | 0.98 | 0.99 |
| min_p | 0.10 | 0.05 | 0.04 |
| repeat_penalty | 1.05 | 1.08 | 1.10 |

## Résultat

`mvn clean test` (4 modules) au vert, 97 tests côté chat (comportement par défaut inchangé
puisque "Fun" == anciens défauts, donc pas de régression de test existante). Trois préréglages
utilisables en un clic, `repeat_penalty` réglable finement en plus.
