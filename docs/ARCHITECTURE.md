# Architecture

TermTune is an MVVM Android application. One view model owns three
collaborators — a repository, a settings store and a player controller — and
exposes a single immutable state object to Compose.

## Why this shape

The hard requirement behind the design is that *typed commands and on-screen
buttons must never disagree about playback state*. The cheap way to satisfy it
is to give both input styles the same exit path. So:

- Buttons do not call the player directly. They call
  `TerminalViewModel.runCommandLine("next")`, the same function the prompt uses.
- The only object that touches the player is `PlayerController`.
- `PlayerController` publishes `PlayerState`; nothing else caches playback data.

A side effect is that tapping *next* writes `termtune:~$ next` into the
terminal, which makes the shared pathway visible during a demo.

## Layers

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

## Kotlin classes

### `data`

| Class | Responsibility |
| --- | --- |
| `Song` | One track: id, title, artist, album, duration, content URI. The URI is a `String`, not an `android.net.Uri`, so the model is free of framework types and can be used in plain JVM unit tests. |
| `MusicRepository` | Queries `MediaStore.Audio` on `Dispatchers.IO`, filtering `IS_MUSIC != 0` and sorting by title. Returns `LibraryResult.Success`, `PermissionDenied` or `Failure` so the UI can distinguish "you said no" from "there is no music" from "the query broke". Falls back to the file name when a track has no title tag, and normalises MediaStore's `<unknown>` artist sentinel. |
| `MediaPermissions` | The single place that knows Android 13 replaced `READ_EXTERNAL_STORAGE` with `READ_MEDIA_AUDIO` and added `POST_NOTIFICATIONS`. |
| `SettingsStore` | DataStore Preferences wrapper exposing `Flow<AppPreferences>` (theme, history, shuffle, repeat). History appends collapse consecutive duplicates and cap at 40 entries. |

### `terminal`

| Class | Responsibility |
| --- | --- |
| `Command` | Sealed interface — one subtype per command, carrying parsed arguments (`Volume(percent)`, `Seek(seconds)`, `Play(target?)`, …). |
| `ParseResult` | `Empty`, `Parsed`, `Invalid(message, usage)` or `Unknown(name)`. |
| `CommandParser` | Pure function from text to `ParseResult`. No Android imports, no side effects. Handles case-insensitivity, extra whitespace, aliases, `%` suffixes on volume, and `m:ss` timecodes on seek. |
| `CommandExecutor` | Applies a `Command` to the player and settings, returning styled output lines and an optional clear-screen flag. All the user-facing wording lives here. |
| `TerminalLine` / `LineStyle` | Output model. Styles (`COMMAND`, `OUTPUT`, `SUCCESS`, `ERROR`, `DIM`, `HEADER`) map to theme colours in the UI, which is how errors come out red without the executor knowing about `Color`. |
| `TerminalFormat.kt` | Pure helpers: `formatDuration`, `formatIndex`, `resolveSong`, `searchSongs`, `formatProgressBar`. |
| `HelpText` | The single source of the command reference, shared by `help` and the Help screen, plus the boot banner and the empty-library instructions. |

### `player`

| Class | Responsibility |
| --- | --- |
| `PlaybackService` | `MediaSessionService` hosting `ExoPlayer`. |
| `PlayerController` | Connects a `MediaController` to that service and publishes `PlayerState`. |
| `PlayerState` | Immutable snapshot: playing, prepared, current song, position, duration, volume, shuffle, repeat, queue size and index, plus a derived `statusLabel`. |

### `ui`

| Class | Responsibility |
| --- | --- |
| `TerminalViewModel` | Owns the shell: output lines, library, scan state, permission state. Combines its own `ShellState` with `SettingsStore.preferences` and `PlayerController.state` into one `TerminalUiState`. |
| `TermTuneRoot` | Applies the theme, draws the shared frame, hosts the `NavHost`. |
| `TerminalScaffold` | Title bar + tab row, drawn once outside the `NavHost` so section switches do not flash the chrome. |
| `PlayerBar` / `SeekRow` | The compact music-control area, reused on the Now Playing screen. |
| `PromptInput` | Prompt glyph, `BasicTextField`, blinking cursor, `[hist]` and `[run]`. |
| `TerminalOutput` | `LazyColumn` of `TerminalLine`s that auto-scrolls to the newest. |
| `Palette.kt` | Three `TerminalPalette`s, a `CompositionLocal`, and a monospace `Typography` applied to every Material text style. |

## The command parser

Parsing is deliberately separated from execution. `CommandParser.parse` has no
Android dependencies and no side effects, which means the entire command
surface — all 16 commands, every argument rule, every error message — is
testable as plain JVM unit tests with no device or emulator.

```
input ──► trim ──► split on whitespace ──► lowercase head
                                               │
        ┌──────────────────────────────────────┘
        ▼
   when (name) {
     "help", "?", "man"   → Parsed(Help)
     "volume", "vol"      → parseVolume(args)   // range + numeric checks
     "seek"               → parseSeek(args)     // int or m:ss
     "shuffle" / "repeat" → parseToggle(args)   // on/off/true/false/1/0
     "theme"              → parseTheme(args)    // green|amber|blue
     "play"               → Parsed(Play(rest))  // whole remainder, so
     …                                          // multi-word titles survive
     else                 → Unknown(name)
   }
```

Each failure path produces a specific message *and* a usage line, e.g.
`volume: 150 is out of range (0-100)` followed by `usage: volume <0-100>`.
Commands that take no arguments reject extra words rather than ignoring them.

Resolution of `play <target>` is also pure, in `resolveSong`, and tries in
order: 1-based track number, exact case-insensitive title, title substring,
artist substring. Exact title is checked before substring so that `play Money`
picks *Money* over *Money For Nothing*.

## Media3 integration

```
MainActivity ──► TerminalViewModel ──► PlayerController
                                            │ MediaController.Builder(...)
                                            │      .buildAsync().await()
                                            ▼
                                   SessionToken(PlaybackService)
                                            │
                                            ▼
                        PlaybackService : MediaSessionService
                                            │
                                   MediaSession(ExoPlayer)
```

`PlaybackService.onCreate` builds the player with:

- `setAudioAttributes(USAGE_MEDIA / AUDIO_CONTENT_TYPE_MUSIC, handleAudioFocus = true)`
  — Media3 then requests and releases audio focus, ducking or pausing when
  another app plays.
- `setHandleAudioBecomingNoisy(true)` — pause instead of playing out loud when
  headphones are unplugged.

`MediaSession` gives the system media notification for free, which is why there
is no custom notification code. The service is declared with
`foregroundServiceType="mediaPlayback"` and the matching
`androidx.media3.session.MediaSessionService` intent filter.

`onTaskRemoved` calls `stopSelf()` when the app is swiped away while nothing is
playing, so an idle foreground service and its notification do not linger.

### State publication

`PlayerController` registers a `Player.Listener` and republishes on
`onEvents`, which catches every change in one callback. Position does not
generate events, so a 500 ms coroutine ticker republishes while playing — and
only while playing, so a paused app does no work.

Tracks become `MediaItem`s with `mediaId = song.id`, which is how the current
`MediaItem` is mapped back to a `Song` for display. Metadata is attached so the
notification shows title, artist and album.

Two behaviours needed explicit handling:

- **`stop` then `play`.** `Player.stop()` leaves the player `IDLE`. `play()`
  therefore calls `prepare()` first when the state is `IDLE`, otherwise the
  second `play` would do nothing.
- **`next` at the end of the queue.** ExoPlayer has no next item with repeat
  off, so `next()` wraps to index 0 rather than silently doing nothing.

## Local storage

DataStore Preferences, one file, four keys:

| Key | Type | Purpose |
| --- | --- | --- |
| `terminal_theme` | String | `green` / `amber` / `blue`, restored at launch |
| `command_history` | String | Newline-joined, newest last, capped at 40 |
| `shuffle_enabled` | Boolean | Re-applied to the player after it connects |
| `repeat_enabled` | Boolean | Re-applied to the player after it connects |

History is stored as a joined string rather than a `stringSet` because a set
loses ordering, and command history is only useful in order. Commands are
single-line, so the newline separator is unambiguous.

On startup the view model awaits the first `AppPreferences`, then pushes the
stored shuffle and repeat modes into the freshly connected controller — the
player is the runtime authority, DataStore is the durable record.
