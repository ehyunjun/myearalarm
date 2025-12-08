package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AddClockAlarmActivity extends AppCompatActivity {

    private NumberPicker npAmPm;
    private NumberPicker npHour;
    private NumberPicker npMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_clock_alarm);

        npAmPm = findViewById(R.id.npAmPm);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);

        Button btnSetClock = findViewById(R.id.btnSetClock);
        Button btnCancelClock = findViewById(R.id.btnCancelClock);

        btnCancelClock.setOnClickListener(v -> {
            finish();
        });

        btnSetClock.setOnClickListener(v -> {
            int ampm = npAmPm.getValue();
            int hour = npHour.getValue();
            int minute = npMinute.getValue();
            Intent result = new Intent();
            result.putExtra("ampm", ampm);
            result.putExtra("hour", hour);
            result.putExtra("minute", minute);
            setResult(RESULT_OK, result);
            finish();
        });

        setupAmPmPicker(npAmPm);
        setupHourPicker(npHour);
        setupMinutePicker(npMinute);

        npAmPm.setValue(0);
        npHour.setValue(0);
        npMinute.setValue(0);
    }

    private void setupAmPmPicker(NumberPicker picker) {
        picker.setMinValue(0);
        picker.setMaxValue(1);
        picker.setDisplayedValues(new String[]{"오전", "오후"});
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    }

    private void setupHourPicker(NumberPicker picker) {
        picker.setMinValue(1);
        picker.setMaxValue(12);
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    }

    private void setupMinutePicker(NumberPicker picker) {
        picker.setMinValue(0);
        picker.setMaxValue(59);
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setFormatter(new NumberPicker.Formatter(){
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%02d", value);
            }
        });
    }
}