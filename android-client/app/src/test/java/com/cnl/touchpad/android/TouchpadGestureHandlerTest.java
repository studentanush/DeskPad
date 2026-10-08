package com.cnl.touchpad.android;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouchpadGestureHandlerTest {

    private final List<String> events = new ArrayList<>();
    private TouchpadGestureHandler handler;

    @BeforeEach
    void setUp() {
        events.clear();
        handler = new TouchpadGestureHandler(new TouchpadGestureHandler.Sink() {
            @Override
            public void onMove(int dx, int dy) {
                events.add("move:" + dx + "," + dy);
            }

            @Override
            public void onLeftClick() {
                events.add("click");
            }

            @Override
            public void onDoubleClick() {
                events.add("double");
            }

            @Override
            public void onRightClick() {
                events.add("right");
            }

            @Override
            public void onScroll(int dy) {
                events.add("scroll:" + dy);
            }

            @Override
            public void onHorizontalScroll(int dx) {
                events.add("hscroll:" + dx);
            }

            @Override
            public void onLeftDown() {
                events.add("down");
            }

            @Override
            public void onLeftUp() {
                events.add("up");
            }
        });
        handler.setSensitivity(1.0f);
    }

    @Test
    void singleFingerMove() {
        handler.onActionDown(0, 100, 100, 0);
        handler.onActionMove(0, 110, 105, 10, 1);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("move:")));
    }

    @Test
    void singleTap() {
        handler.onActionDown(0, 50, 50, 0);
        handler.onActionUp(0, 51, 51, 50);
        assertEquals("click", events.get(0));
    }

    @Test
    void doubleTap() {
        handler.onActionDown(0, 50, 50, 0);
        handler.onActionUp(0, 50, 50, 50);
        handler.onActionDown(0, 50, 50, 100);
        handler.onActionUp(0, 50, 50, 150);
        assertEquals("double", events.get(1));
    }

    @Test
    void longPressDrag() {
        handler.onActionDown(0, 10, 10, 0);
        handler.onActionMove(0, 10, 10, 500, 1);
        assertEquals("down", events.get(0));
        handler.onActionUp(0, 20, 20, 600);
        assertEquals("up", events.get(events.size() - 1));
    }

    @Test
    void twoFingerScroll() {
        handler.onActionDown(0, 10, 10, 0);
        handler.onActionDown(1, 20, 20, 0);
        handler.onActionMove(0, 10, 30, 20, 2);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("scroll:")));
    }

    @Test
    void cancelReleasesDrag() {
        handler.onActionDown(0, 0, 0, 0);
        handler.onActionMove(0, 0, 0, 500, 1);
        handler.onActionCancel();
        assertTrue(events.contains("up"));
    }

    @Test
    void subpixelSlowMovementDoesNotDrop() {
        handler.onActionDown(0, 100f, 100f, 0);
        // Small 0.35px micro-movements across multiple frames (previously dropped to 0)
        for (int i = 1; i <= 10; i++) {
            handler.onActionMove(0, 100f + (i * 0.4f), 100f, i * 15, 1);
        }
        assertTrue(events.stream().anyMatch(e -> e.startsWith("move:")),
                "Subpixel movements must accumulate and emit moves instead of rounding to zero");
    }

    @Test
    void tapAndDragEmitsDrag() {
        // Tap 1
        handler.onActionDown(0, 50, 50, 0);
        handler.onActionUp(0, 50, 50, 50);
        assertEquals("click", events.get(0));

        // Tap 2 down within double-tap window, then drag
        handler.onActionDown(0, 50, 50, 100);
        handler.onActionMove(0, 100, 100, 150, 1);
        assertTrue(events.contains("down"), "Tap followed by drag should engage Left Down");

        handler.onActionUp(0, 100, 100, 200);
        assertEquals("up", events.get(events.size() - 1), "Lifting finger should release Left Up");
    }

    @Test
    void moveAfterTwoFingerScrollWorks() {
        // Two fingers scroll
        handler.onActionDown(0, 10, 10, 0);
        handler.onActionDown(1, 20, 20, 0);
        handler.onActionMove(0, 10, 40, 20, 2);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("scroll:")), "Should emit scroll");

        // Fingers lift
        handler.onActionPointerUp(1, 20, 40, 50, 1);
        handler.onActionUp(0, 10, 40, 60);

        events.clear();

        // Now single finger moves: must work without requiring reconnect!
        handler.onActionDown(0, 100, 100, 100);
        handler.onActionMove(0, 120, 110, 120, 1);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("move:")),
                "Single finger movement must work immediately after two-finger scroll");
    }

    @Test
    void moveAfterDirectScrollReleaseWorks() {
        // Two fingers scroll
        handler.onActionDown(0, 10, 10, 0);
        handler.onActionDown(1, 20, 20, 0);
        handler.onActionMove(0, 10, 50, 20, 2);

        // Direct action up while in scroll mode
        handler.onActionUp(0, 10, 50, 50);

        events.clear();

        // Single finger move afterwards
        handler.onActionDown(0, 200, 200, 100);
        handler.onActionMove(0, 230, 220, 120, 1);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("move:")),
                "Single finger movement must work after direct release from scroll");
    }
}
