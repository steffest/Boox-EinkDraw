# Boox E-Ink Draw (Minimal)

This is a minimal Android drawing app focused on speed and low visual complexity for e-ink devices:

- Fixed canvas: `930x1240` (portrait)
- Draw colors: black or white
- Pen pressure controls size and alpha (grayscale effect)
- Ultra-fast pencil-only rendering path
- Optional Boox raw overlay mode (`Rapid: ON`) using Onyx pen SDK
- Import PNG (`930x1240`) and export PNG

## Build In Android Studio

1. Open Android Studio.
2. Choose **Open** and select this folder (`Android`).
3. Let Gradle sync complete.
4. Connect your Boox tablet by USB (or use wireless ADB).
5. Press **Run** and choose the Boox device.

## Install On Boox (USB)

1. On the Boox tablet, enable:
   - Developer options
   - USB debugging
2. Plug tablet into your computer and accept the debug prompt on the tablet.
3. In Android Studio, pick the Boox device and run the app.

## Optional Command Line Build

If you want command line builds, first generate/update Gradle wrapper from Android Studio:

1. Open the project in Android Studio.
2. Run the Gradle `wrapper` task once.
3. Then use:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- PNG import only accepts images that are exactly `930x1240`.
- Imported images are converted to grayscale.
- Rendering is optimized around an internal bitmap with partial invalidation to keep stroke latency low.
- `Rapid: ON` starts a foreground overlay service and asks for "draw over other apps" permission.
- Rapid mode is Boox-specific and may fail on non-Boox devices.
- Onyx SDK dependencies are resolved from Boox Maven repositories (`repo.boox.com`) plus JitPack, following the official Onyx demo setup.
