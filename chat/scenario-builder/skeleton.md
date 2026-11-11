# Skeleton: the files with fields to fill

Copy the shape, replace every `[field]`. The skeleton has no story on purpose: invent your own from the brief.
Delete the lines that do not apply (comments after `<-`).

## scenario.txt
```
Joueur : [player name]
Langue : [language]

#SCENARIO
World: [where and when]
Premise: [starting situation, third person, no spoiler]
Tone: [mood]
{npc-only [Name], [Name]}                      <- optional: only these NPCs start in the story

# [Step 1 title]
## [Scene 1.1 title]
[What the player sees: who they are, where they are, who is there. Third person.]
Starting point: [the situation right now]
[NPC name]: [what this NPC does or tries in this scene]
[NPC name]: [what this other NPC does or tries in this scene]
{private [NPC name]} [only if the others must not know it: what this NPC does or hides in this scene]
When to write [NEXT ACT]: [one observable fact]

## [Scene 1.2 title]
[What the player sees when this scene opens.]
Starting point: [...]
[NPC name]: [...]
When to write [NEXT ACT]: [one observable fact]
```
A scene (played scene) says nothing about what happens in the next ones. A heading with sub-headings
(container) holds only what is true for all its scenes.

## scenario-1.txt (next step; same shape, starts with a top-level heading)
```
# [Step 2 title]
{npc-arrives [Name]}                           <- optional: someone enters here
{npc-delta public [Name] : [what changed about them]}   <- optional

## [Scene 2.1 title]
[What the player sees.]
Starting point: [...]
[NPC name]: [what this NPC does or tries in this scene]
When to write [NEXT ACT]: [one observable fact]
```
The very last scene of the last file has no `When to write [NEXT ACT]` line.

## characters/[id].txt
```
# [Name]
[Public part, 80 to 120 words: role, age or look if useful, how they speak, two habits others can see,
how they stand with the other NPCs at the start.]

# SECRET
[Secret part, 30 to 60 words: what only they know, what they want and hide, written as a state ("wants
the ledger, hides that he owes money"), never as steps to carry out.]
```

## styles/[mood].txt (optional, only for variety)
```
[Mood of this place in one or two short lines.]
- {20%} [A descriptive detail, never an action.]
```

## style.txt
```
[Sentence length and rhythm.]
[Vocabulary level for the audience.]
[Point of view and tense of narration.]
```

## plan.md
```
# [Working title]

## Brief
- Audience: [...]
- Genre and stakes: [...]
- Promise to the player: [what will make it worth playing]
- Player: [name and role]
- Scale: [number of steps]
- Limits: [what must not appear]
- Language: [...]

## The answer (mystery only: who, what, how)
## Assumptions (only when you could not ask the user)

## Cast (one line each)
| Name | Role | Wants | Secret (one line) | Voice |
|---|---|---|---|---|

## Steps
| # | Place | Who is there | Starting point | What each NPC does in this step | NEXT ACT fact | Character changes | Different from previous by |
|---|---|---|---|---|---|---|---|

## Facts established (append 5 lines after each step file)
```
