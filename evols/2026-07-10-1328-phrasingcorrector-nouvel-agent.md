# 2026-07-10 13h28 — Création de PhrasingCorrector, révision du prompt GrammarCorrector

## 1. Demande

Suite au split de l'ancien `ProofreaderCorrector` (déjà effectué : `GrammarCorrector` garde les
fautes mécaniques — grammaire, orthographe, accord, conjugaison), il manquait l'autre moitié du
split : un agent couvrant les erreurs de génération résiduelles d'un LLM (mot manquant, mot
aberrant, phrase interrompue, calque non idiomatique). Cet agent avait été discuté dans une
session précédente interrompue par une fermeture accidentelle de fenêtre, sans jamais être
sauvegardé sur disque.

Demandes explicites de l'utilisateur :
- Recréer cet agent, nommé `PhrasingCorrector`.
- Le placer **en premier** dans la chaîne des correcteurs de séquence.
- `retryStrategy() = SINGLE_PASS` ("RETRY : ONCE" — une seule passe, jamais de relance).
- Revoir aussi le prompt de `GrammarCorrector` en parallèle (un seul exemple pour 4 catégories,
  clause d'auto-vérification inopérante pour un petit LLM).

## 2. Ce qui a été touché

### Relecture des prompts (modèle Claude en simulation d'un petit LLM cible, type Gemma)
Les deux prompts ont été soumis à une relecture avant écriture (règle de validation obligatoire
sur les prompts LLM), avec itérations demandées par l'utilisateur :
- Suppression d'une clause d'exclusion nommant un autre agent par son nom
  ("traité par PhrasingCorrector") — jugée fragile : le prompt d'un agent ne doit pas dépendre
  de la structure du pipeline (combien d'autres correcteurs existent, comment ils s'appellent).
  Remplacée par une définition positive et autoportée du périmètre.
- Suppression d'une clause "Ne signale jamais X" redondante avec un "UNIQUEMENT" déjà présent en
  tête de prompt (duplication de consigne, interdite par le style du projet).
- Suppression d'un contre-exemple jugé superflu pour une première tentative.
- Ajout d'une clarification pour `PhrasingCorrector` : "Ne change jamais le sens... Si un mot ou
  un morceau manque, complète en restant cohérent avec le reste du texte, en faisant au plus
  simple" — sans cette précision, l'interdiction de changer le sens était incohérente avec la
  catégorie MOT MANQUANT/PHRASE INTERROMPUE qui exige d'inventer un contenu absent.

### Nouveaux fichiers
- `agent/sequence/phrasingcorrector/PhrasingCorrector.java` (+ `Input`/`Output`/`.md`)
- `orchestrator/write/PhrasingCorrectorStep.java`

### Fichiers modifiés
- `agent/sequence/grammarcorrector/GrammarCorrector.java` — prompt reformulé (périmètre positif
  "erreurs mécaniques vérifiables par une règle", un exemple par catégorie, suppression de la
  clause d'auto-vérification inopérante, remplacée par "en cas de doute, ne signale rien").
- `orchestrator/write/WriteWorkflow.java` — nouveau champ/paramètre `phrasingCorrectorStep`,
  bloc Phase 2 ajouté en première position (avant DeusInMachinaCorrector), méthode
  `applyPhrasingCorrections`, diagramme de tête de classe mis à jour.
- `RedacteurModule.java` — instanciation et câblage de l'agent et du step.
- `orchestrator/CLAUDE.md` — diagrammes Write Workflow (séquence et retry chapitre) mis à jour
  avec la nouvelle position de `PhrasingCorrector`.
- `agent/CLAUDE.md` — entrée `PhrasingCorrector` dans la table de convention de nommage,
  passée de "planned" à sa description réelle.

## 3. Résultat

Chaîne des correcteurs de séquence, dans l'ordre d'exécution :
`PhrasingCorrector (retry ONCE) → DeusInMachinaCorrector → NaturalityCorrector → StyleCorrector
→ GrammarCorrector`.

Compilation vérifiée depuis la racine du projet après le lot complet (voir sortie `mvn compile`
associée à cette session).
