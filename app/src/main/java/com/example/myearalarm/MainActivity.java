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
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ADD_CLOCK = 1001;
    private static final int REQ_ADD_TIMER = 1002;

    private ImageButton btnAddClockAlarm;
    private ImageButton btnAddTimerAlarm;
    private ListView listClockAlarm;
    private ListView listTimerAlarm;

    // 리스트 데이터
    private final ArrayList<String> clockAlarms = new ArrayList<>();
    private final ArrayList<String> timerAlarms = new ArrayList<>();

    // 각 타이머의 남은 시간 / 타이머 객체 / 타이머 알람 사운드 URI / 시간 알람 사운드 URI
    private final ArrayList<Long> timerRemainingMillis = new ArrayList<>();
    private final ArrayList<CountDownTimer> timerTimers = new ArrayList<>();
    private final ArrayList<String> timerSoundUris = new ArrayList<>();
    private final ArrayList<String> clockSoundUris = new ArrayList<>();

    // 시간 알람마다 고유 알람 ID 저장
    private final ArrayList<Integer> clockAlarmIds = new ArrayList<>();


    private ArrayAdapter<String> clockAdapter;
    private ArrayAdapter<String> timerAdapter;

    // 새 타이머 추가 + CountDownTimer 시작
    private void addNewTimer(long totalMillis, String initialText, String soundUri) {
        timerAlarms.add(initialText);
        timerRemainingMillis.add(totalMillis);
        timerSoundUris.add(soundUri);    // 사운드도 같이 저장

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

                playerTimerAlarmSound(idx);
            }
        };

        timerTimers.add(timer);
        timer.start();
    }
    // ampm: 0=오전, 1=오후  / hour: 1~12 기준이라고 가정
    private long calculateTriggerAtMillis(int ampm, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = Calendar.getInstance();

        // 12시간제를 24시간제로 변환
        int hour24 = hour;

        if (ampm == 0) { // 오전
            if (hour24 == 12) hour24 = 0;   // 오전 12시는 0시
        } else {           // 오후
            if (hour24 != 12) hour24 += 12; // 오후 1~11시는 13~23시
            // 오후 12시는 그대로 12시
        }

        alarmTime.set(Calendar.HOUR_OF_DAY, hour24);
        alarmTime.set(Calendar.MINUTE, minute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        // 이미 지난 시간이면 내일로 밀기
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DATE, 1);
        }

        return alarmTime.getTimeInMillis();
    }
    // 특정 시간에 시간 알람 예약
    // 특정 시간에 시간 알람 예약
    private void scheduleClockAlarm(long triggerAtMillis, int alarmId,
                                    String displayText, String soundUri) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("alarmId", alarmId);
        intent.putExtra("timeText", displayText);
        intent.putExtra("soundUri", soundUri);

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
                    // 권한 없으면 그냥 set 사용
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
            // 마지막 안전망
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

    // 타이머가 끝났을 때 인덱스로 사운드를 재생
    private void playerTimerAlarmSound(int index) {
        if (index < 0 || index >= timerSoundUris.size()) {
            return;
        }

        String uriString = timerSoundUris.get(index);
        Uri uri = null;

        // 사용자가 선택한 사운드 URI가 있으면 사용
        if (uriString != null && !uriString.isEmpty()) {
            uri = Uri.parse(uriString);
        }

        // 없으면 기본 알람음 사용
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        if (uri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (ringtone !=null) {
                ringtone.play();
            }
        }
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
        listClockAlarm   = findViewById(R.id.listClockAlarm);
        listTimerAlarm   = findViewById(R.id.listTimerAlarm);

        // + 버튼들
        btnAddClockAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddClockAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_CLOCK);
        });

        btnAddTimerAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            startActivityForResult(intent, REQ_ADD_TIMER);
        });

        // 어댑터
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

        // 시간 알람 아이템 클릭 -> 편집 모드로 열기
        listClockAlarm.setOnItemClickListener((parent, view, position, id) -> {
            String display = clockAlarms.get(position);
            int[] values = parseClockDisplay(display);

            Intent intent = new Intent(MainActivity.this, AddClockAlarmActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("index", position);
            intent.putExtra("ampm", values[0]);
            intent.putExtra("hour", values[1]);
            intent.putExtra("minute", values[2]);

            // 이 시간 알람의 사운드 URI도 같이 넘김
            if (position < clockSoundUris.size()) {
                String soundUri = clockSoundUris.get(position);
                if (soundUri != null && !soundUri.isEmpty()) {
                    intent.putExtra("soundUri", soundUri);
                }
            }

            startActivityForResult(intent, REQ_ADD_CLOCK);
        });

        // 타이머 알람 아이템 클릭 -> 편집 모드로 열기
        listTimerAlarm.setOnItemClickListener((parent, view, position, id) -> {
            String display = timerAlarms.get(position);
            int[] values = parseTimerDisplay(display);

            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("hour", values[0]);
            intent.putExtra("minute", values[1]);
            intent.putExtra("second", values[2]);
            intent.putExtra("index", position);

            // 이 타이머 알람의 사운드 URI 도 같이 넘김
            if (position < timerSoundUris.size()) {
                String soundUri = timerSoundUris.get(position);
                if (soundUri != null && !soundUri.isEmpty()) {
                    intent.putExtra("soundUri", soundUri);
                }
            }

            startActivityForResult(intent, REQ_ADD_TIMER);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        boolean isEdit   = data.getBooleanExtra("isEdit", false);
        boolean isDelete = data.getBooleanExtra("isDelete", false);
        int index        = data.getIntExtra("index", -1);
        String displayText = data.getStringExtra("displayText");

        // 1) 시간 알람 (시계 알람)
        if (requestCode == REQ_ADD_CLOCK) {

            // 사운드 URI
            String soundUri = data.getStringExtra("soundUri");

            // 표시용 텍스트가 없으면 ampm/hour/minute 로 만들어주기
            if (displayText == null) {
                int ampmExtra   = data.getIntExtra("ampm", 0);   // 0=오전, 1=오후
                int hourExtra   = data.getIntExtra("hour", 0);
                int minuteExtra = data.getIntExtra("minute", 0);
                String ampmStr = (ampmExtra == 0) ? "오전" : "오후";
                displayText = String.format(
                        Locale.getDefault(), "%s %02d:%02d", ampmStr, hourExtra, minuteExtra);
            }

            // 1순위: AddClockAlarmActivity 에서 직접 넘겨준 alarmTimeMillis 사용
            long triggerAtMillis = data.getLongExtra("alarmTimeMillis", -1);

            // 2순위: ampm/hour/minute extras
            int ampm   = data.getIntExtra("ampm", -1);
            int hour   = data.getIntExtra("hour", -1);
            int minute = data.getIntExtra("minute", -1);

            // 3순위: 그래도 없으면 표시 문자열에서 파싱 ("오전 09:27")
            if (ampm == -1 || hour == -1 || minute == -1) {
                int[] vals = parseClockDisplay(displayText);
                ampm   = vals[0];
                hour   = vals[1];
                minute = vals[2];
            }

            // alarmTimeMillis 가 없으면 우리가 계산
            if (triggerAtMillis <= 0) {
                triggerAtMillis = calculateTriggerAtMillis(ampm, hour, minute);
            }

            if (isEdit) {
                // 편집 모드
                if (index >= 0 && index < clockAlarms.size()) {

                    // 1) 기존 알람이 있었다면 먼저 취소
                    if (index < clockAlarmIds.size()) {
                        int oldId = clockAlarmIds.get(index);
                        cancelClockAlarm(oldId);
                    }

                    if (isDelete) {
                        // 삭제: 텍스트/사운드/알람ID 모두 제거
                        clockAlarms.remove(index);
                        if (index < clockSoundUris.size()) {
                            clockSoundUris.remove(index);
                        }
                        if (index < clockAlarmIds.size()) {
                            clockAlarmIds.remove(index);
                        }

                    } else {
                        // 수정

                        // 표시 텍스트 수정
                        clockAlarms.set(index, displayText);

                        // 사운드 URI 처리
                        if (soundUri != null && !soundUri.isEmpty()) {
                            if (index < clockSoundUris.size()) {
                                clockSoundUris.set(index, soundUri);
                            } else {
                                clockSoundUris.add(soundUri);
                            }
                        } else {
                            // 새 사운드를 선택 안 했으면 기존 값 유지
                            if (index >= clockSoundUris.size()) {
                                clockSoundUris.add("");
                            }
                        }

                        // 새 알람 ID 만들어서 저장 & 예약
                        int newAlarmId = (int) System.currentTimeMillis();
                        if (index < clockAlarmIds.size()) {
                            clockAlarmIds.set(index, newAlarmId);
                        } else {
                            clockAlarmIds.add(newAlarmId);
                        }

                        scheduleClockAlarm(triggerAtMillis, newAlarmId, displayText, soundUri);
                    }
                }

            } else {
                // 새 시간 알람 추가

                clockAlarms.add(displayText);
                clockSoundUris.add(soundUri);

                int alarmId = (int) System.currentTimeMillis();
                clockAlarmIds.add(alarmId);

                scheduleClockAlarm(triggerAtMillis, alarmId, displayText, soundUri);
            }

            clockAdapter.notifyDataSetChanged();

            // 2) 타이머 알람
        } else if (requestCode == REQ_ADD_TIMER) {

            int h = data.getIntExtra("hour", 0);
            int m = data.getIntExtra("minute", 0);
            int s = data.getIntExtra("second", 0);
            String soundUri = data.getStringExtra("soundUri");

            long totalMillis = (h * 3600L + m * 60L + s) * 1000L;

            if (displayText == null) {
                displayText = String.format(
                        Locale.getDefault(), "%02d시간 %02d분 %02d초", h, m, s);
            }

            if (isEdit) {
                if (index >= 0 && index < timerAlarms.size()) {
                    // 기존 타이머 정리
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

                    if (isDelete) {
                        // 삭제
                        timerAlarms.remove(index);
                    } else {
                        // 수정 후 새 타이머로 교체
                        timerAlarms.remove(index);
                        addNewTimer(totalMillis, displayText, soundUri);
                    }
                }
            } else {
                // 새 타이머 추가
                addNewTimer(totalMillis, displayText, soundUri);
            }

            timerAdapter.notifyDataSetChanged();
        }
    }



    // 시간 알람 문자열 파싱 : "오전 07:30"
    private int[] parseClockDisplay(String text) {
        int ampm = text.startsWith("오전") ? 0 : 1;

        int spaceIdx = text.indexOf(' ');
        String timePart = text.substring(spaceIdx + 1);
        String[] parts = timePart.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return new int[]{ampm, hour, minute};
    }

    // 타이머 문자열 파싱 : "00시간 05분 00초"
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
