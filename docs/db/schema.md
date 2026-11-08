# Schema

Linked from docs/README.md. Supersedes the AHK INI layout for the new line. Secrets stay out of SQLite: [S5-secrets-outside-db.md](../adr/S5-secrets-outside-db.md). User language packs stay INI for import, and this version's interface uses bundled XML: [S5-user-lang-ini.md](../adr/S5-user-lang-ini.md).

The database is `%LOCALAPPDATA%\TextTools\texttools.db`. `user_version` 0 creates the SQLDelight schema. A lower version is migrated to the current schema version. There is no hand-written migration file.

Lists are replaced as a whole. `position` is the write order, starting at 0. A missing `LOCALAPPDATA` throws before the database is created.

## action

| Column | Rule |
| --- | --- |
| position | Primary key. Write order. |
| factory_id | Seed id. NULL for a copy or a hand-made action. |
| name, icon, prefix, task | Required text. |
| menu_key | NULL when the action uses no grid key. |
| model | NULL when the action uses the default model. |
| lang | Required. `auto` follows the input. |
| style_on | 1 applies the language style block. |
| open_in_window | 1 opens the result window instead of pasting. |

There is no role column. On read, a seed id gets its role back from `Factory.actions`. At most 200 actions are written. An empty table is filled from that seed on read.

## quick_refine

| Column | Rule |
| --- | --- |
| position | Primary key. |
| label, instruction | Required text. |

## setting

| Column | Rule |
| --- | --- |
| key | Primary key. |
| value | Required text. |

The only key is `quick_saved`. Absent means the factory chips. Present means the rows in `quick_refine`, including zero rows. Hotkeys, the API address and the other switches are not columns here.

## app_profile

| Column | Rule |
| --- | --- |
| position | Primary key. |
| proc, title, name, description, hint | Required text. Empty means an empty string, never NULL. |

An empty title matches any window of that process. An empty hint adds nothing to the prompt. Order is `position`.

## output_language

| Column | Rule |
| --- | --- |
| position | Primary key. |
| code, name, prompt | Required text. |
| enabled | 1 offers the language on the action card. 0 keeps it stored but hidden there. |

## known_model

| Column | Rule |
| --- | --- |
| base_url, position | Primary key. `base_url` is stored exactly as written. |
| model_id | Required text. |
| selected | 1 when the model is ticked. |

Replacing one address leaves every other address in place.

## history

| Column | Rule |
| --- | --- |
| position | Primary key. The runner writes the newest row first. |
| at_epoch_ms | Unix time in milliseconds. |
| action_name, model, source, result | Required text. |
| prompt_tokens, completion_tokens | NULL when the provider sent no usage. Both NULL, or a zero sum, is a dash. Otherwise the history window shows the sum and treats one missing side as zero. |
| cost | NULL when the price was unknown. Never a stored zero that stands for "unknown". |

The store keeps the tail of the list it is given, at most 20 rows, then cuts `source` and `result` to 8000 characters. The runner already passes at most 20, newest first.

## usage_log

| Column | Rule |
| --- | --- |
| position | Primary key. |
| at_epoch_ms | Unix milliseconds. |
| label, model | Required text. |
| prompt_tokens, completion_tokens | Required. One row is one request. |

`appendUsage` reads and replaces under one lock. Clearing the log file does not delete these rows.

## price_cache

| Column | Rule |
| --- | --- |
| id | Primary key. The only row is id 0. |
| body | The raw catalogue body. |
| fetched_at | Unix milliseconds. |

No row means the cache has not been stored.

## usage_event

One row per actual model call: `action`, `refine` or `profile`, with `success`, `failure` or `cancelled`. Model-list and test calls are excluded. `input_chars`, `extra_chars` and `output_chars` are lengths, never text. `duration_ms` uses monotonic time around the provider call only. Nullable token columns mean that usage was not reported. `sentry_bucket` is NULL while a completed hour remains eligible for retry. Local rows remain when telemetry is off.

## ui.properties

`%LOCALAPPDATA%\TextTools\ui.properties`. Absent file means `Factory.defaultSettings`, except that a saved proxy secret turns the proxy on. A missing key keeps the factory value. `proxyOn` with an empty value means the checkbox was never a hard off: the proxy URL stays in force.

| Key | Meaning |
| --- | --- |
| baseUrl | OpenAI-compatible address. Blank falls back to the factory address. |
| defaultModel | Blank falls back to the factory model. |
| proxyOn | `true`, `false`, or empty. The proxy URL itself is not in this file. |
| hotkey, hotkeyRepeat | AutoHotkey text, for example `^!Space`. A blank menu hotkey falls back to the factory chord. |
| markdown | `true` opens a result on the rendered view. |
| uiLang | Interface language code. |
| appProfiling | `true` looks up an unknown executable. |
| clipWaitSeconds | Number, then clamped to 0.2–5. |
| sentryErrors, sentryTraces | Both default off. |
| autostart | `true` or `false`. A missing key is not a stored choice. An installed exe then follows whether the Run value matches. |
| profileModel | Profiling model. Blank uses the default model. |

## Other files

| Path | Contents |
| --- | --- |
| `%LOCALAPPDATA%\TextTools\TextTools_last.txt` | Last result text. Clearing history does not delete it. |
| `%LOCALAPPDATA%\TextTools\TextTools.log` | Append-only run log. `TextTools.log.old` may sit beside it. |
| `%LOCALAPPDATA%\TextTools\secrets\api-key.dpapi` | DPAPI blob for the current user. |
| `%LOCALAPPDATA%\TextTools\secrets\proxy-url.dpapi` | DPAPI blob for the current user. |
| `%LOCALAPPDATA%\TextTools\lang\user\<code>.ini` | User language pack. |
| `%LOCALAPPDATA%\TextTools\sentry.properties` | Last place the error-report file is looked up. |
| `%LOCALAPPDATA%\TextTools\windows.properties` | Outer x, y, width and height of each window id. A missing id uses the first platform position. |

An AHK folder imported by `LegacyImport` is read, not modified. A missing file or a file that is not valid UTF-8 leaves the table it owns as it was.

| Source | Lands in |
| --- | --- |
| `actions.ini` `actionN.*` | `action`. An empty task is skipped. `-` means no id, key or model. |
| `actions.ini` `quickN.label` and `quickN.instr` | `quick_refine`. `quick.count=0` stores an intentional empty list. |
| `apps.ini` `appN.proc`, `title`, `name`, `desc`, `hint` | `app_profile`. |
| `langs.ini` `langN.code`, `name`, `prompt`, `on` | `output_language`. |
| `TextTools.ini` `api.models_url` or `api.base_url`, plus `api.models` and `api.models_known` | `known_model` for that address. |
| `TextTools.ini` `api.api_key` and `api.proxy` | Returned to the caller. Not written to SQLite. |
| `TextTools_history.txt` | `history`, newest first, at most 20. An empty file clears it. A non-blank unreadable file leaves it. |
| `TextTools_usage.log` | `usage_log`. |
| `lang/user/*.ini` and `TextTools_last.txt` | Copied into the data directory. |
