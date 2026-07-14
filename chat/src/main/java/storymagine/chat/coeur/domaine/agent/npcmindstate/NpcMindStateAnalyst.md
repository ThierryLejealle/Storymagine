# NpcMindStateAnalyst

## Role
On-demand, read-only look at the character's current state of mind (situation seen from their
side, private thoughts, current plans) — triggered by the 🧠 button next to the 🔍 (NEXT ACT
readiness) button in the chat UI, answered in the same pop-up, never written to the conversation
history. Added 2026-07-14, same request pattern as `NextActReadinessAnalyst` (the player asked
for it "comme la loupe pour NEXT ACT").

## Why a separate agent, not an OOC question to RoleplayNarrator
Same rationale as `NextActReadinessAnalyst.md` : an OOC question becomes a `ChatTurn` appended to
the session and read by every future prompt, which isn't wanted for a check the player might run
several times just to peek at the story's state. See that doc for the fuller argument — not
repeated here, don't duplicate the same reasoning twice in two `.md` files.

## Same sampling settings as RoleplayNarrator
Also mirrors `NextActReadinessAnalyst` : temperature 1.0 and the four other `RoleplayNarrator`
defaults, read from the session's live `GenerationSettings`, not a colder "analyst" mode. Same
reasoning — this asks what the character actually thinks *right now, under real play conditions*,
not a detached, more "reasonable" judgment that could paper over genuine confusion or a shallower
read the live model would actually have.

## Prompt shape
`NpcMindStatePromptBuilder` (own package, mirrors `NextActReadinessPromptBuilder`'s structure).
System prompt frames the model as the story's author, forbidding narration/dialogue/actions. User
prompt : character sheet, premise, current act's resolved text (context only — unlike
`NextActReadinessPromptBuilder`, no `[NEXT ACT]` framing is relevant here), running summary,
recent transcript (`ChatPromptBuilder.transcript()`/`characterLabel()`, shared plain-data
formatting).

## Output format
Three fixed sections, same `[RIEN]` empty-sentinel convention as `NextActReadinessAnalyst` and
the `Reviewer` agents :
```
SITUATION:
THOUGHTS:
PLANS:
```
`PLANS:` is the one expected to legitimately come back `[RIEN]` sometimes (a character can be
purely reactive with no concrete plan) ; `SITUATION:`/`THOUGHTS:` should always have content in
practice, but the parser treats all three the same way for simplicity (one parsing rule, not a
special case for the section that's allowed to be empty). Parsed inline in `NpcMindStateAnalyst`,
not shared with `NextActReadinessAnalyst`'s parser — different section names, would need the same
kind of generalization work as `ReviewOutputParser` before it's worth extracting, not done
preemptively (see CLAUDE.md guidance against premature abstraction).

## Preconditions
None — unlike `NextActReadinessAnalyst` (only meaningful when a next act is pending), this
analysis is meaningful at any point in the story, so the 🧠 button is never disabled.
