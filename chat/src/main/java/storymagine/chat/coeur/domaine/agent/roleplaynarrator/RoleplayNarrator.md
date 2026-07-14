# RoleplayNarrator

## Role
Plays the character for one turn of the roleplay chat : builds the system+user prompt from the
session's current state (character sheet, scenario, current act, running summary, recent turns,
the player's new line) and calls the LLM. Called by `ChatServiceImpl` for every player message
(`sendMessage`) and for a regenerated reply (`retry`).

## Prompt shape
Uses `ModelCallPort` (`/api/chat`, system + user roles) — never raw completion. The roleplay turn
originally went through `RawCompletionPort` (`/api/generate`, `raw:true`, no chat roles), on the
theory that a model with no custom Modelfile `TEMPLATE` would respond better to unstructured
continuation. It broke on the very first reply : the model echoed the character's name a few times
then degenerated into repeating a single word, because `raw:true` strips the chat-template
scaffolding the model was actually fine-tuned to operate inside. Manually chatting with the same
model through Ollama's normal interface (always `/api/chat`) worked immediately. Confirmed with
Fable before reverting. `RawCompletionPort` stays available in `commun` as a general capability,
just unused here.

The actual prompt text lives in `ChatPromptBuilder` (a plain prompt-builder, no LLM call of its
own) — this agent's job is to call it, then call the LLM, then parse the reply.

## Turn labelling : Player: / {characterName}:
Every line in the transcript — history AND the current turn — is labelled `Player: ...` or
`{characterName}: ...` (falls back to the generic `Character:` if the scenario declares no name,
see `ChatScenario.characterName`). The prompt ends with a bare `{characterName}:` prefill : the
model completes that turn rather than deciding for itself who's speaking next.

This fixed a real, reproducible bug (2026-07-12/13) : the model would occasionally absorb a
sensation the player described about themselves (e.g. a tear) as if it were the character's own,
or invert who acted on whom. Root-caused from an actual captured prompt log : the transcript's
history lines were labelled, but the *current* player line — the one the model was actually
replying to — had no label at all, unlike every other line. Confirmed by Fable (consulted with
the real prompt + a real failing exchange, not a hypothetical) : label every line including the
current one, and end on a bare `{name}:` prefill — the closest shape to what small models have
seen at training time (chat logs, scripts, forum RP), and the fix Fable rated most likely to work.
Do **not** use different action/speech delimiters per side (considered and rejected) : the
*asterisk*/plain-text split already resolves the action-vs-speech axis cleanly ; the bug was about
*who's speaking*, a different axis, and inventing an asymmetric delimiter convention would push the
model further out of its training distribution rather than closer to it.

## Anti-mirroring rule
`ChatPromptBuilder`'s `SYSTEM_FORMATTING` block tells the model explicitly : never copy the
player's actions or sensations onto your own character, with a worked wrong/right example (a tear
rolling down a cheek). Added after the labelling fix above still weren't quite enough on their own
in one observed case (the model, mid-scene, described feeling a sensation the player had just
described about themselves) — a concrete counter-example, not just an abstract rule, is what a
small model needs here.

## Stop sequences
`STOP_SEQUENCES = ["\nPlayer:", "\nNarration:"]`, passed to `ModelCallPort.generate(...)`. Without
this, a small model regularly drifts past its own turn and starts writing the player's next line
(or a narration) itself — the model has no other signal telling it to stop once it reaches a
natural pause. Both prefixes already exist in the transcript the model is shown, so it recognizes
them as turn boundaries. This is a belt-and-suspenders companion to the `Player:`/`{name}:`
labelling above, not a replacement for it — the two fixes address different failure modes (who a
sensation belongs to, vs. the model refusing to stop generating).

## Acts and [NEXT ACT]
`ChatScenario.acts()` is a flat list of already-resolved leaves from a nested outline (see
`ScenarioOutlineParser` — only leaves are ever "current" ; a leaf's `text()` already includes every
ancestor's own body, root-to-leaf, so `ChatPromptBuilder` never needs to know the tree shape).
`currentAct` is a 1-based **position** in that list (0 = the scenario has no acts).

When an act is active, its resolved text is injected under `CURRENT ACT (N of M)`, followed by one
of two rules, chosen by whether the act's own text contains the literal `[NEXT ACT]` marker
(bracketed — a loose `"next act"` substring match was tried first and rejected as too permissive,
it would misfire on an innocuous sentence like "in the next act of her plan...") :
- **The act specifies its own condition** (author wrote `"Quand écrire [NEXT ACT] : ..."` in the
  act's body) → the model is told to use *that* condition, verbatim, never a vague "feels like a
  good time to move on".
- **No condition specified** → falls back to a generic "if you feel this act's events have reached
  their natural conclusion" rule.

When the model's reply contains the `[NEXT ACT]` marker (`NEXT_ACT_MARKER`, case-insensitive), it
is stripped from `replyText` before the turn is stored or shown to the player, and
`triggeredNextAct` is reported so `ChatServiceImpl` can call `ChatSession.advanceAct()`.

## What this agent does NOT do
Session mutation (appending turns, advancing the act, compaction, persistence) stays in
`ChatServiceImpl` — this agent only turns an input into an output, the same POJO-in/POJO-out shape
as every other agent in the project. It also does not compute the context-usage gauge or the
compaction threshold ; those reuse `ChatPromptBuilder` directly from `ChatServiceImpl` to size a
*hypothetical* prompt without spending an LLM call, which is a Service-level concern, not this
agent's.
