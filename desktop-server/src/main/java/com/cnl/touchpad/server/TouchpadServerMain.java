package com.cnl.touchpad.server;

import java.awt.AWTException;

/**
 * Entry point for the Windows desktop touchpad server.
 */
public final class TouchpadServerMain {

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.fromEnvironment();
        TouchpadServer server;
        try {
            server = new TouchpadServer(config);
        } catch (AWTException e) {
            System.err.println("Failed to initialize Robot (headless?): " + e.getMessage());
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.println("==================================================================");
        System.out.println("   CNL Wireless Touchpad & Mouse Server (Optimized Real Mouse)   ");
        System.out.println("==================================================================");
        System.out.println("  Port: " + config.getPort());
        System.out.println("  [USB Connection]:   127.0.0.1:" + config.getPort() + " (run scripts\\connect.bat)");
        System.out.println("  [Wi-Fi Connection]: Connect phone to same Wi-Fi and use PC IP:");
        printLocalIps(config.getPort());
        System.out.println("==================================================================");

        if (config.isDebug()) {
            Thread diag = new Thread(() -> {
                while (server.isRunning()) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("[touchpad] " + server.getDiagnostics().snapshot(server.getMouseController()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "touchpad-diag");
            diag.setDaemon(true);
            diag.start();
        }
    }

    private static void printLocalIps(int port) {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            boolean found = false;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        System.out.println("    -> " + addr.getHostAddress() + ":" + port + " (" + iface.getDisplayName() + ")");
                        found = true;
                    }
                }
            }
            if (!found) {
                System.out.println("    -> (Check Wi-Fi network connection)");
            }
        } catch (Exception e) {
            System.out.println("    -> (Could not enumerate network interfaces: " + e.getMessage() + ")");
        }
    }
}
