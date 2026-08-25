package com.antisleep.keepscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** 前台服务：常驻通知 + 保活，让无障碍点击循环在后台稳定运行 */
public class KeepAwakeService extends Service {
    public static final String CHANNEL_ID = "keepscreen_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static boolean running = false;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, 1000);
        }
    };

    /** 每秒执行：刷新悬浮窗 + 检查定时是否到期 */
    private void tick() {
        // 检查定时到期 → 自动停止并息屏
        int durationMin = Prefs.getDurationMin(this);
        long startTime = Prefs.getStartTime(this);
        if (durationMin > 0 && startTime > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= durationMin * 60_000L) {
                stop(this);
                return; // stop() 会走 onDestroy
            }
        }
        // 刷新悬浮窗
        updateOverlay();
    }

    private void updateOverlay() {
        if (!Prefs.isOverlayEnabled(this)) return;
        int clicks = AutoClickService.getClickCount();
        String remain = formatRemain();
        OverlayView.update("防息屏助手 · 运行中", remain + " · 已点击 " + clicks + " 次");
    }

    private String formatRemain() {
        int durationMin = Prefs.getDurationMin(this);
        if (durationMin <= 0) return "不限时";
        long startTime = Prefs.getStartTime(this);
        if (startTime <= 0) return "不限时";
        long remainMs = durationMin * 60_000L - (System.currentTimeMillis() - startTime);
        if (remainMs < 0) remainMs = 0;
        long totalSec = remainMs / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format("剩余 %d:%02d:%02d", h, m, s);
        return String.format("剩余 %02d:%02d", m, s);
    }

    public static void start(Context ctx) {
        Prefs.setEnabled(ctx, true);
        Prefs.setStartTime(ctx, System.currentTimeMillis()); // 记录本次运行开始时间
        AutoClickService.resetClickCount();
        // 若无障碍服务已连接，立即重启点击循环（停止后循环已暂停）
        AutoClickService svc = AutoClickService.instance;
        if (svc != null) {
            svc.restartLoop();
        }
        Intent i = new Intent(ctx, KeepAwakeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        Prefs.setEnabled(ctx, false);
        ctx.stopService(new Intent(ctx, KeepAwakeService.class));
        // 无障碍服务保持开启（避免用户反复去设置里开），仅停止点击循环
        // AutoClickService 的循环会检查 Prefs.isEnabled，false 时不再点击
    }


    /** 立即重新尝试显示悬浮窗（主界面"重试"时调用） */
    public static void retryOverlay(Context ctx) {
        if (Prefs.isOverlayEnabled(ctx) && Prefs.isEnabled(ctx)) {
            OverlayView.show(ctx);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createChannel();
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, 0);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        // 定时 tick：检查超时 + 刷新悬浮窗
        handler.postDelayed(ticker, 1000);
        // 悬浮窗：延迟创建（等待系统窗口就绪）+ 失败自动重试（VIVO 兼容）
        if (Prefs.isOverlayEnabled(this)) {
            scheduleShowOverlay(0);
        }
    }

    /** 延迟创建悬浮窗，失败按 1s/2s/4s 指数退避重试，最多 5 次 */
    private void scheduleShowOverlay(int attempt) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!Prefs.isEnabled(KeepAwakeService.this)) return; // 已停止
                int result = OverlayView.show(KeepAwakeService.this);
                if (result == OverlayView.RESULT_OK) {
                    updateOverlay();
                } else if (attempt < 5) {
                    scheduleShowOverlay(attempt + 1);
                }
                // 失败且重试耗尽：保持静默，用户可在主界面重新触发
            }
        }, attempt == 0 ? 800 : (1000L << Math.min(attempt - 1, 3)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 被系统杀掉后尝试重建
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        OverlayView.hide();
        super.onDestroy();
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "防息屏运行状态", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("防息屏助手前台运行状态");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setContentTitle("防息屏助手运行中")
                .setContentText("每 " + Prefs.getIntervalSec(this) + " 秒自动点击屏幕一次，防止息屏")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        return b.build();
    }
}
