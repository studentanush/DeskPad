package com.cnl.touchpad.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Decoded application-layer packet.
 */
public final class TouchpadPacket {

    private final byte type;
    private final byte[] payload;

    public TouchpadPacket(byte type, byte[] payload) {
        this.type = type;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public byte getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int readMoveDx() {
        requireType(ProtocolConstants.TYPE_MOVE, ProtocolConstants.PAYLOAD_MOVE);
        return readInt16BE(payload, 0);
    }

    public int readMoveDy() {
        requireType(ProtocolConstants.TYPE_MOVE, ProtocolConstants.PAYLOAD_MOVE);
        return readInt16BE(payload, 2);
    }

    public int readScrollDelta() {
        requireType(ProtocolConstants.TYPE_SCROLL, ProtocolConstants.PAYLOAD_SCROLL);
        return readInt16BE(payload, 0);
    }

    public int readHorizontalScrollDelta() {
        requireType(ProtocolConstants.TYPE_HORIZONTAL_SCROLL, ProtocolConstants.PAYLOAD_HORIZONTAL_SCROLL);
        return readInt16BE(payload, 0);
    }

    private void requireType(byte expectedType, int expectedLen) {
        if (type != expectedType) {
            throw new IllegalStateException("Unexpected packet type: " + type);
        }
        if (payload.length != expectedLen) {
            throw new IllegalStateException("Unexpected payload length: " + payload.length);
        }
    }

    static int readInt16BE(byte[] data, int offset) {
        int hi = data[offset] & 0xFF;
        int lo = data[offset + 1] & 0xFF;
        return (short) ((hi << 8) | lo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TouchpadPacket)) {
            return false;
        }
        TouchpadPacket that = (TouchpadPacket) o;
        return type == that.type && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, Arrays.hashCode(payload));
    }

    @Override
    public String toString() {
        return "TouchpadPacket{type=" + type + ", payloadLen=" + payload.length + "}";
    }
}
