# Writing tips for a small model (one page)

Load this file in phases 3 and 4. It is the short version of what makes a scenario work in the game. Follow
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
- Two scenes in a row must differ in place, adversary or goal. If you cannot say how, merge them.

## No spoilers
- The premise has the world, the tone and the starting situation, never what comes later.
- A twist is written in the scene where it becomes useful. A secret known by one NPC from the start goes in that
  NPC's `# SECRET`; a fact true only from some moment on goes in a scene.
- Under a parent heading that has sub-scenes, write only what is true for all of them. Never summarise what is
  going to happen: every scene below re-reads it, and the NPCs rush to the ending.

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

## Variety lines
A `{N%}` line in a scene stays in the prompt at every turn of that scene: write it as a state, not as an action
that happens once. Keep the global style short and generic; put the mood of a place in its scene style.

## Commands in prose
A command written between braces is always read as a command, even in a note or an example. To talk about a
command in the text, use its name without braces (npc-arrives).
