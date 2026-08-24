package com.antisleep.keepscreen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView statusText;
    private Button btnToggle;
    private EditText editInterval;
    private RadioGroup radioMode;
    private EditText editX, editY;
    private EditText editDuration;
    private android.widget.Switch switchOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statusRefresh = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        btnToggle = findViewById(R.id.btnToggle);
        editInterval = findViewById(R.id.editInterval);
        radioMode = findViewById(R.id.radioMode);
        editX = findViewById(R.id.editX);
        editY = findViewById(R.id.editY);
        editDuration = findViewById(R.id.editDuration);
        switchOverlay = findViewById(R.id.switchOverlay);

        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        Button btnTestClick = findViewById(R.id.btnTestClick);
        Button btnSystemSettings = findViewById(R.id.btnSystemSettings);
        Button btnSave = findViewById(R.id.btnSave);

        // 加载配置到界面
        editInterval.setText(String.valueOf(Prefs.getIntervalSec(this)));
        int mode = Prefs.getMode(this);
        ((RadioButton) findViewById(R.id.radioCenter)).setChecked(mode == Prefs.MODE_CENTER);
        ((RadioButton) findViewById(R.id.radioRandom)).setChecked(mode == Prefs.MODE_RANDOM);
        ((RadioButton) findViewById(R.id.radioCustom)).setChecked(mode == Prefs.MODE_CUSTOM);
        editX.setText(String.valueOf(Prefs.getX(this)));
        editY.setText(String.valueOf(Prefs.getY(this)));
        editDuration.setText(String.valueOf(Prefs.getDurationMin(this)));
        switchOverlay.setChecked(Prefs.isOverlayEnabled(this));

        // Android 13+ 请求通知权限（前台服务通知可见性）
        requestNotificationPermission();

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean enabled = Prefs.isEnabled(MainActivity.this);
                if (enabled) {
                    KeepAwakeService.stop(MainActivity.this);
                    Toast.makeText(MainActivity.this, "已停止", Toast.LENGTH_SHORT).show();
                } else {
                    savePrefs();
                    KeepAwakeService.start(MainActivity.this);
                    int dur = Prefs.getDurationMin(MainActivity.this);
                    String tip = dur > 0
                            ? getString(R.string.duration_saved, dur)
                            : "已启动，请确认无障碍服务已开启";
                    Toast.makeText(MainActivity.this, tip, Toast.LENGTH_LONG).show();
                    // 悬浮窗未授权时提醒
                    if (switchOverlay.isChecked()
                            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && !Settings.canDrawOverlays(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, R.string.overlay_permission_btn, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
                    }
                }
                refreshStatus();
            }
        });

        btnTestClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAccessibilityEnabled()) {
                    Toast.makeText(MainActivity.this, R.string.test_click_no_accessibility, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    return;
                }
                AutoClickService.clickNow(MainActivity.this);
                Toast.makeText(MainActivity.this, R.string.test_click_ok, Toast.LENGTH_LONG).show();
            }
        });

        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        btnSystemSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.System.canWrite(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, R.string.timeout_no_permission, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    // 直接写入系统息屏超时 30 分钟（配合自动点击双保险）
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_OFF_TIMEOUT, 30 * 60 * 1000);
                    Toast.makeText(MainActivity.this, R.string.timeout_extended, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btnOverlayPermission = findViewById(R.id.btnOverlayPermission);
        btnOverlayPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(MainActivity.this)) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    Toast.makeText(MainActivity.this, R.string.overlay_permission_ok, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePrefs();
                Toast.makeText(MainActivity.this, "已保存", Toast.LENGTH_SHORT).show();
            }
        });

        refreshStatus();
    }

    private void savePrefs() {
        try {
            int sec = Integer.parseInt(editInterval.getText().toString());
            Prefs.setIntervalSec(this, sec);
        } catch (NumberFormatException ignored) {
        }
        int checked = radioMode.getCheckedRadioButtonId();
        if (checked == R.id.radioCenter) Prefs.setMode(this, Prefs.MODE_CENTER);
        else if (checked == R.id.radioCustom) Prefs.setMode(this, Prefs.MODE_CUSTOM);
        else Prefs.setMode(this, Prefs.MODE_RANDOM);

        try {
            Prefs.setX(this, Integer.parseInt(editX.getText().toString()));
        } catch (NumberFormatException ignored) {
        }
        try {
            Prefs.setY(this, Integer.parseInt(editY.getText().toString()));
        } catch (NumberFormatException ignored) {
        }
        try {
            Prefs.setDurationMin(this, Integer.parseInt(editDuration.getText().toString()));
        } catch (NumberFormatException ignored) {
        }
        Prefs.setOverlayEnabled(this, switchOverlay.isChecked());
    }

    private void refreshStatus() {
        boolean enabled = Prefs.isEnabled(this);
        boolean accOn = isAccessibilityEnabled();
        if (!enabled) {
            statusText.setText(R.string.status_not_running);
            btnToggle.setText(R.string.btn_start);
        } else if (!accOn) {
            statusText.setText(R.string.status_no_accessibility);
            btnToggle.setText(R.string.btn_stop);
        } else {
            statusText.setText(getString(R.string.status_running, Prefs.getIntervalSec(this)));
            btnToggle.setText(R.string.btn_stop);
        }
    }

    private boolean isAccessibilityEnabled() {
        String expected = getPackageName() + "/" + AutoClickService.class.getName();
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(expected)) return true;
        }
        return false;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        handler.removeCallbacks(statusRefresh);
        handler.postDelayed(statusRefresh, 1500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statusRefresh);
    }
}
