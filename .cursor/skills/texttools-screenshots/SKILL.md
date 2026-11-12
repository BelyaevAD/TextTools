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

3. Do what `open` says. Do not type a key, a proxy password, or a DSN. If one is visible, clear it or skip the shot.
4. If `gate` is true, create `tmp/screenshot-gate` only after that surface is the one on screen.
5. Wait until the script exits. It prints `captured: <file> title: <title>`. If `match` was empty, write that title into `match`.
6. On a non-zero exit, repeat this surface once. Then stop that surface and report the script line.

Do not save a screen grab as the asset. One look is allowed only to see why a click missed.

## A surface the script did not list

If you had to open a window, page, or tab that has no catalog row, append one row with `source: seen-<id>`, `readme: false`, the file path, the exact title as `match`, and the `open` steps you used. Do not edit this skill.

Leave `readme` as it is. A release may set it true when the README should show that surface.

Return the files written and the catalog rows added. Do not paste the images.
