# ChatSummarizer

## Role
Folds turns that are about to fall out of the live window into the session's running summary,
producing one updated summary. Called by `ChatServiceImpl` whenever the uncompacted transcript
grows past `SummaryBudget.wordBudget(contextWindow)` (shared budget helper, see
`commun/coeur/domaine/text/SummaryBudget.java` — same formula redacteur uses for its story
summary, 1/8th of the context window).

## Why fold instead of truncate
A plain "drop the oldest turns" strategy loses established facts (names, promises, relationships)
the moment they scroll out of the window — the character would start contradicting itself. Folding
into a summary keeps the facts, at the cost of exact wording, which does not need to survive.

## Kept verbatim vs folded
`ChatServiceImpl` keeps the last `KEEP_RECENT_TURNS` turns untouched (verbatim, for immediate
continuity) and folds everything older into the summary. Only one compaction pass — no separate
compression step like redacteur's `SummaryCompressor` : chat sessions are simple text, not a
multi-chapter story, so a single running summary is expected to stay bounded on its own as older
material keeps getting superseded turn after turn.

## Prompt shape
Uses `ModelCallPort` (`/api/chat`, system + user roles) — same port as the roleplay turn itself
since 2026-07-11 (see "Why /api/chat, not raw completion" below). Both calls go through the same
`OllamaAdapter` instance, wired once in `ChatModule`.

## Why /api/chat, not raw completion
The roleplay turn originally went through `RawCompletionPort` (`/api/generate`, `raw:true`, no
chat roles), reasoning that instruction-tuned models with no custom Modelfile `TEMPLATE` would
respond better to unstructured continuation. In practice this broke on the very first reply : the
model echoed the character's name a few times then degenerated into repeating a single word,
because `raw:true` strips the chat-template scaffolding the model was actually fine-tuned to
operate inside (Gemma's `<start_of_turn>` tokens etc.) — it was running out-of-distribution.
Manually chatting with the same model through Ollama's normal interface (which always uses
`/api/chat`) worked immediately and well. Confirmed with Fable before reverting : `RawCompletionPort`
stays available in `commun` as a general capability, just unused by `chat` now.

## Transcript format
Turns to fold are formatted via `ChatPromptBuilder.transcript()` ("Player: .../Character: ..."),
the exact same wording used to build the live prompt, so the summarizer sees turns in the same
shape the roleplay call does.

## Prompt design (reviewed by Fable, 2026-07-11)
The prompt was drafted, then handed to Fable for review against this project's small-model house
rules (precise, no duplicated instructions, examples, explicit edge cases). Two things the first
draft missed, now baked in:
- **Pending plans survive compaction.** The player sometimes uses `OOC: ...` to negotiate the
  character's next actions before playing them out. A naive summarizer would fold that
  negotiation away as "small talk". The prompt now requires any agreed plan to be kept as one
  `"Pending: ..."` sentence, with a worked example, so it's still visible — and still marked as
  not-yet-happened — after compaction.
- **Empty-summary sentinel.** The user slot always gets the literal string `(empty)` on the very
  first compaction rather than a blank previous-summary section — small models tend to mirror or
  hallucinate content into a blank slot.

**Language (2026-07-11, follow-up)** : the prompt originally said "Write factual prose in English"
— wrong whenever the chatscenario itself isn't in English (this project's own chatscenarios are
French). Changed to "the same language as the turns" so the summary always matches the
conversation's actual language instead of forcing a translation. Same fix applied to the roleplay
system prompt in `ChatPromptBuilder` ("Reply in the same language as the character and scenario").

**Known open gap, deliberately not addressed yet** : a character's private `*thoughts*` written by
the player can end up folded into the summary, which is later reinjected into the LLM's own
prompt — a possible metagaming leak around the "no reacting to private thoughts" rule in
`ChatPromptBuilder`. Left out of scope for the first version; add a `Drop:` line for private
thoughts here if it turns out to matter in practice.
