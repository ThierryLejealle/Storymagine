# Phase 5: style, validation, test play

**Load:** `plan.md`, `ref-format.md` (style.txt section) and `ref-writing-tips.md` (leak check).
**Produce:** `style.txt` (optional), a clean validation, and a short test play.

## Step 1: style.txt (optional, short)
Write 3 to 6 short lines of writing advice shared by every NPC, from the brief's audience and tone:
- sentence length and rhythm;
- vocabulary level for the audience;
- point of view and tense of the narration lines (third person, present tense, unless the brief says otherwise).
Stay under 1500 characters. No commands, no plot, no names. Skip the file if the tone needs nothing special.

## Step 1 bis: variety (only if the brief asks for it)
If the user wants a livelier story: write one `styles/<mood>.txt` per very different place (2 or 3 lines of
mood), call each with `{style <mood>}` under the first heading of that place, and add one or two `{20%}` state
lines to scenes (see ref-commands.md, "Variety"). Otherwise skip this step.

## Step 2: validate
- If you can run commands, run `validation-scenarios.bat <scenario folder>` from the project.
- Otherwise ask the user to run it and paste the result here.
Each message names the file and the line. Fix the errors, one by one, in the file they name, and validate
again. Read the warnings too: "an NPC is never named", "a scene without NEXT ACT", "file ignored" (a sheet without its `# Name` first line is not loaded) are worth a fix.
Typical errors: a command not alone on its line, an unknown NPC name, a continuation file that does not start
with a `# ` heading, a missing `Joueur :` line.

## Step 2 bis: final leak check
Read only the premise and every container (the text of headings that have sub-headings), one after the other,
with `plan.md` open. None may summarise or announce what happens later in the story. Then run the leak check
of `ref-writing-tips.md` once more on the sheets (`# SECRET` written as states, no sequence of actions) and on
the scenes whose neighbours changed. Fix what you find and validate again.

## Step 3: test play
Ask the user to play the first scene with `chat.bat` and to say what they notice. Useful checks:
- The first message the player sees tells them who they are and where.
- The NPCs sound different from each other.
- The 🔍 button says the `[NEXT ACT]` fact is not met too early, and is met when it should be.
- No NPC alludes to something that has not happened yet, and none rushes the story toward its end. If one
  does, open the 🐞 view (the text really sent to the NPCs), find the line that gave it away and remove it.
Fix the file that causes the problem (a scene's labels, a sheet), validate again.

## Checklist
- Validation shows no error.
- The final leak check is done.
- The user played at least the first scene.
- `plan.md` may stay in the folder (it is a `.md` file, the game ignores it).
