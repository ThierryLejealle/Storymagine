# ScenarioClarityReviewer

## Role
Static QA reader for a `ChatScenario`. Judges a single act in isolation — is it usable, is the
situation clear, and above all, is the "[NEXT ACT]" trigger condition (the author's "Quand écrire
[NEXT ACT] : ..." line) concrete enough for a small model to act on? Also proposes free-text
improvement ideas. Never scored, same as `ScenarioContinuityReviewer` (see that agent's `.md`).

## Context shape — deliberately narrower than the continuity reviewer
Sees ONLY the character sheet, the premise, and the current act's resolved text — exactly what
`ChatPromptBuilder`/`RoleplayNarrator` would put in front of the real roleplay model for that act,
no earlier acts, no later ones, no accumulated history. This is the point: clarity must be judged
under the same blindness the real roleplay model has. Feeding it the story so far (like the
continuity reviewer gets) would let it "understand" an act only because of context the real model
never sees at that point — the opposite of what this agent exists to catch. Confirmed as the right
split by a Fable review (2026-07-13): merging the two reviewers into one would leak continuity
context into clarity judgments.

## The [NEXT ACT] paraphrase test
Rather than asking the model to judge "is the condition vague" directly (too subjective for a
small model), the prompt asks it to try to restate the condition as one concrete, observable event
in the conversation — a small model can do that restatement, or fail at it, far more reliably than
a bare vagueness judgment.

## What this agent does NOT do
Does not judge whether the act fits the larger story — that's `ScenarioContinuityReviewer`'s job,
this agent literally cannot see enough to judge that.
