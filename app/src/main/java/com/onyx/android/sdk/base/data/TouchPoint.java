package com.onyx.android.sdk.base.data;

import android.graphics.Matrix;
import android.view.MotionEvent;
import java.io.Serializable;
import java.util.Objects;

public class TouchPoint implements Serializable, Cloneable {
    public static final int OBJECT_BYTE_COUNT = 32;

    public float pressure;
    public float size;
    public int tiltX;
    public int tiltY;
    public long timestamp;
    public float x;
    public float y;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public TouchPoint(float x, float y, float pressure, float size, long timestamp) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.size = size;
        this.timestamp = timestamp;
    }

    public TouchPoint(float x, float y, float pressure, float size, int tiltX, int tiltY, long timestamp) {
        this(x, y, pressure, size, timestamp);
        this.tiltX = tiltX;
        this.tiltY = tiltY;
    }

    public TouchPoint(MotionEvent motionEvent) {
        this(
            motionEvent.getX(),
            motionEvent.getY(),
            motionEvent.getPressure(),
            motionEvent.getSize(),
            motionEvent.getEventTime()
        );
    }

    public TouchPoint(TouchPoint other) {
        set(other);
    }

    public void applyMatrix(Matrix matrix) {
        if (matrix == null) {
            return;
        }
        float[] points = { x, y };
        matrix.mapPoints(points);
        x = points[0];
        y = points[1];
    }

    public TouchPoint copy() {
        return new TouchPoint(this);
    }

    public boolean equalXY(TouchPoint other) {
        return other != null && x == other.x && y == other.y;
    }

    public float getPressure() {
        return pressure;
    }

    public float getSize() {
        return size;
    }

    public int getTiltX() {
        return tiltX;
    }

    public int getTiltY() {
        return tiltY;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public TouchPoint invertMatrix(Matrix matrix) {
        if (matrix == null) {
            return this;
        }
        Matrix inverted = new Matrix();
        if (matrix.invert(inverted)) {
            applyMatrix(inverted);
        }
        return this;
    }

    public boolean isEmpty() {
        return x == 0f && y == 0f && pressure == 0f && size == 0f && tiltX == 0 && tiltY == 0 && timestamp == 0L;
    }

    public void mapMatrix(Matrix matrix) {
        applyMatrix(matrix);
    }

    public void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }

    public void scale(float scaleX, float scaleY) {
        x *= scaleX;
        y *= scaleY;
    }

    public void set(TouchPoint other) {
        if (other == null) {
            x = 0f;
            y = 0f;
            pressure = 0f;
            size = 0f;
            tiltX = 0;
            tiltY = 0;
            timestamp = 0L;
            return;
        }
        x = other.x;
        y = other.y;
        pressure = other.pressure;
        size = other.size;
        tiltX = other.tiltX;
        tiltY = other.tiltY;
        timestamp = other.timestamp;
    }

    public TouchPoint setPressure(float pressure) {
        this.pressure = pressure;
        return this;
    }

    public TouchPoint setSize(float size) {
        this.size = size;
        return this;
    }

    public TouchPoint setTiltX(int tiltX) {
        this.tiltX = tiltX;
        return this;
    }

    public TouchPoint setTiltY(int tiltY) {
        this.tiltY = tiltY;
        return this;
    }

    public TouchPoint setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public TouchPoint setX(float x) {
        this.x = x;
        return this;
    }

    public TouchPoint setY(float y) {
        this.y = y;
        return this;
    }

    public TouchPoint transform(Matrix matrix) {
        TouchPoint point = copy();
        point.applyMatrix(matrix);
        return point;
    }

    @Override
    public TouchPoint clone() {
        try {
            return (TouchPoint) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return copy();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TouchPoint)) return false;
        TouchPoint that = (TouchPoint) obj;
        return Float.compare(that.x, x) == 0 &&
            Float.compare(that.y, y) == 0 &&
            Float.compare(that.pressure, pressure) == 0 &&
            Float.compare(that.size, size) == 0 &&
            tiltX == that.tiltX &&
            tiltY == that.tiltY &&
            timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, pressure, size, tiltX, tiltY, timestamp);
    }

    @Override
    public String toString() {
        return "x:" + x + " y:" + y + " pressure:" + pressure + " size:" + size;
    }
}
