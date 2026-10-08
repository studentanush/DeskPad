package com.cnl.touchpad.android;

/**
 * Manages TCP client lifecycle and connection state callbacks.
 */
public final class ConnectionManager implements TcpClient.Listener {

    private final TcpClient client = new TcpClient();
    private Listener uiListener;

    public interface Listener {
        void onConnectionState(ConnectionState state);

        void onConnectionError(String error);
    }

    public ConnectionManager() {
        client.setListener(this);
    }

    public TcpClient getClient() {
        return client;
    }

    public void setUiListener(Listener listener) {
        this.uiListener = listener;
    }

    public void start(String host, int port) {
        client.configure(host, port);
        client.connectAsync();
    }

    public void stop() {
        client.disconnect();
    }

    @Override
    public void onStateChanged(ConnectionState state) {
        Listener l = uiListener;
        if (l != null) {
            l.onConnectionState(state);
        }
    }

    @Override
    public void onError(String message) {
        Listener l = uiListener;
        if (l != null) {
            l.onConnectionError(message);
        }
    }
}
