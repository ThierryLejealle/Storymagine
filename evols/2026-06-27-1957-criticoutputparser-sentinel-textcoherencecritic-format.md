# 2026-06-27 19h57 - Fix parser sentinelles + format [RIEN] TextCoherenceCritic

## Evolution demandee

Le ChapterCoherenceCritic a produit un output mal formé :
- `DEFAUT_SIGNIFICATIF : Aucun. Les actions et les observations...` au lieu de `[RIEN]`
- `AMELIORATION : La description... [RIEN]` — `[RIEN]` mélangé à une vraie observation

Le parser `CriticOutputParser` testait `content.equals("AUCUN")` — la phrase "Aucun. Les actions..."
n'égale pas "AUCUN", donc le faux DEFAUT_SIGNIFICATIF était compté, faisant chuter le score à ~6.4
au lieu de ~9 pour un texte jugé cohérent par le LLM.

## Ce qui a été touché

### CriticOutputParser.java
- Ajout méthode privée `isSentinel(String s)` : détecte les sentinelles par `equals` ET `startsWith`
  (couvre "AUCUN", "AUCUNE", "RIEN", "NONE", "NEANT" et leurs variantes suivies d'un espace)
- `parseProblems` : utilise `isSentinel(pn.toUpperCase())` au lieu des égalités codées en dur
- `calculateScore` : utilise `isSentinel(content)` au lieu des égalités codées en dur

### TextCoherenceCritic.java (prompt)
- Section FORMAT STRICT : reformulée avec instruction explicite ("ecrire exactement : ... [RIEN]")
- Ajout instruction "Interdit : ajouter du texte apres [RIEN]..."
- Ajout exemple concret de sortie vide (AMELIORATION/DEFAUT_SIGNIFICATIF/DEFAUT_MAJEUR tous [RIEN])

## Resultat attendu

- "Aucun. [explication]" traité comme sentinelle → ne compte plus comme problème
- Format [RIEN] renforcé dans le prompt → moins de dérives du modèle

## TODO restant

Vérifier et aligner le FORMAT STRICT des 3 autres critics :
- `TextNarrativeCritic` — même prompt faible, même risque
- `PlanNarrativeCritic` — déjà des exemples, à vérifier
- `PlanCoherenceCritic` — déjà des exemples, à vérifier
