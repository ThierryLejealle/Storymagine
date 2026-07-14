# ScenarioContinuityReviewer

## Role
Static QA reader for a `ChatScenario`, not a roleplay participant. Reads the acts in order, one at
a time, and reports narrative inconsistencies against everything already read — a character/fact
used as if already known but never introduced, a contradiction with an earlier act, a dropped
promise, a missing cause-and-effect link. Also proposes free-text improvement ideas, kept separate
from the issues list. Never asked for a score (see project convention: only redacteur's `Critic`
suffix implies a Java-computed score; this agent's suffix `Reviewer` does not).

## Context shape
Sees the character sheet, the scenario premise, the FULL resolved text of every act already
reviewed (numbered, in the order the scenario would actually play them — see
`ScenarioTesterServiceImpl`), and the current act. Deliberately the full text, not a summary:
tested scenarios stay short enough (tens of acts) that summarizing would cost more in lost detail
than it saves in tokens. Sibling acts sharing a parent heading will repeat that parent's inherited
text in the accumulated "story so far" — accepted duplication, not a bug (see `ScenarioAct.text()`
— resolved text always includes inherited ancestor bodies).

## Prompt design notes (informed by a Fable review, 2026-07-13)
- Explicitly tells the model that an element introduced AS NEW in the current act is normal —
  without this, a small model flags every new character/object as an inconsistency, since nothing
  in a single earlier-acts recap "introduced" it either.
- Explicitly tells the model an unresolved mystery is not an issue by itself (a scenario can
  plant a mystery in act 2 that only resolves in act 30) — without this, deliberate foreshadowing
  reads as a plot hole to the model.
- Asks each issue to name the act it conflicts with, to force citation-based reasoning instead of
  vague impressions.
- Two worked examples — one with a real inconsistency, one with nothing to report ("[RIEN]" on
  both sections) — a single example biases a small model toward always finding something to say.
- Output language is not hardcoded: "write in the same language as the scenario text above",
  matching the rest of the module's dynamic-language convention (see `ChatSummarizer`).

## What this agent does NOT do
Does not see later acts (anti-spoiler is irrelevant here, but ordering still matters: it reviews
strictly in play order, one call per act). Does not merge or rank the two reviewers' outputs — see
`ScenarioTesterServiceImpl` for how `ActTestResult` combines this agent's findings with
`ScenarioClarityReviewer`'s.
