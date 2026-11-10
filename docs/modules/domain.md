# Domain

Linked from docs/README.md. Supersedes Lib/*.ahk for the new line.

`BehaviorTest` locks the carried behavior. Open `Factory.kt` for the seed text, the 16 menu keys and the default settings. Unknown-application notes are in [profiling.md](../dev/profiling.md). The three carried decisions that page names are in [D1.md](../dev/D1.md). The result is a markdown tree: [S5-result-ast.md](../adr/S5-result-ast.md).

The module has no Compose, Ktor or SQLDelight dependency. Money is not part of `Usage`. `Action.role` is not stored. `AppException` carries an `ErrorReason`, HTTP status and technical detail; the app maps the reason to XML translations. `RunOutcome.Failed` keeps that reason rather than a Russian UI phrase.

## Run

`ActionRunner.run` refuses a second call while `busy` is set, and refuses a call with no API key. Otherwise:

1. The model is `action.model`, or `settings.defaultModel` when that is null.
2. The call is one system message from `buildInstruction` and one user message from `buildUserContent`.
3. A thrown request becomes `Failed`. Trimmed empty text becomes `Failed` and is not delivered.
4. Delivery is `Window` when `openInWindow` or `forceWindow` is set, otherwise `Paste`.
5. A window delivery, or a paste that does not land, is `InWindow` and keeps the system, user and assistant turns. A landed paste is `Pasted` and does not keep them.
6. `busy` clears before the function returns, including after a failure.

## Prompt

`buildInstruction` joins these blocks with a blank line, in this order: language, context, `SOURCE APPLICATION:` when the hint is not empty, fidelity, the style block when `styleOn` is set, the task, brevity, output. Brevity always follows the task. `auto` and an empty language use the auto language block. Any other code uses the fixed block, with the language list's English name or the code itself. Style is Russian, English, or the auto style for every other code.

`buildUserContent` is the prefix plus the selection. A non-empty extra instruction is appended. The action named `Custom` labels that extra text as the change to make. Every other action labels it as an additional instruction.

## Keys, models and providers

`yieldMenuKey` changes the occupant only. Cancel, or an index outside the list, returns null. Swap gives the occupant the incoming action's old key, and an empty old key becomes null. Take clears the occupant's key. The card being edited is updated by the caller.

`replaceModels` drops ids that contain a non-chat needle, then distinct-sorts the rest and clears every tick. `mergeModels` keeps a ticked id even when the provider no longer lists it, unions that set with the fresh chat ids, and sorts. Ticks survive.

`providerUrls` is the eight-address dropdown. `providerOf` recognises those hosts and also other URLs that contain the same host markers. An unknown host is an empty provider.

`roleModel` returns an empty string for an empty role. A missing role uses that provider's `main` row. A missing provider returns empty. `retarget` rewrites only an action whose `factoryId` is still in the seed, using that seed row's role. An empty role model becomes null. `duplicateFactory` clears `factoryId` before a copy can be retargeted.

`normalizeProxy` accepts `http`, `https`, `socks4`, `socks4a`, `socks5` and `socks5h`. A bare host and port becomes `http`. Quotes and line breaks are rejected. `effectiveProxy` is empty when the checkbox is off, and the trimmed URL when the checkbox is on or missing. `isLoopback` matches `127.0.0.1`, `localhost` and `[::1]`. `clipWaitSeconds` clamps to 0.2–5.

## Match, cost, history and markdown

`matchApp` returns null for an empty process. Among rows of that process, a non-empty title that is contained in the window title wins. Otherwise the first row with an empty title wins. Comparison is case-insensitive.

`estimateTokens` is the rough count shown before the provider answers: about 4 characters per token for ASCII and 2 otherwise, from the first 4000 characters. `costOf` is null when the catalogue row or either per-token price is missing. Otherwise it is prompt tokens times the prompt price plus completion tokens times the completion price. `contextLength` is only the entry's own field.

`appendHistory` trims source and result to 8000 characters and keeps the newest 20. `trimConversation` returns nine or fewer messages unchanged. A longer list keeps the first message and a tail of seven, moved one message later when that start is not a user turn.

`parseMarkdown` splits on lines. At each line it tries, in order, a fence, a table, a bullet list, a numbered list, then a heading. A fence starts with three backticks and runs until the next such line, or to the end. A table needs a `|` row and a following line that contains `---`. Bullets are lines starting with `- `. Numbers match `digits. text`. A heading is one or more `#`. A blank line is skipped. Anything else is one paragraph. Code is literal. The window paints inline markers; this parser does not turn them into HTML.
