package com.cnl.touchpad.server;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;

/**
 * Relative mouse movement with multi-monitor bounds and scroll accumulation.
 */
public final class MouseController {

    private final Robot robot;
    private final Rectangle virtualBounds;
    private int cursorX;
    private int cursorY;
    private boolean leftDown;
    private double scrollAccumulatorY;
    private double scrollAccumulatorX;
    private long moveEvents;

    private long lastMoveNanos;
    private static final long IDLE_SYNC_NANOS = 150_000_000L; // 150ms pause triggers pointer resync
    private boolean rightDown;

    public MouseController() throws AWTException {
        this(new Robot());
    }

    MouseController(Robot robot) {
        this.robot = robot;
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);
        this.virtualBounds = computeVirtualBounds();
        this.cursorX = virtualBounds.x + virtualBounds.width / 2;
        this.cursorY = virtualBounds.y + virtualBounds.height / 2;
        syncFromSystemPointer();
    }

    public synchronized void syncFromSystemPointer() {
        try {
            Point p = MouseInfo.getPointerInfo().getLocation();
            if (p != null && (p.x > 0 || p.y > 0)) {
                cursorX = clampX(p.x);
                cursorY = clampY(p.y);
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void moveRelative(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastMoveNanos > IDLE_SYNC_NANOS) {
            syncFromSystemPointer();
        }
        lastMoveNanos = now;

        cursorX = clampX(cursorX + dx);
        cursorY = clampY(cursorY + dy);
        robot.mouseMove(cursorX, cursorY);
        moveEvents++;
    }

    public synchronized void leftClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public synchronized void rightClick() {
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    }

    public synchronized void middleClick() {
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
    }

    public synchronized void leftDown() {
        if (!leftDown) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            leftDown = true;
        }
    }

    public synchronized void leftUp() {
        if (leftDown) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            leftDown = false;
        }
    }

    public synchronized void rightDown() {
        if (!rightDown) {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            rightDown = true;
        }
    }

    public synchronized void rightUp() {
        if (rightDown) {
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            rightDown = false;
        }
    }

    public synchronized void scrollVertical(int deltaUnits) {
        if (deltaUnits == 0) {
            return;
        }
        scrollAccumulatorY += deltaUnits;
        int steps = (int) scrollAccumulatorY;
        if (steps != 0) {
            robot.mouseWheel(-steps);
            scrollAccumulatorY -= steps;
        }
    }

    public synchronized void scrollHorizontal(int deltaUnits) {
        if (deltaUnits == 0) {
            return;
        }
        scrollAccumulatorX += deltaUnits;
        int steps = (int) scrollAccumulatorX;
        if (steps != 0) {
            robot.mouseWheel(steps);
            scrollAccumulatorX -= steps;
        }
    }

    public synchronized void releaseAllButtons() {
        leftUp();
        rightUp();
    }

    public synchronized boolean isLeftDown() {
        return leftDown;
    }

    public synchronized boolean isRightDown() {
        return rightDown;
    }

    public long getMoveEvents() {
        return moveEvents;
    }

    public Rectangle getVirtualBounds() {
        return new Rectangle(virtualBounds);
    }

    public Point getCursorPosition() {
        return new Point(cursorX, cursorY);
    }

    private int clampX(int x) {
        int min = virtualBounds.x;
        int max = virtualBounds.x + virtualBounds.width - 1;
        return Math.max(min, Math.min(max, x));
    }

    private int clampY(int y) {
        int min = virtualBounds.y;
        int max = virtualBounds.y + virtualBounds.height - 1;
        return Math.max(min, Math.min(max, y));
    }

    static Rectangle computeVirtualBounds() {
        Rectangle bounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : ge.getScreenDevices()) {
            GraphicsConfiguration config = device.getDefaultConfiguration();
            bounds = union(bounds, config.getBounds());
        }
        if (bounds.width <= 0 || bounds.height <= 0) {
            bounds = new Rectangle(0, 0, 1920, 1080);
        }
        return bounds;
    }

    private static Rectangle union(Rectangle a, Rectangle b) {
        if (a.width == 0 && a.height == 0) {
            return new Rectangle(b);
        }
        int x1 = Math.min(a.x, b.x);
        int y1 = Math.min(a.y, b.y);
        int x2 = Math.max(a.x + a.width, b.x + b.width);
        int y2 = Math.max(a.y + a.height, b.y + b.height);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }
}
