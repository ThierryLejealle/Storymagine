# Commands reference (one page)

Load this file only when a phase asks for it. Use only the commands listed here: if you need something
else, write it as plain narration. A command sits **alone on its line**, between braces. A misspelled
command stops the scenario from loading. The examples show the FORMAT only: never reuse their names, places
or plot.

## Who is in the story, who speaks

| Command | Where | Effect |
|---|---|---|
| `{npc-only A, B}` | Own line right after the `Tone:` line of the premise (or in the first scene), once | Only A and B are in the story at the start. Without it, every NPC is. |
| `{npc-arrives A}` | Under a heading | A enters the story from this heading on. |
| `{npc-arrives template Name1, Name2}` | Under a heading | Creates NPCs from the sheet `characters/template.txt`, one per name. A sheet used this way is a template, not an NPC. `%NPC%` inside the sheet becomes the name. |
| `{npc-leaves A, B}` | Under a heading | They leave the story from this heading on. |
| `{npc-active A}` `{npc-present A}` `{npc-passive A}` `{npc-absent A}` | Under a heading | How much A speaks: active = every exchange, present = answers when addressed (default), passive = short reactions, absent = silent but still seen. |
| `{npc-reset}` | Under a heading | Everybody back to present. |
| `{npc-starts}`, `{npc-starts A}` or `{npc-starts A, B}` | Premise or a heading | An NPC speaks first when that part opens. With several names, A opens and B follows in the same round, in the order written (commas; each name must exist and be in the story at that point; a name may come back: `A, B, A`). `{npc-do ...}` is the same with the narrator's pen for all of them. |

```
{npc-only Mira, Tavo}
{npc-arrives guard Hobb, Brenn}
{npc-active Hobb}
{npc-leaves Mira}
```

## Changing a character during the story

| Command | Effect |
|---|---|
| `{npc-delta public Name : text}` | Adds text to the PUBLIC sheet from this heading on. May run over several lines until `}`. |
| `{npc-delta secret Name : text}` | Same for the secret sheet. |
| `{npc-story on Name tag}` / `{npc-story off Name tag}` | Switches a `## tag` section of the sheet on or off. |

```
{npc-delta public Captain Ione : angry, blames the crew for the lost cargo}
{npc-delta secret Dr. Sol : has decided to leave the station tonight}
{npc-story on Dr. Sol worried}
```

## Text shown and progression
- `[text]` on its own line: shown to the player when the scene opens (third person).
- `When to write [NEXT ACT]: condition` inside a leaf: the fact that ends the scene (see ref-format.md).

## Variety (only when the brief asks for it)
| Command | Where | Effect |
|---|---|---|
| `{style name}` | Alone on its line, under a heading or in the premise | Adds `styles/name.txt` (letters, digits, `-`, `_`) to `style.txt` for that scene and the scenes below it. Several lines under one heading add up in the order written. A child heading's own `{style}` replaces its parent's. In the premise it adds to `style.txt` for the whole story. |
| `{30%} text` | Start of a line of scene text | The line is kept (marker removed) 30% of the time, dropped otherwise; decided once when the game starts, then read by every NPC at every turn of the scene. |

Write a `{30%}` line as a **state** ("A minstrel tunes a lute in the corner"), never as a one-off action ("Joe
cleans his gun": it would be repeated every turn). A `{N%}` line in a style file is drawn at every reply instead:
use it for descriptions, not actions.

## Never in a scenario you write
Do not write `{pick}`, `{repeat}` or `{cast}` unless the user asks for random content (advanced: the user
reads guide-redaction-chat.md sections 2.6 to 2.6 ter). Do not invent other commands.
