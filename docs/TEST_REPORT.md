# Test Report

Two levels of verification: automated JVM unit tests for the logic, and a
scripted run on an Android emulator for the behaviours unit tests cannot reach.
Everything below was observed, not assumed. Section 4 lists what was **not**
verified.

## 1. Environment

| Item | Value |
| --- | --- |
| Host | macOS (Darwin 25.6.0), Apple Silicon (arm64) |
| JDK | OpenJDK 17.0.21 |
| Gradle / AGP | 8.9 / 8.7.3 |
| Android SDK | platform 35, build-tools 35.0.0 |
| Emulator | Pixel 6 AVD, Android 15 (API 35), google_apis, arm64-v8a, 1080x2400 |
| Test audio | 4 generated MP3s with title/artist/album tags (12s, 18s, 25s, 31s) |

## 2. Build and automated tests

| Check | Command | Result |
| --- | --- | --- |
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | **Pass** |
| Unit tests | `./gradlew :app:testDebugUnitTest` | **Pass** — 29 tests, 0 failures, 0 errors, 0 skipped |
| Debug APK | `./gradlew assembleDebug` | **Pass** |

APK: `app/build/outputs/apk/debug/app-debug.apk` — 21098460 bytes (20.1 MB),
package `com.termtune.player`, versionCode 1, minSdk 24, targetSdk 35.

### Unit test breakdown

| Suite | Tests | Failures |
| --- | --- | --- |
| `CommandParserTest` | 16 | 0 |
| `TerminalFormatTest` | 11 | 0 |
| `PlayerStateTest` | 2 | 0 |

## 3. Runtime verification on the emulator

Driven with `adb` (`input tap` / `input text`), with results read back from the
app's own UI via `uiautomator dump`, from `dumpsys media_session`, and from
`dumpsys activity services`.

### 3.1 Permissions

| Case | Observed | Result |
| --- | --- | --- |
| First launch with no permissions | System dialog "Allow TermTune to send you notifications?" shown, then "Allow TermTune to access music and audio on this device?" | **Pass** |
| After granting both | `dumpsys package` reports `READ_MEDIA_AUDIO: granted=true` and `POST_NOTIFICATIONS: granted=true` | **Pass** |
| App reaction to the grant | Terminal printed `permission granted: audio access ok` then `mediastore scan: 4 track(s) found` | **Pass** |
| Manifest audit | 4 permissions declared, all used; `READ_EXTERNAL_STORAGE` correctly bounded by `maxSdkVersion="32"` | **Pass** |

### 3.2 Library discovery

| Case | Observed | Result |
| --- | --- | --- |
| MediaStore contents | `content query` returned the 4 pushed tracks with correct tags | **Pass** |
| `list` | Printed all 4 with correct titles, artists and durations (0:25, 0:12, 0:18, 0:31) | **Pass** |
| Sort order | Alphabetical by title: Fork Bomb Anthem, Kernel Panic Blues, Null Pointer Waltz, Quiet Exit Status | **Pass** |
| Library screen | Same 4 tracks, current track row highlighted | **Pass** |

### 3.3 Playback

| Case | Observed | Result |
| --- | --- | --- |
| `play 2` | `dumpsys media_session`: `state=PLAYING(3)`, metadata `Kernel Panic Blues, The Daemons, Segfault Sessions` — the 2nd track in title order | **Pass** |
| Progress advances | Seek bar and `nowplaying` position both increased over time | **Pass** |
| Auto-advance | After the 12s track ended, playback continued into the next queue entry unprompted | **Pass** |
| `pause` | `|| paused: Quiet Exit Status`; play button became a pause button | **Pass** |
| `resume` | `> resumed: Quiet Exit Status` | **Pass** |
| `next` / `prev` | Track changed, terminal reported the new title | **Pass** |
| `stop` | `[] playback stopped` | **Pass** |
| `volume 50` | `volume set to 50%` | **Pass** |
| `seek 10` | `seeked to 0:10` | **Pass** |
| `shuffle on` / `repeat on` | `shuffle on` / `repeat on`; `nowplaying` and the Settings screen both reflected the change | **Pass** |
| `nowplaying` | Full block: track, artist, album, `0:01 / 0:31`, ASCII meter, status, shuffle/repeat, volume, queue 4/4 | **Pass** |
| `search daemons` | `2 match(es) for 'daemons'` — matched both tracks by artist, keeping library numbers | **Pass** |

### 3.4 Background playback and notification

| Case | Observed | Result |
| --- | --- | --- |
| Press Home while playing | `dumpsys activity services`: `isForeground=true foregroundId=1001 types=0x00000002` (FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) | **Pass** |
| Notification posted | `NotificationRecord ... pkg=com.termtune.player`, `category=transport`, `actions=3`, `android.title=Quiet Exit Status` | **Pass** |
| Audio keeps playing | Position advanced 21144 ms → 24335 ms across a 3 s sample while the app was backgrounded | **Pass** |

### 3.5 Commands and buttons share one state

| Case | Observed | Result |
| --- | --- | --- |
| Tap *next* button | Terminal printed `termtune:~$ next` then `>> next: Fork Bomb Anthem` | **Pass** |
| Tap *play/pause* while playing | Terminal printed `termtune:~$ pause` then `|| paused: Fork Bomb Anthem` | **Pass** |
| Tap *shuffle* while shuffle on | Terminal printed `termtune:~$ shuffle off` then `shuffle off` | **Pass** |
| Button taps excluded from history | History panel contained only typed commands | **Pass** |

### 3.6 Error handling

| Input | Output | Result |
| --- | --- | --- |
| `volume 150` | `volume: 150 is out of range (0-100)` + `usage: volume <0-100>` | **Pass** |
| `sudo rm -rf /` | `termtune: command not found: sudo` + `Type 'help' for the list of commands.` | **Pass** |
| `play nonexistent` | `play: no song matching 'nonexistent'` + `Run 'list' for track numbers or 'search <text>'.` | **Pass** |
| `seek abc` | `seek: 'abc' is not a valid position` + usage line | **Pass** |
| `theme pink` | `theme: unknown theme 'pink'` + `usage: theme green|amber|blue` | **Pass** |
| Errors are visually distinct | Rendered in the palette's red, usage hints in dim grey | **Pass** |

### 3.7 Persistence and lifecycle

| Case | Observed | Result |
| --- | --- | --- |
| Theme persists | Set `theme amber`, `am force-stop`, relaunch → still amber | **Pass** |
| Shuffle/repeat persist | Both restored as on after the same restart | **Pass** |
| History persists | After restart, the history panel still listed commands from before it (25 entries) | **Pass** |
| History ordering | Newest first | **Pass** |
| Rescan on launch | `mediastore scan: 4 track(s) found` printed on every cold start | **Pass** |
| No crash | `logcat` showed no `AndroidRuntime` exceptions across the whole session | **Pass** |

### 3.8 UI and accessibility

| Case | Observed | Result |
| --- | --- | --- |
| Soft keyboard does not cover the prompt | With the IME open, the player bar and prompt both remained visible above it | **Pass** |
| All five screens reachable | `[term] [lib] [now] [help] [cfg]` all navigate correctly | **Pass** |
| Three themes render | green, amber and blue each recolour the whole UI including past output | **Pass** |
| Blinking cursor | Present at the prompt and at the end of terminal output | **Pass** |
| Accessibility labels | `uiautomator dump` shows `content-desc` on transport buttons, sliders, tabs and the command field | **Pass** |

## 4. Not verified

These are stated plainly rather than assumed to work.

- **Audio was never heard.** Playback was verified through `dumpsys media_session`
  state and advancing positions, not by listening. The emulator ran headless.
- **No physical device.** Everything runs on an emulator; real-device behaviour
  (actual audio routing, vendor skins, hardware media keys) is untested.
- **Only Android 15 (API 35).** `minSdk` is 24, but no API 24–34 device or
  emulator was tested, so the `READ_EXTERNAL_STORAGE` path for Android ≤12 is
  exercised only by code review, not by running it.
- **Audio focus interruptions.** `handleAudioFocus = true` is configured and
  Media3 implements it, but no competing-audio or incoming-call scenario was
  staged.
- **Headphone unplug.** `setHandleAudioBecomingNoisy(true)` is set but
  ACTION_AUDIO_BECOMING_NOISY was not simulated.
- **Notification transport buttons.** The notification was confirmed posted with
  3 actions; the actions themselves were not clicked.
- **Large libraries.** Tested with 4 tracks. Behaviour and scroll performance
  with thousands of tracks is unknown.
- **Formats other than MP3.** Only MP3 was used. ExoPlayer supports more, but
  only MP3 was exercised here.
- **No automated UI tests.** The Compose layer has no instrumented tests; it was
  verified by the scripted emulator run above. A UI regression would not be
  caught by `./gradlew test`.
- **Release build.** Only the debug variant was built and run.

## 5. Issue found and fixed during verification

**Screen content used `ColumnScope.weight(1f)` on the wrong parent.** Each
screen was declared as a `ColumnScope` extension and applied `Modifier.weight(1f)`,
but the screens are composed inside the `NavHost`'s `AnimatedContent`, not
directly inside the scaffold's `Column`. A weight modifier whose parent is not
the `Column` is not honoured. Fixed by moving `weight(1f)` onto the `NavHost`
itself and having the screens use `fillMaxSize()`.

This was a latent correctness bug in the layout contract rather than a visible
defect — no misrendering was observed before or after the change, and the
screenshots are identical. It is recorded here because it was found and changed
during verification.

## 6. Reproducing this

```bash
# Unit tests
./gradlew testDebugUnitTest

# Build and install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Load test audio
./tools/make-test-audio.sh build/test-audio
adb push build/test-audio/. /sdcard/Music/
adb shell "for f in /sdcard/Music/*.mp3; do am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://\$f; done"

# Inspect playback state while the app runs
adb shell dumpsys media_session | grep -A6 com.termtune.player
adb shell dumpsys activity services com.termtune.player | grep isForeground
```
