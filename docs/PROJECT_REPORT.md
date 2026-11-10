# TermTune — Terminal Music Player
### Project Report

**Course:** Application Development
**Platform:** Android
**Language:** Kotlin
**Project type:** In-class application development project

---

## 1. Abstract

TermTune is an Android music player whose entire interface is a simulated Linux
terminal. Instead of tapping through menus, the user types commands — `list`,
`play 3`, `volume 50`, `seek 30`, `shuffle on` — at a `termtune:~$` prompt, and
real local audio plays back through AndroidX Media3 ExoPlayer.

The project demonstrates that an unconventional interaction model can sit on
top of a conventionally correct Android architecture. Under the terminal
aesthetic the app is an ordinary MVVM application: MediaStore for local audio
discovery, a `MediaSessionService` for background playback and system media
notifications, DataStore for persistence, and Jetpack Compose for the UI. A
pure, dependency-free command parser sits between the two, which is what makes
the command surface fully unit-testable without a device.

The deliverable is an installable debug APK, 29 passing JVM unit tests, and
runtime verification on an Android 15 emulator covering library discovery,
playback, seeking, mode toggles, background playback and error handling.

## 2. Introduction

Music players on mobile converged on one design a decade ago: a grid of album
art, a bottom bar, a swipe-up now-playing sheet. The design is good, and it is
everywhere, which makes it a poor project brief — there is nothing to decide.

A terminal is the opposite interaction model. It is textual, it is modal only in
the sense that you must know the verbs, and it fails loudly rather than
silently. It is also familiar to the audience for this project. Building a music
player as a shell raises concrete engineering questions that a conventional
player does not:

- How do you parse free-text commands reliably, and report errors usefully?
- If the same action is reachable by typing `next` and by tapping a button, how
  do you guarantee the two never disagree?
- How do you keep a command line usable on a device whose keyboard has no arrow
  keys and covers half the screen?

TermTune answers those three questions, and ships a working player while doing
it.

## 3. Problem statement

> Build a fully functional Android music player that looks and feels like a
> Linux terminal while offering a modern music-listening experience. It must be
> a real, installable Android application — not a web demo, a static mockup or a
> command-line program — with:
>
> - a terminal-inspired interface: dark background, monospace type, accent
>   colour, prompt, blinking cursor, scrollable output;
> - a working command parser supporting `help`, `list`, `play`, `pause`,
>   `resume`, `next`, `prev`, `stop`, `nowplaying`, `volume`, `seek`, `shuffle`,
>   `repeat`, `search`, `clear` and `theme`;
> - genuine local audio playback with runtime permissions, seek, shuffle,
>   repeat, background playback, media notifications and audio focus;
> - a local music library with title, artist, duration, search and selection by
>   number or name;
> - a hybrid interface where typed commands and on-screen controls drive the
>   same playback state;
> - command history and persistent settings;
> - accessible, reliable behaviour including sensible empty states.

## 4. Objectives

| # | Objective | Priority |
| --- | --- | --- |
| O1 | A Kotlin/Compose Android project that compiles to an installable APK | P0 |
| O2 | A command parser covering all 16 required commands with useful errors | P0 |
| O3 | Local audio discovery through MediaStore with correct runtime permissions | P0 |
| O4 | Real playback: play, pause, resume, next, previous, stop | P0 |
| O5 | A terminal interface that is genuinely usable on a phone | P0 |
| O6 | Seek, shuffle, repeat, volume and live progress | P1 |
| O7 | Background playback, media notification, audio focus | P1 |
| O8 | Search, persistent command history, three themes | P1 |
| O9 | Five screens with navigation that preserves the aesthetic | P1 |
| O10 | Accessibility labels, keyboard handling, empty and error states | P1 |
| O11 | Automated tests and reproducible verification | P1 |

## 5. Proposed solution

The design rests on three decisions.

**A pure parser.** `CommandParser.parse(input): ParseResult` imports nothing
from Android and causes no side effects. It returns `Parsed(command)`,
`Invalid(message, usage)`, `Unknown(name)` or `Empty`. Because it is pure, every
command, every argument rule and every error message is testable as a plain JVM
unit test. Execution — the part that needs a player — lives separately in
`CommandExecutor`.

**One path to the player.** The requirement that commands and buttons agree is
met structurally rather than by discipline. `PlayerController` is the only class
that touches the player, and it publishes a single `PlayerState`. On-screen
buttons do not call it; they call `runCommandLine("next")`, the same view model
function the prompt calls. A tap therefore takes the identical route as typing,
and also appears in the terminal log — which makes the shared pathway visible
during a demo rather than merely asserted.

**A service, not an in-activity player.** Playback lives in a
`MediaSessionService`. That single choice delivers background playback, the
system media notification and audio focus handling together, instead of three
separate pieces of hand-written plumbing.

Around those: MediaStore for discovery, DataStore for theme/history/mode
persistence, Navigation Compose for five screens inside one shared terminal
frame.

## 6. Requirements

### 6.1 Functional

| ID | Requirement |
| --- | --- |
| F1 | Discover local audio files and present title, artist, album and duration |
| F2 | Request the correct audio permission for the running Android version |
| F3 | Parse and execute all 16 documented commands |
| F4 | Report invalid commands and bad arguments with a specific message and usage |
| F5 | Play, pause, resume, stop, next and previous |
| F6 | Seek to a position; set volume 0–100 |
| F7 | Toggle shuffle and repeat |
| F8 | Search the library by title, artist or album |
| F9 | Select a track by number or by name, exact or partial |
| F10 | Show live playback progress, track title and artist |
| F11 | Continue playback in the background with a media notification |
| F12 | Keep typed commands and on-screen controls in one shared state |
| F13 | Persist command history, theme and playback modes across launches |
| F14 | Clear terminal output; clear stored history |
| F15 | Offer three terminal themes |
| F16 | Show distinct empty states for denied permission, failed scan and no music |

### 6.2 Non-functional

| ID | Requirement |
| --- | --- |
| N1 | Kotlin, Jetpack Compose, MVVM, standard AndroidX libraries |
| N2 | Android 7.0 (API 24) and newer |
| N3 | Monospace type and a dark terminal palette throughout |
| N4 | Soft keyboard must never cover the prompt |
| N5 | Accessibility labels on all interactive controls |
| N6 | No crash when the library is empty or the service is not yet connected |
| N7 | Request no permission the app does not use |
| N8 | A fresh clone must build with `./gradlew assembleDebug` |

### 6.3 Development environment

| Component | Version |
| --- | --- |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| JDK | OpenJDK 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Compose BOM | 2024.10.01 |
| Media3 | 1.4.1 |
| DataStore Preferences | 1.1.1 |
| Navigation Compose | 2.8.4 |

## 7. System architecture

```
            ┌──────────────────────────────────────────┐
            │  Compose UI                              │
            │  TermTuneRoot → 5 screens + components   │
            └───────────────┬──────────────────────────┘
                            │ TerminalUiState (StateFlow)
            ┌───────────────┴──────────────────────────┐
            │  TerminalViewModel                       │
            │  parse → execute → append output         │
            └───┬───────────────┬──────────────────┬───┘
                │               │                  │
      ┌─────────┴────┐  ┌───────┴────────┐  ┌──────┴────────┐
      │ CommandParser│  │ MusicRepository│  │ SettingsStore │
      │ CommandExec  │  │  (MediaStore)  │  │  (DataStore)  │
      └─────────┬────┘  └────────────────┘  └───────────────┘
                │
      ┌─────────┴────────┐        ┌──────────────────────────┐
      │ PlayerController │◄──────►│ PlaybackService          │
      │ (MediaController)│ session│ MediaSessionService +    │
      └──────────────────┘        │ ExoPlayer                │
                                  └──────────────────────────┘
```

State flows one way. `TerminalViewModel` combines its own shell state (output
lines, library, scan and permission status) with `SettingsStore.preferences` and
`PlayerController.state` into one `TerminalUiState`, which Compose observes with
`collectAsStateWithLifecycle`. Nothing else holds playback state, so there is no
cache to go stale.

`docs/ARCHITECTURE.md` describes each class, the parser, the Media3 wiring and
the DataStore schema in detail.

## 8. Implementation

### 8.1 Command parsing

Input is trimmed, split on whitespace, and dispatched on a lowercased head
token, so `HELP`, `  List  ` and `Volume    80` all work. For `play` and
`search` the whole remainder is kept, so multi-word titles survive.

Argument validation is explicit per command. `volume` accepts `0`–`100` with an
optional `%`; `seek` accepts whole seconds or an `m:ss` / `h:mm:ss` timecode;
`shuffle` and `repeat` accept `on/off/true/false/1/0/yes/no`; `theme` accepts
the three theme keys. Commands that take no arguments reject extra words rather
than ignoring them, which catches typos like `pause now`.

Every failure carries a message and a usage line:

```
termtune:~$ volume 150
volume: 150 is out of range (0-100)
usage: volume <0-100>
```

### 8.2 Track resolution

`resolveSong` is a pure function used by both the `play` command and the library
screen. It tries, in order: 1-based track number, exact case-insensitive title,
title substring, artist substring. Exact title is attempted before substring so
that `play Money` selects *Money* rather than *Money For Nothing* — a case the
test suite pins down.

### 8.3 Playback

`PlaybackService` builds `ExoPlayer` with `handleAudioFocus = true` and
`setHandleAudioBecomingNoisy(true)`, then wraps it in a `MediaSession`. Media3
supplies the notification, so the app contains no notification-building code.
The service is declared `foregroundServiceType="mediaPlayback"` with the
`MediaSessionService` intent filter, and `onTaskRemoved` stops an idle service
so no orphan notification survives a swipe-away.

`PlayerController` connects a `MediaController` to the session and republishes
state on `Player.Listener.onEvents`. Position changes raise no events, so a
500 ms coroutine ticker republishes while playing — and only while playing.

Two behaviours needed explicit handling. `Player.stop()` leaves the player
`IDLE`, so `play()` re-`prepare()`s when the state is `IDLE`; without this the
command sequence `stop` then `play` would do nothing. And at the end of the
queue with repeat off, ExoPlayer has no next item, so `next()` wraps to index 0
rather than silently no-op'ing.

### 8.4 Permissions

Android 13 split media access. `MediaPermissions` is the only class that knows
this: it exposes `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE`
below, requests `POST_NOTIFICATIONS` alongside on 33+, and the manifest bounds
the legacy permission with `maxSdkVersion="32"` so no unnecessary permission is
requested on modern devices. After the dialog closes the app re-reads the real
grant state rather than trusting the result map, which omits entries that were
already granted.

### 8.5 Mobile terminal usability

Three concrete accommodations:

- **The keyboard.** `android:windowSoftInputMode="adjustResize"`,
  `enableEdgeToEdge()`, and `imePadding()` on the prompt column. The prompt rides
  above the keyboard; output scrolls behind it.
- **No arrow keys.** Soft keyboards have no Up/Down, so history recall is a
  `[hist]` panel listing recent commands newest-first; tapping one loads it into
  the prompt for editing.
- **Narrow screens.** The boot banner is 32 columns so it never wraps. Track
  listings use two short lines per entry rather than one wide row.

### 8.6 Terminal presentation

`TerminalLine` carries a `LineStyle` — `COMMAND`, `OUTPUT`, `SUCCESS`, `ERROR`,
`DIM`, `HEADER` — which the UI maps to palette colours. The executor decides
*what kind* of line it is emitting and never mentions a colour, so switching
theme recolours all existing output with no re-rendering logic. Three palettes
(green, amber, blue) are provided through a `CompositionLocal`, and the
`Typography` sets `FontFamily.Monospace` on every Material text style.

## 9. Testing

Testing was done at two levels: automated JVM unit tests for the logic, and
scripted runtime verification on an emulator for the Android behaviours that
unit tests cannot reach.

### 9.1 Automated unit tests

29 tests, all passing, run with `./gradlew testDebugUnitTest`:

| Suite | Tests | Covers |
| --- | --- | --- |
| `CommandParserTest` | 16 | Every documented command; case and whitespace tolerance; aliases; multi-word `play` and `search`; unknown commands; `volume` range/numeric/arity errors and `%` suffix; `seek` validation and timecodes; toggle and theme errors; no-argument commands rejecting extra words; and a test asserting every example in the built-in help actually parses |
| `TerminalFormatTest` | 11 | Duration formatting including zero, negative, sub-minute and hour-long; index padding; row formatting; resolution by number, exact title, partial title and artist; exact-beats-substring precedence; search across title/artist/album and case-insensitivity; progress-bar clamping and divide-by-zero on unknown duration |
| `PlayerStateTest` | 2 | The `connecting → idle → playing → paused → stopped` status lifecycle and queue emptiness |

The "every help example parses" test is the useful one: it makes the
documentation and the parser impossible to drift apart silently.

### 9.2 Runtime verification

Verified on an Android 15 (API 35, arm64) emulator with four generated tagged
MP3s. `docs/TEST_REPORT.md` records the exact commands, the observed output and
the results, including what was *not* verified.

### 9.3 Build verification

| Check | Result |
| --- | --- |
| `./gradlew assembleDebug` | Succeeds |
| `./gradlew testDebugUnitTest` | 29 tests, 0 failures |
| APK produced | `app/build/outputs/apk/debug/app-debug.apk` |
| Manifest audit | 4 permissions, all used |

## 10. Results

All P0 objectives and all P1 objectives were met: the project compiles to an
installable APK, all 16 commands work, local audio is discovered and played,
seek/shuffle/repeat/volume function, playback survives backgrounding with a
media notification, search and persistent history and three themes work, and the
five screens are navigable without leaving the terminal aesthetic.

The design decision that paid off most was the pure parser. Having
`CommandParser` free of Android types meant the command surface could be tested
exhaustively in seconds on the JVM, and every argument-validation bug was caught
before the app was ever installed.

The decision that paid off second was routing on-screen buttons through
`runCommandLine`. It made the "commands and buttons share one state" requirement
structurally true rather than a thing to remember, and it produced an unplanned
demo benefit: tapping *next* writes `termtune:~$ next` into the terminal, so an
audience can see that the two input styles are one system.

See `docs/TEST_REPORT.md` for the measured runtime results and the explicit list
of behaviours that remain unverified.

## 11. Conclusion

A terminal is a legitimate interface for a music player on a phone, provided the
mobile realities are taken seriously — the keyboard that covers the screen, the
absence of arrow keys, the 40-column width. The cost is discoverability, which
is why the app ships a `help` command, a dedicated Help screen and a tappable
player bar for everything the commands do.

Architecturally the project is a demonstration that an unusual presentation
layer does not require unusual engineering underneath. The terminal is a view;
MediaStore, Media3, DataStore and MVVM are unchanged beneath it. The two
non-obvious pieces of design — making the parser pure, and giving buttons and
commands a single exit path — were both chosen to make correctness cheap to
verify rather than to make the code clever.

## 12. Future scope

- **Album art and a visualiser.** An ASCII-art spectrum analyser driven by an
  audio-session visualiser would suit the aesthetic better than cover images.
- **Playlists as files.** `save <name>`, `load <name>`, `queue <n>` — a playlist
  model expressed as shell commands, persisted as text.
- **Tab completion.** Completing track names at the prompt would remove most of
  the discoverability cost of a command interface.
- **Piping and chaining.** `search queen | play 1`, or `;`-separated commands,
  would extend the metaphor meaningfully rather than decoratively.
- **A real scripting hook.** A startup `.termtunerc` of commands would let users
  set theme, volume and shuffle on launch.
- **Instrumented UI tests.** Compose UI tests over the prompt and transport
  controls would close the gap the current test suite leaves at the UI layer.
- **Media3 session extras.** Exposing commands over the media session would let
  Wear OS, Android Auto and assistant integrations reuse the same playback core.
