package com.cnl.touchpad.protocol;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Incremental TCP stream decoder with resync on invalid magic.
 */
public final class PacketDecoder {

    private static final int INITIAL_BUFFER = 512;
    private static final int MAX_BUFFER = 8192;

    private byte[] buffer = new byte[INITIAL_BUFFER];
    private int writePos;
    private int readPos;

    private long invalidPackets;
    private long validPackets;

    public void reset() {
        writePos = 0;
        readPos = 0;
    }

    public long getInvalidPackets() {
        return invalidPackets;
    }

    public long getValidPackets() {
        return validPackets;
    }

    /**
     * Append bytes from a TCP read.
     */
    public void feed(byte[] data, int offset, int length) {
        if (length <= 0) {
            return;
        }
        ensureCapacity(length);
        System.arraycopy(data, offset, buffer, writePos, length);
        writePos += length;
        compactIfNeeded();
    }

    public void feed(byte[] data) {
        feed(data, 0, data.length);
    }

    /**
     * Decode all complete packets currently buffered.
     */
    public Deque<TouchpadPacket> drainPackets() throws PacketValidationException {
        Deque<TouchpadPacket> out = new ArrayDeque<>();
        while (true) {
            TouchpadPacket packet = tryDecodeOne();
            if (packet == null) {
                break;
            }
            out.add(packet);
        }
        compactIfNeeded();
        return out;
    }

    /**
     * @return next packet or null if more data is required
     */
    public TouchpadPacket tryDecodeOne() throws PacketValidationException {
        int available = writePos - readPos;
        if (available < ProtocolConstants.HEADER_SIZE) {
            return null;
        }

        int headerIndex = findHeader(readPos);
        if (headerIndex < 0) {
            // No magic in buffer; drop all but last byte (could be start of magic)
            if (available > 1) {
                invalidPackets++;
                readPos = writePos - 1;
            }
            return null;
        }
        if (headerIndex > readPos) {
            invalidPackets++;
            readPos = headerIndex;
            available = writePos - readPos;
            if (available < ProtocolConstants.HEADER_SIZE) {
                return null;
            }
        }

        byte version = buffer[readPos + 2];
        byte type = buffer[readPos + 3];
        int payloadLen = ((buffer[readPos + 4] & 0xFF) << 8) | (buffer[readPos + 5] & 0xFF);

        if (version != ProtocolConstants.VERSION) {
            invalidPackets++;
            readPos++;
            return null;
        }
        if (payloadLen < 0 || payloadLen > ProtocolConstants.MAX_PAYLOAD_LENGTH) {
            invalidPackets++;
            readPos++;
            return null;
        }
        if (!isKnownType(type)) {
            invalidPackets++;
            readPos++;
            return null;
        }

        int frameLen = ProtocolConstants.HEADER_SIZE + payloadLen;
        if (available < frameLen) {
            return null;
        }

        if (!validatePayloadLength(type, payloadLen)) {
            invalidPackets++;
            readPos++;
            return null;
        }

        byte[] payload = new byte[payloadLen];
        if (payloadLen > 0) {
            System.arraycopy(buffer, readPos + ProtocolConstants.HEADER_SIZE, payload, 0, payloadLen);
        }
        readPos += frameLen;
        validPackets++;
        return new TouchpadPacket(type, payload);
    }

    public int getBufferedBytes() {
        return writePos - readPos;
    }

    private int findHeader(int from) {
        for (int i = from; i + 1 < writePos; i++) {
            if (buffer[i] == ProtocolConstants.MAGIC_0 && buffer[i + 1] == ProtocolConstants.MAGIC_1) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isKnownType(byte type) {
        switch (type) {
            case ProtocolConstants.TYPE_MOVE:
            case ProtocolConstants.TYPE_LEFT_CLICK:
            case ProtocolConstants.TYPE_RIGHT_CLICK:
            case ProtocolConstants.TYPE_SCROLL:
            case ProtocolConstants.TYPE_LEFT_DOWN:
            case ProtocolConstants.TYPE_LEFT_UP:
            case ProtocolConstants.TYPE_PING:
            case ProtocolConstants.TYPE_PONG:
            case ProtocolConstants.TYPE_HORIZONTAL_SCROLL:
            case ProtocolConstants.TYPE_MIDDLE_CLICK:
            case ProtocolConstants.TYPE_RIGHT_DOWN:
            case ProtocolConstants.TYPE_RIGHT_UP:
                return true;
            default:
                return false;
        }
    }

    static boolean validatePayloadLength(byte type, int payloadLen) {
        switch (type) {
            case ProtocolConstants.TYPE_MOVE:
                return payloadLen == ProtocolConstants.PAYLOAD_MOVE;
            case ProtocolConstants.TYPE_SCROLL:
            case ProtocolConstants.TYPE_HORIZONTAL_SCROLL:
                return payloadLen == ProtocolConstants.PAYLOAD_SCROLL;
            case ProtocolConstants.TYPE_LEFT_CLICK:
            case ProtocolConstants.TYPE_RIGHT_CLICK:
            case ProtocolConstants.TYPE_LEFT_DOWN:
            case ProtocolConstants.TYPE_LEFT_UP:
            case ProtocolConstants.TYPE_PING:
            case ProtocolConstants.TYPE_PONG:
            case ProtocolConstants.TYPE_MIDDLE_CLICK:
            case ProtocolConstants.TYPE_RIGHT_DOWN:
            case ProtocolConstants.TYPE_RIGHT_UP:
                return payloadLen == ProtocolConstants.PAYLOAD_EMPTY;
            default:
                return false;
        }
    }

    private void ensureCapacity(int additional) {
        int needed = (writePos - readPos) + additional;
        if (needed <= buffer.length - readPos) {
            return;
        }
        compactIfNeeded();
        needed = (writePos - readPos) + additional;
        if (needed > buffer.length) {
            int newSize = Math.min(MAX_BUFFER, Math.max(buffer.length * 2, needed));
            if (needed > newSize) {
                throw new IllegalStateException("Decoder buffer overflow");
            }
            buffer = Arrays.copyOf(buffer, newSize);
        }
    }

    private void compactIfNeeded() {
        if (readPos == 0) {
            return;
        }
        int len = writePos - readPos;
        if (readPos > buffer.length / 2 || len == 0) {
            if (len > 0) {
                System.arraycopy(buffer, readPos, buffer, 0, len);
            }
            readPos = 0;
            writePos = len;
        }
    }
}
