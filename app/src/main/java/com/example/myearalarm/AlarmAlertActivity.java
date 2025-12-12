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
    private int timerIndex = -1;
    private boolean isTimer = false;
    private boolean hasRepeat = false;
    private String soundUriStr;
    private String timeText;
    private boolean safeMode = false;

    private boolean userStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_alert);

        TextView tvTitle = findViewById(R.id.tvAlertTitle);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnStop = findViewById(R.id.btnStop);

        Intent intent = getIntent();
        alarmId = intent.getIntExtra("alarmId", -1);
        timerIndex = intent.getIntExtra("timerIndex", -1);
        isTimer = intent.getBooleanExtra("isTimer", false);
        hasRepeat = intent.getBooleanExtra("repeat", false);
        soundUriStr = intent.getStringExtra("soundUri");
        timeText = intent.getStringExtra("timeText");
        safeMode = intent.getBooleanExtra("safeMode", false);


        if (timeText != null && !timeText.isEmpty()) {
            tvTitle.setText("알람 (" + timeText + ")");
        } else {
            tvTitle.setText("알람");
        }
        if (safeMode && !isEarphoneOutputConnected()) {
            returnToMainAfterStopDelete();
            finish();
            return;
        }


        startRingtone();

        if (safeMode) {
            handler.post(safeModeWatch);
        }


        autoStopRunnable = () -> {
            stopRingtoneIfNeeded();

            if (!userStopped && hasRepeat) {
                long snoozeEndTime = scheduleSnooze();
                returnToMainAfterSnooze(snoozeEndTime);
            } else {
                returnToMainNormal();
            }
            finish();
        };

        handler.postDelayed(autoStopRunnable, 60_000L);

        btnStop.setOnClickListener(v -> {
            userStopped = true;
            handler.removeCallbacks(autoStopRunnable);
            stopRingtoneIfNeeded();
            returnToMainAfterStopDelete();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            handler.removeCallbacks(autoStopRunnable);
            stopRingtoneIfNeeded();
            long snoozeEndTime = scheduleSnooze();
            returnToMainAfterSnooze(snoozeEndTime);
            finish();
        });
    }
    private boolean isEarphoneOutputConnected() {
        android.media.AudioManager am =
                (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.media.AudioDeviceInfo[] outs =
                    am.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS);

            for (android.media.AudioDeviceInfo d : outs) {
                int t = d.getType();
                if (t == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || t == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || t == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
                        || t == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || t == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true;
                }
            }
            return false;
        } else {
            return am.isWiredHeadsetOn() || am.isBluetoothA2dpOn() || am.isBluetoothScoOn();
        }
    }
    private final Runnable safeModeWatch = new Runnable() {
        @Override public void run() {
            if (safeMode && !isEarphoneOutputConnected()) {
                stopRingtoneIfNeeded();
                returnToMainAfterStopDelete();
                finish();
                return;
            }
            handler.postDelayed(this, 1000L);
        }
    };



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

    private long scheduleSnooze() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return 0L;

        int requestCode = (alarmId != -1) ? alarmId : (int) System.currentTimeMillis();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("alarmId", requestCode);
        intent.putExtra("timeText", timeText);
        intent.putExtra("soundUri", soundUriStr);
        intent.putExtra("repeat", hasRepeat);
        intent.putExtra("isTimer", isTimer);
        intent.putExtra("timerIndex", timerIndex);
        intent.putExtra("safeMode", safeMode);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
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
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        } catch (SecurityException e) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        return triggerAtMillis;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoStopRunnable);
        handler.removeCallbacks(safeModeWatch);
        stopRingtoneIfNeeded();
    }

    private void returnToMainNormal() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(mainIntent);
    }

    private void returnToMainAfterStopDelete() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        mainIntent.putExtra("deleteFromAlert", true);
        mainIntent.putExtra("isTimer", isTimer);
        mainIntent.putExtra("timeText", timeText);
        mainIntent.putExtra("timerIndex", timerIndex);
        startActivity(mainIntent);
    }

    private void returnToMainAfterSnooze(long snoozeEndTime) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        mainIntent.putExtra("updateSnoozeUI", true);
        mainIntent.putExtra("isTimer", isTimer);
        mainIntent.putExtra("timeText", timeText);
        mainIntent.putExtra("timerIndex", timerIndex);
        mainIntent.putExtra("snoozeEndTime", snoozeEndTime);
        startActivity(mainIntent);
    }
}
