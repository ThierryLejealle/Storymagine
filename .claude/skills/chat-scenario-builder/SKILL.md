---
name: chat-scenario-builder
description: Build a complete chat scenario (scenario.txt, scenario-N.txt, characters/, style.txt) step by step with a local LLM (60k tokens of context or more), from a brief to a validated result. Use when asked to design or write a whole new chat scenario, not to edit a few lines of an existing one.
---

# Build a chat scenario in phases

The playbook is in `chat/scenario-builder/`. Read `chat/scenario-builder/README.md` first (the whole folder is about 6,000 tokens: load it at
the start), then follow the five phases in order in one conversation. `plan.md` in the scenario folder is the
anchor, re-read before every file. If the model loses the thread, work phase by phase (see the README).

1. `phase-1-brief.md` : questions (only what the spec does not answer) -> brief
2. `phase-2-plan.md` : cast table and steps table, user approves
3. `phase-3-cast.md` : one character sheet at a time
4. `phase-4-steps.md` : one scenario file per step, playable after each
5. `phase-5-finish.md` : style, validation, test play

For editing a few lines of an existing scenario, use the `chat-scenario-writing` skill instead.
When a chat feature changes what a scenario author writes, update this playbook too (`ref-commands.md`,
`ref-format.md`, the phase concerned), in the same change as `chatscenarios/guide-redaction-chat.md`.
