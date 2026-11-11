# Format reference: the files of a scenario (one page)

Load this file only when a phase asks for it.

## Layout
```
<scenario-name>/            letters, digits, - and _ only
  scenario.txt              header + premise + the first step
  scenario-1.txt            next step (scenario-2.txt, scenario-3.txt... consecutive, no gap)
  characters/<id>.txt       one file per NPC (id: lowercase letters, digits, dashes)
  style.txt                 optional writing advice shared by every NPC
  styles/name.txt           optional extra style for the scenes that write {style name}
  plan.md                   your planning notes. ALWAYS .md, never .txt
```
Never create a `run/` folder: the game makes it. Never put a `.txt` file with a "# Title" first line
at the root except the scenario files and style.txt: the game would refuse to load the scenario.

## scenario.txt, from the top
```
Joueur : [player name]
Langue : [English | French | ...]

#SCENARIO
World: [where and when, one line]
Premise: [the starting situation, third person, no spoiler, 80 words at most]
Tone: [the mood]

# [Step 1 title]
...
```
- Prefer English for the scenario (small models are better in it, and the player can switch the game to French
  with the language selector). Write another language only if the story will surely be played in it. A missing
  `Langue :` line means English.
- `Joueur :` and `Langue :` are the two header keywords. Keep them exactly as written, even in an English
  scenario. They must be the first lines, one after the other. A blank line ends the header.
- The labels `World:`, `Premise:`, `Tone:` are free text for the NPCs to read. The game only parses the
  header, `#SCENARIO`, the headings and the `[...]` lines.
- The premise is sent to every NPC at every turn: keep it short and spoiler free.

## Steps and scenes (headings)
- `# Title` is a step. `## Title` under it is a scene of that step. `###` goes one level deeper.
- Only the **deepest** headings (leaves) are ever played. The text of a parent heading is added to every
  leaf under it and re-read at each turn: keep parent text short, put details in the leaf.
- A leaf looks like this (labels are free text, keep the same three):
```
## [Scene title]
[One or two sentences the player sees when the scene opens. Third person.]
Starting point: [where the scene stands at the beginning]
What must happen: [what the scene must achieve before moving on]
When to write [NEXT ACT]: [ONE observable fact that ends the scene]
```
- Every leaf except the last one of the whole scenario has the `[NEXT ACT]` line.
- The first leaf names every NPC who is present.
- `[...]` lines and all act text are in the **third person** ("Alex enters the hall"), never "you".
- A continuation file (scenario-1.txt...) starts with a top-level `# Title` line.

## Character sheet (characters/<id>.txt)
```
# [Name]
[Public part: who they are, role, how they speak and act. What anybody could notice.]

# SECRET
[Secret part: what only this NPC knows.]
```
- The first line `# Name` is required: a file without it is not loaded as a character (the validation warns). `# SECRET` is alone on its line and optional.
- The sheet says who the character is, not what will happen to them.
- Optional: `## tag comment` sections hold text that is added only when a `{npc-story on Name tag}` switches it
  on. A `##` section before `# SECRET` is public, after it is secret.

## styles/name.txt (optional)
One file per scene style called by `{style name}` (see ref-commands.md, "Variety"). Same rules as style.txt; it is
added after style.txt, so keep style.txt generic (language, audience) and put the mood of a place in a scene style.

## style.txt (optional)
A few short lines of writing advice shared by every NPC (sentence length, vocabulary, point of view).
Under 1500 characters. No commands in it, no plot.

## Limits to respect
- One step file: about 600 words at most. One sheet: 110 to 180 words (public part 80 to 120, secret 30 to 60). Premise: 80 words at most.
- Two or three NPCs speaking at each exchange is the most a small model handles well.
