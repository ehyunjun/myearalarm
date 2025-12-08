package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AddTimerAlarmActivity extends AppCompatActivity {
    private NumberPicker npHour;
    private NumberPicker npMinute;
    private NumberPicker npSecond;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timer_alarm);

        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);
        npSecond = findViewById(R.id.npSecond);

        Button btnSetTimer = findViewById(R.id.btnSetTimer);
        Button btnCancelTimer = findViewById(R.id.btnCancelTimer);

        btnCancelTimer.setOnClickListener(v -> {
            finish();
        });

        btnSetTimer.setOnClickListener(v -> {
            int h = npHour.getValue();
            int m = npMinute.getValue();
            int s = npSecond.getValue();
            Intent result = new Intent();
            result.putExtra("hour", h);
            result.putExtra("minute", m);
            result.putExtra("second", s);
            setResult(RESULT_OK, result);
            finish();
        });

        setupNumberPickers(npHour, 0, 23);
        setupNumberPickers(npMinute, 0, 59);
        setupNumberPickers(npSecond, 0, 59);

        npHour.setValue(0);
        npMinute.setValue(5);
        npSecond.setValue(0);
    }

    private void setupNumberPickers(NumberPicker picker, int min, int max) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setWrapSelectorWheel(true);

        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%02d", value);
            }
        });
    }


}
