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

## Text for one NPC only

| Command | Effect |
|---|---|
| `{private A} text` | The text on that line is read only by A. The other NPCs of the scene never see it. |
| `{private A, B}` alone on its line, text lines, then `{end-private}` | Same for a block of lines. Blocks do not nest. Close it before the next heading. |

```
Starting point: Franck and Alex take out their consoles to play.
{private Franck} Franck wants to win badly and will cheat by pulling Alex's cable.
{private Alex}
Alex only wants to have fun.
He suspects nothing.
{end-private}
```
Only for the scene where it is written. Never put the `[NEXT ACT]` line or a `[shown line]` inside.

## Changing a character during the story

| Command | Effect |
|---|---|
| `{npc-delta public Name : text}` | Adds text to the PUBLIC sheet from this heading on, for good: it can never be removed later. May run over several lines until `}`. |
| `{npc-delta secret Name : text}` | Same for the secret sheet. |
| `{npc-story on Name tag}` / `{npc-story off Name tag}` | Switches a `## tag` section of the sheet on or off, until the next on/off. Use it for levels that replace each other. |

```
{npc-delta public Captain Ione : angry, blames the crew for the lost cargo}
{npc-delta secret Dr. Sol : has decided to leave the station tonight}
{npc-story on Dr. Sol worried}
```
A feeling that grows in stages (dislikes, then hates, then respects) is one `## tag` section per stage in the
sheet. At each stage: `{npc-story on Name next-stage}` AND `{npc-story off Name previous-stage}`. Never
stack the stages: the NPC would read all of them at once.

## Text shown and progression
- `[text]` on its own line: shown to the player when the scene opens (third person).
- `When to write [NEXT ACT]: condition` inside a played scene: the fact that ends the scene (see ref-format.md).

## Which mechanism for which need
Use a mechanism only when the scene needs it. Most scenes need only their text, one line per NPC and a
`[NEXT ACT]`.

| Need | Use | Lasts |
|---|---|---|
| What everybody knows or sees, what every NPC must do | Plain scene text | This scene |
| An instruction or a fact only some NPCs may read | `{private Name}` | This scene |
| What an NPC wants deep down since the beginning | The NPC's `# SECRET`, as a state | The whole story |
| A change that is final from this scene on (wounded arm, a vow) | `{npc-delta}` | To the end, cannot be removed |
| A trait or feeling that grows by stages or is temporary | `{npc-story on/off}` | Until switched |
| Who is in the story, who speaks | `{npc-arrives}`, `{npc-leaves}`, `{npc-active}`... | As written |
| Mood, variety | `{style}`, `{N%}` | This scene and below |

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
