package com.cnl.touchpad.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Full-screen touchpad surface forwarding gestures to the TCP client.
 */
public class TouchpadView extends View {

    private final TouchpadGestureHandler gestureHandler;
    private TcpClient client;

    public TouchpadView(Context context) {
        super(context);
        gestureHandler = createHandler();
    }

    public TouchpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureHandler = createHandler();
    }

    public TouchpadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureHandler = createHandler();
    }

    private TouchpadGestureHandler createHandler() {
        return new TouchpadGestureHandler(new TouchpadGestureHandler.Sink() {
            @Override
            public void onMove(int dx, int dy) {
                if (client != null) {
                    client.sendMove(dx, dy);
                }
            }

            @Override
            public void onLeftClick() {
                if (client != null) {
                    client.sendLeftClick();
                }
            }

            @Override
            public void onDoubleClick() {
                if (client != null) {
                    // Tap 1 already sent one click; this second click completes the double click
                    client.sendLeftClick();
                }
            }

            @Override
            public void onRightClick() {
                if (client != null) {
                    client.sendRightClick();
                }
            }

            @Override
            public void onMiddleClick() {
                if (client != null) {
                    client.sendMiddleClick();
                }
            }

            @Override
            public void onScroll(int dy) {
                if (client != null) {
                    client.sendScroll(dy);
                }
            }

            @Override
            public void onHorizontalScroll(int dx) {
                if (client != null) {
                    client.sendHorizontalScroll(dx);
                }
            }

            @Override
            public void onLeftDown() {
                if (client != null) {
                    client.sendLeftDown();
                }
            }

            @Override
            public void onLeftUp() {
                if (client != null) {
                    client.sendLeftUp();
                }
            }
        });
    }

    public void bindClient(TcpClient client) {
        this.client = client;
    }

    public void setSensitivity(float sensitivity) {
        gestureHandler.setSensitivity(sensitivity);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            requestUnbufferedDispatch(event);
        }

        int pointerCount = event.getPointerCount();
        long time = event.getEventTime();

        if (pointerCount >= 2) {
            float avgX = 0f;
            float avgY = 0f;
            for (int i = 0; i < pointerCount; i++) {
                avgX += event.getX(i);
                avgY += event.getY(i);
            }
            avgX /= pointerCount;
            avgY /= pointerCount;

            int actionIndex = event.getActionIndex();
            int pointerId = event.getPointerId(actionIndex);

            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    gestureHandler.onActionDown(pointerId, avgX, avgY, time);
                    break;
                case MotionEvent.ACTION_MOVE:
                    gestureHandler.onActionMove(pointerId, avgX, avgY, time, pointerCount);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    gestureHandler.onActionPointerUp(pointerId, avgX, avgY, time, pointerCount - 1);
                    break;
                case MotionEvent.ACTION_UP:
                    gestureHandler.onActionUp(pointerId, avgX, avgY, time);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    gestureHandler.onActionCancel();
                    break;
            }
            return true;
        }

        int actionIndex = (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
                ? event.getActionIndex() : 0;
        int pointerId = event.getPointerId(actionIndex);
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gestureHandler.onActionDown(pointerId, x, y, time);
                break;
            case MotionEvent.ACTION_MOVE:
                int history = event.getHistorySize();
                for (int h = 0; h < history; h++) {
                    gestureHandler.onActionMove(
                            pointerId,
                            event.getHistoricalX(actionIndex, h),
                            event.getHistoricalY(actionIndex, h),
                            event.getHistoricalEventTime(h),
                            1);
                }
                gestureHandler.onActionMove(pointerId, x, y, time, 1);
                break;
            case MotionEvent.ACTION_UP:
                gestureHandler.onActionUp(pointerId, x, y, time);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                gestureHandler.onActionPointerUp(pointerId, x, y, time, pointerCount - 1);
                break;
            case MotionEvent.ACTION_CANCEL:
                gestureHandler.onActionCancel();
                break;
        }
        return true;
    }

    public void onClientDisconnected() {
        gestureHandler.onConnectionLost();
    }
}
