package com.cnl.touchpad.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class TouchPreferences {

    private static final String PREFS = "touchpad_prefs";
    private static final String KEY_SENSITIVITY = "sensitivity";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";

    private final SharedPreferences prefs;

    public TouchPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public float getSensitivity() {
        return prefs.getFloat(KEY_SENSITIVITY, 1.0f);
    }

    public void setSensitivity(float value) {
        prefs.edit().putFloat(KEY_SENSITIVITY, value).apply();
    }

    public String getHost() {
        return prefs.getString(KEY_HOST, "127.0.0.1");
    }

    public void setHost(String host) {
        prefs.edit().putString(KEY_HOST, host).apply();
    }

    public int getPort() {
        return prefs.getInt(KEY_PORT, 5000);
    }

    public void setPort(int port) {
        prefs.edit().putInt(KEY_PORT, port).apply();
    }
}
