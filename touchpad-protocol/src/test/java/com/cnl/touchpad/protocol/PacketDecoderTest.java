package com.cnl.touchpad.protocol;

import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketDecoderTest {

    private final PacketEncoder encoder = new PacketEncoder();

    @Test
    void partialHeaderThenComplete() throws Exception {
        byte[] move = encoder.encodeMove(1, 2);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(move, 0, 3);
        assertNull(decoder.tryDecodeOne());
        decoder.feed(move, 3, move.length - 3);
        TouchpadPacket p = decoder.tryDecodeOne();
        assertEquals(1, p.readMoveDx());
        assertEquals(2, p.readMoveDy());
    }

    @Test
    void twoPacketsOneRead() throws Exception {
        byte[] a = encoder.encodeLeftClick();
        byte[] b = encoder.encodeRightClick();
        byte[] combined = new byte[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(combined);
        Deque<TouchpadPacket> packets = decoder.drainPackets();
        assertEquals(2, packets.size());
        assertEquals(ProtocolConstants.TYPE_LEFT_CLICK, packets.removeFirst().getType());
        assertEquals(ProtocolConstants.TYPE_RIGHT_CLICK, packets.removeFirst().getType());
    }

    @Test
    void splitAcrossReads() throws Exception {
        byte[] frame = encoder.encodeMove(5, 6);
        PacketDecoder decoder = new PacketDecoder();
        for (int i = 0; i < frame.length; i++) {
            decoder.feed(frame, i, 1);
        }
        TouchpadPacket p = decoder.tryDecodeOne();
        assertEquals(5, p.readMoveDx());
        assertEquals(6, p.readMoveDy());
    }

    @Test
    void garbageBeforeValidPacketResyncs() throws Exception {
        PacketDecoder decoder = new PacketDecoder();
        byte[] garbage = new byte[] {0x00, 0x01, 0x02};
        byte[] move = encoder.encodeMove(9, 9);
        byte[] combined = new byte[garbage.length + move.length];
        System.arraycopy(garbage, 0, combined, 0, garbage.length);
        System.arraycopy(move, 0, combined, garbage.length, move.length);
        decoder.feed(combined);
        TouchpadPacket p = decoder.tryDecodeOne();
        assertEquals(9, p.readMoveDx());
        assertTrue(decoder.getInvalidPackets() >= 1);
    }

    @Test
    void invalidLengthSkipped() throws Exception {
        byte[] bad = {
                ProtocolConstants.MAGIC_0, ProtocolConstants.MAGIC_1, ProtocolConstants.VERSION,
                ProtocolConstants.TYPE_MOVE, 0, 2, 0, 1
        };
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(bad);
        assertNull(decoder.tryDecodeOne());
        assertTrue(decoder.getInvalidPackets() >= 1);
    }
}
