package com.onyx.android.sdk.data.note;

import android.graphics.Matrix;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;

public class TouchPoint extends com.onyx.android.sdk.base.data.TouchPoint {
    public static final int OBJECT_BYTE_COUNT = 32;

    public int action;
    public long time;
    public int toolType;

    public TouchPoint() {
        syncTime();
    }

    public TouchPoint(float x, float y) {
        super(x, y);
        syncTime();
    }

    public TouchPoint(float x, float y, float pressure) {
        this(x, y, pressure, 0f, System.currentTimeMillis());
        this.action = MotionEvent.ACTION_MOVE;
    }

    public TouchPoint(float x, float y, float pressure, float size, long timestamp) {
        super(x, y, pressure, size, timestamp);
        syncTime();
    }

    public TouchPoint(float x, float y, float pressure, float size, int tiltX, int tiltY, long timestamp) {
        super(x, y, pressure, size, tiltX, tiltY, timestamp);
        syncTime();
    }

    public TouchPoint(MotionEvent motionEvent) {
        super(motionEvent);
        action = motionEvent.getActionMasked();
        toolType = motionEvent.getToolType(0);
        syncTime();
    }

    public TouchPoint(TouchPoint other) {
        super(other);
        action = other.action;
        time = other.time;
        toolType = other.toolType;
    }

    public TouchPoint(com.onyx.android.sdk.base.data.TouchPoint other) {
        super(other);
        if (other instanceof TouchPoint) {
            TouchPoint notePoint = (TouchPoint) other;
            action = notePoint.action;
            time = notePoint.time;
            toolType = notePoint.toolType;
        } else {
            syncTime();
        }
    }

    public static int computePointByteSize(List<TouchPoint> list) {
        return list == null ? 0 : list.size() * OBJECT_BYTE_COUNT;
    }

    public static List<TouchPoint> copyList(List<TouchPoint> list) {
        ArrayList<TouchPoint> copies = new ArrayList<>();
        if (list == null) {
            return copies;
        }
        for (TouchPoint point : list) {
            if (point != null) {
                copies.add(point.copy());
            }
        }
        return copies;
    }

    public static TouchPoint create(MotionEvent motionEvent) {
        return new TouchPoint(motionEvent);
    }

    public static TouchPoint fromHistorical(MotionEvent motionEvent, int historyIndex) {
        return new TouchPoint(
            motionEvent.getHistoricalX(historyIndex),
            motionEvent.getHistoricalY(historyIndex),
            motionEvent.getHistoricalPressure(historyIndex),
            motionEvent.getHistoricalSize(historyIndex),
            motionEvent.getHistoricalEventTime(historyIndex)
        );
    }

    public int getAction() {
        return action;
    }

    public long getTime() {
        return time;
    }

    public int getToolType() {
        return toolType;
    }

    public void set(TouchPoint other) {
        super.set(other);
        action = other.action;
        time = other.time;
        toolType = other.toolType;
    }

    public void setAction(int action) {
        this.action = action;
    }

    @Override
    public TouchPoint setPressure(float pressure) {
        super.setPressure(pressure);
        return this;
    }

    @Override
    public TouchPoint setSize(float size) {
        super.setSize(size);
        return this;
    }

    @Override
    public TouchPoint setTiltX(int tiltX) {
        super.setTiltX(tiltX);
        return this;
    }

    @Override
    public TouchPoint setTiltY(int tiltY) {
        super.setTiltY(tiltY);
        return this;
    }

    public void setTime(long time) {
        this.time = time;
        this.timestamp = time;
    }

    @Override
    public TouchPoint setTimestamp(long timestamp) {
        super.setTimestamp(timestamp);
        syncTime();
        return this;
    }

    public void setToolType(int toolType) {
        this.toolType = toolType;
    }

    @Override
    public TouchPoint setX(float x) {
        super.setX(x);
        return this;
    }

    @Override
    public TouchPoint setY(float y) {
        super.setY(y);
        return this;
    }

    public TouchPoint copy() {
        return new TouchPoint(this);
    }

    @Override
    public TouchPoint transform(Matrix matrix) {
        TouchPoint point = copy();
        point.applyMatrix(matrix);
        return point;
    }

    @Override
    public TouchPoint clone() {
        return new TouchPoint(this);
    }

    @Override
    public String toString() {
        return "x:" + x + " y:" + y + " pressure:" + pressure + " size:" + size;
    }

    private void syncTime() {
        time = timestamp;
    }
}
