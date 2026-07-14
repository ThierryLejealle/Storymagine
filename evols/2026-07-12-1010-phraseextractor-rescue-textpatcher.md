# 2026-07-12 10h10 — Nouvel agent PhraseExtractor : rescue quand TextPatcher échoue

## 1. Demande

Observation en usage réel (`094_SequenceStyleCorrector_12.md`, scénario 1998) : 2 corrections sur
5 proposées par `StyleCorrector` citaient une phrase presque identique au texte source (petite
erreur de recopie du LLM : accord, répétition non exacte) — `TextPatcher.apply()` échouait
silencieusement (juste un `log.warn`), la correction n'était jamais appliquée alors que le
défaut signalé était réel.

Idée initiale de l'utilisateur : un agent "SosCorrection" sans thinking pour relocaliser la
phrase. Retenu comme rescue générique dans `WriteWorkflow.applyCorrections()` (mutualisé, corrige
le problème pour tous les Correctors — pas seulement StyleCorrector), avec un nom conforme aux
conventions de `agent/CLAUDE.md` (suffixe `Extractor`), et prompt conçu directement (pas de
consultation Fable pour celui-ci, sur demande explicite), validé avant écriture.

## 2. Ce qui a été touché

### Nouvel agent — `agent/sequence/phraseextractor/`
- `PhraseExtractorInput` — `record(text, wrongPhrase)`
- `PhraseExtractorOutput` — `record(phrase, found)`
- `PhraseExtractor` — `implements Agent` (pas `AgentCorrector`, pas de liste FAUX/JUSTE : une
  seule relocalisation par appel). `thinks() = true` (recherche floue, comparaison de candidats —
  contrairement à `PhrasingCorrector` qui est un jugement fermé). Parsing dédié
  (`PASSAGE: "..."` / `NOT_FOUND`), pas de dépendance à `CorrectionParser` (format différent).
- `PhraseExtractor.md` — documentation complète (rôle, format, choix thinking, intégration,
  origine).

### Orchestration — `orchestrator/write/`
- `PhraseExtractorStep` (nouveau, pattern identique aux autres Step).
- `WriteWorkflow` : + champ/paramètre constructeur `phraseExtractorStep`. `applyCorrections()`
  extrait une méthode `rescueWithPhraseExtractor()` : sur un miss `TextPatcher.apply()`, appelle
  `PhraseExtractor` avec le texte et la citation en échec ; si un passage est trouvé, retente
  `TextPatcher.apply()` une fois avec ce passage relocalisé ; si la relocalisation échoue aussi
  (`NOT_FOUND` ou second miss), le `log.warn()` existant est conservé — comportement inchangé
  dans le pire cas. Diagramme du Write Workflow (commentaire de classe) mis à jour en conséquence.
- `RedacteurModule` : instanciation + câblage de `phraseExtractor`/`phraseExtractorStep`.

### Log alignment (chantier connexe, mémoire `project_redacteur_log_alignment_todo.md`)
`FileLogAdapter.java` : `NAME_W` passé de 30 à 40 (couvre `sequence/SequenceDeusInMachinaCorrector`,
39 caractères, le plus long label actuel). `llmCall()` utilisait un `%-28s` en dur, désynchronisé
de `NAME_W` — remplacé par `%-" + NAME_W + "s"` pour ne plus jamais diverger. Padding statique,
pas de confirmation nécessaire (règle CLAUDE.md globale).

### Tests
`PhraseExtractorTest` (nouveau) — tests directs de `parseResponse` (trouvé, guillemets mixtes,
`NOT_FOUND`, réponse `null`, passage vide, `PASSAGE:` sur une ligne non initiale).

## 3. Résultat

`mvn -q compile test` depuis la racine : **BUILD SUCCESS**, aucun test en échec. Non encore
observé en usage réel (prochain run avec correcteurs déclenchera potentiellement le rescue).
