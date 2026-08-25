package com.antisleep.keepscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 悬浮窗：实时显示运行状态（剩余时间 / 点击次数）。
 * 可拖动，点击打开主界面。由 KeepAwakeService 创建与销毁。
 *
 * VIVO 兼容修复（v1.3）：
 * 1. 不再用 Settings.canDrawOverlays() 预检拦截 —— VIVO 存在"系统级 + i管家级"
 *    双层悬浮窗权限，系统 API 可能误报 false，导致悬浮窗永远不显示。
 *    改为直接尝试 addView，用异常兜底，最大限度兼容各机型。
 * 2. 移除 FLAG_LAYOUT_NO_LIMITS —— 部分 Android 10 设备上会导致布局异常。
 */
public class OverlayView {
    private static OverlayView instance;
    private final Context ctx;
    private WindowManager wm;
    private View view;
    private TextView tvTitle;
    private TextView tvDetail;
    private WindowManager.LayoutParams lp;
    private int downX, downY;
    private boolean moved;

    /** 显示结果状态码 */
    public static final int RESULT_OK = 0;        // 显示成功
    public static final int RESULT_NO_PERM = 1;   // 系统权限未授予
    public static final int RESULT_FAILED = 2;    // 添加窗口失败（权限被系统拦截等）

    private OverlayView(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /**
     * 显示悬浮窗。不依赖 canDrawOverlays() 预检，直接尝试添加窗口。
     * @return RESULT_OK / RESULT_NO_PERM / RESULT_FAILED
     */
    public static int show(Context ctx) {
        hide();
        // 仅作提示用途，不再拦截：VIVO 上即使系统 API 返回 false，addView 仍可能成功
        boolean sysPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(ctx);
        try {
            instance = new OverlayView(ctx);
            instance.build();
            return RESULT_OK;
        } catch (SecurityException e) {
            // 权限被系统拦截
            instance = null;
            return sysPerm ? RESULT_FAILED : RESULT_NO_PERM;
        } catch (Exception e) {
            instance = null;
            return RESULT_FAILED;
        }
    }

    /** 隐藏悬浮窗 */
    public static void hide() {
        if (instance != null) {
            instance.remove();
            instance = null;
        }
    }

    /** 更新显示内容 */
    public static void update(String title, String detail) {
        if (instance != null) {
            instance.setContent(title, detail);
        }
    }

    public static boolean isShowing() {
        return instance != null && instance.view != null;
    }

    private void build() {
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(0xCC202124); // 深色半透明
        root.setGravity(Gravity.CENTER);

        tvTitle = new TextView(ctx);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, Typeface.BOLD);

        tvDetail = new TextView(ctx);
        tvDetail.setTextColor(0xFF8AB4F8);
        tvDetail.setTextSize(12);

        root.addView(tvTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(tvDetail, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // VIVO 兼容：不使用 FLAG_LAYOUT_NO_LIMITS（部分 Android 10 设备布局异常）
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(12);
        lp.y = dp(120);
        lp.alpha = 0.95f;

        // 拖动 + 点击打开主界面
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = (int) e.getRawX();
                        downY = (int) e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - downX);
                        int dy = (int) (e.getRawY() - downY);
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                        if (moved) {
                            lp.x += dx;
                            lp.y += dy;
                            downX = (int) e.getRawX();
                            downY = (int) e.getRawY();
                            try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            // 点击 → 打开主界面
                            Intent i = new Intent(ctx, MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try { ctx.startActivity(i); } catch (Exception ignored) {}
                        }
                        return true;
                }
                return false;
            }
        });

        view = root;
        wm.addView(view, lp);
    }

    private void remove() {
        try {
            if (view != null && wm != null) wm.removeView(view);
        } catch (Exception ignored) {}
        view = null;
    }

    private void setContent(String title, String detail) {
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDetail != null) tvDetail.setText(detail);
    }

    private int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
