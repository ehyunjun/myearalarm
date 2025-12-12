package com.example.myearalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String timeText = intent.getStringExtra("timeText");
        String soundUriString = intent.getStringExtra("soundUri");
        boolean repeat = intent.getBooleanExtra("repeat", false);
        int alarmId = intent.getIntExtra("alarmId", -1);
        boolean isTimer = intent.getBooleanExtra("isTimer", false);
        int timerIndex = intent.getIntExtra("timerIndex", -1);

        Intent alertIntent = new Intent(context, AlarmAlertActivity.class);
        alertIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        alertIntent.putExtra("alarmId", alarmId);
        alertIntent.putExtra("timeText", timeText);
        alertIntent.putExtra("soundUri", soundUriString);
        alertIntent.putExtra("repeat", repeat);
        alertIntent.putExtra("isTimer", isTimer);
        alertIntent.putExtra("timerIndex", timerIndex);

        context.startActivity(alertIntent);
    }
}
