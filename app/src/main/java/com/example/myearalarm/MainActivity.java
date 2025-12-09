package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ADD_CLOCK = 1001;
    private static final int REQ_ADD_TIMER = 1002;
    private ImageButton btnAddClockAlarm;
    private ImageButton btnAddTimerAlarm;
    private ListView listClockAlarm;
    private ListView listTimerAlarm;
    private ArrayList<String> clockAlarms = new ArrayList<>();
    private ArrayList<String> timerAlarms = new ArrayList<>();
    private ArrayAdapter<String> clockAdapter;
    private ArrayAdapter<String> timerAdapter;
    private ArrayList<Long> timerRemainingMillis = new ArrayList<>();
    private ArrayList<android.os.CountDownTimer> timerTimers = new ArrayList<>();

    private void addNewTimer(long totalMillis, String initialText) {
        timerAlarms.add(initialText);
        timerRemainingMillis.add(totalMillis);

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

                Toast.makeText(MainActivity.this,
                        "타이머 알람이 종료되었습니다.", Toast.LENGTH_SHORT).show();
            }
        };

        timerTimers.add(timer);
        timer.start();
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

        clockAdapter = new ArrayAdapter<>(
                this, R.layout.item_clock_alarm,
                R.id.btnClockAlarmItem, clockAlarms
        );
        listClockAlarm.setAdapter(clockAdapter);

        timerAdapter = new ArrayAdapter<>(
                this, R.layout.item_timer_alarm,
                R.id.btnTimerAlarmItem, timerAlarms
        );
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
            startActivityForResult(intent, REQ_ADD_TIMER);
        });
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
            if (displayText == null) {
                int ampm = data.getIntExtra("ampm", 0);
                int hour = data.getIntExtra("hour", 0);
                int minute = data.getIntExtra("minute", 0);
                String ampmStr = (ampm == 0) ? "오전" : "오후";
                displayText = String.format(Locale.getDefault(),
                        "%s %02d:%02d", ampmStr, hour, minute);
            }

            if (isEdit) {
                if (isDelete) {
                    if (index >= 0 && index < clockAlarms.size()) {
                        clockAlarms.remove(index);
                    }
                } else {
                    if (index >= 0 && index < clockAlarms.size()) {
                        clockAlarms.set(index, displayText);
                    }
                }
            } else {
                clockAlarms.add(displayText);
            }

            clockAdapter.notifyDataSetChanged();

        } else if (requestCode == REQ_ADD_TIMER) {
            int h = data.getIntExtra("hour", 0);
            int m = data.getIntExtra("minute", 0);
            int s = data.getIntExtra("second", 0);

            long totalMillis = (h * 3600 + m * 60 + s) * 1000L;

            if (displayText == null) {
                displayText = String.format(Locale.getDefault(),
                        "%02d시간 %02d분 %02d초", h, m, s);
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
                    if (isDelete) {
                        timerAlarms.remove(index);
                    } else {
                        timerAlarms.remove(index);
                        addNewTimer(totalMillis, displayText);
                    }
                }
            } else {
                addNewTimer(totalMillis, displayText);
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
}