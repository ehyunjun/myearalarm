package com.example.myearalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmAlertActivity extends AppCompatActivity {
    private Ringtone ringtone;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoStopRunnable;

    private int alarmId = -1;
    private boolean isTimer = false;
    private boolean hasRepeat = false;
    private String soundUriStr, timeText;

    private boolean userStopped = false;
    private boolean userSnoozed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_alert);

        TextView tvTitle = findViewById(R.id.tvAlertTitle);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnStop = findViewById(R.id.btnStop);

        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        isTimer = intent.getBooleanExtra("isTimer", false);
        hasRepeat = intent.getBooleanExtra("repeat", false);
        soundUriStr = intent.getStringExtra("soundUri");
        timeText = intent.getStringExtra("timeText");

        if (timeText != null && !timeText.isEmpty()) {
            tvTitle.setText("알람 (" + timeText + ")");
        } else {
            tvTitle.setText("알람");
        }
        startRingtone();

        autoStopRunnable = () -> {
            stopRingtoneIfNeeded();

            if (!userStopped && hasRepeat) {
                scheduleSnooze();
            }
            finish();
        };
        handler.postDelayed(autoStopRunnable, 60_000L);

        btnStop.setOnClickListener(v -> {
            userStopped = true;
            handler.removeCallbacks(autoStopRunnable);
            stopRingtoneIfNeeded();
            finish();
        });
        btnSnooze.setOnClickListener(v -> {
            userSnoozed = true;
            handler.removeCallbacks(autoStopRunnable);
            stopRingtoneIfNeeded();
            scheduleSnooze();
            finish();
        });
    }

    private void startRingtone() {
        Uri uri = null;

        if (soundUriStr != null && !soundUriStr.isEmpty()) {
            uri = Uri.parse(soundUriStr);
        }

        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        if (uri != null) {
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (ringtone != null) {
                ringtone.play();
            }
        }
    }

    private void stopRingtoneIfNeeded() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void scheduleSnooze() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int requestCode = (alarmId != -1) ? alarmId : (int) System.currentTimeMillis();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("alarmId", requestCode);
        intent.putExtra("timeText", timeText);
        intent.putExtra("soundUri", soundUriStr);
        intent.putExtra("repeat", hasRepeat);
        intent.putExtra("isTimer", isTimer);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoStopRunnable);
        stopRingtoneIfNeeded();
    }
}
