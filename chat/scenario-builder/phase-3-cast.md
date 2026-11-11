# Phase 3: the character sheets (one NPC at a time)

**Load, for each NPC:** `plan.md` (re-read the brief and the whole cast table; write THIS NPC's row first) and
the sheets already written. References: `skeleton.md` (character sheet), `ref-format.md` (sheet section), `ref-writing-tips.md`.
**Produce:** `characters/<id>.txt` for this NPC. `<id>` is the name in lowercase letters and digits, words
joined by dashes ("Captain Ione" gives `captain-ione`).
**Then:** go to the next NPC. Write one sheet per file, one NPC at a time, even though you could hold them all.

## Step 1: the public part first (80 to 120 words)
Write what anybody could notice or know about the NPC:
- who they are and their role;
- how they speak and act: two habits someone could watch, not adjectives. Write "looks away and answers in
  short sentences", not "shy";
- how they stand with the other NPCs at the start of the story.

## Step 2: then append the secret (30 to 60 words)
Add the line `# SECRET` on its own line, then what only this NPC knows and hides. Add it to the same file.
Keep one file per NPC and never copy text from one file into another.
The secret may hold the NPC's ultimate motive, as a state: what it wants, owes or hides ("works for people
who want Sam, needs Sam to trust him"). Never as steps or events to carry out ("he will follow Sam, then
kidnap him"): the NPC would do them in the first scene. The motive can be completed later with
`{npc-delta secret}` in the step where it becomes useful.

## Step 3: apply the test to every sentence
1. Is it true and known to the NPC from the first turn, whatever the player does? If not, it belongs in a step
   file (phase 4), not here.
2. Does it describe something observable (what the NPC says or does)? If it is a label, replace it by a
   behaviour.
3. Is it free of future events? A sheet tells who the NPC is, never what will happen to them. It does not
   mention steps, scenes or when the NPC arrives or leaves: that goes in the step files.

How a character changes during the story is written in the step files with `{npc-delta}` and
`{npc-story}`, not in the sheet.

## Step 4: one pass over all the sheets
When every sheet exists, read only the **public parts**, one after the other. Check that the voices differ
and that the relationships agree from one sheet to the other. Fix the sheet that is wrong.

## Checklist
- First line is `# Name`; `# SECRET` is alone on its line.
- Public part 80 to 120 words, secret 30 to 60 words.
- No sentence about the future. No two NPCs with the same voice.
