# Boox E-Ink Draw

Minimal but fast Android drawing app focused on Onyx Boox e-ink devices, with priority on low stylus latency and stable stroke replay.

## What it is

This project is a Boox-specific drawing surface that combines:

- hardware pen preview (near-zero latency on e-ink)
- software canvas rendering (persistent bitmap/export/load)

Current app UI includes:

- brush selector (exposing all hardware brush types found in their SDK)
- Brush-width slider
- color swatches and color picker
- layer panel to manage layers
- full-screen drawing surface

## For what tablet

Primary target and test device:

- **Onyx Boox Note Air4C** (Android 13)

It probably can run on other Onyx Boox tablets that expose the same Onyx pen APIs, but behavior can vary by firmware/device generation.

## Onyx SDK

the Onyx Eink devices deliver a native zero-latency drawing experience. 
This is achieved by using a hardware-driven pen experience: during drawing, the "Android screen" gets frozen and there is a direct communication line to the display controller, updating the screen with a pre-defined set of system brushes. 
These strokes are then synced with the software app canvas.

Onyx SDK documentation is sparse, missing or outdated. 
API behavior is partially firmware-dependent.  
 
this is is a call-out to Onyx that
- there are developer out there that want to develop for your devices. Your low-latency Eink drawing libraries deliver a best-in-class drawing experience.
- But .... for peeps sake .... you make it VERY hard for developers to find the correct documentation needed to use these in their own apps.

This app uses Onyx pen/e-ink APIs from:

- `onyxsdk-base`
- `onyxsdk-device`
- `onyxsdk-pen`

It also bundles missing native-pen Java classes used by wrappers:

- `app/libs/onyxsdk-pen-native-classes.jar`

However ... these classes contain wrappers to functions that reside in `libneo_pen.so` - this a system library that only system apps can access.
(so 3rth party apps can't use them directly)

In this project, a copy of this library is included.  
As this is a system-level library, it's device and firmware dependant, so it may very wall fail on your device.

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


## Known missing features:

- Zooming is not present yet. With zooming, the direct mapping between hardware enabled system brushes and the software canvas fall apart very quickly.
- undo/redo
- saving/loading layers to a layer-enabled file format.
