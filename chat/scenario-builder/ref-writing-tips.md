# Writing tips for a small model (one page)

Load this file in phases 3, 4 and 5. It is the short version of what makes a scenario work in the game. Follow
it rather than your usual style.

## Why it matters
Each NPC reads, at every turn, the premise, its own sheet, the text of the current scene and the conversation.
Anything you write is read again and again by a small model, which copies it, anticipates it or gets
confused by it. So: short, concrete, nothing too early.

## Ending a scene (the `[NEXT ACT]` line)
- Write ONE observable fact the player or an NPC can do or say: "As soon as Sam has named the thief."
- Anchor it on what the player does or says, not on an NPC's feeling ("Nell is convinced" cannot be seen).
- Phrase it positively with "as soon as" or "once". Avoid absolute bans ("never before...", "only if not..."):
  a small model loops on them.
- The fact must be **possible and coherent with the scene**: it can happen with the NPCs present, and the NPC
  lines lead toward it (it may take several exchanges, but the scene must head that way). It names the NPC who
  really acts. Wrong: Nell challenges the player, and the condition is "the player accepts Hobb's challenge"
  (Hobb never challenged anybody). If nothing in the scene leads toward the fact, add an NPC line that does.
- It must not be true at the start: starting point "they are in the inn", condition "they are in the inn" is a
  flaw. The game does not branch: do not write alternative exits, the player is expected to follow the road.
  Phase 5 checks every scene.
- Two scenes in a row must differ in place, adversary or goal. If you cannot say how, merge them.

## Nothing from the future (the rule that matters most)
An NPC acts on everything it reads, right now. If it reads what must happen later, it makes it happen at
once, even out of character, and the story is ruined. So:
- A **played scene** holds only: the situation as it is now, and what each NPC does or tries in THIS scene.
  Write each NPC's line like a direction to an actor for this scene only: "Franck wants to win badly and will
  cheat by pulling Alex's cable." Never an event of a later scene, and never a result ("Franck wins").
- A **container** holds only what is true for all its scenes: place, era, who is around. Never a summary of
  what is going to happen in them ("Marcus will kidnap Sam in the end").
- The **premise** has the world, the tone and the starting situation, never what comes later.
- Write a line "What must happen", "What happens next" or "The goal of this part" nowhere. They push the NPCs
  to rush. The end of the scene is the `[NEXT ACT]` fact and nothing else.
- Two or three moments in one scene are fine ("first he reads in silence, he protests only when his idea is
  rejected"). More than that, cut the scene in two.
- A twist is written in the scene where it becomes useful. A secret known by one NPC from the start goes in that
  NPC's `# SECRET`; a fact true only from some moment on goes in a scene.

## Leak check (run it after each scenario file, and again at the end)
A leak is anything an NPC reads that it must not know yet. The validation cannot see it: you check it by
reading, scene by scene. For each played scene of the file you just wrote:
1. List what the NPCs of this scene can read: the premise, the containers above the scene, the scene text,
   and each NPC's sheet. (A `{private Name}` line is read only by that NPC.)
2. In `plan.md`, take the rows AFTER this scene: their `[NEXT ACT]` facts, what the NPCs do, the character
   changes, the reveals. Look for each of them in what you listed. None may appear, not even as a hint or
   with words like "will", "later", "then", "finally", "in the end".
3. Containers and premise: is there a sentence about what happens in the scenes below, or a summary of the
   plot? Delete it. Keep only the place, the era, who is around.
4. Sheets and `# SECRET`: is there a sequence of actions or a future ("he will follow Sam, then...")? Rewrite
   it as a state of the NPC (a desire, a loyalty, something hidden).
5. Each NPC line of the scene: is it something this NPC does or tries in THIS scene, starting now? If it states
   a result or something for later, rewrite it as an attempt for this scene.
6. Each shared line: would another NPC of the scene act differently if it read it? If yes, move it to a
   `{private Name}` line.
7. No line "What must happen", "What happens next" or "The goal of this part" anywhere.
Write the answer in one line at the end of `## Facts established` in `plan.md`: "Leak check: none found" or
the list of what you fixed. When the user plays, the 🐞 button shows the text really sent to the NPCs: if an
NPC alludes to something that has not happened yet, a leak remains. Find the line and remove it.

## Motive, not plan
- An NPC may have an ultimate motive: its `# SECRET` can say what it wants or hides (a desire, a loyalty, a
  debt). Write it as a state of the NPC: "works for people who want Sam, needs Sam to trust him". It colours
  every scene without telling the NPC what to do next.
- Never write the motive as steps or actions to carry out ("he will follow Sam, then kidnap him tonight"). A
  small model reads it as orders and does them in the first scene.
- The motive does not need to be complete from the start. Complete it later with `{npc-delta secret Name : ...}`
  in the scene where it becomes useful (for instance: "tonight is the night, the car waits behind the market").
- What must stay hidden from the other NPCs in a scene goes in `{private Name}`, not in the shared scene text.

## Character sheets
- Behaviours, not labels: "avoids eye contact, answers in short sentences" instead of "shy".
- A sheet says who the character is. What happens to them goes in the scenes (`{npc-delta}`).
- Give each NPC a different voice: length of sentences, favourite words, how they react when pressed.

## Scenes and NPCs
- The first scene names every NPC who is present, so none is forgotten.
- Two or three active NPCs at a time. Bring in extra NPCs as `present`, make them `active` only to lead a scene.
- A newcomer knows all that was said before: have someone tell them what matters.
- Third person everywhere ("Alex opens the door"), never "you". Never write the player's thoughts or actions.
- A scene text of one to three sentences per label is enough. Long text makes the model repeat it.

## Transitions between scenes
The story must flow: if two friends fight in one scene and study together in the next, the player needs to
know that they made peace in between. No need for a played scene: put a short transition at the start of the
next scene's opening `[...]` line (the line shown to the player when the scene opens), in the same sentence
or just before the situation:
`[The two friends made peace after the fight. The next morning they study together in the library.]`
- One or two short sentences, third person, plain facts: what changed since the previous scene (time that
  passed, a new place, a mood, a made-up peace). Nothing about what comes later.
- Say only what the previous `[NEXT ACT]` fact guarantees, or something neutral ("Later that day").
- Never decide what the player did or felt. Describe the world and the NPCs.
- A transition is a proposal: tell the user which ones you added so they can keep, change or remove them.

## Variety lines
A `{N%}` line in a scene stays in the prompt at every turn of that scene: write it as a state, not as an action
that happens once. Keep the global style short and generic; put the mood of a place in its scene style.

## Commands in prose
A command written between braces is always read as a command, even in a note or an example. To talk about a
command in the text, use its name without braces (npc-arrives).
