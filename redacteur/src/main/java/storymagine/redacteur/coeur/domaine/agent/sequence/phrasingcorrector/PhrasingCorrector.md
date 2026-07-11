# PhrasingCorrector

## Rôle
Nouvel agent (2026-07-10). Deuxième moitié du split de l'ancien `ProofreaderCorrector` :
`GrammarCorrector` a gardé les fautes mécaniques (grammaire, orthographe, accord, conjugaison),
`PhrasingCorrector` couvre le reste des erreurs de génération résiduelles d'un LLM.

## Ce qu'il détecte
Quatre catégories, chacune avec un exemple FAUX/JUSTE dans le prompt :
1. Mot manquant — la phrase est incompréhensible sans lui.
2. Mot aberrant — un mot rend la phrase absurde (signalé seulement si évident).
3. Phrase interrompue — la phrase s'arrête avant sa fin.
4. Calque — tournure copiée d'une autre langue, non naturelle en français.

Grammaire, orthographe, accord et conjugaison sont hors périmètre (rôle de `GrammarCorrector`) —
mais le prompt ne nomme aucun autre agent : le périmètre est défini de façon positive et
autoportée ("erreurs de génération résiduelles... parce qu'il manque un mot, un morceau, qu'un
mot erroné a été utilisé, ou qu'une tournure calque une autre langue"), pas par exclusion d'un
agent voisin — un prompt d'agent ne doit pas dépendre de la structure du pipeline (voir
`agent/CLAUDE.md`).

## Format de sortie
Même format que tous les Correctors : `CORRECTIONS:` / `FAUX:` / `JUSTE:`, sentinelle
`PAS D'ERREUR` si rien à signaler. Parsé par `CorrectionParser.parse`, appliqué via
`TextPatcher`/`WriteWorkflow#applyCorrections` (jamais de `String.replace` brut).

## Retry
`retryStrategy() = RetryStrategy.SINGLE_PASS` — une seule passe, jamais de relance sur sa propre
sortie (contrairement à `GrammarCorrector` en `DECREASING_AND_RATIO_THRESHOLD`). Choix délibéré :
l'agent est neuf, non éprouvé, on ne veut pas qu'il s'auto-relance tant que son comportement n'est
pas observé en usage réel.

## Position dans la chaîne
Placé **en premier** dans la chaîne des correcteurs de séquence :
`PhrasingCorrector → DeusInMachinaCorrector → NaturalityCorrector → StyleCorrector → GrammarCorrector`
(voir `orchestrator/CLAUDE.md`, schéma Write Workflow).

## Origine
Créé le 2026-07-10 en split de `ProofreaderCorrector` (catégories mots manquants/mal choisis,
phrases bancales/calques — voir historique dans `GrammarCorrector` pour la partie grammaticale).
Prompt relu par un modèle Claude en simulation d'un petit LLM cible (Gemma), deux itérations :
ajout d'exemples et de critères décidables, puis simplification (suppression d'une clause de
périmètre redondante avec "UNIQUEMENT" et d'un contre-exemple jugés superflus après une première
lecture).
