package com.example.mytoast;

import android.graphics.Color;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 配置Toasty
        Toasty.Builder()
                .setTextColor(Color.BLUE)
                .setGravity(Gravity.CENTER, 0, 0)
                .apply();

        ((Button) findViewById(R.id.show_toast)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toasty.showSuccess("hello");
            }
        });
        
        ((Button) findViewById(R.id.show_origin_toast)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "show origin toast", Toast.LENGTH_SHORT).show();
                Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);
                toast.setText("测试gravity");
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.setGravity(Gravity.TOP, 0, 200);
                toast.setGravity(Gravity.TOP | Gravity.START, 0, 0 );
                toast.show();
            }
        });

        ((Button)findViewById(R.id.thread_show_origin_toast)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "thread show origin toast", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                }).start();
            }
        });


        ((Button)findViewById(R.id.thread_show_toast)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Toasty.showError("thread show error");
                    }
                }).start();
            }
        });
    }
}
