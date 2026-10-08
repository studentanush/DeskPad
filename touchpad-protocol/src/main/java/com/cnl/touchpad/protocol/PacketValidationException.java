package com.cnl.touchpad.protocol;

/**
 * Thrown when a packet fails validation during decode.
 */
public class PacketValidationException extends Exception {

    public PacketValidationException(String message) {
        super(message);
    }
}
