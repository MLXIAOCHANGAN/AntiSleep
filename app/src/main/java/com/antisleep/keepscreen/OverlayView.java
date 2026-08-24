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

    private OverlayView(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /** 显示悬浮窗（无权限时返回 false） */
    public static boolean show(Context ctx) {
        hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(ctx)) {
            return false;
        }
        try {
            instance = new OverlayView(ctx);
            instance.build();
            return true;
        } catch (Exception e) {
            instance = null;
            return false;
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

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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
