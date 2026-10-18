# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A portfolio of Jetpack Compose animation practice, mostly characters drawn by hand on a `Canvas`. One Gradle project; a showcase `app` lists the demos and opens each one full screen. `README.md` is the public face of the repo; keep its demo table and the "Add a new animation" steps in sync when modules change.

## Commands

```bash
./gradlew :app:assembleDebug                      # build everything the app needs
./gradlew :app:installDebug                       # install on the connected device
adb shell am start -S -W -n com.zobaer53.composeanimations/.MainActivity
./gradlew :characters:dog:compileDebugKotlin      # compile one module
./gradlew lint                                    # Android lint
scripts/record-demos.sh [demo...]                 # re-record the README GIFs (+ cover with no args)
```

There are no tests. Verification is done visually on a real device (see below).

## Architecture

Dependencies go one way: `app → demos/* → characters/*`.

- `characters/<name>`: reusable Canvas drawings (`dog`, `sprout`, `stormcloud`). They take plain inputs (e.g. `StretchyDog(stretch, expression)`) and know nothing about any screen. No Material dependency.
- `demos/<name>`: full screens. Each exposes exactly one public `@Composable` entry point (e.g. `ForgetPasswordDemo`) that applies its own `MaterialTheme`, so it renders the same in any host; everything else in the module is `internal`.
- `app`: `Demos.kt` is the registry (`Demo(title, description, content)`); `ShowcaseApp` switches between the list and an open demo with a saveable index plus `BackHandler` (no Navigation library). Each new demo needs `include(...)` in `settings.gradle.kts`, `implementation(project(...))` in `app/build.gradle.kts` and an entry in `Demos.kt`.

### Build setup

- AGP 9 with built-in Kotlin. The Kotlin version is pinned by the `kotlin-gradle-plugin` classpath entry in the root `build.gradle.kts`, not by applying a Kotlin plugin.
- `build-logic/` is an included build with two convention plugins: `composeanimations.character` (Android library + Compose UI/foundation/tooling) and `composeanimations.demo` (character + Material 3). It depends on AGP `compileOnly`, so any plugin it applies by id must also be declared `apply false` in the root `build.gradle.kts`.
- All versions, including compile/target/min SDK, live in `gradle/libs.versions.toml`; build-logic reads the same catalog.

### Dog character (`characters/dog`)

- `DogDrawing.kt`: geometry is in "dog units" (1 unit = 1% of canvas height). Outlines are made by stroking all shapes of a silhouette first, then filling them.
- `DogExpression` is a data class of floats; moods are presets that get lerped, so faces morph instead of swapping. `animateDogExpressionAsState` retargets from the currently blended face.
- `StretchyDog` reads every animated value (length spring, wag phase from a `withFrameNanos` loop, blink) inside the draw lambda, so animation only redraws and never recomposes. Keep it that way.
- The dog draws centred and grows to fill its canvas width. The forget-password screen gives it `requiredWidth(screenWidth)` so it can outgrow the card; that card therefore must not clip (no `Card`/`Surface`, `shadow(clip = false)`).

### Sprout character (`characters/sprout`)

- Same patterns as the dog: "sprout units" (1% of canvas height), stroke-then-fill outlines, `SproutExpression` presets lerped by `animateSproutExpressionAsState`, and every animated value read inside the draw lambda.
- Plain inputs: `hydration` (wilted 0 to upright 1, springy), `expression` and `cheers` (bump it to play the hop: crouch, stretch up, hard squashed landing, petal burst). The initial `cheers` value never plays, so restoring a screen doesn't replay it.
- `restless` in the expression scales the fidget loop (glances, foot taps); idle motion (sway, breathing, blink) is always on but small.
- `sproutFlower(canvasSize)` is where the flower sits when upright, for effects that start from it.
- Blinks and near-shut lids switch the eye outline to a lash line; looking up lifts the lids. Without this the eyes read as empty circles.

### Storm cloud character (`characters/stormcloud`)

- "Cloud units" are 1% of the canvas *width*. The cloud hangs at the top of its canvas; rain and lightning fall to the bottom edge, so the canvas must reach the ground.
- Plain inputs: `charge` (size, darkness and temper; springy, so it swells in puffs), `raining`, `strikes` (bump it for a bolt; the initial value never plays) and `present` (false: it cheers up and floats away).
- Bolts alternate sides and are seeded by the strike count, so a bolt keeps its shape for its whole flicker and never hits the middle, where Sprout stands.
- Leaving fades the canvas with `graphicsLayer` alpha. Fading the puffs one by one would show their overlapping outlines through each other.
- `stormCloudIntake(canvasSize)` is where a jet should feed it (left of the underside).

### Daily check-in demo (`demos/dailycheckin`)

- `CheckInPhase` maps the state (thirsty, watering, gave up, bloomed) to headline and expression. Bloomed starts `Overjoyed` and settles to `Content` after 2.2 s.
- The stage (`weight(1f)`) holds the cloud, top, and Sprout, bottom; the cloud canvas stops 5% of Sprout's height above the stage bottom, which is Sprout's ground. It's the only weighted child, so it gets all spare height.
- While watering, `strikes` goes up every 0.55–0.95 s (and once more on success); each strike buzzes, rumbles the screen (`graphicsLayer` translation on an inner box, background outside it) and flashes the overlay.
- Holding fills an `Animatable` at a fixed rate; releasing restarts the `LaunchedEffect(holding)`, which cancels the fill and drains it. The gesture's `finally` resets `holding` even when `pointerInput` restarts because the ring filled.
- Sprout gets hydration in 4 steps (`derivedStateOf`), so it recomposes once per gulp; the ring, week strip and sun halo read progress in draw lambdas.
- `WateringOverlay` sits on top of the whole screen (`matchParentSize`) and draws the jet from the top of the button's ring, out left past Sprout, up into `stormCloudIntake`; plus button ripples, spray, puffs at the intake, the thunder flash (kept soft so it doesn't bleach the bolt) and the bloom wave. End points come from `onGloballyPositioned` in root coordinates. The jet's head grows on press; on release its tail follows it up.
- The bloom wave: a ring from the flower to the far corner (front eases out), and a jittered grid of tiny flowers, leaves and sparkles that each pop when the front reaches them (arrival time inverts the easing), skipping a circle around Sprout.
- The subtitle is always two lines (`minLines = 2`) so the layout, and with it the jet's end points, doesn't shift when the text changes.
- With a screen reader, the button's semantics `onClick` waters in one go.

### Forget-password demo (`demos/forgetpassword`)

- `PasswordStrength` maps length to label, color and expression. The screen layers a timed sequence on top: tongue out at full strength, settling to `Content` after 2 s, and `Smirk` for 2.5 s after Reset.
- Bullet spacing: Android splits `letterSpacing` around each glyph and drops it at line ends, so n bullets take n·glyph + (n−1)·gap. `rememberBulletStyle` solves for the gap so exactly 12 bullets fill the field; more characters scroll.

### README media (`scripts/record-demos.sh`, `docs/media/`)

- The script picks the phone (one connected device or `ANDROID_SERIAL`), installs debug, and per demo: cold-starts `MainActivity`, taps the demo's title, runs `scenario_<slug>` (which looks up targets, then calls `start_recording`), stops `screenrecord` with `pkill -INT`, and converts to a 320 px, 15 fps GIF with the system bars cropped (from the `statusBars`/`navigationBars` frames in `dumpsys window`).
- `app_input` runs the focus check and `input` in one `adb shell`, so nothing can take focus in between. Use it for every tap, swipe and keystroke.
- Typing demos call `crop_above_keyboard` to stop the GIF at the IME's top. Hiding the keyboard doesn't work (Gboard reopens when text arrives), and anything that closes it, like the dog's Reset, moves centred content out of the crop, so the dog clip ends before Reset.
- The cover is `app/src/debug/.../cover/CoverActivity`: it overrides `LocalDensity` to 2 and draws `Cover` at 640 x 320 dp into a `GraphicsLayer`, so the PNG is always 1280 x 640 whatever the phone; the layer records the whole cover even though it's wider than the screen. It fires one lightning strike and captures 110 ms later, on the bolt's brightest flicker. The script pulls `files/cover.png` with `run-as`. `app` has `debugImplementation` on the characters for this.

## Device testing

- Never start an emulator. Use the user's connected Pixel 6a (ask them to connect it if `adb devices` is empty).
- Before every `adb shell input` command, confirm the app has focus (`adb shell dumpsys window | grep mCurrentFocus`). Otherwise taps and typed text land in whatever other app the user has open.
- Find tap targets with `uiautomator dump` rather than guessing coordinates; the keyboard shifts the layout.
