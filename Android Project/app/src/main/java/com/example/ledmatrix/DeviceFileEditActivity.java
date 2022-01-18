package com.example.ledmatrix;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.Support.ViewTool;
import com.example.ledmatrix.deviceAdapter.Data;
import com.example.ledmatrix.local.AppSettingActivity;
import com.example.ledmatrix.local.FileProcess;
import com.example.ledmatrix.tcpIp.Client;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.example.ledmatrix.deviceAdapter.Data.ActivityConstant.RESULT_CODE_writeFileContetnt;

public class DeviceFileEditActivity extends AppCompatActivity implements Client.OnUpdateListener {
    private ListView lv_onLineFiles;
    private List<Element> elementsAtTree;
    private List<Element> elementsAtSource;
    private TreeViewAdapter adapter;

    private ViewTool.ProcessDialog processDialog;
    private Client client;
    private Gson gson;

    public static final String INTENT_SERVER_CLOSE_MSG = "closeMsg";
    public static final int RESULT_CODE_serverClose = 101;

    private static final int FILEMODE_READ = 0;
    private static final int FILEMODE_CREATE = 1;
    private static final int FILEMODE_MAKEDIR = 2;
    private static final int FILEMODE_REMOVE = 3;
    private int fileMode = FILEMODE_READ;

    private boolean showConnectionRequestDialogEnable, openFileUseTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_file_edit);

        initView();
        getAppSettingData();
        createAndConnectServer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Data.ActivityConstant.REQUEST_CODE_showFileContent
                && resultCode == RESULT_CODE_writeFileContetnt
        ){
            //do write file
            String filePath = data.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
            String fileContent = data.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);
            processDialog = new ViewTool.ProcessDialog(DeviceFileEditActivity.this);
            processDialog.setText("write "+filePath+" data...");
            processDialog.show();
            client.writeToServer(Data.getWriteCommand(filePath,fileContent));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_edit_function,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_createFile:
                Toast.makeText(this,"create!!!",Toast.LENGTH_SHORT).show();
                fileMode = FILEMODE_CREATE;
                setTitle("Select path to create file");
                break;
            case R.id.action_makeDir:
                Toast.makeText(this,"makeDir!!!",Toast.LENGTH_SHORT).show();
                fileMode = FILEMODE_MAKEDIR;
                setTitle("Select path to create folder");
                break;
            case R.id.action_removeFile:
                Toast.makeText(this,"remove!!!",Toast.LENGTH_SHORT).show();
                fileMode = FILEMODE_REMOVE;
                setTitle("Select file to remove");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (fileMode != FILEMODE_READ){
            setFileModeToRead();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        client.close(Client.CLIENT_COMMAND);
        super.onDestroy();
    }

    private void initView() {
        lv_onLineFiles = findViewById(R.id.lv_onLineFiles);
        elementsAtTree = new ArrayList<>();
        elementsAtSource = new ArrayList<>();
        adapter = new TreeViewAdapter(this, elementsAtSource, elementsAtTree);
        lv_onLineFiles.setAdapter(adapter);
        lv_onLineFiles.setOnItemClickListener(new TreeViewItemClickListener(adapter));
        lv_onLineFiles.setOnItemLongClickListener(new TreeViewItemLongClickListener(adapter));
    }

    private void getAppSettingData() {
        showConnectionRequestDialogEnable = AppSettingActivity.getAppSetting(
                this,"showConnectionRequestDialog").equals("on");
        openFileUseTxt = false;
    }

    private void createAndConnectServer() {
        Intent intent = getIntent();
        String name = intent.getStringExtra(OnLineListActivity.INTENT_DEVICE_NAME);
        String ip = intent.getStringExtra(OnLineListActivity.INTENT_DEVICE_IP);
        int port = intent.getIntExtra(OnLineListActivity.INTENT_DEVICE_PORT, -1);

        client = new Client(ip,port,this, new Handler());
        gson = new Gson();

        processDialog = new ViewTool.ProcessDialog(this);
        processDialog.setText("Connect to " + name + "...");
        processDialog.show();

        client.connectToServer();
    }

    private void getListData() {
        processDialog = new ViewTool.ProcessDialog(this);
        processDialog.setText("Read device data...");
        processDialog.show();

        client.writeToServer(Data.getFileListCommand());
    }

    private void setFileModeToRead(){
        fileMode = FILEMODE_READ;
        setTitle("Select file to read");
    }

    private void startReadFileCommand(String filePath){
        processDialog = new ViewTool.ProcessDialog(this);
        processDialog.setText("read "+filePath+" data...");
        processDialog.show();
        client.writeToServer(Data.getReadCommand(filePath));
    }

    @Override
    public void OnUpdateConnectionMsgInfo(String message) {
        //Connect server successful
        if (message.equals(Client.CONNECT_SUCCESSFUL)) {
            processDialog.dismiss();
            getListData();
            return;
        }
        //Connect server failed
        if (message.equals(Client.CONNECT_FAILED)) {
            processDialog.dismiss();
            Intent intent = getIntent();
            intent.putExtra(INTENT_SERVER_CLOSE_MSG,message);
            setResult(RESULT_CODE_serverClose,intent);
            finish();
            return;
        }
        //Filter the write command dialog
        if (!showConnectionRequestDialogEnable
                && message.length() >= Client.WRITE_TITTLE.length()
                && message.substring(0, Client.WRITE_TITTLE.length()).equals(Client.WRITE_TITTLE)){
            return;
        }
        //region Check status for developer
        if(message.equals("")) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("System message");
        builder.setMessage(message);
        //builder.setCancelable(false);
        builder.create().show();
        //endregion
    }

    @Override
    public void OnServerSendData(String message) {
        //Server feedBack filter
        int delLength = Client.READ_TITTLE.length();
        if (!message.substring(0, delLength).equals(Client.READ_TITTLE)) {
            return;
        }

        processDialog.dismiss();
        StringBuilder decodeSrc = new StringBuilder(message);
        decodeSrc.delete(0, delLength);
        Data.Packet serverFeedback = gson.fromJson(decodeSrc.toString(),Data.Packet.class);
        //readFile feedback
        if (serverFeedback.getCommandMode().equals(Data.Command.READ_FILE)) {
            //to start fileClass activity
            boolean openFileUseTxtBuffer = openFileUseTxt;
            openFileUseTxt = false;
            Data.startEditActivity(this,"" , serverFeedback, openFileUseTxtBuffer);
        }
        //feedback of writeFile || removeFile || makeDir || removeDir
        if (serverFeedback.getCommandMode().equals(Data.Command.WRITE_FILE)
                ||serverFeedback.getCommandMode().equals(Data.Command.REMOVE_FILE)
                ||serverFeedback.getCommandMode().equals(Data.Command.MAKE_DIR)
                ||serverFeedback.getCommandMode().equals(Data.Command.REMOVE_DIR)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(serverFeedback.getData());
            builder.setPositiveButton("OK",null);
            builder.show();
            setFileModeToRead();
            if (serverFeedback.getData().equals("true")) {
                getListData();
            }
        }
        //getFileList feedback
        if (serverFeedback.getCommandMode().equals(Data.Command.GET_FILE_LIST)){
            adapter.updateDataFromDevice(serverFeedback.getData());
            FileProcess.writeFile(this,FileProcess.PATH_APP_GLOBAL_TEMP,serverFeedback.getData());
            setFileModeToRead();
        }
    }

    public static class Element {
        private String contentText;
        private int depth;
        private int id;
        private int parentId;
        private String extension;
        private boolean isExpanded;

        //region constant define...
        public static final int NO_PARENT = -1;
        public static final int TOP_DEPTH = 0;

        public static final String TXT = ".txt";
        public static final String FOLDER = "";
        //endregion

        public Element(String contentText, int depth, int id, int parentId, String extension, boolean isExpanded) {
            this.contentText = contentText;
            this.depth = depth;
            this.id = id;
            this.parentId = parentId;
            this.extension = extension;
            this.isExpanded = isExpanded;
        }

        //region setter & getter method...
        public String getContentText() {
            return contentText;
        }

        public void setContentText(String contentText) {
            this.contentText = contentText;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getParentId() {
            return parentId;
        }

        public void setParentId(int parentId) {
            this.parentId = parentId;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public void setExpanded(boolean expanded) {
            isExpanded = expanded;
        }
        //endregion
    }

    private class TreeViewAdapter extends BaseAdapter {
        private List<Element> elementsSource;
        private List<Element> elementsAtView;
        private LayoutInflater layoutInflater;
        private int itemViewContentOffset;
        private Context context;
        private EditText ed_fileNameInput;

        public static final int DIALOG_MODE_CREATE_FILE = 0;
        public static final int DIALOG_MODE_MAKE_DIR = 1;

        public TreeViewAdapter(Context context, List<Element> elementsSource, List<Element> elementsAtView) {
            this.elementsSource = elementsSource;
            this.elementsAtView = elementsAtView;
            this.layoutInflater = LayoutInflater.from(context);
            this.itemViewContentOffset = 50;
            this.context = context;
        }

        public List<Element> getElementsAtView() {
            return elementsAtView;
        }

        public List<Element> getElementsSource() {
            return elementsSource;
        }

        @Override
        public int getCount() {
            return elementsAtView.size();
        }

        @Override
        public Object getItem(int position) {
            return elementsAtView.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_file, null);
                viewHolder = new ViewHolder(
                        (ImageView) convertView.findViewById(R.id.iv_fileNodeImg),
                        (TextView) convertView.findViewById(R.id.tv_fileName)
                );
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            Element element = elementsAtView.get(position);

            viewHolder.iv_fileNodeImg.setPadding(
                    itemViewContentOffset * (element.getDepth() + 1),
                    viewHolder.iv_fileNodeImg.getPaddingTop(),
                    viewHolder.iv_fileNodeImg.getPaddingRight(),
                    viewHolder.iv_fileNodeImg.getPaddingBottom()
            );
            viewHolder.tv_fileName.setText(element.getContentText());
            if (element.getExtension().equals(Element.FOLDER)) {
                if(element.isExpanded()) {
                    viewHolder.iv_fileNodeImg.setImageResource(R.drawable.folder_open);
                }else{
                    viewHolder.iv_fileNodeImg.setImageResource(R.drawable.folder_close);
                }
            } else if(element.getExtension().equals(Element.TXT)){
                viewHolder.iv_fileNodeImg.setImageResource(R.drawable.file);
            }

            return convertView;
        }

        public void updateDataFromDevice(String src) {
            elementsSource.clear();
            elementsAtView.clear();

            StringBuilder fileName = new StringBuilder();
            int depth = Element.TOP_DEPTH;
            Stack<Integer> stackParentId = new Stack<>();
            stackParentId.push(Element.NO_PARENT);
            for (int id = 0; id < src.length(); id++) {
                String s = src.substring(id, id + 1);
                switch (s) {
                    case ","://add file
                        if (fileName.length() == 0) continue;
                        elementsSource.add(new Element(fileName.toString(),
                                depth, elementsSource.size(), stackParentId.peek(), Element.TXT, false));
                        fileName.delete(0, fileName.length());
                        break;
                    case "<"://add folder
                        elementsSource.add(new Element(fileName.toString(),
                                depth, elementsSource.size(), stackParentId.peek(), Element.FOLDER, false));
                        stackParentId.push(elementsSource.get(elementsSource.size() - 1).getId());
                        depth++;
                        fileName.delete(0, fileName.length());
                        break;
                    case ">"://add file & end folder
                        if (fileName.length() > 0) {
                            elementsSource.add(new Element(fileName.toString(),
                                    depth, elementsSource.size(), stackParentId.peek(), Element.TXT, false));
                        }
                        stackParentId.pop();
                        depth--;
                        fileName.delete(0, fileName.length());
                        break;
                    default:
                        fileName.append(s);
                }
            }
            if (fileName.length() > 0){
                elementsSource.add(new Element(fileName.toString(),
                        depth, elementsSource.size(), stackParentId.peek(), Element.TXT, false));
                fileName.delete(0, fileName.length());
            }

            //add Tree View
            for (Element e : elementsSource) {
                if (e.getDepth() == Element.TOP_DEPTH) {
                    elementsAtView.add(e);
                }
            }
            notifyDataSetChanged();
        }

        public String getAbsolutePath(int position){
            Element elementOfClick = getElementsAtView().get(position);
            Element temp;
            StringBuilder path = new StringBuilder().append(elementOfClick.getContentText());
            int depthForStartSearch = elementOfClick.getDepth();
            for(int index = position-1;index >= 0 && depthForStartSearch != Element.NO_PARENT;index--){
                temp = getElementsAtView().get(index);
                //check is folder && thisSearchDepth < lastSearchDepth
                if(depthForStartSearch > temp.getDepth()){
                    path.insert(0, temp.getContentText() + "/");
                    depthForStartSearch = temp.getDepth();
                }
            }

            return path.toString();
        }

        public List<Element> getSubItems(int position){
            List<Element> subList = new ArrayList<>();
            Element e = elementsSource.get(position);
            int parentDepth = e.getDepth() + 1;
            if (e.getExtension().equals(Element.FOLDER)){
                for(int i = 1;e.getDepth() == parentDepth;i++){
                    e = elementsSource.get(position + i);
                    subList.add(e);
                }
            }
            if (e.getExtension().equals(Element.TXT)){
                for(int i=-1;i<2;i+=2) {
                    for (int j = 1;position+i*j>0 && position+i*j<elementsSource.size(); j++) {
                        e = elementsSource.get(position + i*j);
                        if (e.getDepth() == parentDepth) {
                            subList.add(e);
                        } else {
                            break;
                        }
                    }
                }
                subList.add(elementsSource.get(position));
            }
            return subList;
        }

        //create directory or file dialog sample
        public AlertDialog initCreateDialog(int position,int mode){
            //region get dirPath
            Element clickElement = elementsAtView.get(position);
            int clickDepth = elementsAtView.get(position).getDepth();
            String dirPath = "";
            if (clickElement.getExtension().equals(Element.FOLDER)){
                dirPath = getAbsolutePath(position);
            }
            if (clickDepth != Element.TOP_DEPTH
                    && clickElement.getExtension().equals(Element.TXT)){
                StringBuilder clickPath = new StringBuilder(getAbsolutePath(position));
                dirPath = clickPath.substring(0,
                        clickPath.length() - clickElement.getContentText().length() - 1);
            }
            //endregion

            ed_fileNameInput = new EditText(context);
            AlertDialog.Builder createDialogBuilder = new AlertDialog.Builder(context);
            createDialogBuilder.setTitle("Input create name:");
            createDialogBuilder.setMessage(dirPath + "/");
            createDialogBuilder.setView(ed_fileNameInput);
            createDialogBuilder.setPositiveButton("Create", null);
            AlertDialog dialog = createDialogBuilder.create();
            OnShowListener onShowListener = new OnShowListener(dialog, position, dirPath,mode);
            dialog.setOnShowListener(onShowListener);
            return dialog;
        }

        private class OnShowListener implements DialogInterface.OnShowListener{
            AlertDialog alertDialog;
            Button button;
            int position;
            String dirPath;
            int mode;

            public OnShowListener(AlertDialog alertDialog, int position, String dirPath,int mode) {
                this.alertDialog = alertDialog;
                this.position = position;
                this.dirPath = dirPath==""?"":dirPath+"/";
                this.mode = mode;
            }

            @Override
            public void onShow(DialogInterface dialog) {
                button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String fileName = ed_fileNameInput.getText().toString().trim();
                        if(fileName.equals("")){
                            return;
                        }

                        String fileNameCheckMsg = FileProcess.CheckFileName(fileName,10);
                        if(!fileNameCheckMsg.equals("")){
                            alertDialog.setTitle(fileNameCheckMsg);
                            return;
                        }

                        String createName = fileName;
                        if (mode == TreeViewAdapter.DIALOG_MODE_CREATE_FILE){
                            createName += ".txt";
                        }
                        for (Element e : getSubItems(position)){
                            if (e.getContentText().equals(createName)){
                                alertDialog.setTitle("has same name!");
                                return;
                            }
                        }

                        if (mode == TreeViewAdapter.DIALOG_MODE_MAKE_DIR){
                            client.writeToServer(Data.getMakeDirCommand(dirPath + fileName));
                        }
                        fileName += ".txt";
                        if (mode == TreeViewAdapter.DIALOG_MODE_CREATE_FILE
                                && Data.getFileClass(dirPath).equals(Data.FolderPath.UNKNOW)) {
                            client.writeToServer(Data.getWriteCommand(dirPath + fileName, "new file"));
                        }
                        if (mode == TreeViewAdapter.DIALOG_MODE_CREATE_FILE
                                && Data.getFileClass(dirPath).equals(Data.FolderPath.RES_LEDS)) {
                            Data.LedData[] ledData = new Data.LedData[]{new Data.LedData(0,0,0,0)};
                            client.writeToServer(Data.getWriteCommand(dirPath + fileName,gson.toJson(ledData) ));
                        }
                        if (mode == TreeViewAdapter.DIALOG_MODE_CREATE_FILE
                                && Data.getFileClass(dirPath).equals(Data.FolderPath.RES_PROGRAM)) {
                            client.writeToServer(Data.getWriteCommand(dirPath + fileName,"[]"));
                        }
                        alertDialog.cancel();
                    }
                });
            }
        }
    }

    private class ViewHolder {
        public ImageView iv_fileNodeImg;
        public TextView tv_fileName;

        public ViewHolder(ImageView iv_fileNodeImg, TextView tv_fileName) {
            this.iv_fileNodeImg = iv_fileNodeImg;
            this.tv_fileName = tv_fileName;
        }
    }

    private class TreeViewItemClickListener implements AdapterView.OnItemClickListener {
        private TreeViewAdapter treeViewAdapter;

        public TreeViewItemClickListener(TreeViewAdapter treeViewAdapter) {
            this.treeViewAdapter = treeViewAdapter;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Element elementOfClick = (Element) treeViewAdapter.getItem(position);
            List<Element> elementsAtView = treeViewAdapter.getElementsAtView();
            List<Element> elementsSource = treeViewAdapter.getElementsSource();

            //start new Acrivity to show file content
            if (!elementOfClick.getExtension().equals(Element.FOLDER)) {
                if (fileMode != FILEMODE_READ){
                    return;
                }
                String filePath = treeViewAdapter.getAbsolutePath(position);
                startReadFileCommand(filePath);
                return;
            }

            //open/close folder
            if (elementOfClick.isExpanded()) {
                //close sub item
                elementOfClick.setExpanded(false);
                List<Element> elementsToDel = new ArrayList<>();
                for (int i = position + 1; i < elementsAtView.size(); i++) {
                    if (elementOfClick.getDepth() >= elementsAtView.get(i).getDepth())
                        break;
                    elementsToDel.add(elementsAtView.get(i));
                }
                elementsAtView.removeAll(elementsToDel);
                treeViewAdapter.notifyDataSetChanged();
            } else {
                //open sub item
                elementOfClick.setExpanded(true);

                int i = 1;
                for (Element e : elementsSource) {
                    if (e.getParentId() == elementOfClick.getId()) {
                        e.setExpanded(false);//if use memory open the item of isExpanded,
                        // How to solve it? use "Recursive"?
                        elementsAtView.add(position + i, e);
                        i++;
                    }
                }
                treeViewAdapter.notifyDataSetChanged();
            }
        }
    }

    private class TreeViewItemLongClickListener implements AdapterView.OnItemLongClickListener {
        private TreeViewAdapter treeViewAdapter;

        public TreeViewItemLongClickListener(TreeViewAdapter treeViewAdapter) {
            this.treeViewAdapter = treeViewAdapter;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            //remove mode
            if (fileMode == FILEMODE_REMOVE) {
                if (treeViewAdapter.getElementsAtView().get(position).getExtension()
                        .equals(Element.FOLDER)) {
                    //remove dir
                    client.writeToServer(Data.getRemoveDirCommand(
                            treeViewAdapter.getAbsolutePath(position)));
                }
                if (treeViewAdapter.getElementsAtView().get(position).getExtension()
                        .equals(Element.TXT)) {
                    //remove file
                    client.writeToServer(Data.getRemoveFileCommand(
                            treeViewAdapter.getAbsolutePath(position)));
                }
            }
            //create file
            if (fileMode == FILEMODE_CREATE){
                treeViewAdapter.initCreateDialog(
                        position,TreeViewAdapter.DIALOG_MODE_CREATE_FILE).show();
            }
            //create dir
            if (fileMode == FILEMODE_MAKEDIR){
                treeViewAdapter.initCreateDialog(
                        position,TreeViewAdapter.DIALOG_MODE_MAKE_DIR).show();
            }
            if (fileMode == FILEMODE_READ){//debug use Toast
//                Toast.makeText(DeviceFileEditActivity.this,
//                        String.valueOf(treeViewAdapter.getAbsolutePath(position)),
//                        Toast.LENGTH_SHORT).show();
                if (treeViewAdapter.getElementsAtView().get(position).getExtension().equals(Element.TXT)) {
                    openFileUseTxt = true;
                    String filePath = treeViewAdapter.getAbsolutePath(position);
                    startReadFileCommand(filePath);
                }
            }
            // 回傳 false，長按後該項目被按下的狀態會保持。
            return true;
        }
    }
}
