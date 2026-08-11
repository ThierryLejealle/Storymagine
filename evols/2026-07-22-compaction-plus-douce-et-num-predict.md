# 2026-07-22 - Compaction moins agressive + relevement du plafond de generation

## Demande 1 : compaction moins agressive
Retour joueur : la jauge de contexte passait de 50% a 10% d'un coup lors d'une compaction — trop
brutal, risque de perte de qualite/detail dans le resume genere en une seule fois sur un gros
volume de texte.

## Diagnostic
`ChatServiceImpl` compactait (repliait les vieux tours dans le resume) des que les tours bruts
depassaient un seuil qui grossit avec la fenetre de contexte (jusqu'a 50% via
`ChatContextBudget.TURNS_FRACTION`), mais ne gardait ensuite toujours qu'un nombre FIXE de tours
(`KEEP_RECENT_TURNS = 4`, soit 2 echanges) — sans lien avec la taille de la fenetre. D'ou l'ecart
brutal entre "avant" (jusqu'a 50% d'une fenetre potentiellement large) et "apres" (un plancher fixe
minuscule).

## Delta (valide explicitement par le joueur, y compris les pourcentages exacts)
- `ChatContextBudget` : remplace `TURNS_FRACTION` (50% implicite) par deux pourcentages
  independants — `TRIGGER_PERCENT = 70` (declenche la compaction plus tard qu'avant) et
  `KEEP_PERCENT = 25` (nouvelle methode `keepTokensTarget`, remplace le plancher fixe). Les deux
  pourcentages sont volontairement independants : garder nettement moins que le seuil de
  declenchement laisse de la marge avant la prochaine compaction, et un ecart plus petit entre les
  deux fait que chaque repliage traite moins de texte a la fois (plus doux pour la fidelite du
  resume), au prix de compactions plus frequentes — le compromis demande par le joueur.
- `ChatServiceImpl` : `KEEP_RECENT_TURNS` (fixe) remplace par `MIN_KEEP_TURNS = 2` (plancher de
  dernier recours seulement) + nouvelle methode `keepTurnCount` qui approxime combien de tours
  recents garder bruts a partir de la taille moyenne d'un tour dans la fenetre courante (meme
  esprit d'estimation grossiere que `ChatContextBudget.estimateTokens`, pas de passage tour par
  tour).
- Tests : `ChatContextBudgetTest` (2 nouveaux cas pour `keepTokensTarget`, 1 renomme/ajuste pour le
  nouveau 70%) et `ChatServiceImplTest` (2 tests ajustes aux nouveaux nombres — la mecanique testee
  ne change pas, seuls les seuils numeriques changent).
- `ChatSummarizer.md` mis a jour (references perimees a `KEEP_RECENT_TURNS` et a l'ancien
  `SummaryBudget`, qui n'etait deja plus utilise ici).

## Demande 2 : plafond de generation trop juste
Retour joueur : la longueur du "thinking" (reflexion) est tres variable d'un tour a l'autre et
reste parfois tres longue meme au plafond actuel.

## Delta
`chat.properties` : `ollama.num-predict` releve de 3250 a 4400.

## Resultat
Suite de tests `chat` complete verte (217 tests). Aucun prompt LLM (texte envoye au modele) n'a
ete modifie par ces deux changements — uniquement des constantes de configuration/logique de
service.
