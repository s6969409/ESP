package com.example.ledmatrix.local;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.ViewTool;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ApActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private TextView tv_log;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ap);

        initView();
    }

    private void initView() {
        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        tv_log = findViewById(R.id.tv_log);
        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_log, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Start","Cancel",saveCheckListener)
        );
        snackbar.show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.System.canWrite(this)){
                //TODO 這個權限開了感覺很危險!!
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
            }
        }
    }

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            //Start
            startAp("DukeAp","ddd54679",false);
        }

        @Override
        public void onClickNo(View v) {
            tv_log.append(String.valueOf(getConnectedIp().size()) + "\n");
            for (String s : getConnectedIp()) {
                tv_log.append(s + "\n");
            }
            closeAp();
        }
    };

    //AP method....

    private void startAp(String SSID, String password,boolean saftyOpen){
        Method method;

        try {
            method = wifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class,boolean.class);
            WifiConfiguration configuration = new WifiConfiguration();
            configuration.SSID = SSID;
            configuration.preSharedKey = password;
            configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

            if (saftyOpen){
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }

            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            //TODO TKIP已棄用(有安全疑慮?)...找替代...
            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            method.invoke(wifiManager,configuration,true);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            tv_log.append(e.toString() + "\n");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            tv_log.append(e.toString() + "\n");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            tv_log.append(e.toString() + "\n");
        }
    }

    private String getApSSID() {
        //TODO
        return "";
    }

    private boolean isApEnabled(){
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }



    private void closeAp(){
        WifiManager wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (isApEnabled()){
            Method method= null;
            try {
                method = wifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration configuration = (WifiConfiguration) method.invoke(wifiManager);
                Method method2=wifiManager.getClass().getMethod(
                        "setWifiApEnabled",WifiConfiguration.class,boolean.class);
                method2.invoke(wifiManager,configuration,false);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //get list of connect devices ip
    //test ok!! Wifi 熱點都抓地到
    private List<String> getConnectedIp(){
        List<String> connectedIpList = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader("/proc/net/arp"));
            String line;
            while ((line = bufferedReader.readLine()) != null){
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4){
                    String ip = splitted[0];
                    if (!ip.equalsIgnoreCase("ip")){
                        connectedIpList.add(ip);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connectedIpList;
    }
}
