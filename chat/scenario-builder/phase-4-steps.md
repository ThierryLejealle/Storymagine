# Phase 4: the scenario files (one step per file)

**Load, for each step N:** re-read `plan.md` (brief, cast, row N and row N-1 of `## Steps`, and `## Facts
established`). References: `ref-format.md`, `ref-commands.md`, `ref-writing-tips.md`, `skeleton.md`.
The files already written stay available: check names, places and facts against them.
**Produce:** `scenario.txt` for step 1, then `scenario-1.txt` for step 2, `scenario-2.txt` for step 3, and so on.
If a step file grows past about 600 words, split it at a `#` heading into a decimal-numbered file
(`scenario-2.1.txt` comes between 2 and 3: nothing else is renamed).
**Then:** one file per step. Do not write several steps in one go.

## Step 1: the first file, scenario.txt
It holds the header (`Joueur :`, `Langue :`), the `#SCENARIO` premise, and step 1. Keep the premise under 80
words with no spoiler. If the brief asks for only some NPCs at the start, add `{npc-only A, B}` in the
premise or the first scene, before the first `[NEXT ACT]`. In the first scene, the `[...]` line says who
the player is, where they are, and names every NPC who is present.

## Step 2: each following step, in its own file
Start the file with `# Step title` at the top level. Cut the step into 2 to 5 scenes with `##`. Each scene
has its `[...]` line, its `Starting point`, one line per NPC and the `[NEXT ACT]` line (shape in
`ref-format.md`). Rules:
- About 600 words per file at most. If a step needs more, split it into two files.
- Text under a parent heading (a container) is read at every turn of every scene below it: keep it to a
  sentence or two, and only what is true for all those scenes. Do not write what is going to happen in them.
- A scene holds only the situation now and what each NPC does in THIS scene. Nothing from a later scene
  (re-read "Nothing from the future" in `ref-writing-tips.md`). If a scene needs more than two or three
  moments, cut it in two.
- What one NPC must not know goes in a `{private Name}` line, not in the shared text.
- Each scene ends on ONE observable fact in `When to write [NEXT ACT]`. The plan gives the fact that ends the
  step: use it for the last scene of the step. For the earlier scenes of the step, choose your own fact (it
  must differ from the step's) and add it to the step's row in the plan. The very last scene of the story has none.
- Every scene opens with its `[...]` line (who the player is, where, who is there).
- Bring NPCs in and out with `{npc-arrives}` and `{npc-leaves}` at the start of the step that needs them.
  A newcomer knows everything said before: have another character tell them what they need, in the `[...]`
  line or the scene text.
- Apply the character changes from the plan under the heading where the change starts: `{npc-delta}` for a
  final change, `{npc-story on ...}` (and `off` for the previous level) for a feeling that grows by stages.
  Complete an NPC's motive this way when it becomes useful, rather than putting everything in the sheet.
- A secret is revealed in the scene where the plan puts it. Never earlier.

## Step 3: after each file
1. Validate (see phase 5, step 2) and fix every error the validation names. If you cannot run or ask for a
   validation, say so and go on (see "If you cannot ask or run anything" in the README).
2. Run the leak check on every scene of the file (see `ref-writing-tips.md`, "Leak check"): nothing from a
   later scene may appear in the scene, its containers, the premise or the sheets. Fix what you find.
3. Append 5 lines to `## Facts established` in `plan.md`: names given, places, objects, what the player
   knows now, open threads, and the line "Leak check: ...".
4. Go to the next step.

## Checklist for each file
- Starts with a top-level `# Title` (continuation files) or the header (scenario.txt).
- Every scene except the last of the whole story has a `[NEXT ACT]` line with one fact.
- Everything outside commands is in the third person.
- Only commands from `ref-commands.md`, each alone on its line.
- The scenario can be played after this file: it is a complete scenario so far.
- The leak check is done and written in `plan.md`.
