package com.map.to_in.cabmapbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button driver, student;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driver = findViewById(R.id.driver);
        student = findViewById(R.id.customer);

        driver.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DriverLoginLogout.class);
            startActivity(intent);
            finish();
        });

        student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudentLoginLogout.class);
                startActivity(intent);
                finish();
            }
        });
    }

}
