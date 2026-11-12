---
name: chat-scenario-builder
description: Build a complete chat scenario (scenario.txt, scenario-N.txt, characters/, style.txt) step by step with a local LLM (60k tokens of context or more), from a brief to a validated result. Use when asked to design or write a whole new chat scenario, not to edit a few lines of an existing one.
---

# Build a chat scenario in phases

The playbook is in `chat/scenario-builder/`. Read `chat/scenario-builder/README.md` first (the whole folder is about 12,000 tokens: load it at
the start), then follow the six phases in order in one conversation. `plan.md` in the scenario folder is the
anchor, re-read before every file. If the model loses the thread, work phase by phase (see the README).

1. `phase-1-brief.md` : questions (only what the spec does not answer) -> brief
2. `phase-2-plan.md` : cast table and steps table, user approves
3. `phase-3-cast.md` : one character sheet at a time
4. `phase-4-steps.md` : one scenario file per step, playable after each, with a leak check after each file
5. `phase-5-next-act-check.md` : every scene has a `[NEXT ACT]` that its NPC lines and the situation make possible
6. `phase-6-finish.md` : style, validation, final leak check, test play

The rule that matters most: an NPC acts on everything it reads, so a scene never holds anything that happens
later (see `ref-writing-tips.md`, "Nothing from the future" and "Leak check").

For editing a few lines of an existing scenario, use the `chat-scenario-writing` skill instead.
When a chat feature changes what a scenario author writes, update this playbook too (`ref-commands.md`,
`ref-format.md`, the phase concerned), in the same change as `chatscenarios/guide-redaction-chat.md`.
