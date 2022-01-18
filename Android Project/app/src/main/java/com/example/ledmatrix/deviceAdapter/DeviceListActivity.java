package com.example.ledmatrix.deviceAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.ledmatrix.DeviceFileEditActivity2;
import com.example.ledmatrix.OnLineListActivity;
import com.example.ledmatrix.R;
import com.example.ledmatrix.local.FileProcess;

import java.util.List;

public class DeviceListActivity extends AppCompatActivity {
    private ListView lv_devices;
    private List<String> list;
    private DeviceListAdapter adapter;
    private TextView tv_addDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        initData();
        initView();
    }

    private void initData() {
        FileProcess.makeDir(this,FileProcess.FOLDER_deviceBackup);
    }

    private void initView() {
        tv_addDevice = new TextView(this);
        tv_addDevice.setText("<<Add Device Info>>");
        tv_addDevice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

        lv_devices = findViewById(R.id.lv_devices2);

        readDevicesInfoFile();

        adapter = new DeviceListAdapter(list);
        lv_devices.setAdapter(adapter);
        lv_devices.addFooterView(tv_addDevice);
        adapter.notifyDataSetChanged();
        lv_devices.setOnItemClickListener(new DeviceListOnItemClickListener(this, adapter));
        lv_devices.setOnItemLongClickListener(new DeviceListOnItemLongClickListener(this, adapter));
    }

    private void readDevicesInfoFile() {
        list = FileProcess.listItems(this, FileProcess.FOLDER_deviceBackup);
    }

    private class DeviceListAdapter extends BaseAdapter {
        private List<String> list;

        public DeviceListAdapter(List<String> list) {
            this.list = list;
        }

        public List<String> getList() {
            return list;
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
            TextView viewHolder;
            if (convertView == null) {
                viewHolder = new TextView(parent.getContext());
                viewHolder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                convertView = viewHolder;
            } else {
                viewHolder = (TextView) convertView;
            }
            viewHolder.setText(list.get(position));
            return convertView;
        }
    }

    private class DeviceListOnItemClickListener implements AdapterView.OnItemClickListener{
        private Context context;
        private DeviceListAdapter adapter;
        private MainProgramActivity.AdapterRes.CreateDialog createDialog;
        private MainProgramActivity.AdapterRes.CreateDialog.OnCreateListener listener;
        private AlertDialog alertDialog;
        private String addName;

        public DeviceListOnItemClickListener(Context context, DeviceListAdapter adapter) {
            this.context = context;
            this.adapter = adapter;
            createDialog = new MainProgramActivity.AdapterRes.CreateDialog(context);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (id == tv_addDevice.getId()){
                listener = new MainProgramActivity.AdapterRes.CreateDialog.OnCreateListener() {
                    @Override
                    public void onCreate(String dirPath, String createName) {
                        //TODO 做檢查名稱
                        addName = createName;

                        alertDialog.cancel();
                        String dataPath = FileProcess.FOLDER_deviceBackup + "/" +createName;
                        FileProcess.makeDir(context,FileProcess.FOLDER_deviceBackup + "/" +createName);
                        FileProcess.makeDir(context, dataPath + "/" + FileProcess.FOLDER_dataBackup);
                        adapter.getList().add(addName);
                        adapter.notifyDataSetChanged();
                    }
                };
                String defaultName = "New Device";
                boolean isSame = true;
                while (isSame) {
                    for (String s : adapter.getList()) {
                        if (s.equals(defaultName)) {
                            defaultName += " 1";
                            break;
                        }
                    }
                    isSame = false;
                }

                alertDialog = createDialog.builde("" ,defaultName ,listener);
                alertDialog.setMessage("");
                alertDialog.show();
            }

            startEditActivity(list.get(position));
        }
    }

    private class DeviceListOnItemLongClickListener implements AdapterView.OnItemLongClickListener{
        private Context context;
        private DeviceListAdapter adapter;
        private DialogInterface.OnClickListener deleteListener;

        public DeviceListOnItemLongClickListener(Context context, DeviceListAdapter adapter) {
            this.context = context;
            this.adapter = adapter;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            deleteListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FileProcess.removeDir(context,
                            FileProcess.FOLDER_deviceBackup + "/"
                                    + adapter.getItem(position).toString()
                    );
                    adapter.getList().remove(position);
                    adapter.notifyDataSetChanged();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Delete device");
            builder.setMessage("Delete device name: " + adapter.getItem(position) + "?");
            builder.setPositiveButton("Delete", deleteListener);
            builder.setNegativeButton("Cancel",null);
            builder.show();
            return true;
        }
    }

    private void startEditActivity(String name){
        Intent intent = new Intent();
        intent.putExtra(OnLineListActivity.INTENT_DEVICE_NAME, name);
        intent.setClass(this,DeviceFileEditActivity2.class);
        startActivity(intent);
    }
}
