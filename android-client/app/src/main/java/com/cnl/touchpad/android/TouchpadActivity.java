package com.cnl.touchpad.android;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TouchpadActivity extends AppCompatActivity implements ConnectionManager.Listener {

    private ConnectionManager connectionManager;
    private TouchPreferences preferences;
    private TextView statusText;
    private TouchpadView touchpadView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_touchpad);

        preferences = new TouchPreferences(this);
        connectionManager = new ConnectionManager();
        connectionManager.setUiListener(this);

        statusText = findViewById(R.id.statusText);
        touchpadView = findViewById(R.id.touchpadView);
        touchpadView.bindClient(connectionManager.getClient());

        EditText hostInput = findViewById(R.id.hostInput);
        EditText portInput = findViewById(R.id.portInput);
        hostInput.setText(preferences.getHost());
        portInput.setText(String.valueOf(preferences.getPort()));

        SeekBar sensitivitySeek = findViewById(R.id.sensitivitySeek);
        TextView sensitivityValue = findViewById(R.id.sensitivityValue);

        float currentSens = preferences.getSensitivity();
        int progress = Math.max(0, Math.min(280, Math.round((currentSens - 0.2f) * 100f)));
        sensitivitySeek.setProgress(progress);
        sensitivityValue.setText(String.format("%.1fx", currentSens));
        touchpadView.setSensitivity(currentSens);

        sensitivitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sens = 0.2f + (progress / 100f);
                sensitivityValue.setText(String.format("%.1fx", sens));
                touchpadView.setSensitivity(sens);
                preferences.setSensitivity(sens);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button connectButton = findViewById(R.id.connectButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);
        connectButton.setOnClickListener(v -> {
            String host = hostInput.getText().toString().trim();
            int port;
            try {
                port = Integer.parseInt(portInput.getText().toString().trim());
            } catch (NumberFormatException e) {
                port = 5000;
            }
            preferences.setHost(host);
            preferences.setPort(port);
            connectionManager.start(host, port);
        });

        disconnectButton.setOnClickListener(v -> {
            connectionManager.stop();
            touchpadView.onClientDisconnected();
        });

        // Dedicated Mouse Hardware Buttons
        Button btnLeftClick = findViewById(R.id.btnLeftClick);
        Button btnMiddleClick = findViewById(R.id.btnMiddleClick);
        Button btnRightClick = findViewById(R.id.btnRightClick);

        btnLeftClick.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    triggerHaptic();
                    v.setPressed(true);
                    connectionManager.getClient().sendLeftDown();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    connectionManager.getClient().sendLeftUp();
                    return true;
            }
            return false;
        });

        btnRightClick.setOnClickListener(v -> {
            triggerHaptic();
            connectionManager.getClient().sendRightClick();
        });

        btnMiddleClick.setOnClickListener(v -> {
            triggerHaptic();
            connectionManager.getClient().sendMiddleClick();
        });

        // Auto-connect on startup (defaulting to 127.0.0.1:5000 for USB reverse)
        connectionManager.start(preferences.getHost(), preferences.getPort());
    }

    private void triggerHaptic() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(20);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (connectionManager != null && !connectionManager.getClient().isConnected()) {
            connectionManager.start(preferences.getHost(), preferences.getPort());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        touchpadView.onClientDisconnected();
    }

    @Override
    public void onConnectionState(ConnectionState state) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTED:
                    statusText.setText(R.string.status_connected);
                    statusText.setTextColor(0xFF2E7D32); // Green
                    // Clear any previous error banner
                    TextView detail = findViewById(R.id.detailText);
                    if (detail != null) {
                        detail.setText(R.string.instructions);
                    }
                    break;
                case CONNECTING:
                    statusText.setText(R.string.status_connecting);
                    statusText.setTextColor(0xFFF57F17); // Orange
                    break;
                case RECONNECTING:
                    statusText.setText(R.string.status_reconnecting);
                    statusText.setTextColor(0xFFF57F17); // Orange
                    break;
                default:
                    statusText.setText(R.string.status_disconnected);
                    statusText.setTextColor(0xFF757575); // Gray
                    touchpadView.onClientDisconnected();
                    break;
            }
        });
    }

    @Override
    public void onConnectionError(String error) {
        runOnUiThread(() -> {
            TextView detail = findViewById(R.id.detailText);
            detail.setText(getString(R.string.instructions) + "\n\n⚠️ Error: " + error);
        });
    }
}
