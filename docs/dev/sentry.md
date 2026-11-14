# Error reports

Linked from the app page. The test is SentrySetupTest.

The switch starts off. `config/sentry.properties` is read beside the working directory, then beside its parent, then from `%APPDATA%\TextTools\sentry.properties`. `:app:run` uses the app module as the working directory, so the repo file is the parent one. With the error switch on, a crash and a failed model call, settings call, settings save, or history write are sent. A chat completion that receives no response bytes for 180 seconds is one of those failed model calls. Bearer tokens and proxy passwords are removed from the text. No prompt, response, window title, executable name, URL or credential is sent. A disabled switch or blank DSN sends nothing. Local usage statistics are recorded regardless.
