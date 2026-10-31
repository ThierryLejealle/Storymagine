# 2026-10-30 - Squelette du scénario « Cinq filles et une chaise »

## Évolution demandée
Préparer la trame très minimaliste d'un scénario de chat : Alex (18 ans) largué, sa sœur Lina et
quatre amies colocs le « capturent » pour le consoler et l'aider avec les filles. Histoire soft,
entre adultes consentants. Les fiches et les actes seront complétés par l'auteur.

## Ce qui a été touché
- `chatscenarios/cinq-filles-et-une-chaise/scenario.txt` : en-tête, 5 actes (capture,
  interrogatoire, jeux, thérapie de choc, dernier verre) avec conditions `[NEXT ACT]` suggérées,
  `{npc-starts}` en ouverture de l'histoire et de chaque acte (Lina, Erika, Lexi, Lina, Lucie).
- `chatscenarios/cinq-filles-et-une-chaise/characters/` : fiches de Lina, Lexi, Megan, Erika, Lucie
  (champs entre parenthèses à remplacer). Secrets pour Lina, Megan et Lucie. Megan n'est pas
  méchante : joueuse un peu sadique (gages cruelles inutilement), elle aime bien Alex.
- Lina part à la fin de l'analyse (début de l'acte 3, elle parle en premier puis part) : acte 4
  ouvert par Megan, la règle « Alex dit stop = tout s'arrête » passe à toutes les filles. Megan est
  la seconde meneuse, les autres ne la freinent pas. Le départ de Lina utilise `{npc-absent Lina}`
  (répété dans les actes 3, 4 et 5 : non hérité entre actes frères) ; l'acte 3 est donc ouvert par
  Lexi et l'au revoir de Lina est dans la narration d'ouverture.
- Nouvel acte « Les adieux » entre la thérapie et le dernier verre : Lexi, Megan et Erika font
  chacune un câlin à Alex et donnent un dernier conseil avant de partir (Lina absente, Lucie reste).
  Chaque conseil dépend du caractère de la fille et de ce qu'Alex a dit à l'interrogatoire.
- Ordre des actes : capture, jeux, interrogatoire, thérapie de choc, adieux, dernier verre (jeux avant
  l'analyse : changer les idées d'abord, puis analyse et traitement ; Lina reste pour l'analyse).
- Niveaux d'activité : acte 1 Lina active, autres présentes ; acte 2 (jeux) toutes actives ;
  acte 3 (interrogatoire) Erika active, autres passives ; acte 4 Lina absente (départ au début, narration) ; acte 5 seule Lucie reste
  (les autres filles partent en boîte rejoindre Lina), scène Lucie/Alex, fin heureuse possible.
- Scénario adaptatif : l'acte 2 fait avouer ses points faibles à Alex et Erika les résume en trois
  points ; l'acte 4 s'appuie dessus.
- Megan ne s'arrête que si Alex insiste fortement et répète son refus (secret de sa fiche).
- Aucun prompt Java modifié.

## Résultat
Squelette prêt à remplir. Le 5e acte (épilogue Alex/Lucie) est ajouté par rapport à la demande
initiale et peut être supprimé.
