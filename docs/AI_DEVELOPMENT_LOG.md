# AI-Assisted Development Log

This project was built in a single session with Claude (Anthropic) acting as the
implementing engineer, working autonomously from a written problem statement.
This document records what was actually decided and why, including the things
that went wrong.

## Conversation link

**Full transcript:** [docs/AI_CHAT_TRANSCRIPT.md](AI_CHAT_TRANSCRIPT.md)

https://github.com/mridulbansal2006/terminal-music-player-android/blob/main/docs/AI_CHAT_TRANSCRIPT.md

The work was done in a **Claude Code desktop session**, not a claude.ai chat.
Claude Code sessions have an **Export** function that writes a local archive;
they do not produce a public `claude.ai/share/...` URL. The transcript above is
that export, converted to readable Markdown and committed here so the chat
history has a stable public link.

It contains every human prompt and every Claude reply. Internal reasoning blocks
and verbose tool output are omitted for readability, tool calls are summarised,
and email addresses are redacted. Nothing was added or rewritten after the fact.

## How the work was structured

The build followed the phase order in the brief: inspect, implement, test,
package, document. Work was tracked as a visible task list and the phases ran
roughly in this order:

1. Inspect the environment and install a toolchain
2. Scaffold the Gradle/Kotlin/Compose project
3. Implement the pure logic layer (model, formatting, parser)
4. Implement data (MediaStore, DataStore) and playback (Media3)
5. Build the Compose UI and the five screens
6. Write unit tests
7. Build the APK
8. Verify on an emulator
9. Document and publish

## Starting conditions

The working directory was **empty**, and the machine had **no Java, no Android
SDK, no Gradle and no emulator**. The first real task was provisioning:

| Need | Chosen solution | Why |
| --- | --- | --- |
| JDK 17 | `brew install openjdk@17` | Installs into the Homebrew prefix, so no `sudo` and no system-wide Java change |
| Android SDK | `brew install --cask android-commandlinetools` | Same reason; `sdkmanager` then fetched platform 35 and build-tools 35.0.0 |
| Gradle | Downloaded 8.9 from `services.gradle.org`, used once to generate the project wrapper | Gives a committed, reproducible wrapper so a fresh clone builds without a local Gradle |
| Emulator | `sdkmanager` emulator + `system-images;android-35;google_apis;arm64-v8a` | Needed for genuine runtime verification rather than build-only claims |

## Design decisions and the reasoning behind them

### 1. Make the command parser pure

**Decision.** `CommandParser.parse(input): ParseResult` imports nothing from
Android and causes no side effects. Execution lives separately in
`CommandExecutor`.

**Why.** It was clear from the brief that the command surface was the largest
source of behaviour — 16 commands, each with argument rules and error messages.
Keeping parsing pure means every one of those rules is testable as a plain JVM
unit test. The alternative — a single class that parses and plays — would have
required an emulator to test argument validation.

**Outcome.** 27 of the 29 unit tests exist because of this decision, and they
run in a few seconds. Every argument-validation bug was caught before the app
was installed once.

### 2. Route the on-screen buttons through the command pipeline

**Decision.** Transport buttons call `viewModel.runCommandLine("next")` rather
than calling the player directly.

**Why.** The brief required that commands and buttons control the same playback
state. That can be satisfied by discipline (always remember to update both) or
structurally (make there be only one path). The structural version cannot rot.

**Outcome.** It worked, and produced an unplanned benefit: tapping a button
writes the equivalent command into the terminal, which makes the shared pathway
visible during a demo rather than merely claimed. This is now the centrepiece of
the demo script.

### 3. Put playback in a `MediaSessionService`

**Decision.** `PlaybackService : MediaSessionService` hosting ExoPlayer, with
the UI talking to it through a `MediaController`.

**Why.** The brief separately required background playback, media notifications
and audio focus handling. All three are properties of this one choice. An
Activity-hosted player would have needed a hand-written notification and manual
`AudioManager` focus handling, and would still die with the Activity.

**Outcome.** The app contains zero notification-building code. Verification
confirmed `isForeground=true types=0x2` and a posted transport notification.

### 4. Distinguish three kinds of "no music"

**Decision.** `MusicRepository` returns `LibraryResult.Success`,
`PermissionDenied` or `Failure` rather than just a possibly-empty list.

**Why.** "You denied permission", "there is genuinely no music here" and "the
MediaStore query failed" need completely different messages. Collapsing them
into an empty list would have made the most common first-run failure —
permission denied — indistinguishable from an empty device.

### 5. Keep `Song.uri` a `String`

**Decision.** The model holds the content URI as a `String`, not an
`android.net.Uri`.

**Why.** `android.net.Uri` is stubbed in JVM unit tests and throws when called,
which would have made `Song` — and therefore `resolveSong`, `searchSongs` and
all the formatting helpers — impossible to test off-device.
`MediaItem.Builder().setUri()` accepts a `String`, so nothing is lost.

### 6. History panel instead of arrow-key recall

**Decision.** A `[hist]` dialog listing recent commands, newest first.

**Why.** Android soft keyboards have no Up/Down arrows, so the shell convention
is simply unavailable. The brief explicitly permitted a panel. Commands are
persisted in DataStore as a newline-joined string rather than a `stringSet`,
because a set loses ordering and ordered recall is the entire point.

## Problems hit during development

### Type error in `nowplaying` output

The `nowplaying` builder mixed `TerminalLine` values with bare `String`s inside
one `buildList`, which would not compile against the declared
`List<TerminalLine>`. Caught by inspection before the first build and fixed by
wrapping the two offending lines in `.asOutput()`.

### Risk of unstable Compose APIs

`KeyboardOptions(autoCorrectEnabled = ...)` was written first, then replaced
with the stable `keyboardType = KeyboardType.Ascii`. The newer parameter was
plausibly correct but carried a real chance of a compile failure against the
pinned Compose BOM, and the stable form achieves the same thing for a command
line.

### `stop` followed by `play` did nothing

`Player.stop()` leaves ExoPlayer in `STATE_IDLE`, and `play()` on an idle player
is a no-op. Found while reasoning through the command matrix rather than at
runtime. `PlayerController.play()` now calls `prepare()` first when the state is
`IDLE`. For the same reason `next()` wraps to index 0 at the end of the queue
instead of silently doing nothing.

### A chased non-bug: apparent "ghost" rendering

Emulator screenshots appeared to show duplicated player-bar controls near the
top of the terminal output on a cold launch. This was investigated at some
length: the emulator was restarted on a different GPU backend (`swiftshader` →
`host`), animations were disabled, and the UI hierarchy was dumped to look for a
view at those coordinates.

The hierarchy dump showed **no view** at the suspect coordinates, and decoding
the PNG pixels directly showed that region was **pure black** in the actual
screenshot files. The apparent ghosting was an artifact of the image-viewing
pipeline used to inspect the screenshots, not of the app, the emulator or the
PNGs.

The investigation was not wasted: it surfaced a genuine latent bug (below).
Recorded here because "the tool I used to look at the output was lying to me"
is a real and instructive failure mode.

### A real bug the non-bug investigation uncovered

Each screen was declared as a `ColumnScope` extension and applied
`Modifier.weight(1f)`. But the screens are composed inside the `NavHost`'s
`AnimatedContent`, not directly inside the scaffold's `Column`, and a weight
modifier is only honoured by the `Column` that owns the scope. The weight was
being silently ignored. Fixed by moving `weight(1f)` onto the `NavHost` and
giving the screens `fillMaxSize()`.

No misrendering was observed before or after — it was a latent violation of the
layout contract rather than a visible defect, and the documentation says so
rather than claiming a fix for a problem users could see.

## What AI assistance was good and bad at

**Good:** scaffolding a correct multi-module Gradle/Compose project with
compatible version pins on the first attempt; writing the exhaustive parser
tests; remembering Android version splits like the API 33 permission change
without being prompted; and systematically driving the emulator through `adb` to
produce evidence rather than assertions.

**Bad:** the ghost-rendering investigation, where several rounds were spent on a
visual artifact that measurement showed did not exist in the data. The lesson is
that "look at the screenshot" is not verification — decoding the pixels, which
settled the question in one step, should have come first.

**Required human judgement:** the Claude conversation share link, any GitHub
release decisions, and confirming that a terminal UI is an acceptable answer to
the brief at all.

## Verification honesty

`docs/TEST_REPORT.md` records exactly what was run and observed, and has an
explicit section listing what was **not** verified — audio was never actually
heard, no physical device was used, and only API 35 was exercised. No test
result in this repository is invented.
