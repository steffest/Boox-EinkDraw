# Boox E-Ink Draw - Agent Handoff

This document is for future agents working on this project.
It captures intent, architecture, known pitfalls, and guardrails so work can continue without repeating regressions.

## TL;DR (Read This First)

- Build target: ultra-fast Boox e-ink drawing app where latency and stability matter more than feature completeness.
- Two modes exist: `Rapid OFF` (`DrawingView`) and `Rapid ON` (Onyx `TouchHelper` raw path).
- **Core Strategy**: We use the native Onyx hardware render for zero-latency drawing, with a background coordinate ingestion pipeline that reconstructs the permanent stroke on our `DrawingView` canvas.
- **Important**: To achieve realistic pencil textures, `Rapid ON` uses the native hardware `STROKE_STYLE_PENCIL` shader, while the app canvas uses a "stippling" algorithm (scattering solid dots based on density) to perfectly match the grainy look once the hardware layer is cleared.

## 1) What We Are Building

- Android drawing app for Onyx Boox (e-ink tablet).
- Priority order:
  1. Lowest possible pen latency.
  2. Stable behavior (no crashes, no invisible strokes, no erratic replay).
  3. Realism/Texture (e.g., true pencil grain).
- Canvas is portrait `930 x 1240`.
- Supports zooming/panning via standard Android gestures.

## 2) The "Stippled" Pencil Brush (Crucial Find)

**Do not attempt to draw overlapping alpha-blended lines to simulate a pencil.**

If you want the final app canvas to look exactly like the Onyx hardware `STROKE_STYLE_PENCIL` texture:
1. **App Canvas (`drawSegment` / `drawPoint`)**: Use **Stippling**. 
   - Draw pure solid, opaque, 1px dots (`strokePaint.strokeWidth = 1f`, `alpha = 255`).
   - Control the darkness of the stroke by changing the **dot density** (the number of dots scattered across the stroke's radius).
   - Use a deterministic position-based pseudo-random hash to scatter the dots natively into the bitmap.
   - Light pressure = few dots (looks grainy/hazy). Heavy pressure = packed dots (looks solid black).

2. **Onyx Hardware Chip (`TouchHelper`)**: 
   - The hardware chip handles the low-latency preview. 
   - **Do not micromanage the hardware** by pushing dynamic widths or colors on every `MotionEvent`. Just set up the brush at the start and let the chip do it organically.
   - The native pencil shader **WILL CRASH/BREAK** if you give it a custom color with transparency/alpha. You must give it a solid, 100% opaque color (`drawColorArgb or 0xFF000000.toInt()`).
   - The native pencil shader will also fail if you initialize it in the wrong order. 

## 3) The Exact TouchHelper Init Sequence

If the Onyx hardware strokes suddenly turn solid (or invisible) instead of having the pencil texture, the initialization order is broken.

*MUST match this exactly:*
```kotlin
// 1. Set width
helper.setStrokeWidth(initWidth)

// 2. Set SOLID color (no alpha, or shader breaks)
helper.setStrokeColor(drawColorArgb or 0xFF000000.toInt())

// 3. Set limit rect
helper.setLimitRect(bounds, listOf())

// 4. Open Raw Drawing (this resets the hardware chip)
helper.openRawDrawing()

// 5. Set Stroke Style (MUST be after openRawDrawing, or it reverts to FOUNTAIN)
helper.setStrokeStyle(rapidStrokeStyle) // e.g. STROKE_STYLE_PENCIL

// 6. Configure flags
helper.setRawDrawingRenderEnabled(false) // Let the hardware draw it
helper.setRawDrawingEnabled(!rapidInputSuppressed) // Enable the chip
```

## 4) High-Risk Pitfalls Seen Repeatedly

### A) Touch Suppression and UI Updates
Because the hardware chip takes over the screen in `setRawDrawingEnabled(true)`, UI updates (sliders, buttons, color pickers) will NOT show up until you stop the pen and allow the E-ink screen to refresh. 
**Mitigation:** `MainActivity` calls `drawingView.setRapidInputSuppressed(true)` on `ACTION_DOWN` of any UI element, and `false` on `ACTION_UP`. This temporarily suspends the hardware ink overlay so Android can trigger an E-ink screen repaint to show the UI feedback.

### B) Stroke Width Mismatch
The Onyx SDK `setStrokeWidth` takes device **screen pixels**. 
Our internal `DrawingView` canvas is scaled and zoomed (`displayScale * zoom`).
**Mitigation:** Whenever passing a width to `TouchHelper`, you must multiply your base canvas brush width by `(displayScale * zoom)`. 

### C) ANRs during disableRapidMode
Calling `closeRawDrawing()` synchronously on the UI thread while rapid updates are still flowing can cause a hard lock/ANR. 
**Mitigation:** We no longer rely on `closeRawDrawing()`. Disabling rapid mode just drops the `rapidTouchHelper` reference and unbinds the callback. The host Android view handles the cleanup.

### D) App Canvas Coordinate Space
E-ink hardware provides raw coordinates in physical screen pixels. 
**Mitigation:** You must reverse transform them (`toCanvasX`, `toCanvasY`) using the current pan/zoom/displayScale offsets before ingesting them into the permanent bitmap.
