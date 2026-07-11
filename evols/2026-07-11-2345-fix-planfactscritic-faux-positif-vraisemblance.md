# 2026-07-11 23h45 — Fix PlanFactsCritic : faux positif sur vraisemblance de mise en scène

## 1. Demande

Premier vrai run sur le scénario 1998 avec les 5 nouveaux critics de plan : `PlanFactsCritic`
a produit un faux positif — "Thierry arrive seul au stand d'inscription alors qu'il est arrivé
avec sa sœur Catherine dans la séquence 1" signalé en AMELIORATION, alors que :
1. Ce n'est pas une contradiction avec un fait établi (rien n'impose que Thierry reste avec sa
   sœur), c'est un jugement de vraisemblance/mise en scène.
2. Le `check` fourni en entrée disait explicitement "ne critique pas la vraisemblance de
   l'histoire ou des transitions de séquences un peu rapide : c'est une histoire vraie" — le
   modèle l'a ignoré.
3. La séparation était même nécessaire : la consigne du chapitre demande que Thierry rencontre
   Christelle en tête-à-tête à ce moment précis.

Correction demandée à Fable via une question strictement ciblée (prompt actuel + exemple réel
condensé, sans les fiches/plan complets) pour limiter le coût — 27 810 tokens, sans lecture de
fichier.

## 2. Ce qui a été touché

### `agent/plan/factscritic/PlanFactsCritic.java`
Trois changements minimaux dans `buildSystem()`, rien d'autre touché :
- **AMELIORATION** : ajout de "You must be able to name the sheet, check or entity-state fact
  concerned. If you cannot, it is staging, not a defect: do not report it." — force tout
  signalement, même mineur, à s'ancrer sur un fait nommable.
- **OUT OF SCOPE** : ajout explicite "Never judge staging or plausibility: who is present or
  absent in a sequence, unexplained arrivals or departures, fast transitions..." + "Checks may
  also restrict what you are allowed to report: obey them." — rend les `checks` fournis
  opposables au-delà de leur simple contenu factuel.
- **Contre-exemple ajouté** après l'exemple Joran (inchangé, cas positif correct) : un cas
  quasi identique au vrai faux positif (deux personnages voyageant ensemble, l'un apparaît seul
  dans la séquence suivante), sortie attendue entièrement `[RIEN]`.

### Changement de politique (décision utilisateur, applique à ce fix)
Exemple Joran repassé du français à l'anglais (c'était la version d'origine proposée par Fable,
traduite en français plus tôt dans la session — décision annulée : "on tâchera de privilégier
l'anglais, comme fable" pour les exemples illustratifs désormais, pas seulement le squelette de
règles). Contre-exemple rédigé directement en anglais, avec des prénoms génériques (Elena/Marco)
plutôt que les vrais prénoms du scénario 1998 (Thierry/Catherine) qui étaient apparus dans la
proposition initiale de Fable — jamais de vrais noms de personnages utilisateur dans un exemple
de prompt générique.

Mémoire mise à jour : `feedback_prompt_examples_english.md` (nouveau). Les 4 autres critics de
plan (A/C/D/E) gardent leurs exemples en français pour l'instant — pas retraduits
automatiquement, rattrapage à faire si demandé.

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**.
