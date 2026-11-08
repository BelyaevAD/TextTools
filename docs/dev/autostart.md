# Autostart

Linked from the app page. The test is AutostartTest.

The settings checkbox starts on. Saving it writes or removes `HKCU\Software\Microsoft\Windows\CurrentVersion\Run\TextTools` for an installed exe. A development launch does not touch the registry. The MSI asks before the rest of setup; the box starts checked. A quiet install keeps that default.
