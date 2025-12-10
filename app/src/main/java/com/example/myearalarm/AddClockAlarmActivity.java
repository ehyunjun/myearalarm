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
import androidx.appcompat.widget.SwitchCompat;

import java.util.Locale;

public class AddClockAlarmActivity extends AppCompatActivity {

    private NumberPicker npAmPm, npHour, npMinute;
    private Button btnCancelClock, btnSetClock, btnDeleteClock;
    private TextView tvClockTitle, tvSound;
    private SwitchCompat switchRepeat, switchSafeMode;

    private boolean isEdit = false;
    private int editIndex=-1;
    private String selectedSoundUri;
    private static final int REQ_PICK_SOUND = 2001;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_clock_alarm);

        tvClockTitle = findViewById(R.id.tvClockTitle);
        btnDeleteClock = findViewById(R.id.btnDeleteClock);
        btnSetClock = findViewById(R.id.btnSetClock);
        btnCancelClock = findViewById(R.id.btnCancelClock);
        switchRepeat = findViewById(R.id.switchRepeat);
        switchSafeMode = findViewById(R.id.switchSafeMode);


        npAmPm = findViewById(R.id.npAmPm);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);

        tvSound = findViewById(R.id.tvSound);
        View rowSound = findViewById(R.id.rowSound);

        setupAmPmPicker(npAmPm);
        setupHourPicker(npHour);
        setupMinutePicker(npMinute);

        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);
        editIndex = intent.getIntExtra("index", -1);

        boolean repeatFromIntent = intent.getBooleanExtra("repeat", true);
        switchRepeat.setChecked(repeatFromIntent);

        String defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                .toString();
        selectedSoundUri = defaultUri;

        String soundFromIntent = intent.getStringExtra("soundUri");
        if (soundFromIntent != null) {
            selectedSoundUri = soundFromIntent;
        }

        updateSoundTitle();

        rowSound.setOnClickListener(v -> openSoundPicker());

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
            result.putExtra("repeat", switchRepeat.isChecked());
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

            boolean repeat = switchRepeat.isChecked();

            Intent result = new Intent();
            result.putExtra("ampm", ampm);
            result.putExtra("hour", hour);
            result.putExtra("minute", minute);
            result.putExtra("displayText", displayText);

            result.putExtra("soundUri", selectedSoundUri);

            result.putExtra("isEdit", isEdit);
            result.putExtra("index", editIndex);
            result.putExtra("isDelete", false);

            result.putExtra("repeat", repeat);


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
