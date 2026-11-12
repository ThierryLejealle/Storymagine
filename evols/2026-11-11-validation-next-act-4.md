# 2026-11-11 - Phase de validation des NEXT ACT

## Évolution demandée
- Dire dans les skills qu'il faut vérifier la présence d'un `[NEXT ACT]` dans chaque scène, et qu'il est
  possible et cohérent avec ce que le scénariste a fait faire aux PNJ et à la situation. Exemple de faute : le
  PNJ A lance un défi au joueur et le `[NEXT ACT]` est « le joueur accepte le défi de B ».
- Les consignes des PNJ doivent mener vers la condition (ça peut prendre plusieurs échanges) ; si besoin,
  guider les PNJ en ajoutant une consigne.
- Une condition déjà vraie au départ est un défaut (« ils sont dans l'auberge » / condition « ils sont dans
  l'auberge »). Pas de sortie alternative ni d'embranchement : l'histoire n'a qu'une route, le joueur est censé
  la suivre.
- Une vraie phase de validation `[NEXT ACT]` dans le playbook, courte.
- Première étape, avant toute question de cohérence : l'IA rédactrice vérifie qu'elle a pensé à formuler un
  `[NEXT ACT]` pour chaque scène jouable sauf la dernière (liste des titres, scènes jouées / conteneurs,
  « oui » ou « MANQUE », formuler les manquants), sans s'en remettre à l'avertissement du validateur.

## Ce qui a été touché
- `chat/scenario-builder/` : nouvelle phase 5 `phase-5-next-act-check.md` (présence, tableau d'une ligne par
  scène avec la consigne de PNJ citée, trois contrôles : possible, cohérent avec des consignes qui mènent vers
  lui, pas déjà vrai ; continuité par une transition narrative si besoin). L'ancienne phase 5 devient
  `phase-6-finish.md`. Renvois mis à jour : README (six phases, environ 12 000 tokens), phase 4,
  `ref-writing-tips.md`, `phase-2-plan.md`, `skeleton.md` (section `## NEXT ACT check` du plan).
- Skill `chat-scenario-builder` : liste des six phases.
- Skill `chat-scenario-writing` : règle dans « Écrire une bonne condition », section « Vérification des
  `[NEXT ACT]` » (même tableau, mêmes trois contrôles).
- Guide (§7.2) : règle 8 et vérification des `[NEXT ACT]` ; `specs/chat.md`.
- Modèle `ecole-brumeval` : la scène 2 se terminait quand l'esprit se réfugie dans un endroit clos, mais rien
  dans la scène ne l'y poussait ; une phrase ajoutée (original et copie du skill).
- Une première version avait six tests (dont « le joueur peut dire non » avec une condition « accepté ou
  refusé ») : abandonnée, car elle revenait à écrire une sortie alternative.

## Résultat
Aucun code touché, aucun prompt de PNJ ou d'agent modifié. Les deux modèles du skill passent
`validation-scenarios.bat` sans erreur ni avertissement. Le moteur ne vérifie pas la cohérence d'une condition :
c'est une étape de relecture.
