package com.cnl.touchpad.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouchpadServerTest {

    private TouchpadServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void serverAcceptsConnection() throws Exception {
        ServerConfig config = new ServerConfig("127.0.0.1", 0, false);
        server = new TouchpadServer(config);
        server.start();
        int port = server.getLocalPort();
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setTcpNoDelay(true);
            assertTrue(socket.isConnected());
        }
    }
}
