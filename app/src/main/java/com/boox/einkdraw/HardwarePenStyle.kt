package com.boox.einkdraw

import com.onyx.android.sdk.pen.TouchHelper

/**
 * All eight Onyx hardware stroke styles with their software-canvas counterparts.
 * Integer values must match TouchHelper.STROKE_STYLE_* constants exactly.
 */
enum class HardwarePenStyle(
    val hardwareStrokeStyle: Int,
    val label: String,
    val defaultWidthPx: Float,
) {
    PENCIL(TouchHelper.STROKE_STYLE_PENCIL, "Pencil", 5f),
    FOUNTAIN(TouchHelper.STROKE_STYLE_FOUNTAIN, "Fountain", 8f),
    MARKER(TouchHelper.STROKE_STYLE_MARKER, "Marker", 20f),
    NEO_BRUSH(TouchHelper.STROKE_STYLE_NEO_BRUSH, "Neo Brush", 12f),
    CHARCOAL(TouchHelper.STROKE_STYLE_CHARCOAL, "Charcoal", 10f),
    DASH(5, "Dash", 5f),           // STROKE_STYLE_DASH = 5 (not a named constant in pen-1.5.2 SDK)
    CHARCOAL_V2(TouchHelper.STROKE_STYLE_CHARCOAL_V2, "Charcoal V2", 10f),
    SQUARE_PEN(7, "Square Pen", 8f); // STROKE_STYLE_SQUARE_PEN = 7
}
