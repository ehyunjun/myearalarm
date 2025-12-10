package com.example.myearalarm;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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
    private TextView tvTimerTitle, tvSound;
    private boolean isEdit = false;
    private int editIndex = -1;
    private String selectedSoundUri;
    private static final int REQ_PICK_SOUND = 2001;

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

        tvSound = findViewById(R.id.tvSound);
        View rowSound = findViewById(R.id.rowSound);


        setupNumberPickers(npHour, 0, 23);
        setupNumberPickers(npMinute, 0, 59);
        setupNumberPickers(npSecond, 0, 59);

        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);
        editIndex = intent.getIntExtra("index", -1);

        String defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                .toString();
        selectedSoundUri = defaultUri;

        String soundFromIntent = intent.getStringExtra("soundUri");
        if (soundFromIntent != null) {
            selectedSoundUri = soundFromIntent;
        }

        updateSoundTitle();

        rowSound.setOnClickListener(v -> openSoundPicker());

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

            result.putExtra("soundUri", selectedSoundUri);

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

    private void openSoundPicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

        if (selectedSoundUri != null) {
            Uri existing = Uri.parse(selectedSoundUri);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        }

        startActivityForResult(intent, REQ_PICK_SOUND);
    }
    private void updateSoundTitle() {
        if (tvSound == null) return;

        if (selectedSoundUri == null) {
            tvSound.setText("기본 알람음");
            return;
        }

        Uri uri = Uri.parse(selectedSoundUri);
        Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone != null) {
            tvSound.setText(ringtone.getTitle(this));
        } else {
            tvSound.setText("알람음 없음");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_SOUND && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedSoundUri = uri.toString();
                updateSoundTitle();
            }
        }
    }
}
