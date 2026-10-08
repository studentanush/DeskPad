package com.cnl.touchpad.server;

import java.awt.AWTException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP server accepting a single active Android client at a time.
 */
public final class TouchpadServer implements AutoCloseable {

    private final ServerConfig config;
    private final MouseController mouse;
    private final ServerDiagnostics diagnostics = new ServerDiagnostics();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService clientExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "touchpad-client");
        t.setDaemon(true);
        return t;
    });

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ClientConnection activeConnection;

    public TouchpadServer(ServerConfig config) throws AWTException {
        this.config = config;
        this.mouse = new MouseController();
    }

    TouchpadServer(ServerConfig config, MouseController mouse) {
        this.config = config;
        this.mouse = mouse;
    }

    public synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }
        InetAddress bind = InetAddress.getByName(config.getBindAddress());
        serverSocket = new ServerSocket(config.getPort(), 50, bind);
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "touchpad-accept");
        acceptThread.setDaemon(false);
        acceptThread.start();
        System.out.println("[touchpad] Listening on " + config.getBindAddress() + ":" + config.getPort());
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                client.setTcpNoDelay(true);
                diagnostics.onConnectionAccepted();
                ClientConnection connection = new ClientConnection(client, mouse, diagnostics, config.isDebug());
                synchronized (this) {
                    if (activeConnection != null) {
                        // Replace previous client
                        activeConnection.close();
                        diagnostics.onReconnect();
                    }
                    activeConnection = connection;
                }
                if (!running.get()) {
                    client.close();
                    continue;
                }
                try {
                    clientExecutor.execute(connection);
                } catch (java.util.concurrent.RejectedExecutionException ignored) {
                    client.close();
                }
            } catch (IOException e) {
                if (running.get() && config.isDebug()) {
                    System.out.println("[touchpad] Accept error: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void stop() {
        running.set(false);
        mouse.releaseAllButtons();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        clientExecutor.shutdownNow();
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }

    public ServerDiagnostics getDiagnostics() {
        return diagnostics;
    }

    public MouseController getMouseController() {
        return mouse;
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getLocalPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    @Override
    public void close() {
        stop();
    }
}
