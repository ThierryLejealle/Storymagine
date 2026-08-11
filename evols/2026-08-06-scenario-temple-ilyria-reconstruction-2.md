# 2026-08-06 - Reconstruction du scénario temple-ilyria (dossier, prémisse, fiches PNJ)

## Évolution demandée
Le scénario Ambaya/Laina/Mina/Sheera (fix de transition de la veille) s'est avéré ne correspondre à
aucun dossier `chatscenarios/{nom}/` existant dans ce projet : `scenario.txt` traînait à la racine de
`chatscenarios/`, sans fiches personnage. `chatscenarios.root.default` dans `chat.properties` pointe
vers un chemin hors projet (`C:/dev/llm/story/chat`) — confirmé par l'utilisateur comme le vrai
chemin utilisé par l'application, donc non modifié. L'utilisateur a demandé de continuer à travailler
ce scénario dans ce projet (`chatscenarios/` local) malgré tout, et de le rendre réellement jouable.

Appliqué en même temps que l'évolution multi-fichiers (voir `evols/2026-08-06-scenario-multi-fichiers.md`) : le
scénario (découpé la veille en un seul gros `scenario.txt`) a été redécoupé en `scenario-N.txt`.

## Ce qui a été touché
- `chatscenarios/temple-ilyria/` créé ; `scenario.txt` + `scenario-1.txt` à `scenario-27.txt`
  (un fichier par chapitre `#` existant, découpage mécanique, aucun contenu perdu ni déplacé).
- `scenario.txt` : ajout de la ligne `Joueur : Alex` (convention du projet, absente jusqu'ici) et
  d'un bloc `#SCENARIO` (Monde/Prémisse/Ton) qui manquait entièrement — sans lui, le LLM n'avait
  aucun contexte permanent (monde, enjeu, PNJ) au-delà du texte de l'acte courant à partir du
  chapitre 2. Le monde inclut la société amazone d'Ambaya (guerrières au pouvoir, éducation genrée
  déséquilibrée) précisée par l'utilisateur.
- `scenario-1.txt` (premier acte, "Premiers regards") : deux corrections conformes aux règles
  documentées dans `chatscenarios/rules.md` (piège déjà vécu et corrigé sur les 5 autres scénarios,
  raté ici) — (1) Laina, Mina et Sheera nommées explicitement dans les lignes `[...]` du tout premier
  acte (§2.4, "règle d'or" anti-boucle/réponse vide de `SpeakerSelector`) ; (2) fuite en 2e personne
  ("Laina t'entraîne...", "te regarde...") corrigée en 3e personne (§2.4/CLAUDE.md).
- Trois fiches personnage créées ex nihilo (aucune n'existait) : `laina.txt` (chef sérieuse mais
  attentionnée), `mina.txt` (guerrière massive, fleur bleue, secret : rêve de romantisme désuet malgré
  son gabarit), `sheera.txt` (scout halfeline espiègle, grimpe sur les grandes pour épargner ses
  jambes, secret : aime qu'on la porte). Traits et secrets dictés par l'utilisateur.
- Vérification via le script recommandé par `rules.md` §5 (test jetable, supprimé après usage) :
  85 actes chargés, Cast à 3 PNJ, tours NARRATOR d'ouverture nomment bien tout le monde.

## Résultat
Le scénario est structurellement complet et jouable dans ce projet : dossier conforme, prémisse
permanente pour le LLM, Cast complet, premier acte conforme aux règles anti-boucle du projet.
`chat.properties` n'a pas été touché (confirmé volontairement externe par l'utilisateur).
