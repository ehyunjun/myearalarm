package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AddTimerAlarmActivity extends AppCompatActivity {
    private NumberPicker npHour, npMinute, npSecond;
    private Button btnCancelTimer, btnSetTimer, btnDeleteTimer;
    private TextView tvTimerTitle;
    private boolean isEdit = false;
    private int editIndex = -1;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timer_alarm);

        tvTimerTitle = findViewById(R.id.tvTimerTitle);
        btnDeleteTimer = findViewById(R.id.btnDeleteTimer);
        btnSetTimer = findViewById(R.id.btnSetTimer);
        btnCancelTimer = findViewById(R.id.btnCancelTimer);

        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);
        npSecond = findViewById(R.id.npSecond);

        setupNumberPickers(npHour, 0, 23);
        setupNumberPickers(npMinute, 0, 59);
        setupNumberPickers(npSecond, 0, 59);

        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);
        editIndex = intent.getIntExtra("index", -1);

        if (isEdit) {
            int h = intent.getIntExtra("hour", 0);
            int m = intent.getIntExtra("minute", 0);
            int s = intent.getIntExtra("second", 0);

            npHour.setValue(h);
            npMinute.setValue(m);
            npSecond.setValue(s);

            tvTimerTitle.setText("알람 편집");
            btnDeleteTimer.setVisibility(View.VISIBLE);
        } else {
            npHour.setValue(0);
            npMinute.setValue(5);
            npSecond.setValue(0);

            tvTimerTitle.setText("타이머 알람");
            btnDeleteTimer.setVisibility(View.GONE);
        }

        btnDeleteTimer.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("isEdit", true);
            result.putExtra("index", editIndex);
            result.putExtra("isDelete", true);

            setResult(RESULT_OK, result);
            finish();
        });

        btnCancelTimer.setOnClickListener(v -> finish());

        btnSetTimer.setOnClickListener(v -> {
            int h = npHour.getValue();
            int m = npMinute.getValue();
            int s = npSecond.getValue();

            String displayText = String.format(Locale.getDefault(),
                    "%02d시간 %02d분 %02d초", h, m, s);

            Intent result = new Intent();
            result.putExtra("hour", h);
            result.putExtra("minute", m);
            result.putExtra("second", s);
            result.putExtra("displayText", displayText);

            result.putExtra("isEdit", isEdit);
            result.putExtra("index", editIndex);
            result.putExtra("isDelete", false);

            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void setupNumberPickers(NumberPicker picker, int min, int max) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setWrapSelectorWheel(true);

        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
    }
}
