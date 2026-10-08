package com.cnl.touchpad.protocol;

/**
 * Encodes touchpad packets into on-the-wire frames (big-endian).
 */
public final class PacketEncoder {

    private final byte[] frameBuffer = new byte[ProtocolConstants.HEADER_SIZE + ProtocolConstants.MAX_PAYLOAD_LENGTH];

    public byte[] encodeMove(int dx, int dy) {
        dx = clampInt16(dx);
        dy = clampInt16(dy);
        byte[] payload = new byte[ProtocolConstants.PAYLOAD_MOVE];
        writeInt16BE(payload, 0, dx);
        writeInt16BE(payload, 2, dy);
        return encodeFrame(ProtocolConstants.TYPE_MOVE, payload);
    }

    public byte[] encodeScroll(int delta) {
        delta = clampInt16(delta);
        byte[] payload = new byte[ProtocolConstants.PAYLOAD_SCROLL];
        writeInt16BE(payload, 0, delta);
        return encodeFrame(ProtocolConstants.TYPE_SCROLL, payload);
    }

    public byte[] encodeHorizontalScroll(int delta) {
        delta = clampInt16(delta);
        byte[] payload = new byte[ProtocolConstants.PAYLOAD_HORIZONTAL_SCROLL];
        writeInt16BE(payload, 0, delta);
        return encodeFrame(ProtocolConstants.TYPE_HORIZONTAL_SCROLL, payload);
    }

    public byte[] encodeLeftClick() {
        return encodeFrame(ProtocolConstants.TYPE_LEFT_CLICK, null);
    }

    public byte[] encodeRightClick() {
        return encodeFrame(ProtocolConstants.TYPE_RIGHT_CLICK, null);
    }

    public byte[] encodeLeftDown() {
        return encodeFrame(ProtocolConstants.TYPE_LEFT_DOWN, null);
    }

    public byte[] encodeLeftUp() {
        return encodeFrame(ProtocolConstants.TYPE_LEFT_UP, null);
    }

    public byte[] encodePing() {
        return encodeFrame(ProtocolConstants.TYPE_PING, null);
    }

    public byte[] encodePong() {
        return encodeFrame(ProtocolConstants.TYPE_PONG, null);
    }

    public byte[] encodeMiddleClick() {
        return encodeFrame(ProtocolConstants.TYPE_MIDDLE_CLICK, null);
    }

    public byte[] encodeRightDown() {
        return encodeFrame(ProtocolConstants.TYPE_RIGHT_DOWN, null);
    }

    public byte[] encodeRightUp() {
        return encodeFrame(ProtocolConstants.TYPE_RIGHT_UP, null);
    }

    /**
     * Writes a complete frame into the internal buffer and returns a copy.
     */
    public byte[] encodeFrame(byte type, byte[] payload) {
        int payloadLen = payload == null ? 0 : payload.length;
        if (payloadLen > ProtocolConstants.MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("Payload too large: " + payloadLen);
        }
        validatePayloadForType(type, payloadLen);

        frameBuffer[0] = ProtocolConstants.MAGIC_0;
        frameBuffer[1] = ProtocolConstants.MAGIC_1;
        frameBuffer[2] = ProtocolConstants.VERSION;
        frameBuffer[3] = type;
        frameBuffer[4] = (byte) ((payloadLen >> 8) & 0xFF);
        frameBuffer[5] = (byte) (payloadLen & 0xFF);
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, frameBuffer, ProtocolConstants.HEADER_SIZE, payloadLen);
        }
        int total = ProtocolConstants.HEADER_SIZE + payloadLen;
        byte[] out = new byte[total];
        System.arraycopy(frameBuffer, 0, out, 0, total);
        return out;
    }

    /**
     * Encode MOVE into caller-provided buffer with no extra allocations.
     */
    public int encodeMoveIntoBuffer(int dx, int dy, byte[] dest, int offset) {
        dx = clampInt16(dx);
        dy = clampInt16(dy);
        dest[offset] = ProtocolConstants.MAGIC_0;
        dest[offset + 1] = ProtocolConstants.MAGIC_1;
        dest[offset + 2] = ProtocolConstants.VERSION;
        dest[offset + 3] = ProtocolConstants.TYPE_MOVE;
        dest[offset + 4] = 0;
        dest[offset + 5] = ProtocolConstants.PAYLOAD_MOVE;
        writeInt16BE(dest, offset + 6, dx);
        writeInt16BE(dest, offset + 8, dy);
        return ProtocolConstants.HEADER_SIZE + ProtocolConstants.PAYLOAD_MOVE;
    }

    private static void validatePayloadForType(byte type, int payloadLen) {
        switch (type) {
            case ProtocolConstants.TYPE_MOVE:
                if (payloadLen != ProtocolConstants.PAYLOAD_MOVE) {
                    throw new IllegalArgumentException("MOVE payload must be 4 bytes (int16 dx + int16 dy)");
                }
                break;
            case ProtocolConstants.TYPE_SCROLL:
            case ProtocolConstants.TYPE_HORIZONTAL_SCROLL:
                if (payloadLen != 2) {
                    throw new IllegalArgumentException("SCROLL payload must be 2 bytes");
                }
                break;
            case ProtocolConstants.TYPE_LEFT_CLICK:
            case ProtocolConstants.TYPE_RIGHT_CLICK:
            case ProtocolConstants.TYPE_LEFT_DOWN:
            case ProtocolConstants.TYPE_LEFT_UP:
            case ProtocolConstants.TYPE_PING:
            case ProtocolConstants.TYPE_PONG:
            case ProtocolConstants.TYPE_MIDDLE_CLICK:
            case ProtocolConstants.TYPE_RIGHT_DOWN:
            case ProtocolConstants.TYPE_RIGHT_UP:
                if (payloadLen != 0) {
                    throw new IllegalArgumentException("Payload must be empty for type " + type);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    static int clampInt16(int value) {
        if (value > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (value < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return value;
    }

    static void writeInt16BE(byte[] dest, int offset, int value) {
        dest[offset] = (byte) ((value >> 8) & 0xFF);
        dest[offset + 1] = (byte) (value & 0xFF);
    }
}
