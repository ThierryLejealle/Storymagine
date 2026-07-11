# 2026-07-11 20h50 — Fix PlanGoalCritic : ne plus exiger l'explicitation des sentiments

## 1. Demande

Run réel (scénario 1998, chapitre Collioure) : `PlanGoalCritic` a signalé en
`DEFAUT_SIGNIFICATIF` deux cas où un sentiment de la directive ("Thierry se sent de plus en plus
amoureux", "Thierry devient mélancolique car il comprend que Christelle va bientôt partir") était
rendu dans le plan uniquement par du comportement (regard, sourire, silence, tête baissée), sans
être énoncé comme un fait explicite. L'utilisateur juge que ce n'est pas un vrai défaut : un
sentiment montré par des actions est une pratique d'écriture normale, un lecteur humain l'infère
naturellement — et forcer l'explicitation produirait une narration qui "raconte" au lieu de
"montrer", donc artificielle (exactement ce que `NaturalityCorrector` combat déjà en aval — les
deux agents tiraient en sens opposés).

Fix demandé à Fable via une question strictement ciblée (prompt actuel + description condensée
du problème, sans le plan/directives complets) — 27 322 tokens, sans lecture de fichier.

## 2. Ce qui a été touché

### `agent/plan/goalcritic/PlanGoalCritic.java`
Deux retouches dans `buildSystem()` :
- **DEFAUT_SIGNIFICATIF** : "delivered only by implication" devient "delivered only by an
  implication the reader could not reasonably draw" — ne qualifie plus automatiquement toute
  livraison implicite de défaut.
- **Nouvelle règle FEELINGS** ajoutée juste après (avant OUT OF SCOPE) : un sentiment/état
  intérieur montré par des actions/comportements est considéré comme délivré — jamais besoin
  d'être énoncé. Tolérance bornée aux sentiments uniquement : les faits concrets, actions et
  raisons causales restent contrôlés comme avant. Un contre-exemple de 3 lignes est inclus
  (cohérent avec la préférence du projet pour les exemples avec les petits modèles) :
  `"Mara grows melancholic." → "Mara falls silent and looks away." → delivered, report nothing.`

## 3. Résultat

`mvn compile` : OK. `mvn test -pl redacteur,commun` : **19 + 17 tests, 0 échec**.
