# Agent Rules — Storymagine

VITAL : si dans le prompt on mentionne une autre information du prompt (par exemple : "Vérifie soigneusement les Fiches de personnages") il faut que l'élément cité porte strictement le nom mentionné (les fiches de personnages ne doivent pas être dans un section "Acteurs :" dans l'exemple). Le prompt doit être strictement COHERENT

RAPPEL : on cible des PETITS LLM. Il faut s'adapter à leurs capacités : des prompts précis, simples, sans boucle, sans redit.

## Requirement — Contrainte vs Check

Le scénario exprime chaque règle narrative via un `Requirement` (une contrainte + un check,
syntaxe "contrainte | check" dans les fichiers `.md`/YAML — voir `specs/scenario.md`).
Chaque agent ne consomme qu'un seul côté, selon son rôle :
- `Writer` et `Planner` (ex. `ChapterPlanner`) consomment `Requirement.constraint()`,
  préfixé dans le prompt par "Assure-toi que chacun des points suivants est respecté :".
- Tous les autres agents (`Critic`, `Corrector`) consomment `Requirement.check()`,
  préfixé dans le prompt par "Vérifie que chacun des points suivants est respecté :".
Ne jamais mélanger les deux côtés dans un même agent.

## Naming Convention

Every agent class name ends with a suffix that expresses its role:

| Suffix      | Role                                                                 |
|-------------|----------------------------------------------------------------------|
| `Critic`    | Analyses text, computes score 1-10, returns a problems list          |
| `Corrector` | Detects issues, returns (wrong → correct) pairs for inline patching  |

Agent class name examples (all at sequence level unless specified):
- `DeusInMachinaCritic`   — detects instruction leaks, residual check after corrector
- `DeusInMachinaCorrector`— patches deus-ex-machina passages directly
- `StyleCritic`           — evaluates prose style
- `ElementCritic`         — verifies required narrative elements are present
- `ChapterGoalCritic`     — verifies the chapter meets its narrative goal (chapter level)
- `NaturalityCorrector`   — patches over-constructed or analytical phrasing
- `GrammarCorrector`      — patches grammar, spelling, agreement and conjugation errors (accuracy)
- `PhrasingCorrector`     — patches residual generation errors: missing word/chunk, aberrant word, calque (retry ONCE)
| `Writer`    | Generates narrative text                                             |
| `Planner`   | Generates a structured plan (beats, outline…)                        |
| `Extractor` | Extracts structured information from text                            |
| `Tracker`   | Tracks state evolution across sequences                              |
| `Filter`    | Filters a list by semantic criteria                                  |

The suffix `Checker` is **abolished** — use `Critic` instead.

## Critic — Score Rule

A `Critic` MUST NOT ask the LLM to produce a numeric score.
The LLM returns a **list of problems** (optionally weighted by severity).
The score 1–10 is **computed by Java code** from that list.

Rationale: LLM scores are inconsistent and poorly calibrated. A problem count
with known weights is reproducible and tunable without touching the prompt.

Reference implementations: `PlanGoalCritic`, `ChapterGoalCritic` (use `ProblemScoreParser`).

## Corrector — Correction Rule

A `Corrector` MUST NOT ask the LLM to rewrite or paraphrase text directly.
The LLM returns a **list of (wrong phrase → corrected phrase) pairs**.
The Java code applies them via `TextPatcher.apply()` (see below).

Rationale: Direct LLM rewrite is unreliable — it drifts, hallucinates, or changes
content beyond the targeted issue. Pair-based patching is auditable and bounded.

Reference implementations: `GrammarCorrector`, `NaturalityCorrector`, `DeusInMachinaCorrector`.

## Corrector — Retry Strategy

Every `Corrector` implements `AgentCorrector` (extends `Agent`), exposing `retryStrategy(): RetryStrategy`
— how its self-repeat loop (retrying on its own already-patched output, in `WriteWorkflow`) decides to
continue: `SINGLE_PASS`, `RATIO_THRESHOLD` (historical corr/word or absolute-count threshold),
`DECREASING` (repeats while the finding count is still dropping pass to pass), or
`DECREASING_AND_RATIO_THRESHOLD` (both conditions must hold). Chosen per agent based on whether its
criteria are closed (grammar — finite, real convergence expected) or open (style/naturality — "anything
could be improved", no true convergence).

## Corrector — Output Format

All Correctors MUST use the same delimiters so parsers are consistent:

```
CORRECTIONS:
- FAUX: "phrase exacte contenant le problème"
  JUSTE: "phrase corrigée"
```

- The LLM must cite the **exact phrase** to replace — never a paraphrase.
- One `JUSTE:` line per entry — no variants, no comments.
- Empty output sentinel: `PAS DE CORRECTION` — nothing before or after.
- Always provide an example in the prompt.

## Corrector — Parsing is Mutualized

Every Corrector MUST parse its LLM response via
`storymagine.redacteur.coeur.domaine.agent.commun.CorrectionParser#parse` — never
re-implement FAUX:/JUSTE: line parsing locally.

```java
private static List<XxxFinding> parseFindings(String response) {
    return CorrectionParser.parse(response, "PAS DE CORRECTION", XxxFinding::new);
}
```

`CorrectionParser` cleans stray leading/trailing quote characters (`"`, `'`, curly
variants) identically on both the FAUX and the JUSTE side of every pair, and strips a
trailing parenthetical comment the LLM sometimes invents on JUSTE (never on FAUX — a
FAUX phrase may legitimately need to include a source-text annotation in order to
remove it as part of the replacement).

## Corrector — Replace Warning Rule

A literal `String.replace()` is silent on miss: if the LLM cited a phrase that doesn't
match the text exactly, the patch is silently skipped. LLM correctors also sometimes
re-quote an embedded citation with a different quote character than the source uses at
that spot (same words, wrong glyph) — a literal replace would miss that too.

**Every apply method MUST go through the mutualized
`WriteWorkflow#applyCorrections` / `storymagine.commun.coeur.domaine.text.TextPatcher`
— never a bare `String.replace()`.** `TextPatcher.apply()` tolerates quote-family
character substitutions (`"`, `'`, `“`, `”`, `‘`, `’` are treated as equal to each
other, nothing else is) and reports whether a match was found so the caller can warn:

```java
private String applyXxxCorrections(String text, List<XxxFinding> findings) {
    return applyCorrections(text, findings, "XxxCorrector",
            XxxFinding::wrongPhrase, XxxFinding::correctedPhrase);
}
```

`applyCorrections` (private in `WriteWorkflow`) logs via `LogPort.warn()` whenever a
`TextPatcher.Result` comes back with `applied() == false`.
