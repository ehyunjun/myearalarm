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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ADD_CLOCK = 1001;
    private static final int REQ_ADD_TIMER = 1002;

    private ImageButton btnAddClockAlarm;
    private ImageButton btnAddTimerAlarm;
    private ListView listClockAlarm;
    private ListView listTimerAlarm;

    private final ArrayList<String> clockAlarms = new ArrayList<>();
    private final ArrayList<String> timerAlarms = new ArrayList<>();

    private final ArrayList<Long> timerRemainingMillis = new ArrayList<>();
    private final ArrayList<CountDownTimer> timerTimers = new ArrayList<>();
    private final ArrayList<String> timerSoundUris = new ArrayList<>();
    private final ArrayList<String> clockSoundUris = new ArrayList<>();

    private final ArrayList<Integer> clockAlarmIds = new ArrayList<>();

    private final ArrayList<Boolean> clockRepeatFlags = new ArrayList<>();
    private final ArrayList<Boolean> timerRepeatFlags = new ArrayList<>();

    private final ArrayList<Long> clockSnoozeEndTimes = new ArrayList<>();
    private final ArrayList<Long> timerSnoozeEndTimes = new ArrayList<>();

    private ArrayAdapter<String> clockAdapter;
    private ArrayAdapter<String> timerAdapter;

    private final Handler snoozeUiHandler = new Handler(Looper.getMainLooper());
    private final Runnable snoozeUiRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            boolean anyActive = false;

            for (int i = 0; i < clockSnoozeEndTimes.size(); i++) {
                if (clockSnoozeEndTimes.get(i) > now) {
                    anyActive = true;
                    break;
                }
            }
            if (!anyActive) {
                for (int i = 0; i < timerSnoozeEndTimes.size(); i++) {
                    if (timerSnoozeEndTimes.get(i) > now) {
                        anyActive = true;
                        break;
                    }
                }
            }

            if (anyActive) {
                if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
                if (timerAdapter != null) timerAdapter.notifyDataSetChanged();
                snoozeUiHandler.postDelayed(this, 1000L);
            }
        }
    };

    private void addNewTimer(long totalMillis, String initialText, String soundUri, boolean repeat) {
        timerAlarms.add(initialText);
        timerRemainingMillis.add(totalMillis);
        timerSoundUris.add(soundUri);
        timerRepeatFlags.add(repeat);
        timerSnoozeEndTimes.add(0L);

        CountDownTimer timer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int idx = timerTimers.indexOf(this);
                if (idx == -1) return;

                timerRemainingMillis.set(idx, millisUntilFinished);

                int totalSec = (int) (millisUntilFinished / 1000);
                int h = totalSec / 3600;
                int m = (totalSec % 3600) / 60;
                int s = totalSec % 60;

                String text = String.format(Locale.getDefault(),
                        "%02d시간 %02d분 %02d초", h, m, s);
                timerAlarms.set(idx, text);
                timerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFinish() {
                int idx = timerTimers.indexOf(this);
                if (idx == -1) return;

                timerRemainingMillis.set(idx, 0L);
                String text = String.format(Locale.getDefault(),
                        "%02d시간 %02d분 %02d초", 0, 0, 0);
                timerAlarms.set(idx, text);
                timerAdapter.notifyDataSetChanged();

                Intent alertIntent = new Intent(MainActivity.this, AlarmAlertActivity.class);
                alertIntent.putExtra("alarmId", -1);
                alertIntent.putExtra("timeText", initialText);
                String uriForThisTimer =
                        (idx < timerSoundUris.size()) ? timerSoundUris.get(idx) : null;
                alertIntent.putExtra("soundUri", uriForThisTimer);
                boolean repeatFlag =
                        (idx < timerRepeatFlags.size() && Boolean.TRUE.equals(timerRepeatFlags.get(idx)));
                alertIntent.putExtra("repeat", repeatFlag);
                alertIntent.putExtra("isTimer", true);

                alertIntent.putExtra("timerIndex", idx);
                startActivity(alertIntent);
            }
        };

        timerTimers.add(timer);
        timer.start();
    }

    private long calculateTriggerAtMillis(int ampm, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = Calendar.getInstance();

        int hour24 = hour;

        if (ampm == 0) {
            if (hour24 == 12) hour24 = 0;
        } else {
            if (hour24 != 12) hour24 += 12;
        }

        alarmTime.set(Calendar.HOUR_OF_DAY, hour24);
        alarmTime.set(Calendar.MINUTE, minute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DATE, 1);
        }

        return alarmTime.getTimeInMillis();
    }

    private void scheduleClockAlarm(long triggerAtMillis, int alarmId,
                                    String displayText, String soundUri,
                                    boolean repeat) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("alarmId", alarmId);
        intent.putExtra("timeText", displayText);
        intent.putExtra("soundUri", soundUri);
        intent.putExtra("repeat", repeat);
        intent.putExtra("isTimer", false);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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
    }

    private void cancelClockAlarm(int alarmId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnAddClockAlarm = findViewById(R.id.btnAddClockAlarm);
        btnAddTimerAlarm = findViewById(R.id.btnAddTimerAlarm);
        listClockAlarm = findViewById(R.id.listClockAlarm);
        listTimerAlarm = findViewById(R.id.listTimerAlarm);

        btnAddClockAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddClockAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_CLOCK);
        });

        btnAddTimerAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_TIMER);
        });

        clockAdapter = new ClockAlarmAdapter(this, clockAlarms);
        listClockAlarm.setAdapter(clockAdapter);

        timerAdapter = new TimerAlarmAdapter(this, timerAlarms);
        listTimerAlarm.setAdapter(timerAdapter);

        listClockAlarm.setOnItemClickListener((parent, view, position, id) -> {
            String display = clockAlarms.get(position);
            int[] values = parseClockDisplay(display);

            Intent intent = new Intent(MainActivity.this, AddClockAlarmActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("index", position);
            intent.putExtra("ampm", values[0]);
            intent.putExtra("hour", values[1]);
            intent.putExtra("minute", values[2]);

            if (position < clockSoundUris.size()) {
                String soundUri = clockSoundUris.get(position);
                if (soundUri != null && !soundUri.isEmpty()) {
                    intent.putExtra("soundUri", soundUri);
                }
            }

            if (position < clockRepeatFlags.size()) {
                boolean repeat = clockRepeatFlags.get(position);
                intent.putExtra("repeat", repeat);
            }
            startActivityForResult(intent, REQ_ADD_CLOCK);
        });

        listTimerAlarm.setOnItemClickListener((parent, view, position, id) -> {
            String display = timerAlarms.get(position);
            int[] values = parseTimerDisplay(display);

            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("hour", values[0]);
            intent.putExtra("minute", values[1]);
            intent.putExtra("second", values[2]);
            intent.putExtra("index", position);

            if (position < timerSoundUris.size()) {
                String soundUri = timerSoundUris.get(position);
                if (soundUri != null && !soundUri.isEmpty()) {
                    intent.putExtra("soundUri", soundUri);
                }
            }

            if (position < timerRepeatFlags.size()) {
                boolean repeat = timerRepeatFlags.get(position);
                intent.putExtra("repeat", repeat);
            }

            startActivityForResult(intent, REQ_ADD_TIMER);
        });

        handleAlertResultIntent(getIntent());
    }

    private void handleAlertResultIntent(Intent intent) {
        if (intent == null) return;

        if (intent.getBooleanExtra("deleteFromAlert", false)) {
            boolean isTimer = intent.getBooleanExtra("isTimer", false);
            String timeText = intent.getStringExtra("timeText");
            int timerIndexFromIntent = intent.getIntExtra("timerIndex", -1);

            if (isTimer) {
                int idx = timerIndexFromIntent;

                if (idx < 0 || idx >= timerAlarms.size()) {
                    if (timeText != null) {
                        idx = timerAlarms.indexOf(timeText);
                    } else {
                        idx = -1;
                    }
                }

                if (idx >= 0) {
                    if (idx < timerTimers.size()) {
                        CountDownTimer old = timerTimers.get(idx);
                        if (old != null) old.cancel();
                        timerTimers.remove(idx);
                    }
                    if (idx < timerRemainingMillis.size()) {
                        timerRemainingMillis.remove(idx);
                    }
                    if (idx < timerSoundUris.size()) {
                        timerSoundUris.remove(idx);
                    }
                    if (idx < timerRepeatFlags.size()) {
                        timerRepeatFlags.remove(idx);
                    }
                    if (idx < timerSnoozeEndTimes.size()) {
                        timerSnoozeEndTimes.remove(idx);
                    }

                    timerAlarms.remove(idx);
                    timerAdapter.notifyDataSetChanged();
                }
            } else if (timeText != null) {
                int idx = clockAlarms.indexOf(timeText);
                if (idx >= 0) {
                    if (idx < clockAlarmIds.size()) {
                        cancelClockAlarm(clockAlarmIds.get(idx));
                        clockAlarmIds.remove(idx);
                    }
                    if (idx < clockSoundUris.size()) {
                        clockSoundUris.remove(idx);
                    }
                    if (idx < clockRepeatFlags.size()) {
                        clockRepeatFlags.remove(idx);
                    }
                    if (idx < clockSnoozeEndTimes.size()) {
                        clockSnoozeEndTimes.remove(idx);
                    }

                    clockAlarms.remove(idx);
                    clockAdapter.notifyDataSetChanged();
                }
            }

            intent.removeExtra("deleteFromAlert");
        }

        if (intent.getBooleanExtra("updateSnoozeUI", false)) {
            boolean isTimer = intent.getBooleanExtra("isTimer", false);
            String timeText = intent.getStringExtra("timeText");
            long snoozeEndTime = intent.getLongExtra("snoozeEndTime", 0L);
            int timerIndexFromIntent = intent.getIntExtra("timerIndex", -1);

            if (snoozeEndTime > 0L) {
                if (isTimer) {
                    int idx = timerIndexFromIntent;
                    if (idx < 0 || idx >= timerAlarms.size()) {
                        if (timeText != null) {
                            idx = timerAlarms.indexOf(timeText);
                        } else {
                            idx = -1;
                        }
                    }

                    if (idx >= 0) {
                        while (timerSnoozeEndTimes.size() <= idx) {
                            timerSnoozeEndTimes.add(0L);
                        }
                        timerSnoozeEndTimes.set(idx, snoozeEndTime);
                        timerAdapter.notifyDataSetChanged();
                    }
                } else if (timeText != null) {
                    int idx = clockAlarms.indexOf(timeText);
                    if (idx >= 0) {
                        while (clockSnoozeEndTimes.size() <= idx) {
                            clockSnoozeEndTimes.add(0L);
                        }
                        clockSnoozeEndTimes.set(idx, snoozeEndTime);
                        clockAdapter.notifyDataSetChanged();
                    }
                }

                startSnoozeUiLoop();
            }

            intent.removeExtra("updateSnoozeUI");
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAlertResultIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        boolean isEdit = data.getBooleanExtra("isEdit", false);
        boolean isDelete = data.getBooleanExtra("isDelete", false);
        int index = data.getIntExtra("index", -1);
        String displayText = data.getStringExtra("displayText");

        if (requestCode == REQ_ADD_CLOCK) {

            String soundUri = data.getStringExtra("soundUri");
            boolean repeat = data.getBooleanExtra("repeat", true);

            if (displayText == null) {
                int ampmExtra = data.getIntExtra("ampm", 0);
                int hourExtra = data.getIntExtra("hour", 0);
                int minuteExtra = data.getIntExtra("minute", 0);
                String ampmStr = (ampmExtra == 0) ? "오전" : "오후";
                displayText = String.format(
                        Locale.getDefault(), "%s %02d:%02d", ampmStr, hourExtra, minuteExtra);
            }

            long triggerAtMillis = data.getLongExtra("alarmTimeMillis", -1);

            int ampm = data.getIntExtra("ampm", -1);
            int hour = data.getIntExtra("hour", -1);
            int minute = data.getIntExtra("minute", -1);

            if (ampm == -1 || hour == -1 || minute == -1) {
                int[] vals = parseClockDisplay(displayText);
                ampm = vals[0];
                hour = vals[1];
                minute = vals[2];
            }

            if (triggerAtMillis <= 0) {
                triggerAtMillis = calculateTriggerAtMillis(ampm, hour, minute);
            }

            if (isEdit) {
                if (index >= 0 && index < clockAlarms.size()) {
                    if (index < clockAlarmIds.size()) {
                        int oldId = clockAlarmIds.get(index);
                        cancelClockAlarm(oldId);
                    }

                    if (isDelete) {
                        clockAlarms.remove(index);
                        if (index < clockSoundUris.size()) {
                            clockSoundUris.remove(index);
                        }
                        if (index < clockAlarmIds.size()) {
                            clockAlarmIds.remove(index);
                        }
                        if (index < clockRepeatFlags.size()) {
                            clockRepeatFlags.remove(index);
                        }
                        if (index < clockSnoozeEndTimes.size()) {
                            clockSnoozeEndTimes.remove(index);
                        }

                    } else {
                        clockAlarms.set(index, displayText);

                        if (soundUri != null && !soundUri.isEmpty()) {
                            if (index < clockSoundUris.size()) {
                                clockSoundUris.set(index, soundUri);
                            } else {
                                clockSoundUris.add(soundUri);
                            }
                        } else {
                            if (index >= clockSoundUris.size()) {
                                clockSoundUris.add("");
                            }
                        }
                        if (index < clockRepeatFlags.size()) {
                            clockRepeatFlags.set(index, repeat);
                        } else {
                            clockRepeatFlags.add(repeat);
                        }

                        int newAlarmId = (int) System.currentTimeMillis();
                        if (index < clockAlarmIds.size()) {
                            clockAlarmIds.set(index, newAlarmId);
                        } else {
                            clockAlarmIds.add(newAlarmId);
                        }

                        scheduleClockAlarm(triggerAtMillis, newAlarmId, displayText, soundUri, repeat);
                    }
                }

            } else {
                clockAlarms.add(displayText);
                clockSoundUris.add(soundUri);
                clockRepeatFlags.add(repeat);
                clockSnoozeEndTimes.add(0L);

                int alarmId = (int) System.currentTimeMillis();
                clockAlarmIds.add(alarmId);

                scheduleClockAlarm(triggerAtMillis, alarmId, displayText, soundUri, repeat);
            }

            clockAdapter.notifyDataSetChanged();

        } else if (requestCode == REQ_ADD_TIMER) {

            int h = data.getIntExtra("hour", 0);
            int m = data.getIntExtra("minute", 0);
            int s = data.getIntExtra("second", 0);
            String soundUri = data.getStringExtra("soundUri");
            boolean repeat = data.getBooleanExtra("repeat", true);

            long totalMillis = (h * 3600L + m * 60L + s) * 1000L;

            if (displayText == null) {
                displayText = String.format(
                        Locale.getDefault(), "%02d시간 %02d분 %02d초", h, m, s);
            }

            if (isEdit) {
                if (index >= 0 && index < timerAlarms.size()) {
                    if (index < timerTimers.size()) {
                        CountDownTimer oldTimer = timerTimers.get(index);
                        if (oldTimer != null) oldTimer.cancel();
                        timerTimers.remove(index);
                    }
                    if (index < timerRemainingMillis.size()) {
                        timerRemainingMillis.remove(index);
                    }
                    if (index < timerSoundUris.size()) {
                        timerSoundUris.remove(index);
                    }
                    if (index < timerRepeatFlags.size()) {
                        timerRepeatFlags.remove(index);
                    }
                    if (index < timerSnoozeEndTimes.size()) {
                        timerSnoozeEndTimes.remove(index);
                    }

                    if (isDelete) {
                        timerAlarms.remove(index);
                    } else {
                        timerAlarms.remove(index);
                        addNewTimer(totalMillis, displayText, soundUri, repeat);
                    }
                }
            } else {
                addNewTimer(totalMillis, displayText, soundUri, repeat);
            }

            timerAdapter.notifyDataSetChanged();
        }
    }

    private int[] parseClockDisplay(String text) {
        int ampm = text.startsWith("오전") ? 0 : 1;

        int spaceIdx = text.indexOf(' ');
        String timePart = text.substring(spaceIdx + 1);
        String[] parts = timePart.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return new int[]{ampm, hour, minute};
    }

    private int[] parseTimerDisplay(String text) {
        String[] tokens = text.split(" ");
        int h = extractNumber(tokens[0]);
        int m = extractNumber(tokens[1]);
        int s = extractNumber(tokens[2]);

        return new int[]{h, m, s};
    }

    private int extractNumber(String token) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return Integer.parseInt(sb.toString());
    }

    private void startSnoozeUiLoop() {
        snoozeUiHandler.removeCallbacks(snoozeUiRunnable);
        snoozeUiHandler.post(snoozeUiRunnable);
    }

    private class ClockAlarmAdapter extends ArrayAdapter<String> {
        private final LayoutInflater inflater;

        ClockAlarmAdapter(Context context, ArrayList<String> list) {
            super(context, 0, list); // ★ 중요: super.getView() 쓰면 안되니까 0
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_clock_alarm, parent, false);
            }

            TextView left = view.findViewById(R.id.tvClockLeft);
            TextView right = view.findViewById(R.id.tvClockRight);

            String baseText = getItem(position);
            long now = System.currentTimeMillis();
            long end = (position < clockSnoozeEndTimes.size()) ? clockSnoozeEndTimes.get(position) : 0L;

            if (end > now) {
                long remainingSec = (end - now + 999) / 1000;
                int m = (int) (remainingSec / 60);
                int s = (int) (remainingSec % 60);

                left.setText(baseText);
                left.setTextColor(ContextCompat.getColor(getContext(), R.color.btn_add_alarm)); // 회색

                right.setVisibility(View.VISIBLE);
                right.setText(String.format(Locale.getDefault(), "미루기 %02d:%02d", m, s));
                right.setTextColor(ContextCompat.getColor(getContext(), R.color.button_text)); // 검정
            } else {
                left.setText(baseText);
                left.setTextColor(ContextCompat.getColor(getContext(), R.color.button_text)); // 검정
                right.setVisibility(View.GONE);
            }

            return view;
        }
    }


    private class TimerAlarmAdapter extends ArrayAdapter<String> {
        private final LayoutInflater inflater;

        TimerAlarmAdapter(Context context, ArrayList<String> list) {
            super(context, 0, list); // ★ 중요
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_timer_alarm, parent, false);
            }

            TextView left = view.findViewById(R.id.tvTimerLeft);
            TextView right = view.findViewById(R.id.tvTimerRight);

            String baseText = getItem(position);
            long now = System.currentTimeMillis();
            long end = (position < timerSnoozeEndTimes.size()) ? timerSnoozeEndTimes.get(position) : 0L;

            if (end > now) {
                long remainingSec = (end - now + 999) / 1000;
                int m = (int) (remainingSec / 60);
                int s = (int) (remainingSec % 60);

                left.setText(baseText);
                left.setTextColor(ContextCompat.getColor(getContext(), R.color.btn_add_alarm)); // 회색

                right.setVisibility(View.VISIBLE);
                right.setText(String.format(Locale.getDefault(), "미루기 %02d:%02d", m, s));
                right.setTextColor(ContextCompat.getColor(getContext(), R.color.button_text)); // 검정
            } else {
                left.setText(baseText);
                left.setTextColor(ContextCompat.getColor(getContext(), R.color.button_text)); // 검정
                right.setVisibility(View.GONE);
            }

            return view;
        }
    }
}
