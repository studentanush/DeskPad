package com.cnl.touchpad.protocol;

/**
 * Binary touchpad protocol constants (see PROTOCOL.md).
 */
public final class ProtocolConstants {

    public static final byte MAGIC_0 = 0x54; // 'T'
    public static final byte MAGIC_1 = 0x50; // 'P'
    public static final byte VERSION = 0x01;

    public static final int HEADER_SIZE = 6;

    public static final int MAX_PAYLOAD_LENGTH = 64;

    public static final byte TYPE_MOVE = 0x01;
    public static final byte TYPE_LEFT_CLICK = 0x02;
    public static final byte TYPE_RIGHT_CLICK = 0x03;
    public static final byte TYPE_SCROLL = 0x04;
    public static final byte TYPE_LEFT_DOWN = 0x05;
    public static final byte TYPE_LEFT_UP = 0x06;
    public static final byte TYPE_PING = 0x07;
    public static final byte TYPE_PONG = 0x08;
    public static final byte TYPE_HORIZONTAL_SCROLL = 0x09;
    public static final byte TYPE_MIDDLE_CLICK = 0x0A;
    public static final byte TYPE_RIGHT_DOWN = 0x0B;
    public static final byte TYPE_RIGHT_UP = 0x0C;

    /** Two signed int16 values: dx, dy. */
    public static final int PAYLOAD_MOVE = 4;
    public static final int PAYLOAD_SCROLL = 2;
    public static final int PAYLOAD_HORIZONTAL_SCROLL = 2;
    public static final int PAYLOAD_EMPTY = 0;

    public static final int DEFAULT_PORT = 5000;

    private ProtocolConstants() {
    }
}
