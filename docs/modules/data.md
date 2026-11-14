# Data

Linked from docs/README.md. Supersedes the INI stores. Tables and files are in [schema.md](../db/schema.md).

`TextToolsStore` replaces each list in one transaction. An empty action table is seeded from the factory list. `quick_saved` keeps an intentional empty quick list. Roles are filled from the seed on read. `appendUsage` inserts one row with the next position under one lock. History and usage limits are the domain functions the store calls.

`LlmClient.complete` posts `{baseUrl}/chat/completions` with `stream` false and a bearer header. The body is the model and the chat messages. An empty HTTP body is an error. A non-success status throws `AppException` with the reason and the HTTP status. Empty message content is an error. Usage is read from `prompt_tokens` and `completion_tokens`; a missing usage object is null, and a missing count is zero only when the other count is present. `shouldProxy` is true only when `effectiveProxy` is not empty and the address is not loopback. The proxied client is used only then. The app supplies both clients and each waits 180 seconds for response bytes.

`LegacyImport` reads an AHK folder and does not modify it. The file-to-table map is on the schema page. Secrets come back on the result object.

`AppWebSearch.notes` queries `https://html.duckduckgo.com/html/` for the executable, the words `windows application`, and at most 80 characters of the window title. The request waits at most 8 seconds. A timeout, a non-success status or a parse failure returns an empty note. `duckSnippets` keeps at most four title and snippet pairs. Cancellation other than a timeout is not swallowed.

`PriceCatalogue.refresh` fetches `https://openrouter.ai/api/v1/models` when `force` is set or the stored body is missing, blank, or 24 hours old. The request waits at most 20 seconds. A useful body contains `"data"` in its first 400 characters. A failed load keeps the previous body and does not throw. The proxy client is used when the effective proxy is not empty. The test is PriceCatalogueTest.

The settings screen and the history cost read that stored body by `"id"`. An id without `/` on an OpenAI or Anthropic address is also looked up as `openai/<id>` or `anthropic/<id>`. The settings row shows `context_length` and the prompt and completion prices per million tokens. A missing number stays missing. History cost uses `costOf` and does not invent a context length.
