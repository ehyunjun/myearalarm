package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AddClockAlarmActivity extends AppCompatActivity {

    private NumberPicker npAmPm, npHour, npMinute;
    private Button btnCancelClock, btnSetClock, btnDeleteClock;
    private TextView tvClockTitle;
    private boolean isEdit = false;
    private int editIndex=-1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_clock_alarm);

        tvClockTitle = findViewById(R.id.tvClockTitle);
        btnDeleteClock = findViewById(R.id.btnDeleteClock);
        btnSetClock = findViewById(R.id.btnSetClock);
        btnCancelClock = findViewById(R.id.btnCancelClock);

        npAmPm = findViewById(R.id.npAmPm);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);

        setupAmPmPicker(npAmPm);
        setupHourPicker(npHour);
        setupMinutePicker(npMinute);

        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);
        editIndex = intent.getIntExtra("index", -1);

        if (isEdit){
            int ampm = intent.getIntExtra("ampm", 0);
            int hour = intent.getIntExtra("hour", 0);
            int minute = intent.getIntExtra("minute", 0);

            npAmPm.setValue(ampm);
            npHour.setValue(hour);
            npMinute.setValue(minute);

            tvClockTitle.setText("알람 편집");
            btnDeleteClock.setVisibility(View.VISIBLE);
        } else {
            tvClockTitle.setText("시간 알람");
            btnDeleteClock.setVisibility(View.GONE);
        }

        btnDeleteClock.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("isEdit", true);
            result.putExtra("index", editIndex);
            result.putExtra("isDelete", true);
            setResult(RESULT_OK, result);
            finish();
        });

        btnCancelClock.setOnClickListener(v -> finish());

        btnSetClock.setOnClickListener(v -> {
            int ampm = npAmPm.getValue();
            int hour = npHour.getValue();
            int minute = npMinute.getValue();

            String ampmText = (ampm == 0) ? "오전" : "오후";
            String displayText = String.format(Locale.getDefault(),
                    "%s %02d:%02d", ampmText, hour, minute);

            Intent result = new Intent();
            result.putExtra("ampm", ampm);
            result.putExtra("hour", hour);
            result.putExtra("minute", minute);
            result.putExtra("displayText", displayText);

            result.putExtra("isEdit", isEdit);
            result.putExtra("index", editIndex);
            result.putExtra("isDelete", false);

            setResult(RESULT_OK, result);
            finish();
        });
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