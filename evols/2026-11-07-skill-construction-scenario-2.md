# 2026-11-07 - Skill de construction de scénario pour petit LLM (`chat/scenario-builder`)

## Évolution demandée
Un skill utilisable par n'importe quelle IA (pas seulement Claude) pour construire un scénario de chat complet,
destiné à un petit LLM à contexte limité : structure des fichiers et commandes présentées de façon synthétique,
interview pour fixer l'objectif et l'intérêt de l'histoire (public, genre), plan d'une page, puis un fichier par
grande étape, des fiches personnage écrites une à la fois, et le style à la fin.

Décisions de l'auteur : livrable dans le module chat (comme le guide) ; skill et scénario en anglais, la langue
du scénario n'étant demandée que si la spec d'entrée ne la précise pas ; squelette annoté plutôt qu'un scénario
d'exemple (un petit modèle imite le sujet de l'exemple) ; pas de test anti-dérive (la mise à jour du skill est à
ma charge à chaque évolution du chat).

## Ce qui a été touché
- `chat/scenario-builder/` (nouveau) : `README.md`, cinq phases (`phase-1-brief.md` à `phase-5-finish.md`),
  `ref-format.md`, `ref-commands.md`, `ref-writing-tips.md` (mini guide d'écriture pour LLM, condensé des §3 et §7
  du guide humain), `skeleton.md`. Mémoire entre les phases : un seul fichier `plan.md`
  (brief, cast, tableau d'étapes, faits établis), écrit en `.md` à la racine du scénario (un `.txt` commençant
  par `# ` y serait pris pour une fiche de l'ancien format et ferait refuser le scénario).
- `.claude/skills/chat-scenario-builder/SKILL.md` : pointeur vers le dossier.
- Test `ScenarioBuilderFormatTest` : un scénario écrit comme le skill l'enseigne (étiquettes anglaises, deux
  fichiers, `{npc-only}`, un modèle, `plan.md` à la racine) se charge et se valide sans erreur.
- Documentation : règle de maintenance dans `CLAUDE.md`, pointeurs dans `specs/chat.md`, le guide (§7.3) et le
  skill `chat-scenario-writing`.

## Résultat
618 tests verts depuis la racine. Aucun prompt d'agent modifié.
**Cible de contexte** : le skill vise un modèle local à 60k tokens ou plus (Gemma, Qwen), précisé après la première version : le dossier entier (environ 6 000 tokens) se donne d'emblée, les phases s'enchaînent dans une conversation, `plan.md` est relu avant chaque fichier. Les limites de taille des fichiers du scénario restent, car elles protègent le contexte du jeu et non celui de l'IA qui écrit. Un mode strict (une phase à la fois, on oublie chaque fichier) reste décrit pour un modèle qui perd le fil ou n'a que 16k utilisables.
**À savoir** : les mots-clés d'en-tête `Joueur :` et `Langue :` restent français même dans un scénario anglais
(seuls ceux-là, `#SCENARIO`, les titres et les lignes `[...]` sont analysés ; les étiquettes `Point de départ`,
`Ce qui doit se passer`, `Quand écrire [NEXT ACT]` sont libres). Le skill n'a pas été essayé avec un vrai petit
LLM. Suite prévue, après accord : changement de langue en cours de partie (bouton, anglais par défaut).

## Essai avec un petit modèle (Haiku)
Le playbook a été donné à un petit modèle Anthropic, sur une spec (musée, gardien de nuit, 3 étapes, 2 PNJ au départ
plus 1 arrivant en étape 2). Le scénario produit (`generated/skill-test/museum/`, non versionné) se charge et se
valide sans erreur ni avertissement. Frictions relevées, toutes corrigées dans `chat/scenario-builder/` :
- aucune consigne si l'IA ne peut ni interroger l'utilisateur ni lancer la validation : section README « If you cannot ask or run anything » et rubrique `## Assumptions` dans `plan.md` ;
- taille d'une fiche contradictoire (80-150 vs 80-120 + 30-60) : `ref-format.md` dit 110 à 180 mots ;
- une seule « NEXT ACT fact » par étape alors qu'une étape a plusieurs scènes : phase 4 précise que la dernière scène reprend le fait du plan, les autres ont leur propre fait ;
- placement de `{npc-only}` ambigu : juste après `Tone:` ; temps de narration de `style.txt` : présent par défaut ; ligne `[...]` ajoutée à la checklist de la phase 4 ;
- réponse intermédiaire à une question de la phase 1 : prendre la première qui convient ;
- pour un mystère, ligne « réponse » et vérification des indices (phase 2) ;
- défaut observé dans la sortie : la fiche d'Hale disait « arrive à la 2e étape » (futur dans une fiche) : phase 3 interdit de citer étapes et arrivées.
