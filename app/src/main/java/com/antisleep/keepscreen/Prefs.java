package com.antisleep.keepscreen;

import android.content.Context;
import android.content.SharedPreferences;

/** 配置读写（点击间隔 / 点击模式 / 自定义坐标 / 运行开关） */
public class Prefs {
    private static final String NAME = "antisleep_prefs";

    public static final int MODE_CENTER = 0;
    public static final int MODE_RANDOM = 1;
    public static final int MODE_CUSTOM = 2;

    public static final String KEY_INTERVAL_SEC = "interval_sec";
    public static final String KEY_MODE = "mode";
    public static final String KEY_X = "x_percent";
    public static final String KEY_Y = "y_percent";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    public static final String KEY_DURATION_MIN = "duration_min";
    public static final String KEY_START_TIME = "start_time";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int getIntervalSec(Context c) {
        int v = sp(c).getInt(KEY_INTERVAL_SEC, 30);
        if (v < 5) v = 5;          // 最小 5 秒
        if (v > 3600) v = 3600;    // 最大 1 小时
        return v;
    }

    public static long getIntervalMs(Context c) {
        return getIntervalSec(c) * 1000L;
    }

    public static int getMode(Context c) {
        int m = sp(c).getInt(KEY_MODE, MODE_RANDOM);
        if (m < MODE_CENTER || m > MODE_CUSTOM) m = MODE_RANDOM;
        return m;
    }

    public static int getX(Context c) {
        int x = sp(c).getInt(KEY_X, 50);
        if (x < 0) x = 0;
        if (x > 100) x = 100;
        return x;
    }

    public static int getY(Context c) {
        int y = sp(c).getInt(KEY_Y, 50);
        if (y < 0) y = 0;
        if (y > 100) y = 100;
        return y;
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, false);
    }

    public static void setIntervalSec(Context c, int sec) {
        sp(c).edit().putInt(KEY_INTERVAL_SEC, sec).apply();
    }

    public static void setMode(Context c, int mode) {
        sp(c).edit().putInt(KEY_MODE, mode).apply();
    }

    public static void setX(Context c, int x) {
        sp(c).edit().putInt(KEY_X, x).apply();
    }

    public static void setY(Context c, int y) {
        sp(c).edit().putInt(KEY_Y, y).apply();
    }


    public static boolean isOverlayEnabled(Context c) {
        return sp(c).getBoolean(KEY_OVERLAY_ENABLED, false);
    }

    public static void setOverlayEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_OVERLAY_ENABLED, v).apply();
    }

    /** 定时运行时长（分钟），0 = 不限时 */
    public static int getDurationMin(Context c) {
        int v = sp(c).getInt(KEY_DURATION_MIN, 0);
        if (v < 0) v = 0;
        if (v > 24 * 60) v = 24 * 60; // 上限 24 小时
        return v;
    }

    public static void setDurationMin(Context c, int v) {
        sp(c).edit().putInt(KEY_DURATION_MIN, v).apply();
    }

    /** 本次运行开始时间戳（毫秒） */
    public static long getStartTime(Context c) {
        return sp(c).getLong(KEY_START_TIME, 0);
    }

    public static void setStartTime(Context c, long t) {
        sp(c).edit().putLong(KEY_START_TIME, t).apply();
    }

    public static void setEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
