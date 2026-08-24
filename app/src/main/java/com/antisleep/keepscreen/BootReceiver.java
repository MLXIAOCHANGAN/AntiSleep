package com.antisleep.keepscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机自启：手机重启后，如果上次是开启状态，自动恢复前台服务 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (Prefs.isEnabled(context)) {
                KeepAwakeService.start(context);
            }
        }
    }
}
