package com.onyx.android.sdk.data.note;

import com.boox.einkdraw.RapidInputTransform;
import com.boox.einkdraw.RapidRawPointRelay;

/**
 * Compatibility shim for Onyx pen native callbacks.
 *
 * Some Onyx pen SDK builds reference this class from native code but do not
 * package it in the AAR dependency graph used by modern AndroidX projects.
 * A minimal implementation keeps raw drawing initialization from crashing.
 */
public class TouchPoint {
    public float x;
    public float y;
    public float size;
    public float pressure;
    public int toolType;
    public int action;
    public long timestamp;
    public long time;
    private long lastPublishedTime;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y, float pressure) {
        this.x = RapidInputTransform.mapX(x);
        this.y = RapidInputTransform.mapY(y);
        this.pressure = pressure;
        this.action = 2;
        this.time = System.currentTimeMillis();
        this.timestamp = this.time;
        maybePublish();
    }

    /**
     * Signature required by Onyx pen native reader:
     * <init>(FFFFIIJ)V
     */
    public TouchPoint(
        float x,
        float y,
        float size,
        float pressure,
        int toolType,
        int action,
        long time
    ) {
        this.x = RapidInputTransform.mapX(x);
        this.y = RapidInputTransform.mapY(y);
        // On some firmware builds these two floats can swap meaning.
        // Prefer a non-zero pressure candidate to keep pressure-sensitive
        // rendering active in raw mode.
        if (pressure <= 0f && size > 0f) {
            this.size = pressure;
            this.pressure = size;
        } else {
            this.size = size;
            this.pressure = pressure;
        }
        this.toolType = toolType;
        this.action = action;
        this.timestamp = time;
        this.time = time;
        maybePublish();
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = RapidInputTransform.mapX(x);
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = RapidInputTransform.mapY(y);
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public int getToolType() {
        return toolType;
    }

    public void setToolType(int toolType) {
        this.toolType = toolType;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
        maybePublish();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
        this.timestamp = time;
        maybePublish();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.time = timestamp;
        maybePublish();
    }

    private void maybePublish() {
        if (time <= 0L) {
            return;
        }
        if (time == lastPublishedTime && action == 2) {
            return;
        }
        lastPublishedTime = time;
        RapidRawPointRelay.publish(this.x, this.y, this.pressure, this.action, this.time);
    }
}
