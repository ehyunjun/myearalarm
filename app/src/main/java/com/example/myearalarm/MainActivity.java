package com.example.myearalarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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
            startActivity(intent);
        });

        btnTimerAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTimerAlarmActivity.class);
            startActivity(intent);
        });

    }
}