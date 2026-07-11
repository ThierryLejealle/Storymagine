# Writer

## Rôle
Génère la prose d'une séquence narrative à partir de `WriterInput`.
C'est l'agent le plus riche en contexte : il reçoit personnages, directives, focus, lore, historique, état, etc.

Le Writer ne reçoit **que** les directives de la séquence en cours (`sequencePlan`/`sequenceDescription`,
déjà scopées à cette séquence), jamais le plan des autres séquences du chapitre — pour ne jamais
risquer de lui faire écrire ou même laisser deviner un événement d'une séquence future (spoiler).
`focusText`/`loreText`/`redactionConstraints` sont eux aussi déjà fusionnés (scénario + chapitre +
séquence) par le code Java appelant (`WriterStep`/`ScenarioFormatters`) avant d'arriver ici : le
Writer n'a aucun scoping à faire lui-même.

## Slots de contexte (proportionnels à contextWindow, ~4 chars/token)
| Slot | Taille |
|------|--------|
| entityState | 1/16 |
| focusText | 1/16 |
| loreText | 1/16 |
| redactionConstraints | 1/16 |
| styleGuide | 1/20 |
| charactersText | 1/8 |
| summary | 1/8 (`SummaryBudget`, résumé de l'histoire, tronqué — TODO : peut-être trop conservateur, à étudier) |
| writingExample | 1/16 |
| previousSequenceText | **aucune troncature** — texte intégral de la dernière séquence écrite du chapitre |

Budget total utilisateur : 60% du contexte (hors `previousSequenceText`, volontairement non
plafonné — voir evols/2026-07-11 sur la refonte de la règle d'ouverture). Un dépassement de
contexte est rattrapé automatiquement par `OllamaAdapter` (agrandissement + relance), donc le
risque n'est plus une troncature/perte silencieuse mais un aller-retour un peu plus lent.

## Ordre du prompt utilisateur — inverse de la liste de priorité

Le system prompt liste un ordre de priorité en cas de conflit (1 = gagne toujours) :
1. Directives détaillées de l'auteur — 2. Contraintes de rédaction — 3. Fiches personnages —
4. Guide de style — 5. Exemple de rédaction — 6. Focus — 7. Lore.

Les interdictions (expressions/schémas bannis) et les faits déjà établis (état des entités, texte
précédent) sont volontairement **hors** de cette liste : ce ne sont pas des éléments créatifs
pouvant entrer en conflit les uns avec les autres, mais des contraintes absolues toujours
respectées — les y intégrer aurait allongé une liste déjà jugée longue pour un petit LLM sans
gagner en clarté.

`buildUser()` place les sections dans l'**ordre inverse** de la liste de priorité (7 → 1) : le
moins prioritaire (Lore) en tête du prompt, le plus prioritaire (Directives) juste avant
l'instruction finale "Écris le texte...". Raison : un petit LLM pondère plus fort ce qui est
proche du point de génération (effet de récence) — placer la donnée qui doit gagner tous les
conflits loin de la fin du prompt serait contre-productif. Les interdictions/faits établis restent
en tête (hors arbitrage, donc hors de cet effet de récence par construction). `previousSequenceText`
reste la toute dernière section, après les Directives, car elle contraint directement la première
phrase générée.

Chaque titre de section reprend **exactement** le libellé de la liste de priorité (règle VITAL de
`agent/CLAUDE.md`) — ex. "Fiches personnages", "Contraintes de rédaction", pas de paraphrase.
Toutes les sections (y compris interdictions et cadre de la scène) utilisent désormais le même
format `PromptBuilder.section("Titre", contenu)` — plus de mélange entre "### Titre", en-têtes en
MAJUSCULES et `[Crochets]`.

## Logique de la directive
- Si `sequencePlan` (plan de séquence issu de ChapterPlanner) est fourni → utilisé comme directive
- Sinon → `sequenceDescription` du scénario
- Si `isRewrite` (`rewriteProblems != null`) → "### Problèmes à corriger" ajouté à la suite, dans
  cette même section (jamais mêlé au plan des autres séquences, ni tronqué : voir §"Mode réécriture")

## Règles d'ouverture — stitch > continuité > point d'entrée varié
Trois cas mutuellement exclusifs, dans cet ordre de priorité :
1. **`stitch` fourni** (consigne de transition explicite du scénario) → le system prompt l'encadre
   explicitement ("Applique cette consigne pour gérer le passage...") plutôt que de l'injecter brut
   sans contexte. Le texte de la séquence précédente reste montré (voir ci-dessous) mais SANS
   l'obligation stricte "ta première phrase DOIT en être la suite directe" — le stitch peut
   légitimement demander autre chose qu'une continuité directe (ellipse, saut temporel).
2. **Pas de stitch, séquence précédente existante** (`previousSequenceText` non vide) → règle par
   défaut "raccorde sans résumer, poursuis l'action", et le user prompt impose la continuité directe.
3. **Ni stitch ni séquence précédente** (première séquence du chapitre) → règle "point d'entrée
   varié" (liste de points d'entrée valables + interdiction du prénom seul/pronom sujet nu), **sans**
   comparaison à la séquence précédente : dans ce cas précis, le Writer n'a justement aucun texte
   précédent à comparer — l'ancienne formulation demandait une comparaison invérifiable.

`previousSequenceText` (renommé depuis `prevSentences`, qui ne portait que les 3 dernières phrases)
est désormais le texte **intégral** de la dernière séquence écrite du chapitre, sans troncature —
donne au Writer de quoi vraiment s'inspirer du style/rythme immédiat plutôt qu'un fragment coupé.

## Mode réécriture
Si `isRewrite=true` (`rewriteProblems != null`), le system prompt commence par un préfixe imposant
la correction des problèmes — ajoutés en section "### Problèmes à corriger" à la suite des
Directives détaillées de l'auteur, jamais mêlés au plan des autres séquences.

## Source Redacteur
`story.context.WriterContext.writeSequence`
