# 2026-07-10 19h46 — PlanCoherenceCritic : ajout de la consigne, priorité sur tout le reste

## 1. Demande

En analysant un run réel (`scenarios/1998/generated/2026-07-10_18h37/`), plusieurs faux positifs
des critics de plan/livre ont été tracés à une seule et même cause : la description (consigne) du
chapitre était incomplète au moment du run, or plusieurs critics (`StoryCausalCritic`,
`StoryNarrativeCritic`, `PlanNarrativeCritic`) ne signalent jamais un élément qui découle
directement de la consigne — mais `PlanCoherenceCritic`, lui, ne recevait pas du tout la consigne
en entrée, contrairement aux autres.

Exemple observé : `PlanCoherenceCritic` (fichier `007_PlanCoherenceCritic_2.md`) a signalé en
DEFAUT_SIGNIFICATIF que la séquence 2 d'un chapitre fait revenir le groupe de 4 amis autour d'une
table alors que l'`intention_de_scene` de la séquence 1 était d'isoler le couple — alors que la
consigne du chapitre demande explicitement une alternance de moments à deux et de moments de
groupe. Ce même chapitre a aussi révélé que ce finding précis relevait en réalité d'un jugement de
progression narrative (mandat de `PlanNarrativeCritic`, pas de `PlanCoherenceCritic`, qui exclut
lui-même explicitement "la progression narrative" de son périmètre) — mais la décision retenue a
été de ne pas ajouter une règle d'exclusion ciblée, et de traiter la cause structurelle commune :
donner la consigne à `PlanCoherenceCritic`, avec la même primauté que les autres critics lui
donnent déjà, ce qui neutralise ce type de faux positif sans avoir à énumérer les cas.

Demande explicite de l'utilisateur : passer la description du chapitre à `PlanCoherenceCritic`,
et préciser dans son prompt que la description prime sur tout le reste (fiches personnage,
contraintes, état des entités) — pas seulement "n'est jamais un défaut", comme formulé chez les
autres critics, mais explicitement prioritaire en cas de conflit avec une fiche ou un fait établi.

Delta de prompt présenté et validé avant écriture (règle projet).

## 2. Ce qui a été touché

### `PlanCoherenceCriticInput.java`
Nouveau champ `description` (avant `chapterGoal`).

### `PlanCoherenceCriticStep.java`
Passe désormais `chapter.description()` en plus de `chapter.goal()`.

### `PlanCoherenceCritic.java`
- Nouveau préambule dans `SYSTEM` : la consigne (description) et l'objectif du chapitre font foi
  et **priment sur tout le reste** — fiches personnage, contraintes, état des entités — pas
  seulement "non fautifs". Remplace l'ancienne ligne "Si l'objectif du chapitre est fourni..."
  (absorbée par le nouveau préambule, plus fort — anti-duplication).
- Nouvelle section `"Consigne de l'auteur (ce chapitre)"` dans le prompt utilisateur, ajoutée
  avant `"Objectif du chapitre"` (même ordre que dans `PlanNarrativeCritic`), tronquée à
  `ctx*4/12` caractères comme le fait déjà `PlanNarrativeCritic` pour ce même champ.

## 3. Résultat

`PlanCoherenceCritic` reçoit maintenant la même consigne que `PlanNarrativeCritic` et
`PlanGoalCritic`, avec une primauté explicitement plus forte (prime sur fiches/contraintes/état
des entités, pas seulement "non fautive"). Compilation vérifiée depuis la racine du projet
(`mvn -q compile`, aucune erreur).

Décision explicite : ne pas ajouter de règle d'exclusion ciblée ("ne compare jamais
intention_de_scene entre séquences") pour le chevauchement de rôle avec `PlanNarrativeCritic`
identifié en cours d'analyse — la primauté de la consigne couvre déjà le cas concret observé, et
une règle négative supplémentaire aurait dupliqué/complexifié le prompt sans certitude de couvrir
plus de cas réels.

Aucune modification apportée à `StoryCausalCritic`, `StoryNarrativeCritic` ou `PlanNarrativeCritic`
dans cette session (déjà correctement conçus pour ce problème) ni à `PlanGoalCritic` (non
investigué). Aucun changement de code n'a été fait pour l'opération de Christelle ou le détour par
Paris (voir chapitres concernés) : la cause était uniquement une description de scénario
incomplète, corrigée directement par l'auteur dans `chap_4.yaml`/`chap_2.yaml`.
