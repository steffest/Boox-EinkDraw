# Onyx Brush Transparency & Alpha Mechanisms

Based on the reverse engineering and analysis of the decompiled `knote` and `neo-reader` applications, here is how Onyx hardware natively handles stroke alpha and transparency. 

## 1. No Universal Alpha Slider
There is no generic user-facing "Alpha" or "Opacity" slider variable in the native apps for standard pen strokes. Transparency is treated as an immutable visual characteristic of specific brush types rather than a universally adjustable property. 

While the `NeoPenConfig` model contains properties like `alphaFactor` and standard 32-bit `color` integers (ARGB), they are used internally and are not exposed as continuous transparency settings for the user.

## 2. Marker Brush: Hardcoded off-screen 50% Alpha
The Marker brush achieves its translucent highlighter aesthetic through an explicit, hardcoded alpha composite rather than vector path transparency.

When the user draws a Marker stroke, the SDK delegates rendering to `NeoMarkerPenWrapper`:
1. It creates an off-screen, completely opaque bitmap layer matching the canvas dimensions.
2. The stroke geometry is drawn entirely opaque onto this temporary off-screen buffer using the selected color.
3. The wrapper then hardcodes the `Paint` alpha value to exactly **`128`** (which corresponds to exactly 50% opacity on the 0-255 scale).
4. Finally, the off-screen buffer is drawn onto the main Canvas using this 50% alpha paint.

*(Reference: `NeoMarkerPenWrapper.java` lines 42-45)*

## 3. Charcoal Brushes: Pseudo-transparency via Grain Density
Both `CHARCOAL` and `CHARCOAL_V2` brushes achieve their transparent, "smudged" look without actually modifying the drawing alpha channel.

Instead of applying a translucent Alpha mask, the charcoal renderer uses physical texture stamps:
1. It translates the vector stroke points into an array of physical point clusters using `NeoCharcoalPenWrapper.computeStrokeRenderPoints()`.
2. It draws highly detailed, opaque monochromatic bitmaps (grain dots) rapidly along the path. 
3. The "transparency" or lightness of a charcoal stroke is fundamentally driven by **grain density** and pressure. A lighter pressure results in scattered, sparse granular dots, letting more white canvas bleed through, whereas harder pressure piles the texture dots closely together.

Because this relies on solid e-ink pixels simulating analog grit rather than standard Android alpha blending, trying to replicate Charcoal using standard transparent vector vectors will look fundamentally incorrect against the hardware E-Ink rendering layer.

## 4. Eraser Strokes: PorterDuff.Mode.CLEAR
Strokes designated as erasers do not use 0% alpha or white paint. Instead, they check a flag (`ShapeOptionsKt.isTransparent(...)`) and swap the paint's Xfermode to `PorterDuffXfermode(PorterDuff.Mode.CLEAR)`. This means they physically delete pixel data from the canvas rather than rendering a transparent object.

## Summary
To correctly map software canvas strokes to Boox hardware layer outputs:
* **Marker**: Must be stroked onto a buffer, then composited to the target canvas at exactly `alpha = 128`.
* **Charcoal**: Must bypass standard poly-lines and utilize the SDK wrapper to stamp discrete texture bitmaps across the canvas.
