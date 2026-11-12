# Phase 5: the NEXT ACT check

**Load:** `plan.md`, every scenario file written so far, and `ref-writing-tips.md` ("Ending a scene").
**Produce:** a table of verdicts in `plan.md` (`## NEXT ACT check`) and the fixes it calls for.
**Why:** the NPC who is playing decides alone when to write `[NEXT ACT]`. If the condition cannot happen with
what this scene puts on stage, or if nothing in the scene leads toward it, the story gets stuck. Read the
scenes, not your plan: the plan says what you meant, the files say what the NPCs will really read.
The game does not branch: the story has one road, and the player is expected to stay on it. So do not write
alternative exits. Make the road clear instead: the NPCs lead toward the condition.

## Step 1: did you think of a NEXT ACT for every playable scene? (do this FIRST)
Before asking whether any condition is good, make sure each scene HAS one. Authors forget it.
1. List all the headings of all the files, in reading order (`scenario.txt`, then the numbered files in order).
2. Mark which are **played scenes** (no sub-heading under them) and which are containers (they have
   sub-headings). Only played scenes need a `[NEXT ACT]`.
3. For EVERY played scene except the very last one of the whole story, write "yes" or "MISSING" next to it:
   is there a `When to write [NEXT ACT]` line, with ONE fact, inside that scene?
4. For each "MISSING": formulate one now, even a first draft (step 3 will judge it). A line in a container
   does not count: move it into the played scenes below. The very last scene must have none: remove it if
   it has one.
Go to step 2 only when every playable scene but the last has its line.

## Step 2: one row per scene
Fill this table in `plan.md` by reading each scene's text:

| Scene | The fact that ends it | The NPC line that leads toward it (quote it) | Verdict |
|---|---|---|---|

## Step 3: the three checks (every answer must be yes)
1. **Possible.** With the situation and the NPCs of this scene, the fact can happen: every person, place and
   object the condition names is in the scene (starting point, an NPC line or the opening `[...]` line), and
   every NPC it names is present.
2. **Coherent with the NPC lines, which lead toward it.** At least one NPC line pushes toward the fact, and the
   condition names the NPC who really acts in the scene. It may take several exchanges, but the scene must head
   that way. Wrong: Nell challenges the player, and the condition is "the player accepts Hobb's challenge".
   Hobb never challenged anybody. Right: "the player accepts Nell's challenge" (or give the challenge to Hobb).
   No NPC line toward the fact? Add one (that is how you guide the NPCs), or change the condition.
3. **Not already true at the start.** The starting point and the NPC lines do not already make the fact true:
   a condition that is true from the first turn is a flaw of the scenario. Wrong: starting point "they are in
   the inn", condition "they are in the inn".

## Step 4: continuity with the next scene
If the next scene does not follow naturally from the fact that ends this one (time passed, a new place, a
mood that changed: a fight, then a quiet study), bridge it with a short narrative transition at the start of
the next scene's opening `[...]` line (`ref-writing-tips.md`, "Transitions between scenes"). It is a sentence,
not a new scene. List the transitions you add, one line each, so the user can keep, change or remove them.

## Step 5: fix, then report
- Fix by adding or changing an NPC line first when the scene is the problem; change the condition when the
  condition is the problem.
- Every edited scene goes through the leak check again (`ref-writing-tips.md`).
- Update the NEXT ACT column of the steps table in `plan.md`.
- Show the user the table with the fixes and the transitions, in a few lines, before going on.
- Add one line at the end of `## Facts established`: "NEXT ACT check: all scenes ok" or the list of what you
  fixed.

## Checklist
- Step 1 came first: each playable scene but the last has exactly one `[NEXT ACT]` line, in the scene.
- Each row of the table is "yes" on the three checks, with a quoted NPC line.
- Fixes and transitions are written in the files and in `plan.md`.
