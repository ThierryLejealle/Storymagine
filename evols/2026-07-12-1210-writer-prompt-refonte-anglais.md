# 2026-07-12 12h10 — Writer : refonte du prompt système en anglais

## 1. Demande

Même traitement que `ChapterPlanner` (fiche `2026-07-12-1145-...`) pour le prompt système de
`Writer`, à partir du log `069_SequenceWriter_1.md` (scénario 1998). Consigne explicite : envoyer
le prompt système + l'exemple complet à Fable, avec avertissement renforcé anti-surapprentissage
("EN AUCUN CAS N'OPTIMISE LE PROMPT POUR CET EXEMPLE PRECIS"), demander une traduction en anglais
ET de vraies améliorations (pas juste une traduction), en respectant la structure dynamique du
prompt (préfixe réécriture optionnel, contrainte de longueur variable, règle d'ouverture à 3
variantes).

Revue manuelle du texte proposé par Fable avant écriture — 3 corrections apportées :
1. Clause perdue dans la traduction ("sauf si les directives te le demandent explicitement" sur
   la règle de continuité) — restaurée.
2. Le bloc RÉÉCRITURE proposé par Fable était une invention (il n'avait reçu que la description
   conceptuelle du bloc, pas son texte source — signalé honnêtement par Fable lui-même) et
   perdait la référence à la section "### Problèmes à corriger" — gardé l'original traduit
   fidèlement à la place.
3. Ton affaibli sur la consigne "stitch" (passé d'impératif à simple étiquette) — restauré.

Une 4e correction ajoutée après relecture finale (avant validation) : la règle "never copy a beat
sentence as-is" était trop large — elle interdirait aussi de reprendre un dialogue déjà écrit par
le Planner dans un beat, alors que le vrai défaut observé (log 252, séquence précédente) portait
uniquement sur de la narration recopiée verbatim. Ajout d'une exception explicite pour les
dialogues cités entre guillemets dans un beat.

Note : Fable a identifié seul (sans qu'on le lui signale) le défaut réel observé précédemment
dans `252_SequenceWriter_15.md` — un beat de narration recopié mot pour mot dans le texte final
("le regard perdu dans le noir") — et l'a corrigé par la nouvelle règle "expand each beat into
prose; never copy a beat sentence as-is and never keep its number."

Discussion parallèle (non résolue, notée pour plus tard) : l'utilisateur observe qu'en usage
libre (chat direct avec gemma4, sans scriptage), la prose lui semble meilleure qu'en sortie du
pipeline scripté (beats + toutes les couches de contraintes). Hypothèses discutées : surcharge de
contraintes simultanées, beats déjà mécaniques/granulaires, appel froid sans montée en confiance
narrative (vs conversation qui s'installe). Décision : traiter ce fix Writer maintenant (bug fix
valable indépendamment), remettre la question d'architecture plus large à plus tard.

## 2. Ce qui a été touché

### `Writer.java` — `buildSystem()`
Entièrement réécrit en anglais (structure dynamique conservée à l'identique : préfixe réécriture
conditionnel, contrainte de longueur avec `in.minWords()` variable, règle d'ouverture à 3
variantes selon stitch/texte précédent/première séquence). Ajouts substantifs :
- Interdiction de recopier verbatim un beat de narration (avec exception dialogue).
- Interdiction explicite de titres/en-têtes/numéros de beat/notes dans la sortie.
- Repli "approfondir plutôt qu'inventer" si la longueur minimale est atteinte avant la fin des
  beats (évite l'invention d'événements de remplissage).
- Exemple générique d'incarnation de trait de personnage (ongles rongés → tapote des doigts).
- Règle "les sensoriels sont des ingrédients, pas une checklist" (évite le dump en bloc).
- Ligne de langue explicite en fin de prompt ("Write the prose in French.").

Préfixe RÉÉCRITURE traduit fidèlement à la main (pas la version Fable), référence à
"### Problèmes à corriger" conservée en français — doit correspondre exactement au titre généré
par `buildUser()`.

Genericité vérifiée : les seuls exemples du prompt (Marc/rivière — repris de la session
`ChapterPlanner` —, ongles rongés/tapote des doigts) sont inventés, aucune référence au scénario
en cours.

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec (aucun test
couplé au texte français exact du prompt Writer). Non encore observé en usage réel.

## Note — pas traité ici

Discussion ouverte sur le niveau de scriptage global (beats granulaires + empilement de
contraintes vs conversation libre) — explicitement mise de côté par l'utilisateur pour cette
session, à reprendre séparément si besoin.
