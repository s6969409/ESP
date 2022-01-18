package com.example.ledmatrix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ledmatrix.deviceAdapter.Data;
import com.example.ledmatrix.deviceAdapter.MainProgramActivity;
import com.example.ledmatrix.local.AppSettingActivity;
import com.example.ledmatrix.local.FileProcess;
import com.example.ledmatrix.tcpIp.Client;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

public class DeviceFileEditActivity2 extends AppCompatActivity {
    private ListView lv_onLineFiles;
    private MainProgramActivity.AdapterRes adapterRes;
    private LedDeviceItemClickListener ledDeviceItemClickListener;
    private LedDeviceItemLongClickListener ledDeviceItemLongClickListener;

    private View onLineView;
    private AdapterOnLine adapterOnLine;
    private OnLineItemClickListener onLineItemClickListener;

    private Menu menu;
    private static final int MENU_ITEM_ID_preview = 0;
    private static final int MENU_ITEM_ID_create = 1;
    private static final int MENU_ITEM_ID_makeDir = 2;
    private static final int MENU_ITEM_ID_remove = 3;

    private static final int MENU_ITEM_ID_goOnline = 4;
    private static final int MENU_ITEM_ID_goOnlineFromDevice = 5;
    private static final int MENU_ITEM_ID_goOnlineToDevice = 6;
    private int menuId = MENU_ITEM_ID_preview;

    private TextView lvItem_ledMatrix;

    private String devicePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_file_edit);

        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Data.ActivityConstant.REQUEST_CODE_showFileContent
                && resultCode == Data.ActivityConstant.RESULT_CODE_writeFileContetnt
        ){
            //do write file
            String filePath = devicePath + "/"
                    + data.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
            String fileContent = data.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);
            boolean needUpdateList = !FileProcess.pathExists(this,filePath);
            FileProcess.writeFile(this,filePath,fileContent);
            if (needUpdateList) {
                initData();//write file ok, update listView
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        initData();//init data
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        menuId = item.getItemId();
        Toast.makeText(this,item.getTitle() + "!!!",Toast.LENGTH_SHORT).show();
        setTitle(item.getTitle());

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (menuId == MENU_ITEM_ID_preview || menuId == MENU_ITEM_ID_goOnline) {
            super.onBackPressed();
        } else if(menuId == MENU_ITEM_ID_create
                || menuId == MENU_ITEM_ID_makeDir
                || menuId == MENU_ITEM_ID_remove){
            menuId = MENU_ITEM_ID_preview;
            setTitle("preview");
        } else {
            menuId = MENU_ITEM_ID_goOnline;
            setTitle("onLine");
        }
    }

    private void initView() {
        lv_onLineFiles = findViewById(R.id.lv_onLineFiles);
        menuId = MENU_ITEM_ID_preview;

        adapterRes = new MainProgramActivity.AdapterRes();
        ledDeviceItemClickListener = new LedDeviceItemClickListener(adapterRes);
        ledDeviceItemLongClickListener = new LedDeviceItemLongClickListener(adapterRes);

        lv_onLineFiles.setAdapter(adapterRes);

        adapterOnLine = new AdapterOnLine();
        onLineItemClickListener = new OnLineItemClickListener(adapterOnLine);
    }

    private void initData() {
        String name = getIntent().getStringExtra(OnLineListActivity.INTENT_DEVICE_NAME);
        devicePath = FileProcess.FOLDER_deviceBackup + "/" + name
                + "/"+ FileProcess.FOLDER_dataBackup;

        String src = getDataPathTree(devicePath);

        if (src == "") {
            initSelectionItems();
        } else {
            adapterRes.updateDataFromDevice(src);
            lv_onLineFiles.setOnItemClickListener(ledDeviceItemClickListener);
            lv_onLineFiles.setOnItemLongClickListener(ledDeviceItemLongClickListener);
            initOffLineMenu();
            setTitle("preview");

            if (lv_onLineFiles.getFooterViewsCount()==0) {
                onLineView = LayoutInflater.from(this).inflate(R.layout.item_file, null);
                onLineView.setId(View.generateViewId());

                MainProgramActivity.ViewNodeHolder onlineViewHolder = new MainProgramActivity.ViewNodeHolder(onLineView);
                onlineViewHolder.iv_fileNodeImg.setPadding(
                        50,
                        onlineViewHolder.iv_fileNodeImg.getPaddingTop(),
                        onlineViewHolder.iv_fileNodeImg.getPaddingRight(),
                        onlineViewHolder.iv_fileNodeImg.getPaddingBottom()
                );

                onlineViewHolder.iv_fileNodeImg.setImageResource(R.drawable.device);
                onlineViewHolder.tv_fileName.setText("GoOnLine");
                lv_onLineFiles.addFooterView(onLineView);
            }
        }
        menuId = MENU_ITEM_ID_preview;
    }

    private void initOffLineMenu(){
        menu.clear();
        menu.add(0,MENU_ITEM_ID_create,0,"create");
        menu.add(0,MENU_ITEM_ID_makeDir,0,"makeDir");
        menu.add(0,MENU_ITEM_ID_remove,0,"remove");
    }

    private void initOnLineMenu(){
        menu.clear();
        menu.add(0,MENU_ITEM_ID_goOnlineFromDevice,0,"fromDevice");
        menu.add(0,MENU_ITEM_ID_goOnlineToDevice,0,"toDevice");
    }

    private void initSelectionItems(){
        setTitle("Select device");

        lvItem_ledMatrix = new TextView(this);
        lvItem_ledMatrix.setText("LedMatrix");
        lvItem_ledMatrix.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);

        lv_onLineFiles.addFooterView(lvItem_ledMatrix);
        lv_onLineFiles.setOnItemClickListener(initSelectDeviceTyprClickListener);
    }

    private String getDataPathTree(String path){
        String dataPathTree = "";
        File file = new File(getExternalFilesDir(null), path);

        for (File f : file.listFiles()){
            if (dataPathTree != "")dataPathTree += ",";

            dataPathTree += f.getName();
            if (f.isDirectory()){
                dataPathTree += "<"
                        + getDataPathTree(path + "/" + f.getName())
                        + ">";
            }
        }
        return dataPathTree;
    }

    private AdapterView.OnItemClickListener initSelectDeviceTyprClickListener
            = new AdapterView.OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (id == lvItem_ledMatrix.getId()){
                lv_onLineFiles.removeFooterView(lvItem_ledMatrix);

                //initDataTree
                FileProcess.makeDir(DeviceFileEditActivity2.this,
                        devicePath + "/" + "deviceConfig");
                FileProcess.writeFile(DeviceFileEditActivity2.this,
                        devicePath + "/" + Data.FolderPath.DEVICE_CONFIG_AP_SETTING,
                        Data.FolderPath.UNKNOW
                );
                FileProcess.writeFile(DeviceFileEditActivity2.this,
                        devicePath + "/" + Data.FolderPath.DEVICE_CONFIG_MAIN_PROGRAM,
                        ""
                );

                FileProcess.makeDir(DeviceFileEditActivity2.this,
                        devicePath + "/" + "resource");
                FileProcess.makeDir(DeviceFileEditActivity2.this,
                        devicePath + "/" + Data.FolderPath.RES_PROGRAM);
                FileProcess.makeDir(DeviceFileEditActivity2.this,
                        devicePath + "/" + Data.FolderPath.RES_LEDS);
                initData();//create new device dateTree, update listView
            }

        }
    };

    private class LedDeviceItemClickListener extends MainProgramActivity.FolderOpenCloseByItemClickListener{


        public LedDeviceItemClickListener(MainProgramActivity.AdapterRes adapterRes) {
            super(adapterRes);
        }

        @Override
        public void onSeletedPath(String seletedPath) {
            if (menuId != MENU_ITEM_ID_preview){
                return;
            }
            Activity activity = DeviceFileEditActivity2.this;
            Data.startEditActivity(activity, devicePath,
                    new Data.Packet("", seletedPath,
                            FileProcess.readFile(activity,devicePath + "/" + seletedPath)
                    ),
                    false
            );
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (view.getId() == onLineView.getId()){
                menuId = MENU_ITEM_ID_goOnline;//set status
                setTitle("onLine");
                ((TextView)onLineView.findViewById(R.id.tv_fileName)).setText("GoOffLine");
                lv_onLineFiles.setAdapter(adapterOnLine);
                adapterOnLine.updateData(FileProcess.GetConnectedInfo(true));
                lv_onLineFiles.setOnItemClickListener(onLineItemClickListener);
                lv_onLineFiles.setOnItemLongClickListener(null);
                initOnLineMenu();
            }else {
                super.onItemClick(parent, view, position, id);
            }
        }
    }

    private class LedDeviceItemLongClickListener implements AdapterView.OnItemLongClickListener{
        private MainProgramActivity.AdapterRes adapterRes;
        private MainProgramActivity.AdapterRes.CreateDialog dialog;
        private AlertDialog alertDialog;
        private List<String> clickSubItems;
        private MainProgramActivity.AdapterRes.CreateDialog.OnCreateListener createFileListener,createFolderListener;

        public LedDeviceItemLongClickListener(MainProgramActivity.AdapterRes adapterRes) {
            this.adapterRes = adapterRes;
            dialog = new MainProgramActivity.AdapterRes.CreateDialog(DeviceFileEditActivity2.this);
            createFileListener = new MainProgramActivity.AdapterRes.CreateDialog.OnCreateListener() {

                @Override
                public void onCreate(String dirPath, String createName) {
                    if (!checkFileName(createName)
                            ||!checkSameName(createName + ".txt"))return;

                    alertDialog.cancel();
                    String path = dirPath.substring(devicePath.length()+1);
                    Data.startCreateActivity(DeviceFileEditActivity2.this,
                            path + "/" + createName + ".txt");
                }
            };
            createFolderListener = new MainProgramActivity.AdapterRes.CreateDialog.OnCreateListener() {

                @Override
                public void onCreate(String dirPath, String createName) {
                    if (!checkFileName(createName)
                            ||!checkSameName(createName))return;

                    alertDialog.cancel();
                    if (FileProcess.makeDir(DeviceFileEditActivity2.this,
                            dirPath + "/" + createName)) {
                        initData();//create new folder, update listView
                    } else {
                        Toast.makeText(DeviceFileEditActivity2.this,"createFaild!",Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }

        private boolean checkFileName(String fileName){
            if(fileName.equals("")){
                return false;
            }

            String fileNameCheckMsg = FileProcess.CheckFileName(fileName,10);
            if(!fileNameCheckMsg.equals("")){
                alertDialog.setTitle(fileNameCheckMsg);
                return false;
            }
            return true;
        }

        private boolean checkSameName(String fileName){
            for (String e : clickSubItems){
                if (e.equals(fileName )){
                    alertDialog.setTitle("has same name!");
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (view.getId() == onLineView.getId()){
                return true;
            }

            clickSubItems = adapterRes.getSubItems(position);
            String createPath = devicePath + "/" + adapterRes.getFileFolderAbsolutePath(position);
            String editPath = devicePath + "/" + adapterRes.getAbsolutePath(position);

            if (menuId == MENU_ITEM_ID_preview){
                if (adapterRes.getElementsAtView().get(position).getExtension().equals(DeviceFileEditActivity.Element.FOLDER)){
                    return true;
                }

                Toast.makeText(DeviceFileEditActivity2.this,"----",Toast.LENGTH_SHORT).show();
                Activity activity = DeviceFileEditActivity2.this;
                Data.startEditActivity(activity, devicePath,
                        new Data.Packet("",
                                editPath,
                                FileProcess.readFile(activity,editPath)),
                        true
                );
            }
            if (menuId == MENU_ITEM_ID_create){
                alertDialog = dialog.builde(createPath, "" , createFileListener);
                alertDialog.show();
            }
            if (menuId == MENU_ITEM_ID_makeDir){
                alertDialog = dialog.builde(createPath ,"" , createFolderListener);
                alertDialog.show();
            }
            if (menuId == MENU_ITEM_ID_remove){
                Toast.makeText(DeviceFileEditActivity2.this,"remove:" + editPath,Toast.LENGTH_SHORT).show();
                FileProcess.removeDir(DeviceFileEditActivity2.this,editPath);
                adapterRes.getElementsSource().remove(adapterRes.getElementsAtView().remove(position));
                adapterRes.notifyDataSetChanged();
            }
            return true;
        }
    }

    /**
     * onLine control class....
     */
    private class AdapterOnLine extends MainProgramActivity.AdapterRes{

        public void updateData(List<FileProcess.WifiConnectedInfo> list){
            getElementsSource().clear();
            for (FileProcess.WifiConnectedInfo w : list){
                String contentText = w.ip + "\n" + w.mac + " " + w.device;
                int id = getElementsSource().size();
                getElementsSource().add(new DeviceFileEditActivity.Element(contentText,
                        DeviceFileEditActivity.Element.TOP_DEPTH,id,
                        DeviceFileEditActivity.Element.NO_PARENT,
                        DeviceFileEditActivity.Element.FOLDER,false
                ));
                getElementsSource().add(new DeviceFileEditActivity.Element(
                        "MainProgram.txt", 1,id+1,id,
                        DeviceFileEditActivity.Element.TXT,false
                ));
                getElementsSource().add(new DeviceFileEditActivity.Element(
                        "ApSetting.txt", 1,id+2,id,
                        DeviceFileEditActivity.Element.TXT,false
                ));
            }
            updateTreeView();
        }
    }

    private class OnLineItemClickListener extends MainProgramActivity.FolderOpenCloseByItemClickListener{

        public OnLineItemClickListener(MainProgramActivity.AdapterRes adapterRes) {
            super(adapterRes);
        }

        @Override
        public void onSeletedPath(String seletedPath) {
            Log.v("OnLineItemClick",seletedPath);
            String[] splitted = seletedPath.split("[\n /]");
            String ip = splitted[0];
            String mac = splitted[1];
            String device = splitted[2];
            String subItem = "deviceConfig/" + splitted[3];

            if (menuId == MENU_ITEM_ID_goOnlineFromDevice){
                readDeviceData(ip, subItem);
            }
            if (menuId == MENU_ITEM_ID_goOnlineToDevice){
                String programPath = FileProcess.readFile(DeviceFileEditActivity2.this,subItem);
                String data = FileProcess.readFile(DeviceFileEditActivity2.this,programPath);

                writeDeviceData(ip, subItem,data);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (view.getId() == onLineView.getId()){
                //Change to offline
                menuId = MENU_ITEM_ID_preview;//set status
                setTitle("preview");
                ((TextView)onLineView.findViewById(R.id.tv_fileName)).setText("GoOnLine");
                lv_onLineFiles.setAdapter(adapterRes);
                adapterRes.notifyDataSetChanged();
                lv_onLineFiles.setOnItemClickListener(ledDeviceItemClickListener);
                lv_onLineFiles.setOnItemLongClickListener(ledDeviceItemLongClickListener);
                initOffLineMenu();
            }else {
                super.onItemClick(parent, view, position, id);
            }
        }

        private void readDeviceData(String ip, String path){
            new ConnectionDevice(ip,Data.getReadCommand(path));
        }

        private void writeDeviceData(String ip, String path, String data){
            new ConnectionDevice(ip,Data.getWriteCommand(path,data));
        }
    }

    private class ConnectionDevice implements Client.OnUpdateListener{
        private Client client;
        private String requestData,commandMode,path,feedback;

        public ConnectionDevice(String ip,String requestData) {
            this.requestData = requestData;
            Gson gson = new Gson();
            Data.Packet packet = gson.fromJson(requestData,Data.Packet.class);
            this.commandMode = packet.getCommandMode();
            this.path = packet.getFilePath();
            client = new Client(ip,23,this,new Handler());
            client.connectToServer();
        }

        @Override
        public void OnUpdateConnectionMsgInfo(String message) {
            if (message.equals(Client.CONNECT_SUCCESSFUL)){
                client.writeToServer(requestData);
                Log.v("requestData",requestData);
            }
            if (message.equals("close")){
                //TODO updateUI
                Data.startEditActivity(DeviceFileEditActivity2.this,
                        devicePath,new Data.Packet(commandMode,path,feedback),
                        false
                );
            }
        }

        @Override
        public void OnServerSendData(String message) {
            feedback = message;
            client.close("close");
        }
    }
}