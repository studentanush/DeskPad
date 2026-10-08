package com.cnl.touchpad.server;

import com.cnl.touchpad.protocol.PacketEncoder;
import com.cnl.touchpad.protocol.ProtocolConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationTest {

    private TouchpadServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void decodesOrderedGestureSequence() throws Exception {
        ServerConfig config = new ServerConfig("127.0.0.1", 0, false);
        server = new TouchpadServer(config);
        server.start();
        int port = server.getLocalPort();
        PacketEncoder encoder = new PacketEncoder();

        List<byte[]> frames = new ArrayList<>();
        frames.add(encoder.encodeMove(10, 5));
        frames.add(encoder.encodeLeftClick());
        frames.add(encoder.encodeMove(3, 3));
        frames.add(encoder.encodeScroll(2));
        frames.add(encoder.encodeLeftDown());
        frames.add(encoder.encodeMove(1, 0));
        frames.add(encoder.encodeLeftUp());

        int totalLen = frames.stream().mapToInt(f -> f.length).sum();
        byte[] batch = new byte[totalLen];
        int pos = 0;
        for (byte[] frame : frames) {
            System.arraycopy(frame, 0, batch, pos, frame.length);
            pos += frame.length;
        }

        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setTcpNoDelay(true);
            OutputStream out = socket.getOutputStream();
            out.write(batch);
            out.flush();

            Thread.sleep(200);
        }

        MouseController mouse = server.getMouseController();
        assertTrue(mouse.getMoveEvents() >= 3);
        assertEquals(false, mouse.isLeftDown());
        assertTrue(server.getDiagnostics().getPacketsProcessed() >= 7);
    }

    @Test
    void pingReceivesPong() throws Exception {
        ServerConfig config = new ServerConfig("127.0.0.1", 0, false);
        server = new TouchpadServer(config);
        server.start();
        PacketEncoder encoder = new PacketEncoder();
        try (Socket socket = new Socket("127.0.0.1", server.getLocalPort())) {
            socket.setTcpNoDelay(true);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            out.write(encoder.encodePing());
            out.flush();
            byte[] header = in.readNBytes(ProtocolConstants.HEADER_SIZE);
            assertEquals(ProtocolConstants.MAGIC_0, header[0]);
            assertEquals(ProtocolConstants.TYPE_PONG, header[3]);
        }
    }
}
