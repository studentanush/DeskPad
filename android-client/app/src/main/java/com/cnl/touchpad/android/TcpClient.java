package com.cnl.touchpad.android;

import com.cnl.touchpad.protocol.PacketEncoder;
import com.cnl.touchpad.protocol.ProtocolConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistent TCP client with dedicated reader/writer threads.
 */
public final class TcpClient {

    public interface Listener {
        void onStateChanged(ConnectionState state);

        void onError(String message);
    }

    private final PacketEncoder encoder = new PacketEncoder();
    private final OutgoingPacketQueue outgoing = new OutgoingPacketQueue();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final int[] moveScratch = new int[2];
    private final byte[] moveFrame = new byte[ProtocolConstants.HEADER_SIZE + ProtocolConstants.PAYLOAD_MOVE];

    private volatile Listener listener;
    private volatile String host = "127.0.0.1";
    private volatile int port = ProtocolConstants.DEFAULT_PORT;

    private Socket socket;
    private Thread writerThread;
    private Thread readerThread;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void configure(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void sendPacket(byte[] packet, byte type) {
        if (!connected.get()) {
            return;
        }
        outgoing.enqueue(packet, type);
    }

    public void sendMove(int dx, int dy) {
        if (!connected.get()) {
            return;
        }
        outgoing.enqueueMove(dx, dy);
    }

    public void sendLeftClick() {
        sendPacket(encoder.encodeLeftClick(), ProtocolConstants.TYPE_LEFT_CLICK);
    }

    public void sendRightClick() {
        sendPacket(encoder.encodeRightClick(), ProtocolConstants.TYPE_RIGHT_CLICK);
    }

    public void sendMiddleClick() {
        sendPacket(encoder.encodeMiddleClick(), ProtocolConstants.TYPE_MIDDLE_CLICK);
    }

    public void sendLeftDown() {
        sendPacket(encoder.encodeLeftDown(), ProtocolConstants.TYPE_LEFT_DOWN);
    }

    public void sendLeftUp() {
        sendPacket(encoder.encodeLeftUp(), ProtocolConstants.TYPE_LEFT_UP);
    }

    public void sendRightDown() {
        sendPacket(encoder.encodeRightDown(), ProtocolConstants.TYPE_RIGHT_DOWN);
    }

    public void sendRightUp() {
        sendPacket(encoder.encodeRightUp(), ProtocolConstants.TYPE_RIGHT_UP);
    }

    public void sendScroll(int delta) {
        sendPacket(encoder.encodeScroll(delta), ProtocolConstants.TYPE_SCROLL);
    }

    public void sendHorizontalScroll(int delta) {
        sendPacket(encoder.encodeHorizontalScroll(delta), ProtocolConstants.TYPE_HORIZONTAL_SCROLL);
    }

    public void connectAsync() {
        stopRequested.set(false);
        Thread t = new Thread(this::connectLoop, "touchpad-connect");
        t.setDaemon(true);
        t.start();
    }

    public void disconnect() {
        stopRequested.set(true);
        closeSocket();
        setState(ConnectionState.DISCONNECTED);
    }

    private void connectLoop() {
        long[] backoff = {500L, 1000L, 2000L, 5000L};
        int attempt = 0;
        while (!stopRequested.get()) {
            setState(attempt == 0 ? ConnectionState.CONNECTING : ConnectionState.RECONNECTING);
            try {
                openConnection();
                attempt = 0;
                setState(ConnectionState.CONNECTED);
                Thread heartbeat = startHeartbeat();
                if (readerThread != null) {
                    readerThread.join();
                }
                heartbeat.interrupt();
            } catch (Exception e) {
                notifyError(e.getMessage());
            } finally {
                closeSocket();
                connected.set(false);
            }
            if (stopRequested.get()) {
                break;
            }
            long delay = backoff[Math.min(attempt, backoff.length - 1)];
            attempt++;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        setState(ConnectionState.DISCONNECTED);
    }

    private void openConnection() throws IOException {
        Socket s = new Socket();
        s.setPerformancePreferences(0, 2, 1);
        s.connect(new InetSocketAddress(host, port), 3000);
        s.setTcpNoDelay(true);
        s.setKeepAlive(true);
        s.setTrafficClass(0x10);
        socket = s;
        connected.set(true);
        startIoThreads(s);
    }

    private void startIoThreads(Socket s) throws IOException {
        final OutputStream out = s.getOutputStream();
        final InputStream in = s.getInputStream();

        writerThread = new Thread(() -> {
            PacketEncoder writerEncoder = new PacketEncoder();
            try {
                while (!stopRequested.get() && connected.get()) {
                    byte[] packet = outgoing.take();
                    if (packet != null) {
                        out.write(packet);
                    }
                    if (outgoing.hasPendingMove()) {
                        outgoing.consumeMove(moveScratch);
                        int len = writerEncoder.encodeMoveIntoBuffer(moveScratch[0], moveScratch[1], moveFrame, 0);
                        out.write(moveFrame, 0, len);
                    }
                    out.flush();
                }
            } catch (IOException | InterruptedException e) {
                if (!stopRequested.get()) {
                    connected.set(false);
                }
            }
        }, "touchpad-writer");
        writerThread.start();

        readerThread = new Thread(() -> {
            byte[] buf = new byte[256];
            try {
                while (!stopRequested.get() && connected.get()) {
                    int read = in.read(buf);
                    if (read == -1) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (!stopRequested.get()) {
                    connected.set(false);
                }
            }
        }, "touchpad-reader");
        readerThread.start();
    }

    private Thread startHeartbeat() {
        Thread heartbeat = new Thread(() -> {
            try {
                while (!stopRequested.get() && connected.get()) {
                    Thread.sleep(8000);
                    if (connected.get()) {
                        sendPacket(encoder.encodePing(), ProtocolConstants.TYPE_PING);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "touchpad-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
        return heartbeat;
    }

    private void closeSocket() {
        connected.set(false);
        outgoing.clear();
        if (writerThread != null) {
            writerThread.interrupt();
            writerThread = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    private void setState(ConnectionState state) {
        Listener l = listener;
        if (l != null) {
            l.onStateChanged(state);
        }
    }

    private void notifyError(String message) {
        Listener l = listener;
        if (l != null) {
            l.onError(message);
        }
    }
}
