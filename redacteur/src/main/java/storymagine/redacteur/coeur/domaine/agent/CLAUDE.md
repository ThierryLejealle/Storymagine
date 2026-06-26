# Agent Rules — Storymagine

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
- `GoalTextCritic`        — verifies the chapter meets its narrative goal (chapter level)
- `NaturalityCorrector`   — patches over-constructed or analytical phrasing
- `ProofreaderCorrector`  — patches grammar and spelling errors
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

Reference implementations: `GoalPlanChecker`, `GoalTextChecker` (use `ProblemScoreParser`).

## Corrector — Correction Rule

A `Corrector` MUST NOT ask the LLM to rewrite or paraphrase text directly.
The LLM returns a **list of (wrong phrase → corrected phrase) pairs**.
The Java code applies them via `String.replace()`.

Rationale: Direct LLM rewrite is unreliable — it drifts, hallucinates, or changes
content beyond the targeted issue. Pair-based patching is auditable and bounded.

Reference implementations: `ProofreaderCorrector`, `NaturalityCorrector`, `DeusInMachinaCorrector`.

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

## Corrector — Replace Warning Rule

`String.replace()` is silent on miss: if the LLM cited a phrase that doesn't match
the text exactly, the patch is silently skipped.

**Every apply method MUST log a warning via `LogPort.warn()` when a replace produces
no change** (i.e., `result.equals(text)` after the replace call).

```java
String patched = result.replace(f.wrongPhrase(), f.correctedPhrase());
if (patched.equals(result))
    log.warn("XxxCorrector: replace miss — phrase not found in text: " + f.wrongPhrase());
result = patched;
```

Apply methods must be instance methods (not `static`) to access `log`.
