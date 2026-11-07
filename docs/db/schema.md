# Schema

Linked from docs/README.md. Supersedes the AHK INI layout for the new line.

`action.model` and `action.menu_key` are NULL when the action uses the default. There is no role column. At most 200 actions are written. An empty `action` table is filled from the factory list on read.

`setting.quick_saved` absent means the factory chips. Present means the rows in `quick_refine`, including zero rows.

`app_profile` is the source-application list, in `position` order. `title` and `hint` stay empty strings when they are empty: an empty title matches any window of that process, and an empty hint adds nothing to the prompt. Neither is stored as NULL.

`output_language` is the output-language list, in `position` order. `enabled` is 1 when the language is offered on the action card, and 0 when it is not.

`known_model` is the model list of one API address. `base_url` is that address, stored exactly as written. Replacing one address leaves every other address in place. `selected` is 1 when the model is ticked.

`history` keeps at most 20 rows. On write, rows past 20 are dropped from the front of the given list, then `source` and `result` are cut to 8000 characters. `prompt_tokens` and `completion_tokens` are NULL when the provider sent no usage; the token count in the history window is their sum. `cost` is NULL when the price was unknown. `at_epoch_ms` is Unix time in milliseconds.

`usage_log` is one row per request, in `position` order: `at_epoch_ms`, `label`, `model`, `prompt_tokens`, `completion_tokens`. Times are Unix milliseconds.

`price_cache` holds one catalogue body in `body` and the load time in `fetched_at` (Unix milliseconds). No row means the cache has not been stored.
