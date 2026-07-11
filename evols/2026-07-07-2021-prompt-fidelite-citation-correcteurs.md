# 2026-07-07 20h21 - Consigne de fidélité de citation dans les 4 Correcteurs

## Description de l'évolution demandée

Analyse d'un run réel : au-delà des bugs de parsing déjà corrigés (guillemets, commentaire
sur JUSTE), deux défauts de citation restaient impossibles à corriger en code car ils ne
sont pas des artefacts de format mais des écarts de fidélité du LLM :
1. Commentaire parasite collé directement sur **FAUX** (pas seulement JUSTE) :
   `FAUX: "...ce cadre idyllique." (Répétition structurelle)`.
2. Citation abrégée avec des points de suspension :
   `FAUX: "...déposant Thierry et Catherine. ... Ils reçoivent leurs clés..."`.

Delta proposé et validé par l'utilisateur avant écriture (règle projet "modification de
prompt LLM — validation obligatoire") : ajouter, juste après l'exemple FAUX/JUSTE de
chaque correcteur, une règle courte avec un contre-exemple concret ("MAUVAIS"/"BON").

## Ce qui a été touché

Ajout du même bloc de consigne (adapté à la casse/vocabulaire de chaque fichier — accents
absents dans Style/Naturality qui utilisent un style sans diacritiques) dans les 4
correcteurs, juste après leur exemple FAUX/JUSTE existant :

- `DeusInMachinaCorrector.java` — après l'exemple type 4 (LISTE NARRATIVISÉE)
- `NaturalityCorrector.java` — après l'exemple, avant `ATTENTION : conserve le sens...`
- `StyleCorrector.java` — après les deux exemples, avant `Exemple 2 - aucun defaut`
- `ProofreaderCorrector.java` — après l'exemple, avant `ATTENTION : une seule phrase...`

Texte ajouté (variante avec accents) :
```
FAUX doit être recopié mot pour mot, en entier — jamais de "..." pour abréger un passage long.
Ni FAUX ni JUSTE ne portent de commentaire ou de justification après la citation.
  MAUVAIS : - FAUX: "...ce cadre idyllique." (Répétition structurelle)
  BON     : - FAUX: "...ce cadre idyllique."
```

## Résultat

Compilation et tests (`commun`, `redacteur`) verts. Aucune valeur/logique de code
modifiée — uniquement le texte des 4 prompts. À observer sur un prochain run réel pour
confirmer la baisse des citations abrégées/commentées ; les prompts restent faciles à
ajuster si le petit modèle continue d'ignorer la consigne.
