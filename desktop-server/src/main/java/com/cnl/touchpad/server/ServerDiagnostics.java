package com.cnl.touchpad.server;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional debug counters (not logged per movement).
 */
public final class ServerDiagnostics {

    private final AtomicLong packetsProcessed = new AtomicLong();
    private final AtomicLong invalidPackets = new AtomicLong();
    private final AtomicLong reconnectCount = new AtomicLong();
    private final AtomicLong connectionsAccepted = new AtomicLong();

    public void onPacketProcessed() {
        packetsProcessed.incrementAndGet();
    }

    public void onInvalidPacket() {
        invalidPackets.incrementAndGet();
    }

    public void onReconnect() {
        reconnectCount.incrementAndGet();
    }

    public void onConnectionAccepted() {
        connectionsAccepted.incrementAndGet();
    }

    public long getPacketsProcessed() {
        return packetsProcessed.get();
    }

    public long getInvalidPackets() {
        return invalidPackets.get();
    }

    public long getReconnectCount() {
        return reconnectCount.get();
    }

    public long getConnectionsAccepted() {
        return connectionsAccepted.get();
    }

    public String snapshot(MouseController mouse) {
        return "connections=" + connectionsAccepted.get()
                + ", packets=" + packetsProcessed.get()
                + ", invalid=" + invalidPackets.get()
                + ", moves=" + mouse.getMoveEvents();
    }
}
