package com.timeslily;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity implements OnClickListener {
    Button button1;
    Button button2;
    Button button3;
    Button button4;
    Button button5;
    Button button6;
    Button button7;
    Button button8;
    Button button9;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test1);
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(this);
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(this);
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(this);
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(this);
        button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(this);
        button6 = (Button) findViewById(R.id.button6);
        button6.setOnClickListener(this);
        button7 = (Button) findViewById(R.id.button7);
        button7.setOnClickListener(this);
        button8 = (Button) findViewById(R.id.button8);
        button8.setOnClickListener(this);
        button9 = (Button) findViewById(R.id.button9);
        button9.setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        String fileName = "";
        switch (v.getId()) {
        case R.id.button1:
            fileName = button1.getText().toString();
            break;
        case R.id.button2:
            fileName = button2.getText().toString();
            break;
        case R.id.button3:
            fileName = button3.getText().toString();
            break;
        case R.id.button4:
            fileName = button4.getText().toString();
            break;
        case R.id.button5:
            fileName = button5.getText().toString();
            break;
        case R.id.button6:
            fileName = button6.getText().toString();
            break;
        case R.id.button7:
            fileName = button7.getText().toString();
            break;
        case R.id.button8:
            fileName = button8.getText().toString();
            break;
        case R.id.button9:
            fileName = button9.getText().toString();
            break;
        }
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }
}
