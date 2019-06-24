package com.zeal.acrademo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_nullponiter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a = null;
                Log.e(TAG, "nullPoniter: " + a.length());
            }
        });

        findViewById(R.id.btn_outofbounds).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List list = new ArrayList();
                Log.e(TAG, "outOfBounds: " + list.get(1));
            }
        });

        findViewById(R.id.btn_anr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Thread.sleep(10000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.am_btn_cast1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView= (TextView) findViewById(R.id.am_btn_cast1);

            }
        });

    }

}
