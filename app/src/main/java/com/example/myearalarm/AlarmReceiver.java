package com.example.myearalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String timeText = intent.getStringExtra("timeText");
        String soundUriString = intent.getStringExtra("soundUri");

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
    }
}
