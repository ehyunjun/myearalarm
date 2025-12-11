package com.example.myearalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarmId", -1);
        String timeText = intent.getStringExtra("timeText");
        String soundUri = intent.getStringExtra("soundUri");
        boolean repeat = intent.getBooleanExtra("repeat", true);
        boolean isTimer = intent.getBooleanExtra("isTimer", false);

        Intent alertIntent = new Intent(context, AlarmAlertActivity.class);
        alertIntent.putExtra("alarmId", alarmId);
        alertIntent.putExtra("timeText", timeText);
        alertIntent.putExtra("soundUri", soundUri);
        alertIntent.putExtra("repeat", repeat);
        alertIntent.putExtra("isTimer", isTimer);

        alertIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        context.startActivity(alertIntent);
    }
}


