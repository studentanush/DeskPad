package com.cnl.touchpad.server;

import com.cnl.touchpad.protocol.ProtocolConstants;

/**
 * Server configuration from environment variables and system properties.
 */
public final class ServerConfig {

    private final String bindAddress;
    private final int port;
    private final boolean debug;

    public ServerConfig(String bindAddress, int port, boolean debug) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.debug = debug;
    }

    public static ServerConfig fromEnvironment() {
        String bind = System.getProperty("touchpad.bind", System.getenv().getOrDefault("TOUCHPAD_BIND", "0.0.0.0"));
        int port = parseInt(System.getProperty("touchpad.port", System.getenv().getOrDefault("TOUCHPAD_PORT",
                String.valueOf(ProtocolConstants.DEFAULT_PORT))), ProtocolConstants.DEFAULT_PORT);
        boolean debug = Boolean.parseBoolean(System.getProperty("touchpad.debug",
                System.getenv().getOrDefault("TOUCHPAD_DEBUG", "false")));
        return new ServerConfig(bind, port, debug);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public int getPort() {
        return port;
    }

    public boolean isDebug() {
        return debug;
    }
}
