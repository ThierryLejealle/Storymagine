# NextActReadinessAnalyst

## Role
On-demand, read-only check of whether the current act's "[NEXT ACT]" condition has actually been
met by the story so far — triggered by a player button next to "Suivant" in the chat UI, answered
in a pop-up, never written to the conversation history. Added 2026-07-14 after the player reported
using "OOC: ..." for this exact question and finding it polluted the chat transcript.

## Why a separate agent, not an OOC question to RoleplayNarrator
`RoleplayNarrator` answers in character (or, for `OOC:`, as the author but still inside the live
turn sequence) and its reply always becomes a `ChatTurn` appended to the session. This agent never
touches `ChatSession` : `ChatServiceImpl.analyzeNextActReadiness` calls it and returns the result
directly, no `session.append(...)`, no `storage.saveSession(...)`. Same reason a dedicated prompt
was worth it over reusing `ChatPromptBuilder`'s system prompt with a synthetic `OOC:` message :
the framing ("you are analyzing this session as its author") and the strict 3-section output only
make sense for this one-off check, not for a turn that's also supposed to read naturally in the
transcript.

## Same sampling settings as RoleplayNarrator — not a colder "analyst" mode
First draft used a fixed low temperature (0.3, like the `Reviewer` agents used by
`ScenarioTesterService`). Rejected by the player before implementation : the point of this button
is to see what *this* model, with *these* settings, actually makes of the situation mid-play — not
a detached, more "reasonable" judgment from different sampling. A colder pass could paper over a
real confusion the model would have during actual play (e.g. rate the condition as clearly met
when the live RoleplayNarrator would in fact hesitate over it), which defeats the diagnostic
purpose. So `NextActReadinessAnalyst` mirrors `RoleplayNarrator`'s five sampling defaults exactly
(temperature 1.0, topK 100, topP 0.98, minP 0.05, repeatPenalty 1.08) and reads the session's live
`GenerationSettings` the same way.

## Prompt shape
`NextActReadinessPromptBuilder` (own package, not `ChatPromptBuilder` — that one is built for
roleplay turns and its `SYSTEM_INTRO`/`SYSTEM_FORMATTING` don't apply here). System prompt frames
the model as the story's author, explicitly forbidding narration/dialogue/actions. User prompt :
character sheet, premise, current act's resolved text (whatever `[NEXT ACT]` condition it embeds,
verbatim — this agent does not receive `ChatPromptBuilder`'s `NEXT_ACT_RULE_DEFAULT/CONDITIONAL`
preambles, since those instruct how to *signal* the marker during roleplay, not how to *analyze*
it), running summary, recent transcript (reuses `ChatPromptBuilder.transcript()` /
`characterLabel()` — plain data formatting, not roleplay-specific, safe to share).

## Output format
Three fixed sections, same `[RIEN]` empty-sentinel convention as the `Reviewer` agents :
```
CONDITION:
STATE:
MISSING:
```
`MISSING:` is expected to be `[RIEN]` (parsed to an empty string) once the condition is judged
fully met. Parsed inline in `NextActReadinessAnalyst` (not `ReviewOutputParser` — that parser is
locked to the `ISSUES:`/`SUGGESTIONS:` two-bullet-list shape shared by the two `Reviewer` agents ;
this is a different, three-section, prose-not-bullets format).

## Preconditions
Only valid when there is an actual next act pending : `ChatServiceImpl.analyzeNextActReadiness`
throws `IllegalStateException` if `currentAct <= 0` (no acts in use) or `currentAct >=
scenario.acts().size()` (already on the last act, nothing left to advance to) — mirrors the guard
style of `retry()`. The chat UI additionally disables the button in that state (same condition as
the "Suivant" button) so the error path is a defensive backstop, not the expected flow.
