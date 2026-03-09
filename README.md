# Boox E-Ink Draw

Fast Android drawing app focused on Onyx Boox e-ink devices, with priority on low stylus latency and stable stroke replay.

## What it is

This project is a Boox-specific drawing surface that combines:

- hardware pen preview (near-zero latency on e-ink)
- software canvas rendering (persistent bitmap/export/load)
- brush parity work to keep software replay close to hardware preview

Current app UI includes:

- brush selector row (all hardware brush types)
- width slider
- color swatches (black/white/blue)
- `Load`, `Clear`, `Save`
- full-screen drawing surface

## For what tablet

Primary target and test device:

- **Onyx Boox Note Air4C** (Android 13)

It can run on other Onyx Boox tablets that expose the same Onyx pen APIs, but behavior can vary by firmware/device generation.

## Onyx SDK

Onyx SDK documentation is sparse and API behavior is partially firmware-dependent.

This app uses Onyx pen/e-ink APIs from:

- `onyxsdk-base`
- `onyxsdk-device`
- `onyxsdk-pen`

It also bundles missing native-pen Java classes used by wrappers:

- `app/libs/onyxsdk-pen-native-classes.jar`

## The problem.

The Onyx SDK contains wrappers around native pen engines, but non-system apps can hit classpath/runtime gaps and undocumented behavior differences.

Historically:

- AAR wrappers referenced classes that were expected from system/framework scope.
- Native implementation lives in Onyx system libs (for example `libneo_pen.so`) and is not always directly usable by third-party apps.
- Wrapper calls could fail or behave differently depending on firmware + packaging.

In this project, reliability required:

- shipping a known-good `libneo_pen.so` in `app/src/main/jniLibs/arm64-v8a/`
- validating behavior against decompiled reference apps (`neo-reader`, `knote_decompiled`)
- adding fallbacks/guards where wrapper/native behavior is inconsistent

## Hardware-enabled drawing approach (zero-latency)

Core idea: let the hardware path draw immediately, while software reconstruction happens in parallel.

1. `TouchHelper` is created with `FEATURE_ALL_TOUCH_RENDER`.
2. On brush/width/color changes, hardware is configured in strict order:
   - width
   - color
   - limit rect
   - `openRawDrawing()`
   - style
   - reapply width/color (some styles reset these internally)
   - `setRawDrawingRenderEnabled(false)` for hardware preview
   - enable/disable raw input based on UI suppression state
3. During pen movement, `RawInputCallback` receives points.
4. On pen-up, points are rendered into a software bitmap (`OnyxStrokeRenderer`).
5. Hardware preview clears, software bitmap is shown, then e-ink refresh is triggered.

This gives low perceived latency while still producing a persistent/exportable canvas.

## Hardware brushes available

From `HardwarePenStyle`:

- `PENCIL` (default width `5px`)
- `FOUNTAIN` (`8px`)
- `MARKER` (`20px`)
- `NEO_BRUSH` (`12px`)
- `CHARCOAL` (`10px`)
- `DASH` (`5px`)
- `CHARCOAL_V2` (`10px`)
- `SQUARE_PEN` (`8px`)

These map to Onyx hardware stroke-style IDs used by `TouchHelper`.

## Moving from hardware preview to software canvas

This is the critical handoff path:

1. Hardware preview draws live stroke immediately.
2. Callback streams are collected:
   - authority list stream (`onRawDrawingTouchPointListReceived`)
   - raw move stream (`onRawDrawingTouchPointMoveReceived`)
3. On pen-up:
   - app selects the best point source (especially for charcoal where one stream can lose tilt/pressure fidelity)
   - stroke is rendered into software canvas with matching brush renderer
   - snapshot is updated for fast rebuild/clear/load paths
4. `onPenUpRefresh` (or fallback timer) invalidates view so software result replaces hardware preview cleanly.

Implementation anchors:

- hardware bridge + callback orchestration: `HardwarePenSurfaceView`
- per-brush software renderer: `OnyxStrokeRenderer`

## Build requirements

- Android Gradle Plugin: `8.6.1`
- Kotlin plugin: `1.9.25`
- Compile SDK / Target SDK: `34`
- Min SDK: `26`
- Java/Kotlin target: `17`

## Build

### Android Studio

1. Open the project folder.
2. Let Gradle sync.
3. Connect Boox device (ADB USB or wireless).
4. Run `app` on device.

### Command line

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```


## Rebuild / handoff

- Agent handoff notes: [AGENTS.md](AGENTS.md)
