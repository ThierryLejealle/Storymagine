# demo-npc-do : une visite guidée

Montre le **mode DO** d'un personnage : `{npc-mode do on|off nom|*}`. Le personnage tient la plume du narrateur
pendant un moment : il raconte la visite et décrit aussi ce que fait le personnage du joueur.

- `{npc-mode do on Camille}` est écrit dans le titre parent « La visite commentée » : le mode vaut pour ses deux
  salles (héritage). L'acte suivant, « Le banc du hall », est hors de ce titre : Camille redevient une simple
  interlocutrice.
- `{npc-starts Camille}` : Camille ouvre la scène. Comme elle est en mode DO, c'est une ouverture avec la plume
  (comme `{npc-do Camille}`).
- Une petite étoile ★ s'affiche sur la vignette de Camille tant que le mode est actif.
- Le joueur continue à taper normalement. Un seul personnage en mode DO ici : avec plusieurs, ils se doubleraient.

À regarder : pendant la visite, Camille décrit les gestes d'Alex ; sur le banc, elle ne le fait plus.
Guide : §2.12, §4.5.
