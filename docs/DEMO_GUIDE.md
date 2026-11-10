# Demo Guide

A five-minute demonstration script for TermTune, plus the questions a viva
examiner is most likely to ask and honest answers to them.

## Before you start

1. Install the APK on a device or emulator.
2. Make sure there is music on it. On an emulator:
   ```bash
   ./tools/make-test-audio.sh build/test-audio
   adb push build/test-audio/. /sdcard/Music/
   adb shell "for f in /sdcard/Music/*.mp3; do am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://\$f; done"
   ```
3. Launch TermTune once and grant both permission prompts, so the demo does not
   open on a dialog.
4. Turn the device volume up — the point of the demo is that sound comes out.
5. Optional: run `clear` so you start on a clean screen.

---

## The five-minute script

### 0:00 — What it is (20 seconds)

> "This is TermTune. It's a real Android music player, but the entire interface
> is a Linux terminal. You type commands to control playback. Underneath it's a
> normal Android app — MediaStore for the library, Media3 ExoPlayer for audio,
> MVVM, Jetpack Compose."

Point at the screen: the banner, the `$` prompt with its blinking cursor, the
player bar at the bottom.

### 0:20 — Discoverability (30 seconds)

Type:

```
help
```

> "Every command is documented in the app itself. There's also a Help screen
> with the same reference, so you never have to guess the verbs."

Scroll the output briefly. Don't read the list out loud.

### 0:50 — The library (40 seconds)

```
list
```

> "This is MediaStore — these are the actual audio files on the device, with
> title, artist and duration read from their tags. Each one gets a number."

### 1:30 — Playing something (50 seconds)

```
play 2
```

**Let the sound be heard.** Pause talking for two seconds.

> "That's real playback through ExoPlayer. Notice three things happened at once:
> the status in the title bar changed to `playing`, the player bar picked up the
> track and started its progress bar, and the play button became a pause button."

Then:

```
nowplaying
```

> "The `nowplaying` command gives the full state — position, an ASCII progress
> meter, shuffle, repeat, volume, and the queue position."

### 2:20 — Commands and buttons are one system (40 seconds)

This is the most important 40 seconds of the demo.

**Tap the next button** on the player bar — do not type anything.

> "Watch the terminal."

The terminal prints `termtune:~$ next` and the result.

> "The buttons don't have their own code path. Tapping *next* literally runs the
> `next` command. That's how the project guarantees the typed interface and the
> touch interface can never disagree about playback state — there's only one
> path to the player."

### 3:00 — The rest of the command surface (50 seconds)

Run these back to back, narrating lightly:

```
volume 50
seek 10
shuffle on
search daemons
```

> "Volume, seeking, shuffle and repeat all work. Search matches title, artist or
> album, and the results keep their library numbers, so you can search and then
> `play` the number you see."

### 3:50 — Error handling (35 seconds)

```
volume 150
sudo rm -rf /
```

> "A shell that silently ignores bad input is a bad shell. Every error gives you
> the specific problem and the correct usage. Unknown commands are reported by
> name."

### 4:25 — Themes and persistence (20 seconds)

```
theme amber
```

> "Three themes. The theme, the command history and the shuffle and repeat modes
> are all persisted in DataStore, so they survive a restart."

### 4:45 — Background playback (15 seconds)

Press the **home** button. Pull down the notification shade.

> "Playback continues in the background with a system media notification and
> proper audio focus handling, because the player lives in a Media3
> `MediaSessionService`, not in the Activity."

Return to the app to finish.

---

## If something goes wrong mid-demo

| Symptom | Say this, then do this |
| --- | --- |
| `list` shows an empty library | "No audio on this device — here's the empty state, which tells you exactly how to load some." Then switch to a device that has music. |
| A command says the service isn't connected | "It connects asynchronously on launch." Wait two seconds, run it again. |
| No sound | Check the device volume and that it isn't routed to a disconnected Bluetooth device. The progress bar advancing proves playback is running. |
| Keyboard covers something | It shouldn't — that's `imePadding`. Tap outside the field to dismiss it. |

---

## Likely viva questions

**Q: Why a terminal? Isn't this just a gimmick?**

The brief asked for one, but it's defensible on its own: a command line is
faster than menu navigation once you know the verbs, it's precise (`seek 90` is
exact in a way that dragging a bar isn't), and it fails loudly instead of
silently. The cost is discoverability, which is why there's a `help` command, a
Help screen, and a full set of tappable controls for everything the commands do.

**Q: Walk me through what happens when I type `play 3`.**

`TerminalViewModel.runCommandLine` echoes the line, pushes it to history, then
calls `CommandParser.parse`. The parser returns `Parsed(Command.Play("3"))`.
`CommandExecutor` resolves `"3"` through `resolveSong`, which tries a track
number first, so it returns index 2. The executor calls
`PlayerController.setQueue(library, 2, playWhenReady = true)`, which maps each
`Song` to a `MediaItem` and hands them to the `MediaController` connected to
`PlaybackService`. The service's ExoPlayer prepares and plays. `PlayerController`
republishes `PlayerState` from the player's `onEvents` callback, the view model
combines it into `TerminalUiState`, and Compose recomposes the player bar.

**Q: Why is the parser a separate class from the executor?**

So it can be pure. `CommandParser` imports nothing from Android and has no side
effects, which means all 16 commands, every argument rule and every error
message are testable as plain JVM unit tests that run in seconds. If parsing and
execution were one class, testing argument validation would need an emulator.

**Q: How do you guarantee the buttons and the commands agree?**

Structurally, not by discipline. `PlayerController` is the only class that
touches the player, and it publishes one `PlayerState` that nothing else caches.
The buttons don't call it — they call `runCommandLine("next")`, the same
function the prompt calls. So a tap and a typed command take the identical path.

**Q: Why `MediaSessionService` instead of just an ExoPlayer in the Activity?**

Three requirements come for free: background playback, the system media
notification, and audio focus. An Activity-hosted player dies with the Activity,
needs a hand-written notification, and needs manual `AudioManager` focus
handling. The service costs about forty lines and replaces all three.

**Q: How do you handle the Android 13 permission change?**

Android 13 replaced `READ_EXTERNAL_STORAGE` with the scoped `READ_MEDIA_AUDIO`
and added a runtime `POST_NOTIFICATIONS` permission. `MediaPermissions` is the
only class that knows this; it returns the right permission for the running SDK.
The manifest bounds the legacy permission with `maxSdkVersion="32"` so modern
devices are never asked for broad storage access. After the dialog closes the app
re-reads the actual grant state rather than trusting the result map, because that
map omits permissions that were already granted.

**Q: What happens if there's no music on the device?**

It's a distinct state from "permission denied" and from "the query failed" —
`MusicRepository` returns `LibraryResult.Success(emptyList())`,
`PermissionDenied` or `Failure` respectively. For an empty library, `list` prints
the `adb push` and media-scanner commands needed to load test audio. Nothing
crashes.

**Q: Why is `Song.uri` a String instead of a `Uri`?**

So the model has no Android framework types. `android.net.Uri` is stubbed in
JVM unit tests and throws when called, which would make `Song` untestable off
device. `MediaItem.Builder().setUri()` accepts a String, so nothing is lost.

**Q: `stop` then `play` — why did that need special handling?**

`Player.stop()` leaves ExoPlayer in `STATE_IDLE`. Calling `play()` on an idle
player does nothing. So `PlayerController.play()` checks for `STATE_IDLE` and
calls `prepare()` first. Without it, `stop` followed by `play` silently fails.

**Q: What would you do next?**

Tab completion for track names, because that removes most of the discoverability
cost of a command interface. After that, playlists expressed as commands
(`save`, `load`, `queue`) and Compose UI tests, which is the main gap in the
current test suite — the logic is well covered, the UI layer is verified
manually and by scripted emulator runs rather than automated tests.

**Q: What's the weakest part of the project?**

The UI layer has no automated tests. The command logic has 29 unit tests and the
runtime behaviour was verified on an emulator with recorded evidence, but there
are no Compose UI tests, so a regression in, say, the history panel would not be
caught by CI. That's the first thing I'd add.
