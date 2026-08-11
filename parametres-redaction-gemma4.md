# Paramètres de rédaction — Gemma 4

Synthèse d'une consultation Fable (sans accès outil, réponse basée sur ses connaissances générales)
sur le paramétrage d'inférence à utiliser pour une rédaction narrative naturelle et agréable avec Gemma 4, via Ollama.

**Avertissement de Fable** : sa connaissance s'arrête à janvier 2026, où la dernière génération Gemma documentée
est Gemma 3 / Gemma 3n. Il n'a donc pas d'information fiable et spécifique sur "Gemma 4". Les valeurs ci-dessous
sont extrapolées depuis la famille Gemma 2/3, qui partage vraisemblablement la même philosophie de calibration.
À valider en priorité avec `ollama show gemma4 --modelfile` (les valeurs par défaut livrées par l'éditeur)
et par un A/B test à seed fixée sur vos propres scénarios.

## Tableau de synthèse

| Paramètre | Valeur recommandée | Rôle |
|---|---|---|
| `temperature` | 1.1 – 1.2 | Pousse la variété lexicale et les tournures moins convenues |
| `min_p` | 0.05 (plage 0.03–0.08) | Coupe les tokens vraiment improbables, proportionnellement au meilleur token |
| `top_p` | 1.0 (désactivé) | Redondant avec min_p, contre-productif si activé en même temps |
| `top_k` | 0 (désactivé) ou 64 | Redondant avec min_p ; 64 en repli si min_p non utilisé |
| `repeat_penalty` | 1.0 – 1.03 max | Anti-répétition, mais Gemma s'y dégrade vite au-delà |
| `repeat_last_n` | ~256 | Fenêtre sur laquelle porte `repeat_penalty` |
| `mirostat` | désactivé (0) | Alternative de contrôle de la "surprise", déconseillée ici |
| `num_ctx` | 8192+ | Taille du contexte — évite la troncature silencieuse de l'historique |
| `seed` | non fixé en production | Utile uniquement pour du debug/A-B test reproductible |

## Détail par paramètre

### `temperature` (1.1 – 1.2)
Contrôle le caractère aléatoire du choix du prochain mot : plus elle est haute, plus le modèle
s'autorise des tournures inhabituelles plutôt que la suite la plus probable. La famille Gemma est
calibrée pour tourner "chaud" (1.0 de base), contrairement à d'autres modèles réglés autour de 0.7 ;
en dessous de 0.8 la prose devient plate et prévisible. Monter à 1.1–1.2 renforce la variété pour
la rédaction créative, mais au-delà d'environ 1.3 un petit modèle perd en cohérence (incohérences
de faits, de personnages). Antagonisme : une température haute combinée à un `min_p` bas et un
`repeat_penalty` élevé cumule les effets de désorganisation — n'ajuster qu'un seul de ces trois à la fois.

### `min_p` (0.05, plage 0.03–0.08)
Filtre les tokens dont la probabilité est inférieure à une fraction de celle du token le plus probable
(contrairement à `top_p`/`top_k` qui tronquent en absolu). Il s'adapte donc dynamiquement à la confiance
du modèle à chaque étape : large marge de manœuvre quand plusieurs suites sont plausibles, coupe sec
quand une seule s'impose. C'est ce qui permet de monter la température sans obtenir de sorties absurdes.
Redondant avec `top_p` et `top_k` : les utiliser ensemble sur-tronque la distribution et annule une
partie de l'effet recherché — on désactive les deux autres quand `min_p` est actif.

### `top_p` (1.0, désactivé)
Tronque la distribution en ne gardant que les tokens dont la somme cumulée des probabilités atteint
un seuil (0.95 typiquement). Fait à peu près le même travail que `min_p` mais de façon moins adaptative
(le seuil ne varie pas selon la confiance du modèle). Redondant et antagoniste avec `min_p` : les deux
combinés sur-tronquent la distribution, ce qui réduit la variété au lieu de la préserver. On le laisse
à 1.0 (désactivé) dès que `min_p` est utilisé ; il ne redevient utile qu'en profil de repli, sans `min_p`.

### `top_k` (0, désactivé ou 64 en repli)
Limite le choix aux k tokens les plus probables, quelle que soit leur probabilité réelle. Encore une
troncature "en absolu", au même titre que `top_p`, et donc redondante avec `min_p` pour la même raison.
Utile uniquement en garde-fou simple si l'on n'utilise ni `min_p` ni `top_p` : 64 est la valeur de
calibration Gemma, plus généreuse que le défaut Ollama (40) qui bride un peu la diversité lexicale
attendue en narratif.

### `repeat_penalty` (1.0 – 1.03 max)
Pénalise la probabilité des tokens déjà apparus récemment, pour éviter les boucles et tics de langage
("her eyes widened" en continu, etc.). Antagoniste direct avec la qualité grammaticale sur Gemma :
au-delà d'environ 1.05–1.1, le modèle se met à éviter aussi les mots fonctionnels qui reviennent
légitimement (articles, prénoms de personnages), ce qui produit un style artificiel — l'effet inverse
du but recherché. Sur Gemma, on le garde donc quasi désactivé, et on traite la répétition d'abord par
d'autres leviers (variation du prompt, `repeat_last_n`, voire changement de moteur d'inférence).

### `repeat_last_n` (~256)
Définit la fenêtre de tokens récents sur laquelle `repeat_penalty` s'applique. Un lien direct avec
ce dernier : une pénalité activée mais une fenêtre trop courte (défaut Ollama à 64) ne capte pas les
répétitions de tournures d'un paragraphe à l'autre, tandis qu'une fenêtre trop longue pénalise à tort
les prénoms de personnages récurrents sur plusieurs pages. Pas d'effet seul si `repeat_penalty` est
proche de 1.0 — les deux fonctionnent en paire.

### `mirostat` (désactivé)
Mode de contrôle alternatif qui vise à maintenir un niveau de "surprise" (perplexité) constant plutôt
que d'appliquer un seuil de troncature fixe — objectif proche de `min_p` mais par une autre méthode.
Antagoniste structurel : `mirostat` désactive `top_p`, `top_k` et `min_p` quand il est actif, il ne
peut donc pas se combiner avec le profil recommandé ci-dessus. Son réglage est moins prévisible et
le consensus actuel est que `temperature` + `min_p` obtient un résultat au moins aussi bon en étant
plus simple à ajuster. À réserver en plan B si la répétition persiste malgré tout le reste.

### `num_ctx` (8192+)
Ce n'est pas un paramètre d'échantillonnage mais la taille du contexte que le modèle peut voir. Sous
Ollama, le défaut historique (2048–4096 tokens) tronque silencieusement le début du prompt ou de
l'historique de conversation : le modèle "oublie" la fiche personnage ou le début du chapitre, ce qui
produit des incohérences et un ton robotique qui ressemble à un problème de sampling mais n'en est pas
un. Sans lien d'antagonisme avec les autres paramètres — c'est souvent le réglage à corriger en premier,
avant même de toucher `temperature` ou `min_p`.

### `seed`
Fixe la graine du générateur aléatoire pour obtenir une sortie reproductible à prompt et paramètres
identiques. Utile uniquement pour comparer deux réglages de façon contrôlée (A/B test) ; à ne jamais
fixer en production, sinon toutes les relances produisent un texte quasi identique et on perd la
variété recherchée.
