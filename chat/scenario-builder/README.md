# Scenario builder: write a chat scenario with a local LLM

A playbook to build a complete scenario for the Storymagine chat (roleplay with one or several NPCs). It is
written for a local model with a **60k tokens context or more** (Gemma, Qwen...). It works with any AI: the files
are plain markdown, nothing here depends on a tool.

## The idea
A local model drifts when it is asked to do everything at once: it forgets a name, writes a scene too long,
slips into "you". So the work is cut into **six phases** that run in one conversation, one after the other.
A short file, **`plan.md`** (brief, cast, steps, facts established so far), is the anchor: the AI re-reads it
before every new file instead of trusting its memory of the conversation.

## How to use it
1. Give the AI this whole folder at the start (about 12,000 tokens), then the user's request (the "spec").
2. The user says where the scenario goes (a folder inside the scenarios directory). The AI creates
   `<scenario-name>/` there and writes `plan.md` in it.
3. Run the phases in order, in the same conversation. The AI announces each phase before starting it.

| Phase | File | Produces |
|---|---|---|
| 1 | phase-1-brief.md | the brief, written in `plan.md` |
| 2 | phase-2-plan.md | the cast table and the steps table in `plan.md`; the user approves brief and plan together |
| 3 | phase-3-cast.md | `characters/<id>.txt`, one NPC at a time |
| 4 | phase-4-steps.md | `scenario.txt`, then `scenario-1.txt`, `scenario-2.txt`... (decimal numbers allowed: `scenario-2.1.txt`) one step at a time |
| 5 | phase-5-next-act-check.md | a check that every scene's `[NEXT ACT]` exists and can really happen, written in `plan.md` |
| 6 | phase-6-finish.md | `style.txt`, validation, a test play |

References: `ref-format.md` (the files), `ref-commands.md` (the commands), `ref-writing-tips.md` (what makes a
scenario work), `skeleton.md` (the shapes to fill). Together they are the short guide for an AI; the long human
guide is not needed.

## What this folder needs from outside
Nothing to read: this folder is complete on its own (copy it anywhere). Only the game itself lives elsewhere,
and the AI never needs to open it:
- the **scenarios directory** where the scenario folder is created (the user tells where);
- the game's **validation tool** and the game to **play** the first scene (in the Storymagine project:
  `validation-scenarios.bat <scenario folder>` and `chat.bat`, run by the user or by the AI if it can run
  commands). Without them, see "If you cannot ask or run anything" below and the hand checks in phase 6.

## Rules for the AI (they apply to every phase)
- Do only what the current phase asks, then report before going on.
- If a decision is missing, ask the user **one question at a time**, with two or three proposed answers.
- Use only the commands in `ref-commands.md`. When you need something else, write plain narration.
- Write the scenario text in the **third person**: "Alex enters the hall", never "you enter".
- The examples show a format, never a subject. Do not reuse their names, places or plot.
- Write the scenario in the language the brief gives. The two header keywords `Joueur :` and `Langue :` always
  stay as written.
- Before writing any file, re-read the matching rows of `plan.md`. After writing it, add what is now established.

## Size limits belong to the game, not to you
Your own context is large, but the game's is not: at every turn each NPC re-reads the premise, its sheet, the
current scene and the whole conversation so far. A long premise, sheet or scene is paid again at every
turn for the whole game. So the limits in `ref-format.md` (premise, sheet and step sizes) stay, whatever your
context size. When a file grows past its limit, split it, do not squeeze it.

## If you cannot ask or run anything
Some setups give the AI no way to question the user or run the validator. Then: treat the spec as the answer to
every question (choose the first fitting answer when it is silent) and as the approval of the plan; write each
choice you made under `## Assumptions` in `plan.md`; skip the validation and the test play, and end by telling
the user which two steps remain for them (run the validation, play the first scene).

## If the model loses the thread
If the AI forgets names, mixes up steps or ignores the rules, switch to the strict way of working: give one phase
file at a time with only the references it names, and have the AI forget each file once written (`plan.md`
carries on). This is also the way to work with a model whose usable context is 16k tokens or less.

## What makes a good result
A scenario where the player always has something to find out or decide, NPCs who sound different from each
other, each scene ending on one clear fact, and no secret revealed before the scene that needs it.
