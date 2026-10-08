package com.cnl.touchpad.android;

import com.cnl.touchpad.protocol.ProtocolConstants;

import java.util.ArrayDeque;

/**
 * Wakes the writer immediately. Coalesces MOVE by accumulating dx/dy so deltas are not dropped.
 */
final class OutgoingPacketQueue {

    private static final int MAX_IMPORTANT = 64;

    private final Object lock = new Object();
    private final ArrayDeque<byte[]> important = new ArrayDeque<>();
    private int pendingDx;
    private int pendingDy;
    private boolean hasMove;

    void enqueue(byte[] packet, byte type) {
        if (type == ProtocolConstants.TYPE_MOVE) {
            return;
        }
        synchronized (lock) {
            if (important.size() >= MAX_IMPORTANT) {
                important.pollFirst();
            }
            important.addLast(packet);
            lock.notifyAll();
        }
    }

    void enqueueMove(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        synchronized (lock) {
            pendingDx = clampAdd(pendingDx, dx);
            pendingDy = clampAdd(pendingDy, dy);
            hasMove = true;
            lock.notifyAll();
        }
    }

    /**
     * Blocks until a packet is available.
     * @return important packet, or null if a coalesced move is ready (caller reads takeMoveDx/Dy).
     */
    byte[] take() throws InterruptedException {
        synchronized (lock) {
            while (important.isEmpty() && !hasMove) {
                lock.wait();
            }
            if (!important.isEmpty()) {
                return important.pollFirst();
            }
            return null;
        }
    }

    boolean hasPendingMove() {
        synchronized (lock) {
            return hasMove;
        }
    }

    void consumeMove(int[] outDxDy) {
        synchronized (lock) {
            outDxDy[0] = pendingDx;
            outDxDy[1] = pendingDy;
            pendingDx = 0;
            pendingDy = 0;
            hasMove = false;
        }
    }

    void clear() {
        synchronized (lock) {
            important.clear();
            pendingDx = 0;
            pendingDy = 0;
            hasMove = false;
            lock.notifyAll();
        }
    }

    private static int clampAdd(int a, int b) {
        long s = (long) a + (long) b;
        if (s > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (s < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (int) s;
    }
}
