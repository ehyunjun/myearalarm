package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ADD_CLOCK = 1001;
    private static final int REQ_ADD_TIMER = 1002;
    private ImageButton btnAddClockAlarm;
    private ImageButton btnTimerAlarm;


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
        btnTimerAlarm = findViewById(R.id.btnTimerAlarm);

        btnAddClockAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddClockAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_CLOCK);
        });

        btnTimerAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_TIMER);
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQ_ADD_CLOCK) {
            int ampm = data.getIntExtra("ampm", 0);
            int hour = data.getIntExtra("hour", 0);
            int minute = data.getIntExtra("minute", 0);
            String ampmStr = (ampm == 0) ? "오전" : "오후";
            String msg = String.format(Locale.getDefault(),
                    "저장된 시간 알람 : %s %02d:%02d", ampmStr, hour, minute);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_ADD_TIMER) {
            int h = data.getIntExtra("hour", 0);
            int m = data.getIntExtra("minute", 0);
            int s = data.getIntExtra("second", 0);
            String msg = String.format(Locale.getDefault(),
                    "저장된 타이머 : %02d시간 %02d분 %02d초", h, m, s);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        }

    }
}