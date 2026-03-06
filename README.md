# Boox E-Ink Draw

Fast, e-ink-focused Android drawing app for Boox tablets.

## Current Features

- Fixed internal canvas: `930x1240` (portrait)
- Pressure-sensitive drawing with strongly exaggerated response
  - pressure affects stroke width and opacity
- Pan/zoom viewport
  - pinch to zoom, two-finger pan, reset view action
- Color system
  - 9 quick color dots: black, white, green, turquoise, blue, purple, red, orange, yellow
  - visual color wheel picker popup for arbitrary RGB colors
- Top-right hamburger menu actions
  - `Open PNG`
  - `Export PNG`
  - `Clear`
  - `Reset View`
- PNG open/export
  - open requires exact `930x1240`
  - imported images are converted to grayscale
- Boox rapid mode (`Rapid: ON`)
  - uses Onyx raw drawing APIs for low-latency stylus behavior
  - includes slider touch suppression while rapid mode is active

## UI Overview

- Top row:
  - `Rapid: ON/OFF` toggle
  - color dots
  - rainbow color picker dot
  - top-right hamburger menu
- Left column:
  - vertical brush size slider
  - vertical brush opacity slider
- Main area:
  - drawing canvas viewport

## Build Requirements

- Android Gradle Plugin: `8.6.1`
- Kotlin plugin: `1.9.25`
- Compile SDK / Target SDK: `34`
- Min SDK: `26`
- Java/Kotlin target: `17`

## Build In Android Studio

1. Open Android Studio.
2. Choose **Open** and select this project folder.
3. Let Gradle sync complete.
4. Connect a Boox device (USB or wireless ADB).
5. Press **Run** and choose the device.

## Command Line Build

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Boox / Rapid Mode Notes

- Rapid mode is Boox-specific and may fail on non-Boox devices.
- Overlay permission is required for some rapid drawing paths.
- The app includes foreground service + special-use FGS wiring for overlay integration.

## Rebuild / Agent Handoff

- Detailed implementation spec: [SPEC.md](SPEC.md)
- Additional project notes: [AGENT_HANDOFF.md](AGENT_HANDOFF.md)
