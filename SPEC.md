# Boox E-Ink Draw: Full Build Specification

## 1. Purpose
This document is a source-of-truth implementation spec for recreating the current Boox E-Ink Draw Android app from scratch.

The target is a fast, e-ink-friendly drawing app for Boox devices with:
- A fixed-size internal canvas (`930x1240`)
- Pressure-sensitive drawing (strongly exaggerated)
- Pan/zoom viewport gestures
- Preset color dots + visual color wheel picker
- PNG open/export
- Optional Boox rapid drawing integration

This spec is intentionally implementation-level. Another agent should be able to rebuild the app behaviorally equivalent to the current project.

## 2. Platform & Build Requirements
- Language: Kotlin + small Java utility class
- Android Gradle Plugin: `8.6.1`
- Kotlin plugin: `1.9.25`
- Compile SDK: `34`
- Target SDK: `34`
- Min SDK: `26`
- Java/Kotlin target: `17`

### 2.1 Module structure
- Single app module: `:app`
- Package namespace: `com.boox.einkdraw`

### 2.2 Repositories
Must include:
- `google()`
- `mavenCentral()`
- `https://jitpack.io`
- Boox Maven repositories (HTTP, insecure allowed):
  - `http://repo.boox.com/repository/proxy-public/`
  - `http://repo.boox.com/repository/maven-public/`

### 2.3 Dependencies
Core:
- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.activity:activity-ktx`

Boox/interop:
- Local AAR/JAR fileTree from `app/libs` (Onyx SDK artifacts)
- RxJava 2 (`rxjava`, `rxandroid`)
- RxJava 1 fallback (`io.reactivex:rxjava:1.3.8`, `rxandroid:1.2.1`)
- `org.lsposed.hiddenapibypass:hiddenapibypass`

### 2.4 Packaging rules
- Exclude `/META-INF/{AL2.0,LGPL2.1}`
- `jniLibs.pickFirsts += "lib/*/libc++_shared.so"`

## 3. Manifest & App-level behavior
### 3.1 Permissions
- `SYSTEM_ALERT_WINDOW`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`

### 3.2 Components
- `MainActivity` (launcher, exported)
- `BooxRapidOverlayService` (not exported, foreground special-use)
- `BooxEinkDrawApp` (Application class)

### 3.3 Application bootstrap (`BooxEinkDrawApp`)
On app startup:
- Initialize Onyx `RxManager` context in try/catch (`runCatching`)
- On Android R+ call `HiddenApiBypass.addHiddenApiExemptions("")` in try/catch

## 4. Core UX Layout
Single screen (`activity_main.xml`) with:

### 4.1 Top row (horizontal)
1. `Rapid: ON/OFF` button (`buttonRapidMode`)
2. Preset color palette container (`colorPalette`) populated at runtime
3. Color picker trigger (`buttonColorPicker`):
   - 22dp circular rainbow dot (`@drawable/rainbow_dot`)
4. Flexible spacer
5. Hamburger menu button (`buttonMenu`):
   - 40dp, simple black 3-bar icon (`@drawable/hamburger_black`)

### 4.2 Main drawing area row
Left column (`56dp` wide):
- Vertical brush size slider (`sliderBrushSize`)
- Vertical brush opacity slider (`sliderBrushOpacity`)

Right area:
- `DrawingView` fills remaining space

## 5. Color System
### 5.1 Selected color model
- Store selected drawing color as RGB `Int` (`drawColorArgb`) in `DrawingView`
- Normalize incoming colors to fully opaque RGB via `Color.rgb(r,g,b)`

### 5.2 Preset color dots
Exactly 9 dots, in order:
1. Black
2. White
3. Green
4. Turquoise
5. Blue
6. Purple
7. Red
8. Orange
9. Yellow

Dot visuals:
- Circular swatches (`22dp`)
- Selected state shown as inner inset ring effect (layer drawable composition)
- `6dp` horizontal spacing between dots

### 5.3 Visual color picker popup
- Triggered by rainbow dot button
- Anchored to the top row (dropdown from picker button), not centered modal
- Dark popup style (`@drawable/color_picker_popup_bg`)
- Header with title `Pick color` and close `×`
- Main control: `ColorWheelPickerView` at `150dp x 150dp`
- Bottom info row:
  - 24dp color preview swatch
  - text: `#RRGGBB  H:x S:y% B:z%`

Color picker interaction:
- Outer ring controls hue
- Inner square controls saturation/value
- Updates selected app color live while dragging

## 6. Hamburger Action Menu
Popup menu behavior:
- Anchored to top-right menu button
- Fixed width: `104dp`
- Black border around menu
- Black separators between rows
- High-contrast row style (white/black pressed inversion)
- Large text (`18sp`)

Actions in order:
1. Open PNG
2. Export PNG
3. Clear
4. Reset View

## 7. Drawing Engine (`DrawingView`)
### 7.1 Canvas model
- Internal bitmap: `930x1240`, `Bitmap.Config.RGB_565`
- Backed by `Canvas` for stroke drawing
- View draws bitmap mapped into viewport rect

### 7.2 Viewport model
State:
- `displayScale` to fit canvas in view
- `zoom` (`1.0..4.0`)
- `panX/panY` clamped so content remains bounded

Gestures:
- Two-finger pinch zoom (scale detector)
- Two-finger pan when not scaling
- Single-finger draw when drawing is enabled

### 7.3 Stroke acceptance/throttling
- Time gate: `minEventIntervalMs = 4`
- Distance gate: `minSegmentDistancePx = 0.9`
- Accept sample when either time or movement threshold exceeded

### 7.4 Pressure model (aggressively exaggerated)
Current required behavior:
- `minStrokePx = 0.35`
- `maxStrokePx = 9.5`

Pressure normalization:
- Clamp raw pressure to `[0.01, 1.0]`
- Transform: `normalized = raw^1.25`

Width mapping:
- For each segment/point, compute `pressureForWidth = pressure^2.4`
- Stroke width = `lerp(minStrokePx, maxStrokePx, pressureForWidth) * brushSizeMultiplier`

Opacity mapping:
- `t = pressure^2.8`
- alpha = `lerp(4, 255, t)` then clamp `[4,255]`
- final alpha multiplied by brush opacity multiplier

### 7.5 Brush sliders
Brush size slider mapping (`0..100`):
- `t = progress/100`
- `brushSizeMultiplier = lerp(0.05, 10.4, t)`

Opacity slider mapping (`0..100`):
- `t = progress/100`
- `brushOpacityMultiplier = t^3`

### 7.6 Clear/load/export
- `clear()`: fill canvas white
- `loadBitmap(source)`:
  - requires `930x1240`
  - convert all pixels to grayscale luminance
- `getBitmapCopy()`: return ARGB_8888 copy for PNG export

### 7.7 Partial invalidation
After drawing each point/segment, invalidate only affected on-screen rect transformed through current zoom/pan/fit for performance.

## 8. File Open/Save Behavior
Use Activity Result APIs:
- Open: `OpenDocument("image/png")`
- Save: `CreateDocument("image/png")`, default file name `drawing.png`

Open flow:
- Decode stream
- Validate dimensions exactly `930x1240`
- On success: load grayscale version into canvas
- On error/invalid: toast (`Could not open PNG` or `PNG must be 930x1240`)

Save flow:
- Open output stream (mode `"w"`)
- Compress bitmap as PNG, quality `100`
- Toast on success/failure

## 9. Rapid Mode (Boox-specific)
### 9.1 In-View rapid mode in `DrawingView`
- Create `TouchHelper` with raw callback when enabling
- Configure helper:
  - fountain style
  - raw drawing enabled/render enabled
  - brush raw drawing enabled, eraser disabled
  - pen up refresh 1000ms
  - no repeat-move filtering
- Rapid input suppression support:
  - used while dragging vertical sliders in rapid mode

### 9.2 UI behavior in `MainActivity`
When rapid mode ON:
- `drawingEnabled = false` (normal touch drawing disabled)
- viewport gestures remain enabled
- sliders force e-ink refresh and GU_FAST update mode
- slider touch listeners call `setRapidInputSuppressed(true/false)` during drag

When rapid mode OFF:
- disable rapid mode, release suppression, reset update state

### 9.3 Overlay service (`BooxRapidOverlayService`)
Foreground overlay service exists for Boox raw drawing support with:
- transparent `SurfaceView` as overlay
- notification channel + ongoing notification + stop action
- brush controls via intents
- viewport transform handling via `RapidInputTransform`

The service must be robust to unsupported environments and stop itself with user-visible feedback if helper creation fails.

## 10. Custom Widgets & Visual Components
### 10.1 `VerticalSeekBar`
- Subclass `AppCompatSeekBar`
- Custom draw track/fill manually as vertical rounded bars
- Handles touch to map vertical position into progress
- Optionally forces Onyx e-ink refresh (`EpdController`) when configured

### 10.2 `ColorWheelPickerView`
- Custom View drawing:
  - hue sweep ring
  - inner SV square using composed gradients:
    - horizontal white→hue
    - vertical white→black (multiply blend)
- Ring hit detection must use actual ring center radius and thickness
- Updates `onColorChanged` continuously during drag

## 11. Strings / Copy (required)
Important labels:
- App: `Boox E-Ink Draw`
- Menu actions: `Open PNG`, `Export PNG`, `Clear`, `Reset View`
- Rapid toggle text: `Rapid: ON` / `Rapid: OFF`
- Color picker: `Pick color`

## 12. Resource Contract
Required drawable resources:
- `hamburger_black.xml` (simple 3 horizontal black bars)
- `rainbow_dot.xml` (sweep gradient circular dot with black stroke)
- `menu_popup_background.xml` (white with black border)
- `menu_item_background.xml` (pressed/unpressed high contrast)
- `color_picker_popup_bg.xml` (dark rounded panel with border)
- `menu_item_text_color.xml` (pressed white, default black)

## 13. Non-functional Requirements
- Optimize for e-ink readability and contrast
- Keep UI latency low while drawing
- Avoid unnecessary full-screen invalidation during strokes
- Degrade gracefully if Boox APIs unavailable

## 14. Acceptance Criteria (rebuild verification)
1. App launches to single-screen drawing UI.
2. Canvas is fixed to `930x1240` internal resolution.
3. Top row contains rapid toggle, 9 color dots, rainbow picker dot, right-side hamburger.
4. Hamburger opens compact bordered menu with four actions and large text.
5. Open PNG accepts only exact `930x1240`, converts to grayscale.
6. Export PNG writes current canvas content.
7. Color dots set drawing color immediately.
8. Color picker opens top-attached dark popup with wheel + SV area + live readout.
9. Picking hue updates center area correctly.
10. Picker fits fully (no clipping) at current size.
11. Pressure response is strongly exaggerated (clear width/alpha spread between light/hard pressure).
12. Two-finger pan/zoom works while drawing; reset view returns to 1x centered.
13. Rapid mode toggles ON/OFF, with slider suppression behavior active while dragging in rapid mode.
14. `./gradlew :app:assembleDebug` succeeds.

