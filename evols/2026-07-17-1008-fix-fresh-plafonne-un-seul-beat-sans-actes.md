# 2026-07-17 10h08 - Fix : un scénario sans actes n'ouvre plus qu'avec un seul beat

## 1. Demande

Bug rapporté sur un scénario personnel de l'utilisateur (fonctionnait "avant qu'on attaque le
multi-personnage") : "ça défile tous les actes et aucun n'est affiché dans le suivi des actes. Mais
tous les textes des actes sont affichés comme si tu avais tout passé d'un coup" — et ce avant même
le premier message, sans appel LLM.

## 2. Diagnostic

Investigation par les logs de session fournis par l'utilisateur (dans un dossier temporaire
`chatscenarios/temp/`, jamais committé — voir consigne explicite de l'utilisateur à ce sujet) :
`act.txt` contenait "0" (aucun acte actif) et `history.md` contenait trois tours NARRATOR
consécutifs, sans aucun tour PLAYER/LLM entre eux — confirmant que les trois textes avaient été
déversés d'un seul coup à la création de la session, pas via une progression normale (qui exige un
appel LLM par transition).

Cause : `scenario.acts()` était vide pour ce fichier (raison exacte non reproductible, le fichier
original ayant depuis été vidé par l'utilisateur) — dans ce cas, `ChatSession.fresh()` retombe sur
`Teaser.extractAll(scenario.premise())`, qui scanne TOUTE la prémisse et extrait TOUS les blocs
`[...]` qu'elle contient, sans plafond. Si la prémisse contient plusieurs scènes narratives
distinctes (pensées pour s'enchaîner comme des actes informels), elles sont toutes affichées d'un
coup au lieu d'une seule au démarrage.

## 3. Ce qui a été touché

`ChatSession.fresh()` : le chemin "pas d'actes" ne garde désormais que le PREMIER bloc `[...]` de
la prémisse — même convention qu'un scénario avec actes (toujours un seul beat d'ouverture par
transition). Les blocs suivants sont ignorés plutôt que tous affichés ensemble.

`chatscenarios/rules.md` (§2.4) : nouvelle règle documentée, avec la recommandation de migrer vers
de vrais actes (`#`/`##`) si un scénario a plusieurs scènes d'ouverture distinctes.

## 4. Tests

Nouveau `ChatSessionTest.freshWithoutActsOnlyUsesTheFirstBracketedLineEvenIfThePremiseHasSeveral` —
prémisse à 3 blocs `[...]`, vérifie qu'un seul tour NARRATOR est créé (le premier).

`mvn -pl chat -am clean test` : 208 tests (+1), tous verts. `mvn compile` (racine) : aucune
régression cross-module.

## 5. Mise à jour — cause racine trouvée et corrigée séparément

La cause exacte a finalement été identifiée une fois l'utilisateur en mesure de fournir le vrai
fichier (non vidé) : fins de ligne **CRLF** (Windows), qui cassaient totalement la détection des
titres `#`/`##` dans `ScenarioOutlineParser`. Voir
`evols/2026-07-17-1015-fix-crlf-casse-detection-titres.md` pour le détail complet — ce fix-ci
(plafonner à un seul beat) reste utile en soi comme filet de sécurité défensif, indépendamment de
la cause du fichier de l'utilisateur.
