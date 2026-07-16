# 2026-07-15 21h57 - Introductions renforcees + bug de structure d'actes corrige

## 1. Demande

"Tu n'as rien mis qui introduit le scenario auberge... Je ne sais pas qui je suis, ou je suis, qui
ils sont et ce qu'on doit faire" — l'intro d'`auberge-deux-pnj` (une seule ligne atmospherique sur
le tonnerre) ne donnait ni identite, ni noms des PNJ, ni objectif. Demande de verifier tous les
scenarios.

## 2. Audit des 4 scenarios

Verifie en relisant les "[...]" (story-beats, seuls affiches au joueur en debut de partie) de
chaque `scenario.txt` :
- `auberge-deux-pnj` : une ligne, purement atmospherique, aucun nom, aucun objectif — insuffisant.
- `temple-noir` (sans actes) : une ligne situationnelle correcte mais sans nommer Sylka ni
  l'objectif (la Larme de Vaskorreth) — plus faible que sa version `-actes`.
- `temple-noir-actes` : deja bon (2 lignes, nomme Sylka + objectif explicite).
- `carnet-bleu` : deja bon (identite + situation claires des la 1ere ligne).

## 3. Ce qui a ete touche

- `chatscenarios/auberge-deux-pnj/scenario.txt` : 2e ligne de beat ajoutee, nommant Elena et Marcus
  par leur role (tenanciere / ancien garde).
- `chatscenarios/temple-noir/scenario.txt` : ligne de beat ajoutee avant l'existante, nommant Sylka
  et l'objectif (recuperer la Larme de Vaskorreth), alignee sur `temple-noir-actes`.

## 4. Bug trouve en verifiant (pas juste la richesse du texte — la structure d'actes elle-meme)

En testant le rendu reel des beats d'ouverture (`ChatSession.fresh()` + `ChatFileStorageAdapter
.loadScenario`), `auberge-deux-pnj` affichait 3 lignes au lieu des 2 attendues pour l'acte 1 — la
ligne de l'acte 2 ("Autour du feu") s'y mélangeait. Cause : `# L'arrivée` suivi de `## Autour du
feu` (nesting `#`/`##`) n'est PAS une paire d'actes sequentiels dans la convention du projet — c'est
parent/enfant. Or `ScenarioOutlineParser.flatten()` : seules les feuilles (headings sans
sous-heading) deviennent des actes jouables ; un parent avec enfant n'est JAMAIS "current" lui-meme,
son contenu est simplement herite par son (unique) enfant. Resultat : le fichier original n'avait
qu'UN SEUL acte reel (fusion des deux sections), pas deux comme prevu — la condition [NEXT ACT]
ecrite sous "# L'arrivée" ne pouvait donc jamais jouer son role de transition separee.

**Fix** : `## Autour du feu` devient `# Autour du feu` (siblings top-level, pas parent/enfant) —
confirme par temple-noir-actes/carnet-bleu qui utilisent bien `#`/`##` pour de la hierarchie
thematique volontaire (chapitre → sous-scenes), pas pour un simple "acte suivant".

Verifie apres fix : `auberge-deux-pnj` a desormais bien 2 actes distincts, l'acte 1 n'affiche que
ses 2 lignes propres.

## 5. Resultat

`mvn -pl chat -am test` : propre. Verification par script Java direct (charge chaque scenario,
liste les tours NARRATOR d'une session fraiche) plutot que suppose — a permis de reperer le bug de
structure qu'une simple relecture du texte n'aurait pas revele.

Session de test en cours de l'utilisateur sur `auberge-deux-pnj` au moment du fix — recharger le
scenario (bouton ⟳) ou redemarrer recommande pour repartir sur la structure a 2 actes correcte.
