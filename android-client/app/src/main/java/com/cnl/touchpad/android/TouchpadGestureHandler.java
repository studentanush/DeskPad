package com.cnl.touchpad.android;

/**
 * Pure gesture logic driven by MotionEvent actions (testable without Android UI).
 * Features pointer ballistics (dynamic acceleration curve), Bresenham sub-pixel
 * delta accumulation, jitter reduction, tap-to-drag, and calibrated smooth scrolling.
 */
public final class TouchpadGestureHandler {

    public interface Sink {
        void onMove(int dx, int dy);

        void onLeftClick();

        void onDoubleClick();

        void onRightClick();

        void onScroll(int dy);

        void onHorizontalScroll(int dx);

        void onLeftDown();

        void onLeftUp();

        default void onMiddleClick() {}
    }

    private static final float TAP_SLOP_PX = 24.0f;
    private static final long DOUBLE_TAP_MS = 300;
    private static final long LONG_PRESS_MS = 450;
    private static final float SCROLL_STEP_PX = 10.0f;
    private static final float JITTER_THRESHOLD_PX = 0.35f;

    private final Sink sink;
    private float sensitivity = 1.0f;

    // Single finger tracking
    private int activePointerId = -1;
    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private long downTime;
    private long lastEventTime;
    private long lastTapTime = -1;
    private boolean hasMoved;
    private boolean dragging;
    private boolean longPressTriggered;
    private boolean potentialTapDrag;

    // Sub-pixel fractional accumulator
    private float subpixelX;
    private float subpixelY;

    // Two finger tracking
    private boolean scrollMode;
    private boolean twoFingerTapTracking;
    private long twoFingerDownTime;
    private float lastScrollX;
    private float lastScrollY;
    private float twoFingerDownX;
    private float twoFingerDownY;
    private float scrollAccumY;
    private float scrollAccumX;
    private boolean twoFingerMoved;

    // Three finger tracking
    private boolean threeFingerTapTracking;
    private long threeFingerDownTime;
    private boolean threeFingerMoved;

    public TouchpadGestureHandler(Sink sink) {
        this.sink = sink;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
    }

    public void onActionDown(int pointerId, float x, float y, long eventTime) {
        if (activePointerId == -1 || scrollMode) {
            activePointerId = pointerId;
            lastX = x;
            lastY = y;
            downX = x;
            downY = y;
            downTime = eventTime;
            lastEventTime = eventTime;
            hasMoved = false;
            longPressTriggered = false;
            scrollMode = false;
            twoFingerTapTracking = false;
            threeFingerTapTracking = false;

            if (lastTapTime > 0 && (eventTime - lastTapTime) <= DOUBLE_TAP_MS) {
                potentialTapDrag = true;
            } else {
                potentialTapDrag = false;
            }
        } else if (pointerId != activePointerId) {
            cancelDrag();
            enterTwoFingerMode(x, y, eventTime);
        }
    }

    public void onActionMove(int pointerId, float x, float y, long eventTime, int pointerCount) {
        if (pointerCount >= 3) {
            threeFingerMoved = true;
            return;
        }
        if (pointerCount == 2) {
            handleTwoFingerMove(x, y);
            return;
        }
        if (pointerId != activePointerId || scrollMode) {
            return;
        }

        float distFromDown = (float) Math.hypot(x - downX, y - downY);
        if (distFromDown > TAP_SLOP_PX) {
            hasMoved = true;
        }

        if (potentialTapDrag && hasMoved && !dragging) {
            dragging = true;
            potentialTapDrag = false;
            sink.onLeftDown();
        }

        if (dragging) {
            emitMove(x, y, eventTime);
            return;
        }

        if (!hasMoved && !longPressTriggered && (eventTime - downTime) >= LONG_PRESS_MS) {
            longPressTriggered = true;
            dragging = true;
            sink.onLeftDown();
        }

        emitMove(x, y, eventTime);
    }

    public void onActionUp(int pointerId, float x, float y, long eventTime) {
        if (threeFingerTapTracking) {
            if (!threeFingerMoved && (eventTime - threeFingerDownTime) <= 250) {
                sink.onMiddleClick();
            }
            threeFingerTapTracking = false;
            reset();
            return;
        }

        if (twoFingerTapTracking) {
            maybeTwoFingerTap(eventTime);
            twoFingerTapTracking = false;
        }

        if (scrollMode) {
            scrollMode = false;
            twoFingerMoved = false;
            activePointerId = -1;
            subpixelX = 0f;
            subpixelY = 0f;
            return;
        }

        finishOneFingerUp(x, y, eventTime);
    }

    public void onActionPointerUp(int pointerId, float x, float y, long eventTime, int remainingPointers) {
        if (remainingPointers <= 1) {
            scrollMode = false;
            twoFingerMoved = false;
            twoFingerTapTracking = false;
            threeFingerTapTracking = false;
            threeFingerMoved = false;
        }
        if (pointerId == activePointerId) {
            cancelDrag();
            activePointerId = -1;
        }
    }

    public void onActionCancel() {
        cancelDrag();
        reset();
    }

    public void onConnectionLost() {
        cancelDrag();
        reset();
    }

    private void finishOneFingerUp(float x, float y, long eventTime) {
        if (dragging) {
            emitMove(x, y, eventTime);
            sink.onLeftUp();
            dragging = false;
            potentialTapDrag = false;
            activePointerId = -1;
            lastTapTime = -1;
            return;
        }

        if (potentialTapDrag) {
            sink.onDoubleClick();
            potentialTapDrag = false;
            lastTapTime = -1;
            activePointerId = -1;
            return;
        }

        float dist = (float) Math.hypot(x - downX, y - downY);
        if (dist <= TAP_SLOP_PX && !hasMoved && !scrollMode) {
            if (lastTapTime > 0 && (eventTime - lastTapTime) <= DOUBLE_TAP_MS) {
                sink.onDoubleClick();
                lastTapTime = -1;
            } else {
                sink.onLeftClick();
                lastTapTime = eventTime;
            }
        } else {
            lastTapTime = -1;
        }
        activePointerId = -1;
    }

    private void enterTwoFingerMode(float x, float y, long eventTime) {
        scrollMode = true;
        twoFingerTapTracking = true;
        twoFingerDownTime = eventTime;
        lastScrollX = x;
        lastScrollY = y;
        twoFingerDownX = x;
        twoFingerDownY = y;
        scrollAccumY = 0;
        scrollAccumX = 0;
        twoFingerMoved = false;
    }

    private void handleTwoFingerMove(float x, float y) {
        if (!scrollMode) {
            enterTwoFingerMode(x, y, System.currentTimeMillis());
            return;
        }
        float dy = y - lastScrollY;
        float dx = x - lastScrollX;
        lastScrollY = y;
        lastScrollX = x;

        if (Math.hypot(x - twoFingerDownX, y - twoFingerDownY) > TAP_SLOP_PX) {
            twoFingerMoved = true;
            twoFingerTapTracking = false;
        }

        scrollAccumY += dy;
        int stepsY = (int) (scrollAccumY / SCROLL_STEP_PX);
        if (stepsY != 0) {
            sink.onScroll(stepsY);
            scrollAccumY -= (stepsY * SCROLL_STEP_PX);
        }

        scrollAccumX += dx;
        int stepsX = (int) (scrollAccumX / SCROLL_STEP_PX);
        if (stepsX != 0) {
            sink.onHorizontalScroll(stepsX);
            scrollAccumX -= (stepsX * SCROLL_STEP_PX);
        }
    }

    private void maybeTwoFingerTap(long eventTime) {
        if (!twoFingerMoved && (eventTime - twoFingerDownTime) <= 250) {
            sink.onRightClick();
        }
        twoFingerTapTracking = false;
    }

    private void emitMove(float x, float y, long eventTime) {
        float rawDx = x - lastX;
        float rawDy = y - lastY;
        lastX = x;
        lastY = y;

        float dist = (float) Math.hypot(rawDx, rawDy);
        if (dist == 0) {
            return;
        }

        long dt = eventTime - lastEventTime;
        lastEventTime = eventTime;
        if (dt <= 0) {
            dt = 8;
        } else if (dt > 100) {
            dt = 16;
        }

        float speed = dist / dt; // pixels per ms

        // Filter micro-jitter when finger is resting/holding still
        if (dist < JITTER_THRESHOLD_PX && speed < 0.05f) {
            return;
        }

        // Pointer ballistics curve (dynamic acceleration)
        float accel;
        if (speed < 0.2f) {
            accel = 0.90f; // Sub-pixel precision for fine adjustments
        } else if (speed < 1.0f) {
            accel = 0.90f + (speed - 0.2f) * 0.75f;
        } else {
            accel = 1.50f + (float) Math.min(2.0, Math.pow(speed - 1.0f, 1.1) * 0.85);
        }

        float gain = sensitivity * accel;
        float scaledDx = rawDx * gain;
        float scaledDy = rawDy * gain;

        // Sub-pixel accumulator: zero dropped fractions
        subpixelX += scaledDx;
        subpixelY += scaledDy;

        int sendDx = Math.round(subpixelX);
        int sendDy = Math.round(subpixelY);

        subpixelX -= sendDx;
        subpixelY -= sendDy;

        if (sendDx != 0 || sendDy != 0) {
            sink.onMove(sendDx, sendDy);
        }
    }

    private void cancelDrag() {
        if (dragging) {
            sink.onLeftUp();
            dragging = false;
        }
        potentialTapDrag = false;
    }

    private void reset() {
        activePointerId = -1;
        scrollMode = false;
        twoFingerTapTracking = false;
        twoFingerMoved = false;
        threeFingerTapTracking = false;
        threeFingerMoved = false;
        longPressTriggered = false;
        hasMoved = false;
        potentialTapDrag = false;
        subpixelX = 0f;
        subpixelY = 0f;
        scrollAccumY = 0f;
        scrollAccumX = 0f;
    }
}
