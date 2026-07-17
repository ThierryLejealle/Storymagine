# 2026-07-17 10h15 - Cause racine trouvée : les fins de ligne CRLF cassaient la détection des titres

## 1. Contexte

Suite de l'investigation du scénario externe cassé (voir
`evols/2026-07-17-1008-fix-fresh-plafonne-un-seul-beat-sans-actes.md`). L'utilisateur avait d'abord
mal copié le contenu ("j'avais mal copié le texte au début"), d'où un `scenario.txt` vide lors du
premier essai. Une fois le vrai fichier placé dans `chatscenarios/temp/` (dossier temporaire,
jamais committé — voir `feedback_never_commit_temp_chats_stories`), le bug se reproduisait encore
malgré un contenu non vide : "là ce n'est pas vide et ça file."

## 2. Diagnostic

`file scenario.txt` → "CRLF line terminators" confirmé, et `cat -A` montrait un `^M` (retour
chariot `\r`) en fin de chaque ligne. Le fichier venait d'un éditeur/environnement Windows classique
(hors du dépôt git, où `core.autocrlf` gère normalement la conversion).

Mécanisme exact : `ScenarioOutlineParser.splitHeadings` découpe le texte sur `"\n"` uniquement
(`raw.split("\n", -1)`). Avec des fins de ligne CRLF, chaque ligne garde donc un `\r` final
invisible (ex. `"#SCENARIO\r"`). La regex de détection `HEADING = Pattern.compile("^(#{1,6})\\s*(.*)$")`
est testée avec `.matches()` (correspondance sur la CHAÎNE ENTIÈRE). Or en Java, `.` ne matche PAS
`\r` par défaut (seul `Pattern.DOTALL` changerait ça) — donc `(.*)$ ` ne peut jamais absorber ce `\r`
final, et `.matches()` échoue systématiquement sur CHAQUE ligne, même une ligne `"# Acte"` parfaitement
valide. Résultat : `splitHeadings` ne trouve JAMAIS aucun titre, `scenario.acts()` reste vide, et le
fichier entier retombe en prémisse plate — d'où le fallback qui déverse tous les blocs `[...]`
qu'elle contient (voir l'évol précédente).

Vérifié avec le vrai fichier de l'utilisateur via un script jetable (`ScenarioCheck.java`, supprimé
après usage) : avant le fix, 0 acte détecté ; après, 3 actes, un beat chacun, une seule bulle
d'ouverture — comportement attendu confirmé de bout en bout sur le fichier réel.

## 3. Ce qui a été touché

`ScenarioOutlineParser.parse(String rawInput)` : normalise `\r\n` puis `\r` isolé en `\n` en tout
début de fonction, avant tout découpage — la prémisse et le texte de chaque acte, dérivés de ce
texte normalisé, en héritent automatiquement en aval (`Teaser.extractAll` sur la prémisse plus tard
dans `ChatSession.fresh()` en profite aussi).

Portée du bug limitée à `ScenarioOutlineParser` : les autres parsers du module (détection de
`# Nom`/`# SECRET` dans une fiche perso, ligne "Joueur : X") utilisent `.strip()` avant comparaison,
qui gère déjà `\r` correctement — non affectés.

## 4. Tests

Nouveau `ScenarioOutlineParserTest.windowsCrlfLineEndingsDoNotHideTheHeadingsFromTheParser` — même
contenu que `scenarioMarkerDelimitsThePremiseFromTheFirstAct` mais avec `\r\n`, vérifie que les
titres sont bien détectés malgré ça.

`mvn -pl chat -am clean test` : 209 tests (+1), tous verts. `mvn compile` (racine) : aucune
régression cross-module.
