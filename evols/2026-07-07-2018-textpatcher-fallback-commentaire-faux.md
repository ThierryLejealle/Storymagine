# 2026-07-07 20h18 - TextPatcher : repli sur commentaire parasite côté FAUX

## Description de l'évolution demandée

Après déploiement du correctif précédent (`CorrectionParser` + `TextPatcher`), un nouveau
run réel (`scenarios/1998/generated/2026-07-07_17h22`) montrait encore des `replace miss`.
Analyse fichier par fichier :

- La majorité des corrections passaient déjà (confirmé par simulation) — le correctif
  précédent fonctionne.
- Nouveau cas trouvé : le LLM (StyleCorrector, NaturalityCorrector) colle parfois son
  propre commentaire directement sur le **FAUX** (pas seulement sur JUSTE comme observé
  précédemment) : `FAUX: "...ce cadre idyllique." (Répétition structurelle)` — alors que
  le texte source ne contient jamais cette parenthèse.
- Autres cas trouvés dans le même run, **hors périmètre code** (fidélité de citation du
  LLM, pas un problème de guillemets) : citation abrégée avec des `...`, citation d'une
  phrase déjà remplacée par une passe précédente, dérive de paraphrase (mot changé en
  citant). Signalés à l'utilisateur comme nécessitant une évolution de prompt séparée,
  soumise à validation.

Demande : corriger le cas FAUX-avec-commentaire en code (sans toucher aux prompts), en
repli uniquement si la citation complète ne matche pas (pour ne pas casser le cas
légitime où le commentaire final fait partie du texte source, ex. DeusInMachinaCorrector).

## Ce qui a été touché

- **`TextPatcher.java`** (`commun/coeur/domaine/text`) : `apply()` tente d'abord un match
  intégral (tolérant aux guillemets), puis, seulement en cas d'échec, retente avec un
  commentaire final entre parenthèses retiré du `search`. Ajout de `stripTrailingQuotes`
  pour nettoyer le guillemet qui reste exposé une fois le commentaire retiré (ex.
  `...idyllique." (Répétition structurelle)` → retrait du commentaire laisse
  `...idyllique."` → retrait du guillemet final laisse `...idyllique.`, qui matche alors
  le texte source).

## Résultat

Vérifié par simulation Python rejouant la logique exacte contre le texte source réel du
run avant modification du Java : le cas `"(Répétition structurelle)"` qui échouait est
désormais retrouvé. Compilation et tests (`commun`, `redacteur`) verts.

Reste hors périmètre (nécessite une évolution de prompt, soumise à validation séparée) :
citations abrégées avec `...`, citation de phrases déjà modifiées par une passe
antérieure, dérive de paraphrase.
