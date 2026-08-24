package com.antisleep.keepscreen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * 无障碍服务：通过 dispatchGesture 模拟点击屏幕。
 * 触摸事件会重置系统息屏计时器，因此定期点击即可防止自动息屏。
 */
public class AutoClickService extends AccessibilityService {

    public static AutoClickService instance;
    private static volatile int clickCount = 0;

    public static int getClickCount() {
        return clickCount;
    }

    public static void resetClickCount() {
        clickCount = 0;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clickLoop = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.isEnabled(AutoClickService.this)) {
                // 已停止：不再空转，等待外部重启循环
                return;
            }
            performClick();
            handler.postDelayed(this, Prefs.getIntervalMs(AutoClickService.this));
        }
    };

    /** 重新启动点击循环（配置变化或服务恢复时调用） */
    public void restartLoop() {
        handler.removeCallbacks(clickLoop);
        if (Prefs.isEnabled(this)) {
            handler.postDelayed(clickLoop, Prefs.getIntervalMs(this));
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        handler.removeCallbacks(clickLoop);
        if (Prefs.isEnabled(this)) {
            handler.postDelayed(clickLoop, Prefs.getIntervalMs(this));
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要监听事件
    }

    @Override
    public void onInterrupt() {
        // 忽略
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        handler.removeCallbacks(clickLoop);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        handler.removeCallbacks(clickLoop);
        super.onDestroy();
    }

    /** 计算点击坐标并模拟一次点击 */
    private void performClick() {
        // 说明：dispatchGesture 在服务未就绪/无权限时会自动失败，
        // 无需额外 canPerformGestures() 预检（部分平台 stub 无此 API）。
        Point size = getScreenSize();
        if (size == null) return;

        int x, y;
        int mode = Prefs.getMode(this);
        if (mode == Prefs.MODE_CENTER) {
            x = size.x / 2;
            y = size.y / 2;
        } else if (mode == Prefs.MODE_CUSTOM) {
            x = size.x * Prefs.getX(this) / 100;
            y = size.y * Prefs.getY(this) / 100;
        } else {
            // 随机：屏幕中央 60% 区域内随机，避免点到边缘返回键/状态栏
            int rangeX = size.x / 3;
            int rangeY = size.y / 3;
            x = size.x / 2 + (int) ((Math.random() - 0.5) * rangeX * 2);
            y = size.y / 2 + (int) ((Math.random() - 0.5) * rangeY * 2);
        }

        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 60);
        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();
        boolean ok = dispatchGesture(gesture, null, null);
        if (ok) clickCount++;
    }

    private Point getScreenSize() {
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return null;
            Display display = wm.getDefaultDisplay();
            Point p = new Point();
            display.getRealSize(p);
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    /** 立即执行一次点击（供外部调用，如测试按钮） */
    public static void clickNow(Context ctx) {
        AutoClickService svc = instance;
        if (svc != null) svc.performClick();
    }
}
