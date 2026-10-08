package com.cnl.touchpad.server;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseControllerTest {

    @Test
    void virtualBoundsHasPositiveSize() {
        Rectangle bounds = MouseController.computeVirtualBounds();
        assertTrue(bounds.width > 0);
        assertTrue(bounds.height > 0);
    }

    @Test
    void clampWithinVirtualBounds() throws Exception {
        MouseController controller = new MouseController();
        Rectangle bounds = controller.getVirtualBounds();
        controller.syncFromSystemPointer();
        int maxDx = bounds.width;
        controller.moveRelative(maxDx * 2, 0);
        assertEquals(bounds.x + bounds.width - 1, controller.getCursorPosition().x);
    }
}
