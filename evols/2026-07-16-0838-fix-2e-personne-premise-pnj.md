# 2026-07-16 08h38 - Correction de la 2e personne dans les prémisses/beats injectés aux PNJ

## 1. Demande

Suite au playtest de `quete-des-amazones` (log `011_RoleplayNarrator_10.md`, réponse de Sheera) :
"ATTENTION : Prémisses -> tu as laissé filer un truc DANGEREUX là. Comme le LLM sait qu'il n'est
pas Alex. Il faut faire à la troisième personne !!!!! Corriges tout ça !!!"

Diagnostic confirmé : `scenario.premise()` et `act.text()` (y compris les lignes `[...]`) sont
injectés tels quels dans le system prompt de CHAQUE PNJ (`ChatPromptBuilder.build()`), juste après
son assignation de personnage ("YOUR CHARACTER: Sheera"). Ces textes étaient écrits à la 2e
personne ("Tu es Alex...", "Toi et Sylka...", "vous venez de..."), ce qui contredit frontalement
l'identité du PNJ dans son propre prompt.

Choix entre deux options proposées à l'utilisateur : (a) ne plus injecter les lignes `[...]` dans
le prompt des PNJ, ou (b) les réécrire à la 3e personne façon narrateur. Option (b) retenue,
confirmée par l'utilisateur — `Teaser.java` explique que les beats restent volontairement dans
`act.text()` "so the LLM reads them too, in place" (contexte de mise en scène utile aux PNJ), donc
les en retirer aurait été contraire à une décision de conception existante. L'utilisateur a aussi
demandé un `CLAUDE.md` dans `chatscenarios/` documentant la règle pour l'avenir.

## 2. Ce qui a été touché

Réécriture à la 3e personne (contenu et ton inchangés, seule la personne grammaticale change) sur
les 5 scénarios existants :
- `quete-des-amazones/scenario.txt` — Prémisse + les 2 blocs `[...]` de l'acte "Premiers regards"
- `auberge-deux-pnj/scenario.txt` — Prémisse + les 2 blocs `[...]` de l'acte "L'arrivée"
- `carnet-bleu/scenario.txt` — Prémisse + le bloc `[...]` de l'acte "Le carnet bleu"
- `temple-noir/scenario.txt` — les 2 blocs `[...]` (ce scénario n'a aucun heading `#`, donc tout
  le fichier devient `premise()` — les blocs `[...]` y étaient bien injectés)
- `temple-noir-actes/scenario.txt` — 7 blocs `[...]` répartis sur 6 actes

Aucun changement de code : le bug était entièrement dans le contenu des scénarios, pas dans
`ChatPromptBuilder`/`ScenarioOutlineParser`/`Teaser`.

Nouveau fichier `chatscenarios/CLAUDE.md` : règle "toujours écrire Prémisse et lignes `[...]` à la
3e personne", avec l'explication du mécanisme (transmis à chaque PNJ, pas seulement narré au
joueur). `chatscenarios/rules.md` mis à jour (§2.2 et §2.4) avec un renvoi vers cette règle et un
exemple corrigé.

## 3. Résultat

Aucun test unitaire concerné (changement de contenu narratif, pas de code). Vérifié par grep
qu'aucune ligne `[...]` ni aucune Prémisse des 5 scénarios ne contient plus de formulation à la
2e personne ("tu", "toi", "vous", "ton/ta/tes/votre/vos").

Non encore fait : rejouer `quete-des-amazones` pour confirmer que Sheera (et les autres PNJ) ne
recommencent plus à se confondre avec Alex sur ce point précis — à vérifier à la prochaine
session de jeu.
