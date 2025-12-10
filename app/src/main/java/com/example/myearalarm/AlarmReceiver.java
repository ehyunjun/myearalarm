package com.example.myearalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String timeText = intent.getStringExtra("timeText");
        String soundUriString = intent.getStringExtra("soundUri");
        boolean repeat = intent.getBooleanExtra("repeat", false);
        int alarmId = intent.getIntExtra("alarmId", -1);


        // 사운드 URI 정하기
        Uri uri = null;
        if (soundUriString != null && !soundUriString.isEmpty()) {
            uri = Uri.parse(soundUriString);
        }

        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        if (uri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone != null) {
                ringtone.play();
            }
        }
        if (repeat && alarmId != -1) {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) ;
            {
                long nextTrigger = System.currentTimeMillis() + 5 * 60 * 1000L;

                Intent newIntent = new Intent(context, AlarmReceiver.class);
                newIntent.putExtra("alarmId", alarmId);
                newIntent.putExtra("timeText", timeText);
                newIntent.putExtra("soundUri", soundUriString);
                newIntent.putExtra("repeat", true);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarmId,
                        newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nextTrigger,
                                pendingIntent
                        );
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                nextTrigger,
                                pendingIntent
                        );
                    } else {
                        alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                nextTrigger,
                                pendingIntent
                        );
                    }
                } catch (SecurityException e) {
                    // 정확한 알람 권한 없으면 그냥 set
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            nextTrigger,
                            pendingIntent
                    );
                }
            }
        }
    }
}
