# Boox E-Ink Draw - Agent Handoff

This document is for future agents working on this project.
It captures intent, architecture, known pitfalls, and guardrails so work can continue without repeating regressions.

## TL;DR (Read This First)

- Build target: ultra-fast Boox e-ink drawing app where latency and stability matter more than feature completeness.
- Two modes exist: `Rapid OFF` (`DrawingView`) and `Rapid ON` (Onyx `TouchHelper` raw path).
- Biggest recurring failure: rapid strokes become invisible when raw render is disabled without a reliable replay pipeline.
- Second biggest failure: replayed rapid points produce delayed corner spikes (bad/stale coordinates or batched list replay).
- Pick one rapid strategy per change and stick to it:
  - native-visible rapid (`setRawDrawingRenderEnabled(true)`), or
  - app-rendered rapid (`setRawDrawingRenderEnabled(false)` + robust callback replay).
- Do not half-mix native render and replay.
- If rapid is broken, first verify: host view, `setLimitRect`, raw input reader enable, raw render flag.
- If replay artifacts appear, disable `TouchPointList` replay first and validate only move callbacks.
- After every rapid edit, manually test ON/OFF toggle, visibility, pressure, size, opacity, zoom/pan.
- Prefer the stable, visible rapid baseline over risky “almost working” complexity.

## 1) What We Are Building

- Android drawing app for Onyx Boox (e-ink tablet).
- Priority order:
  1. Lowest possible pen latency.
  2. Stable behavior (no crashes, no invisible strokes, no erratic replay).
  3. Useful controls (color, size, opacity, zoom/pan, import/export).
- Canvas is portrait `930 x 1240`.
- PNG open/save works for this exact size.

## 2) Core Product Expectations

- Two user modes:
  - `Rapid OFF`: regular `DrawingView` rendering path.
  - `Rapid ON`: Boox low-level pen SDK path.
- User expects in rapid mode:
  - immediate visible strokes,
  - pressure response,
  - brush size/opacity controls,
  - pinch zoom/pan behavior.

## 3) Current Code Reality (Important)

- `MainActivity` currently toggles rapid mode via `DrawingView.setRapidModeEnabled(...)`.
- There is also `BooxRapidOverlayService`, but it may not be the active runtime path depending on latest toggling logic.
- Multiple rapid implementations were attempted:
  1. overlay service with TouchHelper raw rendering,
  2. overlay service + callback replay into canvas,
  3. direct-in-`DrawingView` TouchHelper host.
- Regressions happened when mixing these patterns.

## 4) High-Risk Pitfalls Seen Repeatedly

### A) Invisible rapid strokes

Common causes:
- `setRawDrawingRenderEnabled(false)` while replay path is broken or disabled.
- raw input reader not enabled.
- host view / limit rect mismatch.

What has helped:
- explicit setup sequence:
  - `openRawDrawing()`
  - `setRawDrawingRenderEnabled(true)` (if relying on native visible strokes)
  - `setRawDrawingEnabled(true)`
  - `setLimitRect(...)`
  - `setRawInputReaderEnable(!isRawDrawingInputEnabled)`

### B) Delayed/erratic replay lines (often from bottom-right corner)

Causes:
- relaying malformed or stale callback points to `DrawingView`.
- mixing screen and local coordinates.
- replaying `TouchPointList` batches late.

Mitigations:
- if replay is used, aggressively filter invalid coordinates.
- prefer per-point move callbacks over delayed list replay.
- avoid dual-path rendering (native raw + replay) unless intentional.

### C) Rapid mode cannot be turned back on/off

Causes:
- state race between UI flag and actual service/runtime state.
- stop/start logic depends on stale checks.

Mitigations:
- update UI state deterministically on toggle.
- avoid relying only on delayed service-state polling.

### D) Rapid strokes disappear after a few seconds

Causes:
- strokes only exist in temporary raw layer and are not committed.
- raw layer refresh/clear behavior after pen-up.

Mitigation depends on chosen strategy:
- Native-visible strategy: keep raw render enabled while rapid is active.
- Commit strategy: explicitly replay/commit points to canvas (requires robust coordinate pipeline).

### E) Side sliders freeze visually in Rapid ON (values still change)

Symptoms:
- On Boox device, first slider touch repaints, then slider appears visually frozen.
- Slider callbacks still fire (brush size/opacity values keep changing).
- Android Studio mirrored view can still show movement, but physical e-ink panel does not repaint reliably.

Cause:
- In rapid mode, raw drawing/e-ink update path can monopolize or interfere with normal UI repaint for nearby controls.

Reliable workaround:
- While user is touching a side slider, temporarily suppress rapid raw input/render:
  - `setRawDrawingEnabled(false)`
  - `setRawDrawingRenderEnabled(false)`
- On slider release/cancel, restore rapid path:
  - `setRawDrawingEnabled(true)`
  - `setRawDrawingRenderEnabled(true)`
- Keep this behavior scoped to slider interaction only.

Notes:
- Extra `invalidate()` calls and even explicit e-ink refresh calls may still be insufficient on some devices.
- Slider-touch suppression has been the most reliable fix observed in this project.

## 5) Boox SDK Specific Notes

Reference docs:
- [Onyx Pen SDK doc](https://raw.githubusercontent.com/onyx-intl/OnyxAndroidDemo/master/doc/Onyx-Pen-SDK.md)

Key points from docs:
- Preferred API shape: `TouchHelper.create(view, RawInputCallback)`.
- Callback flow:
  - `onBeginRawDrawing`
  - `onRawDrawingTouchPointMoveReceived`
  - `onRawDrawingTouchPointListReceived`
  - `onEndRawDrawing`
- `setRawDrawingRenderEnabled(false)` means app must render strokes itself.
- Supported styles documented: `PENCIL`, `FOUNTAIN`.

## 6) Strategic Choice (Decide Before Editing)

Before touching rapid mode, choose one strategy and stay consistent:

### Strategy 1: Native Visible Rapid Layer (simpler, more stable)
- Keep `setRawDrawingRenderEnabled(true)`.
- Do not replay into `DrawingView` while rapid is on.
- Treat rapid output as temporary display layer.
- Optional explicit "commit" action later.

### Strategy 2: App-Rendered Rapid (harder, feature-rich)
- Set `setRawDrawingRenderEnabled(false)`.
- Must build reliable callback point pipeline to `DrawingView`.
- Required for guaranteed custom size/opacity behavior.
- Most regressions came from an incomplete/unstable version of this.

Do not mix both in partial form.

## 7) Current Known Friction

- User repeatedly reports rapid visibility regressions when implementation changes.
- User also wants size/opacity/pressure to affect rapid strokes.
- In practice, this is easiest with Strategy 2, but that has been unstable.
- Strategy 1 gives visibility stability but less guaranteed custom brush behavior.

## 8) Recommended Next Steps for Future Agent

1. Freeze a known visible baseline first.
2. Add one feature at a time with immediate device verification:
   - visibility,
   - pressure response,
   - size response,
   - opacity response,
   - zoom/pan.
3. If adding replay:
   - log raw point x/y/action/time briefly,
   - validate coordinate space once,
   - disable `TouchPointList` replay until single-point path is stable.
4. Never ship both active native render and active replay unintentionally.

## 9) Regression Test Checklist (Manual)

After any rapid-mode change:
- Rapid ON shows immediate strokes.
- No delayed line burst after lifting pen.
- No corner-to-point spikes.
- Rapid OFF still draws normally.
- Toggle ON/OFF repeatedly (5 times).
- Size slider visibly changes stroke width in both modes.
- Opacity slider visibly changes stroke darkness in both modes.
- Pressure changes stroke appearance in both modes.
- Pinch zoom/pan behavior verified in current intended mode design.

## 10) Practical Guardrails

- Keep changes small and reversible.
- Build after every substantial rapid edit.
- If visibility breaks, first verify:
  - raw render flag,
  - raw input reader enable,
  - limit rect,
  - host view.
- If replay artifacts appear, disable batch replay first.

---

If a future agent is uncertain, prioritize user-visible stability over advanced behavior.
The user strongly prefers a working rapid mode over theoretically complete architecture.
