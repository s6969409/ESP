package com.example.ledmatrix;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.ledmatrix.deviceAdapter.DeviceListActivity;
import com.example.ledmatrix.local.AppSettingActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_onLine,btn_test,btn_offLine,btn_setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        btn_onLine = findViewById(R.id.btn_onLine);
        btn_onLine.setOnClickListener(this);
        btn_test = findViewById(R.id.btn_test);
        btn_test.setOnClickListener(this);
        btn_offLine = findViewById(R.id.btn_offLine);
        btn_offLine.setOnClickListener(this);
        btn_setting = findViewById(R.id.btn_setting);
        btn_setting.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()){
            case R.id.btn_onLine:
                intent.setClass(this,
                        AppSettingActivity.getAppSetting(this,"dataSaveAt").equals("onLine")?
                                OnLineListActivity.class: DeviceListActivity.class
                );
                startActivity(intent);
                break;
            case R.id.btn_test:
                String testClassName = AppSettingActivity.getAppSetting(this,"Activity");
                String a = "android";
                if (testClassName.substring(0,8).equals("android.")){

                    intent = new Intent(testClassName);
                }else {
                    try {
                        intent.setClass(this, Class.forName(testClassName));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                startActivity(intent);
                break;
            case R.id.btn_offLine:
                intent.setClass(this,CustomerActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting:
                intent.setClass(this, AppSettingActivity.class);
                startActivity(intent);
                break;
        }
    }
}
