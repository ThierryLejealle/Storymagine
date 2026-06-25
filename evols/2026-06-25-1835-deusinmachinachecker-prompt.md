# 2026-06-25 18h35 — Révision des prompts agents (session complète)

## Évolution demandée
Révision systématique des prompts de tous les agents à partir d'une analyse externe
(`specs/proposition-ameliorations-prompts.md`). Objectifs : meilleure précision pour les
petits modèles, exemples concrets, uniformisation des échelles de notation, suppression
des redondances et ambiguïtés.

---

## Ce qui a été touché

### DeusInMachinaChecker
`agent/writer/deusinmachinachecker/DeusInMachinaChecker.java`
- Séparateurs Unicode `────` supprimés
- PRINCIPE réécrit : cible explicitement les deux fuites (fragment de consigne recopié,
  confirmation de suivi type "conformément au plan")
- Exemples FUITE/OK ajoutés pour les 5 types de fuites

### TextCoherenceCritic
`agent/writer/textcoherencecritic/TextCoherenceCritic.java`
- "ces trois lignes" → "ces trois sections"
- Section `### Éléments à utiliser (focus)` supprimée du user (le focus n'était pas dans
  la liste UNIQUEMENT du system — incohérence silencieuse)
- Exemples AMELIORATION / DEFAUT_SIGNIFICATIF / DEFAUT_MAJEUR ajoutés

### TextNarrativeCritic
`agent/writer/textnarrativecritic/TextNarrativeCritic.java`
- "ces trois lignes" → "ces trois sections"
- Exemples ajoutés, ancrés dans la prose (dialogues, paragraphes) et non dans la structure du plan

### TextWhatIfCritic
`agent/writer/textwhatifcritic/TextWhatIfCritic.java`
- Échelle complétée : 4, 2, 1 étaient absents (saut de 5 à 3)
- `(entier 0-10)` → `(entier 1-10)`

### Proofreader
`agent/writer/proofreader/Proofreader.java`
- Exemple FAUX/JUSTE ajouté (les petits modèles citaient parfois un seul mot
  au lieu de la phrase entière)

### RepetitionFilter
`agent/writer/repetitionfilter/RepetitionFilter.java`
- Reformulation positive de la tâche : "filtrer la liste, écarter ce qui est proche d'un
  leitmotiv, retourner ce qui reste" — évite la double négation sémantique qui faisait
  inverser la liste aux petits modèles

### SequenceChecker
`agent/writer/sequencechecker/SequenceChecker.java`
- Score calculé en Java (supprimé du prompt LLM — l'arithmétique `-1 pt/élément` était
  trop mécanique pour les petits modèles)
- Formule : 10 si tout bon, 1 si tous ratés, proportionnel 1-5 sinon
  (`5 * (total - missing) / (total - 1)`)

### StoryCompressor
`agent/global/storycompressor/StoryCompressor.java` +
`orchestrator/evaluate/StoryCompressorStep.java`
- BASE_WORDS 150 → 800, WORDS_PER_CHAPTER 30 → 200 (adapté aux LLM 128k-256k tokens)
- EXCLURE assoupli : "ressenti émotionnel ponctuel" seulement (plus "sauf tournant dramatique")
- Redite de la limite de mots supprimée du system (gardée uniquement dans le user)

### Writer
`agent/writer/sequencewriter/Writer.java`
- Section AJOUTS AUTORISÉS ajoutée : clarifie ce que le modèle peut ajouter au-delà
  des beats (actions, réactions, dialogues locaux) et ce qu'il ne peut pas modifier
- Ordre de priorité explicite en cas de conflit (Directives > Contraintes > Trame >
  Personnages > Style > Focus > Lore)
- `bannedPhrases` et `bannedThemes` déplacés du system vers le début du user,
  plus proches du moment de génération

### CausalAnalyzer + NarrativeArcAnalyzer
`agent/global/causalanalyzer/CausalAnalyzer.java`
`agent/global/narrativearcanalyzer/NarrativeArcAnalyzer.java`
- Guillemets supprimés des exemples (risquaient d'être reproduits dans la sortie)
- Cas zéro-problème normalisé : `PROBLEME: [RIEN] / SCORE: 10` (cohérent avec le parser)
- `(entier 0-10)` → `(entier 1-10)`

### SequenceStyleChecker
`agent/writer/sequencestylechecker/SequenceStyleChecker.java`
- `(entier 0-10)` → `(entier 1-10)`
- Faute grammaticale : "4 = de défauts graves" → "4 = défauts graves"

### ChapterStyleChecker
`agent/global/chapterstylechecker/ChapterStyleChecker.java`
- `(entier 0-10)` → `(entier 1-10)`

### GoalTextChecker
`agent/writer/goaltextchecker/GoalTextChecker.java`
- "ces trois lignes" → "ces lignes" (il n'y a que 2 types de lignes : PROBLEME: et SCORE:)

---

## Résultat
Uniformisation des échelles 1-10 sur l'ensemble des agents, ajout d'exemples concrets
sur les agents qui en manquaient, suppression des ambiguïtés de format et des
redondances. Le Writer gagne un mécanisme de priorité explicite et des interdictions
mieux positionnées. Le SequenceChecker délègue l'arithmétique à Java.
