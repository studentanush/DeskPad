package com.cnl.touchpad.server;

import com.cnl.touchpad.protocol.PacketDecoder;
import com.cnl.touchpad.protocol.PacketEncoder;
import com.cnl.touchpad.protocol.PacketValidationException;
import com.cnl.touchpad.protocol.ProtocolConstants;
import com.cnl.touchpad.protocol.TouchpadPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Deque;

/**
 * Handles one Android client over a persistent TCP connection.
 */
public final class ClientConnection implements Runnable {

    private final Socket socket;
    private final MouseController mouse;
    private final ServerDiagnostics diagnostics;
    private final boolean debug;
    private final PacketDecoder decoder = new PacketDecoder();
    private final PacketEncoder encoder = new PacketEncoder();
    private final byte[] readBuffer = new byte[4096];

    public ClientConnection(Socket socket, MouseController mouse, ServerDiagnostics diagnostics, boolean debug) {
        this.socket = socket;
        this.mouse = mouse;
        this.diagnostics = diagnostics;
        this.debug = debug;
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(0x10);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            if (debug) {
                System.out.println("[touchpad] Client connected: " + socket.getRemoteSocketAddress());
            }
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                int read = in.read(readBuffer);
                if (read == -1) {
                    break;
                }
                if (read == 0) {
                    continue;
                }
                decoder.feed(readBuffer, 0, read);
                handlePackets(out);
            }
        } catch (IOException e) {
            if (debug) {
                System.out.println("[touchpad] Client disconnected: " + e.getMessage());
            }
        } finally {
            mouse.releaseAllButtons();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            if (debug) {
                System.out.println("[touchpad] Released mouse buttons after disconnect");
            }
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void handlePackets(OutputStream out) throws IOException {
        try {
            Deque<TouchpadPacket> packets = decoder.drainPackets();
            int pendingMoveDx = 0;
            int pendingMoveDy = 0;
            boolean hasPendingMoves = false;

            while (!packets.isEmpty()) {
                TouchpadPacket packet = packets.removeFirst();
                if (packet.getType() == ProtocolConstants.TYPE_MOVE) {
                    pendingMoveDx += packet.readMoveDx();
                    pendingMoveDy += packet.readMoveDy();
                    hasPendingMoves = true;
                    diagnostics.onPacketProcessed();
                } else {
                    if (hasPendingMoves) {
                        mouse.moveRelative(pendingMoveDx, pendingMoveDy);
                        pendingMoveDx = 0;
                        pendingMoveDy = 0;
                        hasPendingMoves = false;
                    }
                    applyPacket(packet, out);
                    diagnostics.onPacketProcessed();
                }
            }
            if (hasPendingMoves) {
                mouse.moveRelative(pendingMoveDx, pendingMoveDy);
            }
        } catch (PacketValidationException e) {
            diagnostics.onInvalidPacket();
            if (debug) {
                System.out.println("[touchpad] Invalid packet: " + e.getMessage());
            }
        }
    }

    private void applyPacket(TouchpadPacket packet, OutputStream out) throws IOException {
        switch (packet.getType()) {
            case ProtocolConstants.TYPE_MOVE:
                mouse.moveRelative(packet.readMoveDx(), packet.readMoveDy());
                break;
            case ProtocolConstants.TYPE_LEFT_CLICK:
                mouse.leftClick();
                break;
            case ProtocolConstants.TYPE_RIGHT_CLICK:
                mouse.rightClick();
                break;
            case ProtocolConstants.TYPE_MIDDLE_CLICK:
                mouse.middleClick();
                break;
            case ProtocolConstants.TYPE_LEFT_DOWN:
                mouse.leftDown();
                break;
            case ProtocolConstants.TYPE_LEFT_UP:
                mouse.leftUp();
                break;
            case ProtocolConstants.TYPE_RIGHT_DOWN:
                mouse.rightDown();
                break;
            case ProtocolConstants.TYPE_RIGHT_UP:
                mouse.rightUp();
                break;
            case ProtocolConstants.TYPE_SCROLL:
                mouse.scrollVertical(packet.readScrollDelta());
                break;
            case ProtocolConstants.TYPE_HORIZONTAL_SCROLL:
                mouse.scrollHorizontal(packet.readHorizontalScrollDelta());
                break;
            case ProtocolConstants.TYPE_PING:
                out.write(encoder.encodePong());
                out.flush();
                break;
            case ProtocolConstants.TYPE_PONG:
                break;
            default:
                diagnostics.onInvalidPacket();
        }
    }
}
