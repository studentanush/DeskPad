package com.cnl.touchpad.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketEncoderTest {

    private final PacketEncoder encoder = new PacketEncoder();

    @Test
    void encodeMoveRoundTrip() throws Exception {
        byte[] frame = encoder.encodeMove(12, -7);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(frame);
        TouchpadPacket packet = decoder.tryDecodeOne();
        assertEquals(ProtocolConstants.TYPE_MOVE, packet.getType());
        assertEquals(12, packet.readMoveDx());
        assertEquals(-7, packet.readMoveDy());
    }

    @Test
    void encodeClickEmptyPayload() throws Exception {
        byte[] frame = encoder.encodeLeftClick();
        assertEquals(6, frame.length);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(frame);
        TouchpadPacket packet = decoder.tryDecodeOne();
        assertEquals(ProtocolConstants.TYPE_LEFT_CLICK, packet.getType());
        assertEquals(0, packet.getPayload().length);
    }

    @Test
    void encodeScroll() throws Exception {
        byte[] frame = encoder.encodeScroll(-3);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(frame);
        TouchpadPacket packet = decoder.tryDecodeOne();
        assertEquals(-3, packet.readScrollDelta());
    }

    @Test
    void clampMoveOverflow() throws Exception {
        byte[] frame = encoder.encodeMove(40000, -40000);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(frame);
        TouchpadPacket packet = decoder.tryDecodeOne();
        assertEquals(Short.MAX_VALUE, packet.readMoveDx());
        assertEquals(Short.MIN_VALUE, packet.readMoveDy());
    }

    @Test
    void encodeMiddleClickRoundTrip() throws Exception {
        byte[] frame = encoder.encodeMiddleClick();
        assertEquals(6, frame.length);
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(frame);
        TouchpadPacket packet = decoder.tryDecodeOne();
        assertEquals(ProtocolConstants.TYPE_MIDDLE_CLICK, packet.getType());
        assertEquals(0, packet.getPayload().length);
    }

    @Test
    void encodeRightDownAndUpRoundTrip() throws Exception {
        byte[] downFrame = encoder.encodeRightDown();
        byte[] upFrame = encoder.encodeRightUp();
        PacketDecoder decoder = new PacketDecoder();
        decoder.feed(downFrame);
        decoder.feed(upFrame);
        TouchpadPacket p1 = decoder.tryDecodeOne();
        TouchpadPacket p2 = decoder.tryDecodeOne();
        assertEquals(ProtocolConstants.TYPE_RIGHT_DOWN, p1.getType());
        assertEquals(ProtocolConstants.TYPE_RIGHT_UP, p2.getType());
    }

    @Test
    void invalidTypeRejectedAtEncode() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeFrame((byte) 0x7F, null));
    }
}
