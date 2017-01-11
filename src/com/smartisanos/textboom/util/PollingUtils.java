package com.smartisanos.textboom.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class PollingUtils {

    /**
     * Use alarm repeate send broadcast to polling check up if dict update. but after 4.4 alarm
     * might be delay, because power-optimize, reduce the wake up the CPU.
     *
     * @param context
     * @param seconds
     * @param cls
     * @param action
     */
    public static void startPollingService(Context context, int seconds, Class<?> cls, String action) {
        AlarmManager manager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        Intent wakeUpIntent = new Intent(context, cls);
        wakeUpIntent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, wakeUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtTime = SystemClock.elapsedRealtime();

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, seconds * 1000,
                pendingIntent);
    }

    public static void stopPollingService(Context context, Class<?> cls, String action) {
        AlarmManager manager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        Intent wakeUpIntent = new Intent(context, cls);
        wakeUpIntent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, wakeUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        manager.cancel(pendingIntent);
    }
}
