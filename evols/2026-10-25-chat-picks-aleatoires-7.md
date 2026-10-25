# 2026-10-25 - Chat : paragraphes aléatoires dans le scénario, [PICK] et [PICK-UNIQUE]

## 1. Évolution demandée

Rapprocher le chat de story en rendant le scénario variable : un tag `[PICK: nom]` remplacé, au
lancement du scénario, par un paragraphe tiré au hasard dans `picks/nom.txt` (paragraphes séparés par
`---` ou plus). Les fichiers du scénario sont recopiés avec les tags résolus. Variante
`[PICK-UNIQUE: nom]` : tirage sans répétition. Pas de cohérence entre tirages. Le joueur ne voit un
tirage que s'il tombe dans un texte déjà affiché. À documenter, exemple compris.

## 2. Ce qui a été touché

- Domaine (`coeur/domaine/pick`, nouveau) : `PickTag`, `PickParagraphs` (découpe sur `---`, CRLF
  toléré), `PickLibrary`, `PickResolver` (pur ; mémoire des tirages partagée entre `scenario.txt` et
  ses suites ; `PICK-UNIQUE` exclut ce que `PICK` ou `PICK-UNIQUE` a déjà donné, échec explicite si
  plus rien). Port `RandomnessPort` (`coeur/ports`).
- Infra : `JavaRandomnessAdapter`, `PickFileLibrary` (lit `picks/`), `RunLayout.resolvedDir`,
  `RunArchiver` (`resolved/` accepté dans les zips). `ChatFileStorageAdapter.loadScenario` lit la copie
  `run/current/resolved/` si elle existe, sinon résout et l'écrit (seulement si un tag est présent).
- Service : `openSession(reset)` et `restartSession` rechargent le scénario APRÈS le reset (sinon la
  partie neuve garderait les tirages de la précédente) ; `loadSavePoint` relit le scénario de la
  sauvegarde. Aucun prompt modifié.
- Docs : `chatscenarios/rules.md` (§2.6 avec exemple complet, §1, §6.1, §6.7), `specs/chat.md`
  (section « Contenu aléatoire », arborescence, zips), skill `chat-scenario-writing`.
- Tests : `PickResolverTest` (13), `ChatFileStoragePicksTest` (7) : tirage figé sur la durée du run,
  nouveau tirage après reset, sauvegarde qui emporte ses tirages, suites de scénario, scénario sans tag
  sans effet de bord, fichier de picks manquant.

## 3. Résultat

Compilation de tout le projet OK ; les 20 nouveaux tests passent.

Points d'attention : (1) tant qu'une partie est en cours, modifier `scenario*.txt` est sans effet (⟳
relit la copie résolue) jusqu'à « Recommencer ». (2) Charger un scénario avec tags, même via le testeur
de scénario, écrit `run/current/resolved/` et fige donc un tirage. (3) Une ligne `[...]` contenant un
tag exige des paragraphes d'une seule ligne. (4) Les fiches personnage ne sont pas résolues. (5) Non
vérifié à la main dans le navigateur ni avec un vrai LLM.
