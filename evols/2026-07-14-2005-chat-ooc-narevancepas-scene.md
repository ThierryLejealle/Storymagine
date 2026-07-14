# 2026-07-14 20h05 - Chat : OOC ne doit plus faire progresser la scène

## 1. Évolution demandée
L'utilisateur a signalé que lorsqu'il parle en OOC (hors-jeu) au LLM, celui-ci comprend et répond
bien à la note, mais enchaîne systématiquement en faisant progresser la scène dans la même
réponse — alors qu'il voudrait juste une réponse hors-jeu, sans suite narrative imposée, et
reprendre la main lui-même au tour suivant en jeu.

Cause identifiée directement dans le prompt (pas besoin de consultation Fable pour le diagnostic) :
la règle OOC disait littéralement "Follow the note, then continue the roleplay" — le modèle fait
exactement ce qui est écrit.

Nuance ajoutée par l'utilisateur en cours de discussion : un OOC n'est pas toujours une question,
ça peut être une instruction ("sois plus méfiant à partir de maintenant") ou une demande de
modification du scénario — la reformulation ne doit donc pas supposer une forme question/réponse.

Fable a été consulté (poursuite de la conversation d'agent des fiches précédentes, avec la
contrainte habituelle "pas d'exploration du dépôt") pour affiner la formulation. Deux ajustements
apportés à ma proposition initiale : clarifier explicitement le POINT DE VUE de la réponse (l'auteur
derrière le personnage, pas le personnage lui-même — sinon un petit modèle glisse vers "je" =
le personnage donc vers la narration), et fusionner deux négations empilées ("do not advance the
scene or act as your character") en une seule formulation opérationnelle ("stop there"). Delta
présenté à l'utilisateur et validé ("go") avant écriture.

## 2. Ce qui a été touché
- `chat/src/main/java/storymagine/chat/coeur/domaine/session/ChatPromptBuilder.java`,
  constante `SYSTEM_FORMATTING` : remplacement de la puce OOC existante par une version qui
  précise "Answer as yourself, not as your character, and stop there" + exemple wrong/right sur
  un cas instruction (pas question, pour couvrir explicitement ce cas et parce que c'est là que
  la dérive est la plus visible : le modèle a tendance à "prouver" qu'il applique l'instruction
  en la mettant en scène tout de suite).
- Vérifié : pas de collision avec la règle "Turn scope" (déclencheur différent, portée narrative
  en roleplay normal vs interdiction totale de narration en réponse OOC) ni avec "DO:" (préfixe
  distinct choisi consciemment par le joueur, règle OOC ne s'applique qu'aux messages tagués OOC).
- Compilation vérifiée (`mvn -pl chat -am compile`) : OK.

## 3. Résultat
Changement de prompt uniquement. Pas de test unitaire dédié (contenu de prompt). À valider par
l'utilisateur en session de chat réelle avec gemma4, en même temps que les deux règles ajoutées
dans la fiche précédente (`2026-07-14-1937-...`).
