package com.boox.einkdraw;

public final class RapidInputTransform {
    private static volatile boolean enabled = false;
    private static volatile float zoom = 1f;
    private static volatile float panX = 0f;
    private static volatile float panY = 0f;
    private static volatile int viewLeft = 0;
    private static volatile int viewTop = 0;
    private static volatile int viewWidth = 0;
    private static volatile int viewHeight = 0;
    private static volatile int canvasWidth = DrawingView.CANVAS_WIDTH;
    private static volatile int canvasHeight = DrawingView.CANVAS_HEIGHT;

    private RapidInputTransform() {
    }

    public static void update(
        float zoomValue,
        float panXValue,
        float panYValue,
        int left,
        int top,
        int width,
        int height,
        int sourceCanvasWidth,
        int sourceCanvasHeight
    ) {
        zoom = Math.max(1f, zoomValue);
        panX = panXValue;
        panY = panYValue;
        viewLeft = left;
        viewTop = top;
        viewWidth = Math.max(0, width);
        viewHeight = Math.max(0, height);
        canvasWidth = Math.max(1, sourceCanvasWidth);
        canvasHeight = Math.max(1, sourceCanvasHeight);
        enabled = viewWidth > 0 && viewHeight > 0;
    }

    public static void clear() {
        enabled = false;
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        viewLeft = 0;
        viewTop = 0;
        viewWidth = 0;
        viewHeight = 0;
        canvasWidth = DrawingView.CANVAS_WIDTH;
        canvasHeight = DrawingView.CANVAS_HEIGHT;
    }

    public static float mapX(float rawX) {
        if (!enabled) return rawX;
        float localX = rawX - viewLeft;
        if (localX < 0f || localX > viewWidth) return rawX;
        float displayScale = Math.min(viewWidth / (float) canvasWidth, viewHeight / (float) canvasHeight);
        float baseOffsetX = (viewWidth - (canvasWidth * displayScale)) * 0.5f;
        float mappedLocalX = ((localX - baseOffsetX - panX) / zoom) + baseOffsetX;
        return viewLeft + mappedLocalX;
    }

    public static float mapY(float rawY) {
        if (!enabled) return rawY;
        float localY = rawY - viewTop;
        if (localY < 0f || localY > viewHeight) return rawY;
        float displayScale = Math.min(viewWidth / (float) canvasWidth, viewHeight / (float) canvasHeight);
        float baseOffsetY = (viewHeight - (canvasHeight * displayScale)) * 0.5f;
        float mappedLocalY = ((localY - baseOffsetY - panY) / zoom) + baseOffsetY;
        return viewTop + mappedLocalY;
    }
}
