package com.example.ledmatrix.local;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.FileEditActivity;
import com.example.ledmatrix.R;
import com.example.ledmatrix.deviceAdapter.LedMatrixActivity;
import com.example.ledmatrix.deviceAdapter.MainProgramActivity;
import com.example.ledmatrix.deviceAdapter.ProgramActivity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class AppSettingActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_appSettingSave,btn_appSettingCencel,btn_appSettingInitial;
    private ListView lv_appSettingList;
    private Adapter adapter;
    private List<SettingData> list;
    private Gson gson;

    private static final String APP_SETTING_FILE_NAME = "appSetting.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_setting);

        initView();
    }

    private void initView() {
        btn_appSettingSave = findViewById(R.id.btn_appSettingSave);
        btn_appSettingSave.setOnClickListener(this);
        btn_appSettingCencel = findViewById(R.id.btn_appSettingCencel);
        btn_appSettingCencel.setOnClickListener(this);
        btn_appSettingInitial = findViewById(R.id.btn_appSettingInitial);
        btn_appSettingInitial.setOnClickListener(this);
        lv_appSettingList = findViewById(R.id.lv_appSettingList);
        readAppSettingData();
    }

    private void readAppSettingData() {
        gson = new Gson();
        String data = FileProcess.readFile(this,APP_SETTING_FILE_NAME);
        SettingData[] datas = gson.fromJson(data, SettingData[].class);
        if(datas == null){
            list = new ArrayList<>();
            initSettingData();
        } else {
            list = new ArrayList<>(Arrays.asList(datas));
            adapter = new Adapter(list);
            lv_appSettingList.setAdapter(adapter);
        }
    }

    private void saveAppSettingData(){
        SettingData[] settingDatas = new SettingData[list.size()];
        for(int i=0;i<list.size();i++){
            settingDatas[i] = list.get(i);
        }
        String data = gson.toJson(settingDatas);
        FileProcess.writeFile(this,APP_SETTING_FILE_NAME,data);
    }

    private void initSettingData() {
        list.clear();
        list.add(new SettingData("showConnectionRequestDialog", new String[]{"off","on"}, 0));
        list.add(new SettingData("Activity",new String[]{
                FileEditActivity.class.getName(),
                LedMatrixActivity.class.getName(),
                ProgramActivity.class.getName(),
                MainProgramActivity.class.getName(),
                ApActivity.class.getName(),
                Settings.ACTION_WIFI_IP_SETTINGS,
                Settings.ACTION_WIFI_SETTINGS,
                Settings.ACTION_WIRELESS_SETTINGS
        },0));
        list.add(new SettingData("LogView", new String[]{"off","on"}, 0));
        list.add(new SettingData("dataSaveAt", new String[]{"local","onLine"}, 0));
        adapter = new Adapter(list);
        lv_appSettingList.setAdapter(adapter);
    }

    /**
     * @param context The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param name Variable name of app setting item, you can see method "initSettingData()" of class "AppSettingActivity"
     * @return Variable selected item conten , you can see method "initSettingData()" of class "AppSettingActivity"
     */
    public static String getAppSetting(Context context, String name){
        String data = FileProcess.readFile(context,APP_SETTING_FILE_NAME);
        Gson gson = new Gson();
        SettingData[] datas = gson.fromJson(data, SettingData[].class);
        if(datas != null){
            for (SettingData d : datas){
                if (d.getName().equals(name)){
                    return d.getItems()[d.getSelected()];
                }
            }
        }
        return "";
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_appSettingSave:
                saveAppSettingData();
                finish();
                break;
            case R.id.btn_appSettingCencel:
                finish();
                break;
            case R.id.btn_appSettingInitial:
                initSettingData();
                adapter.notifyDataSetChanged();
                Toast.makeText(this,"btn_appSettingInitial",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public static class SettingData {
        private String name;
        private String[] items;
        private int selected;

        public SettingData(String name, String[] items, int selected) {
            this.name = name;
            this.items = items;
            this.selected = selected;
        }

        //region Member of Setter & Getter
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String[] getItems() {
            return items;
        }

        public void setItems(String[] items) {
            this.items = items;
        }

        public int getSelected() {
            return selected;
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }
        //endregion
    }

    private class Adapter extends BaseAdapter{
        List<SettingData> list;

        public Adapter(List<SettingData> list) {
            this.list = list;
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
            SettingData settingData = list.get(position);
            if (settingData.getName().equals("ledNums")){//for EditText view
                return getItemViewForEditText(position, convertView, parent);
            }
            //for Spinner view
            return getItemViewForSpinner(position, convertView, parent);
        }

        private View getItemViewForSpinner(int position, View convertView, ViewGroup parent){
            ViewHolder holder;
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_app_setting_spinner, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SettingData settingData = list.get(position);
            holder.tv_appSettingName.setText(settingData.name);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    AppSettingActivity.this,
                    R.layout.support_simple_spinner_dropdown_item,
                    settingData.getItems());
            holder.sp_appSettingItem.setAdapter(arrayAdapter);
            holder.sp_appSettingItem.setOnItemSelectedListener(
                    new OnItemSelectedListener(list.get(position)));
            holder.sp_appSettingItem.setSelection(settingData.getSelected());
            return convertView;
        }

        private View getItemViewForEditText(int position, View convertView, ViewGroup parent){
            ViewHolder holder;
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_app_setting_edittext, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder.ed_appSettingItem == null){
                return convertView;
            }

            SettingData settingData = list.get(position);
            holder.tv_appSettingName.setText(settingData.name);
            holder.ed_appSettingItem.setInputType(InputType.TYPE_CLASS_NUMBER);
            holder.ed_appSettingItem.setText(settingData.getItems()[settingData.getSelected()]);
            holder.ed_appSettingItem.setOnEditorActionListener(
                    new OnEditorActionListener(settingData));
            return convertView;
        }
    }

    private class ViewHolder{
        TextView tv_appSettingName;
        Spinner sp_appSettingItem;
        EditText ed_appSettingItem;

        public ViewHolder(View convertView) {
            this.tv_appSettingName = convertView.findViewById(R.id.tv_appSettingName);
            this.sp_appSettingItem = convertView.findViewById(R.id.sp_appSettingItem);
            this.ed_appSettingItem = convertView.findViewById(R.id.ed_appSettingItem);
        }
    }

    private class OnItemSelectedListener implements AdapterView.OnItemSelectedListener{
        private SettingData settingData;

        public OnItemSelectedListener(SettingData settingData) {
            this.settingData = settingData;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            settingData.setSelected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class OnEditorActionListener implements TextView.OnEditorActionListener{
        SettingData settingData;

        public OnEditorActionListener(SettingData settingData) {
            this.settingData = settingData;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            settingData.setItems(new String[]{v.getText().toString()});
            return false;
        }
    }
}
