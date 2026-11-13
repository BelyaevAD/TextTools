---
name: texttools-screenshots
description: Capture TextTools screenshots. Use when the README, changelog, or release needs pictures of the app. The parent agent does not capture the screen.
---

# TextTools screenshots

The parent agent does not call the computer and does not read the PNG back. It starts one subagent and gives it this file. The subagent may click and type. `packaging/screenshots/capture-foreground.ps1` saves the window and blurs the outer 5 pixels.

## Before any capture

From the repository root:

```text
powershell -NoProfile -File packaging/screenshots/sync-catalog.ps1
```

That script is what learns a new window, settings tab, or editor page. A window is a function that opens one: it calls `Window(` or another window function. An alert dialog is not a shot. A private function that only draws a window already opened by another function in the same file is not a second shot. Settings and the action editor are one shot per tab or page, not one shot of the shell. Last result is the `ResultWindow` shot. A settings tab is a `settings_tab_` string. An editor page is a function whose name ends with `Page` under `app/ui/editor`. Do not add those names to this skill.

## One surface

Read `docs/screenshots/catalog.yaml`. Skip a surface whose file already exists unless this run is a release refresh or the user asked for new shots. For each surface you take:

1. If `gate` is true, delete `tmp/screenshot-gate`.
2. Start the capture in the background, from the repository root. Pass `-Gate tmp/screenshot-gate` only when `gate` is true. Pass `-Popup` and omit `-Match` when `popup` is true. `-Match` is the exact window title.

```text
powershell -NoProfile -File packaging/screenshots/capture-foreground.ps1 -Out <file> -Match "<match>" -Wait 25
```

3. Do what `open` says. An API key that shows only asterisks or dots may stay in the picture. If any character of the key is readable, clear that field for the shot and do not save. A proxy password and a DSN must not be visible. The hint under an empty proxy field may stay.
4. If `gate` is true, create `tmp/screenshot-gate` only after that surface is the one on screen.
5. Wait until the script exits. It prints `captured: <file> title: <title>`. If `match` was empty, write that title into `match`.
6. On a non-zero exit, repeat this surface once. Then stop that surface and report the script line.

Do not save a screen grab as the asset. One look is allowed only to see why a click missed.

## A surface the script did not list

If you had to open a window, page, or tab that has no catalog row, append one row with `source: seen-<id>`, the file path, the exact title as `match`, and the `open` steps you used. Do not edit this skill.

A window, editor page, or settings tab that a person uses to do the work gets `readme: true` in the release that adds it. Add its picture and one sentence to the README, and add the English and Russian pictures to both GitHub Pages pages, each page in its own language. An alert stays `readme: false` and stays off the README and the site.

Return the files written and the catalog rows added. Do not paste the images.

## Two languages

The old script saved one folder per interface language, `docs/assets/screenshots/en/` and `docs/assets/screenshots/ru/`, with the same file names. Keep that. The catalog `file` path is the English shot. The Russian shot is that path with `/en/` replaced by `/ru/`.

For a release refresh of every `readme: true` surface, capture the English pass first with the interface set to English, then the Russian pass with the interface set to Russian. Use `docs/screenshots/sample.txt` in both. After the last shot, put the user's interface language back.

The GitHub Pages commit copies both folders into `assets/screenshots/`. `index.html` (Russian) links `assets/screenshots/ru/`. `en/index.html` links `assets/screenshots/en/`. Do not leave the Russian page on the English pictures.

## Sample text

`docs/screenshots/sample.txt` is the only body text for a shot. Copy it exactly into the selection or the result window. It is three short paragraphs about improving selected text with TextTools. Do not rewrite it, shorten it, or replace it with a new model answer for a release. If showing it replaces the user's last result, put the previous result back after the shot.
