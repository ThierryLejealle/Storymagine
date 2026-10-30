# ReplyOriginAnalyst

## Role
On-demand, read-only **debug** look at the last reply of the last Npc : given the exact prompt the
character received and the reply it wrote, it maps each element of the reply (an action, a fact, a
line of dialogue, the form of the reply) onto the passage of the prompt that relates to it, and lists
the elements for which no passage exists. Triggered by the ❓ button next to the 🧠 button, answered
in the same pop-up, never written to the conversation history, never archived. Added 2026-10-28
after the author saw replies invent part of the story and actions "outside the frame" and wanted to
understand what, in the program and in the scenario, pointed there.

## What it can and cannot tell
The model cannot report *why* it wrote what it wrote : any "reason" is a reconstruction after the
fact. So the agent does a **traceability exercise**, not introspection : it matches text to text
(element of the reply -> quote of the prompt) and says how close the match is (`[direct]` : the
passage states or commands the element ; `[indirect]` : it only suggests it or is close in theme).
Every output is a **lead for the author to check**, never a verdict. The prompt it receives is framed
the same way : neutral, descriptive, nothing is evaluated (no "why did you invent...").

The most useful part is usually `UNTRACED` : an element with no counterpart in the prompt comes from
the model's own prior, not from the scenario or from our rules.

## Which prompt : the one actually sent
`RoleplayNarrator` returns the prompt it built (`RoleplayNarratorOutput.prompt`) ; `ChatServiceImpl`
keeps it with the reply in the session (`ChatSession.rememberReplyTrace`, `ReplyTrace`), **in memory
only**. Rebuilding it afterwards would be approximate (summary, presence, interjection may have
changed). `ChatSession.lastReplyTrace()` offers the trace only while that reply is still the last LLM
turn of the conversation : after an undo, a rollback, a restart or a load nothing matches and the
button answers "no recent reply to analyse". A regeneration produces a new reply and a new trace.
"Last Npc" in a multi-Npc round = the last one that spoke (an interjection counts).

## Reasoning
When the session shows the model's reasoning (`GenerationSettings.showThinking`), it is given to the
agent as a pointer : the passages the reasoning mentions or paraphrases are strong candidates. Without
it the section is left out entirely.

## Sampling
Calmer than the roleplay agents (temperature 0.3, top-k 40, top-p 0.9) : this maps and quotes text, it
does not play a character. Still overridden by the session's `GenerationSettings` when set. `think` is
requested, as for the other agents.

## Prompt shape
`ReplyOriginPromptBuilder`. System : the neutral observer, how to work (split the reply into elements,
look through the labelled blocks of the prompt, use the reasoning as a pointer), the output format and
one example. User : the analysed prompt (system part, user part), the reply and the reasoning, between
`===== ... =====` separators, because the analysed prompt has headings of its own.

## Output format
Three fixed sections, same `[RIEN]` empty-sentinel convention as the other analysts :
```
TRACED:
- <element> -> <block> : "<short exact quote>" [direct|indirect]
UNTRACED:
- <element>
READING:
<two or three sentences>
```
`TRACED` and `UNTRACED` keep one element per line ; `READING` is joined into a paragraph.

## Budget
The agent receives the whole prompt plus the reply, almost twice the size of a roleplay turn. On a
small context it may exceed it ; this is accepted (debug tool) and not truncated silently.

## Preconditions
A reply must exist and still be the last one (see above) ; otherwise `IllegalStateException`, shown in
the pop-up. The service also refuses when the agent is not wired (`ChatServiceImpl` built without it).
