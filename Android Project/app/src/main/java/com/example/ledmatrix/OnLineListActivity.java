package com.example.ledmatrix;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.deviceAdapter.DeviceInfo;
import com.example.ledmatrix.local.AppSettingActivity;
import com.example.ledmatrix.local.FileProcess;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnLineListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener {
    private ListView lv_devices;
    private List<DeviceConnectionInfo> list;
    private LvDevicesAdapter lvDevicesAdapter;
    private TextView tv_addDevice;

    private ConstraintLayout layout_createDevice;
    private int layout_createDevice_heightOri;
    private EditText ed_createDeviceName, ed_createDeviceIp, ed_createDevicePort;
    private Button btn_createDeviceCreate, btn_createDeviceCancel;

    private Gson gson;

    public static final String INTENT_DEVICE_NAME = "name";
    public static final String INTENT_DEVICE_IP = "ip";
    public static final String INTENT_DEVICE_PORT = "port";
    public static final int REQUEST_CODE_serverClose = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_line_list);

        initData();
        initView();
        initViewOfCreateDevice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_serverClose
                && resultCode == DeviceFileEditActivity.RESULT_CODE_serverClose){
            String serverMsg = data.getStringExtra(
                    DeviceFileEditActivity.INTENT_SERVER_CLOSE_MSG);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(serverMsg);
            builder.setPositiveButton("OK",null);
            builder.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (layout_createDevice.getMaxHeight() == layout_createDevice_heightOri){
            hideCreateDeviceDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lv_devices.setEnabled(true);
    }

    private void initData(){
        FileProcess.makeDir(this,FileProcess.FOLDER_deviceBackup);
    }

    private void initView() {
        tv_addDevice = new TextView(this);
        tv_addDevice.setId(View.generateViewId());
        tv_addDevice.setText("<<Add Device Info>>");
        tv_addDevice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

        lv_devices = findViewById(R.id.lv_devices);
        lv_devices.setOnItemClickListener(this);
        lv_devices.setOnItemLongClickListener(this);

        gson = new Gson();

        readDevicesInfoFile();

        lvDevicesAdapter = new LvDevicesAdapter(this, list);
        lv_devices.setAdapter(lvDevicesAdapter);
        lv_devices.addFooterView(tv_addDevice);
        lvDevicesAdapter.notifyDataSetChanged();
    }

    private void initViewOfCreateDevice() {
        layout_createDevice = findViewById(R.id.layout_createDevice);
        ed_createDeviceName = findViewById(R.id.ed_createDeviceName);
        ed_createDeviceIp = findViewById(R.id.ed_createDeviceIp);
        ed_createDevicePort = findViewById(R.id.ed_createDevicePort);
        btn_createDeviceCreate = findViewById(R.id.btn_createDeviceCreate);
        btn_createDeviceCreate.setOnClickListener(this);
        btn_createDeviceCancel = findViewById(R.id.btn_createDeviceCancel);
        btn_createDeviceCancel.setOnClickListener(this);
        layout_createDevice_heightOri = layout_createDevice.getMaxHeight();
        hideCreateDeviceDialog();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getId() == tv_addDevice.getId()) {
            //add device
            showCreateDeviceDialog();
            return;
        }
        //connect...
        lv_devices.setEnabled(false);
        Intent intent = new Intent();
        intent.setClass(this,
                AppSettingActivity.getAppSetting(this,"dataSaveAt").equals("onLine")?
                        DeviceFileEditActivity.class:
                        DeviceFileEditActivity2.class
        );
        intent.putExtra(INTENT_DEVICE_NAME,list.get(position).name);
        intent.putExtra(INTENT_DEVICE_IP,list.get(position).ip);
        intent.putExtra(INTENT_DEVICE_PORT,list.get(position).port);
        startActivityForResult(intent, REQUEST_CODE_serverClose);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        //delete device connection Info
        if(view.getId() == tv_addDevice.getId()){//if click at "add device", return this function
            return true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete device");
        builder.setMessage("Delete device name: " + list.get(position).name + "?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDevicesInfoFile(list.get(position).name);
                list.remove(position);
                lvDevicesAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel",null);
        //builder.setIcon();
        builder.show();
        // 回傳 false，長按後該項目被按下的狀態會保持。
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_createDeviceCreate:
                //check list is have ... not finish
                if(checkHadSameName()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Warning!");
                    builder.setMessage("Had the same device name!");
                    //builder.setIcon();
                    builder.setPositiveButton("OK",null);
                    builder.show();
                    return;
                }

                DeviceConnectionInfo deviceConnectionInfo = new DeviceConnectionInfo(
                        ed_createDeviceName.getText().toString(),
                        ed_createDeviceIp.getText().toString(),
                        Integer.parseInt(ed_createDevicePort.getText().toString())
                );
                list.add(deviceConnectionInfo);
                lvDevicesAdapter.notifyDataSetChanged();

                saveDevicesInfoFile(deviceConnectionInfo.name,gson.toJson(deviceConnectionInfo));

                hideCreateDeviceDialog();
                break;
            case R.id.btn_createDeviceCancel:
                hideCreateDeviceDialog();
                break;
        }
    }

    private boolean checkHadSameName() {
        String addName = ed_createDeviceName.getText().toString();
        for(DeviceConnectionInfo d : list){
            if(addName.equals(d.name)){
                return true;
            }
        }
        return false;
    }

    private void readDevicesInfoFile(){
        list = new ArrayList<>();

        List<String> items = FileProcess.listItems(this, FileProcess.FOLDER_deviceBackup);
        for (String item : items){
            String path = FileProcess.FOLDER_deviceBackup + "/" + item + "/" + item + ".txt";
            String data = FileProcess.readFile(this, path);
            DeviceConnectionInfo deviceConnectionInfo = gson.fromJson(data, DeviceConnectionInfo.class);
            list.add(deviceConnectionInfo);
        }
    }

    private void saveDevicesInfoFile(String dirPath, String data){
        String dataPath = FileProcess.FOLDER_deviceBackup + "/" + dirPath;
        FileProcess.makeDir(this, dataPath);
        FileProcess.writeFile(
                this,
                dataPath + "/" + dirPath + ".txt",
                data
        );
        FileProcess.makeDir(this, dataPath + "/" + FileProcess.FOLDER_dataBackup);
    }

    private void removeDevicesInfoFile(String dirPath){
        String dataPath = FileProcess.FOLDER_deviceBackup + "/" + dirPath;
        FileProcess.removeDir(this, dataPath);
    }

    private void showCreateDeviceDialog(){
        layout_createDevice.setMaxHeight(layout_createDevice_heightOri);
    }

    private void hideCreateDeviceDialog(){
        layout_createDevice.setMaxHeight(0);
    }

    public class DeviceConnectionInfo {
        private String name;
        private String ip;
        private int port;

        public DeviceConnectionInfo(String name, String ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }
    }

    private class LvDevicesAdapter extends BaseAdapter {
        private List<DeviceConnectionInfo> list;
        private LayoutInflater layoutInflater;

        public LvDevicesAdapter(Context context, List<DeviceConnectionInfo> list) {
            this.list = list;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_online_list, null);
                viewHolder = new ViewHolder(
                        (TextView) convertView.findViewById(R.id.tv_deviceListName),
                        (TextView) convertView.findViewById(R.id.tv_deviceListParam)
                );
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            DeviceConnectionInfo deviceConnectionInfo = (DeviceConnectionInfo) getItem(position);
            viewHolder.tv_deviceListName.setText(deviceConnectionInfo.name);
            viewHolder.tv_deviceListParam.setText(
                    deviceConnectionInfo.ip + ":" + deviceConnectionInfo.port);
            return convertView;
        }
    }

    private class ViewHolder {
        public TextView tv_deviceListName, tv_deviceListParam;

        public ViewHolder(TextView tv_deviceListName, TextView tv_deviceListParam) {
            this.tv_deviceListName = tv_deviceListName;
            this.tv_deviceListParam = tv_deviceListParam;
        }
    }
}
