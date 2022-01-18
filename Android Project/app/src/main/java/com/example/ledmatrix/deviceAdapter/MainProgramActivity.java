package com.example.ledmatrix.deviceAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.ledmatrix.DeviceFileEditActivity;
import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.ViewTool;
import com.example.ledmatrix.local.AppSettingActivity;
import com.example.ledmatrix.local.FileProcess;
import com.example.ledmatrix.tcpIp.Client;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.example.ledmatrix.deviceAdapter.Data.ActivityConstant.RESULT_CODE_writeFileContetnt;

public class MainProgramActivity extends AppCompatActivity {
    private TextView tv_log;
    private ViewPathEdit viewPathEdit;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_program);

        initView();
    }

    @Override
    public void onBackPressed() {
        if (snackbar.isShown()){
            snackbar.dismiss();
        } else {
            snackbar.show();
        }
        //super.onBackPressed();
    }

    private void initView() {
        Intent intent = getIntent();
        String filePath = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
        String fileData = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);

        tv_log = ViewTool.InitLogView(this);
        viewPathEdit = new ViewPathEdit(
                this,"mainProgram", fileData, Data.FolderPath.RES_PROGRAM);
        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_log, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Save","Cancel",saveCheckListener)
        );
    }

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            Intent intent = getIntent();
            intent.putExtra(Data.ActivityConstant.INTENT_FILE_DATA, viewPathEdit.getSeletedPath());
            setResult(RESULT_CODE_writeFileContetnt,intent);
            finish();
        }

        @Override
        public void onClickNo(View v) {
            finish();
        }
    };

    //****************************res***************

    public static class AdapterRes extends BaseAdapter {
        private List<DeviceFileEditActivity.Element> elementsSource;
        private List<DeviceFileEditActivity.Element> elementsAtView;
        private int itemViewContentOffset,folderOpenImageId,folderCloseImageId,fileImageId;

        public AdapterRes() {
            this.elementsSource = new ArrayList<>();
            this.elementsAtView = new ArrayList<>();
            this.itemViewContentOffset = 50;
            this.folderOpenImageId = R.drawable.folder_open;
            this.folderCloseImageId = R.drawable.folder_close;
            this.fileImageId = R.drawable.file;
        }

        public List<DeviceFileEditActivity.Element> getElementsSource() {
            return elementsSource;
        }

        public List<DeviceFileEditActivity.Element> getElementsAtView() {
            return elementsAtView;
        }

        public void setItemImage(int folderOpenImageId,int folderCloseImageId,int fileImageId){
            this.folderOpenImageId = folderOpenImageId;
            this.folderCloseImageId = folderCloseImageId;
            this.fileImageId = fileImageId;
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
            ViewNodeHolder holder;
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, null);
                holder = new ViewNodeHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewNodeHolder) convertView.getTag();
            }

            DeviceFileEditActivity.Element element = elementsAtView.get(position);
            holder.iv_fileNodeImg.setPadding(
                    itemViewContentOffset * (element.getDepth() + 1),
                    holder.iv_fileNodeImg.getPaddingTop(),
                    holder.iv_fileNodeImg.getPaddingRight(),
                    holder.iv_fileNodeImg.getPaddingBottom()
            );
            holder.tv_fileName.setText(element.getContentText());
            if (element.getExtension().equals(DeviceFileEditActivity.Element.FOLDER)) {
                if(element.isExpanded()) {
                    holder.iv_fileNodeImg.setImageResource(folderOpenImageId);
                }else{
                    holder.iv_fileNodeImg.setImageResource(folderCloseImageId);
                }
            } else if(element.getExtension().equals(DeviceFileEditActivity.Element.TXT)){
                holder.iv_fileNodeImg.setImageResource(fileImageId);
            }

            return convertView;
        }

        public void updateDataFromDevice(String src) {
            elementsSource.clear();

            StringBuilder fileName = new StringBuilder();
            int depth = DeviceFileEditActivity.Element.TOP_DEPTH;
            Stack<Integer> stackParentId = new Stack<>();
            stackParentId.push(DeviceFileEditActivity.Element.NO_PARENT);
            for (int i = 0; i < src.length(); i++) {
                String s = src.substring(i, i + 1);
                switch (s) {
                    case ","://add file
                        if (fileName.length() == 0) continue;
                        elementsSource.add(new DeviceFileEditActivity.Element(fileName.toString(),
                                depth, elementsSource.size(), stackParentId.peek(), DeviceFileEditActivity.Element.TXT, false));
                        fileName.delete(0, fileName.length());
                        break;
                    case "<"://add folder
                        elementsSource.add(new DeviceFileEditActivity.Element(fileName.toString(),
                                depth, elementsSource.size(), stackParentId.peek(), DeviceFileEditActivity.Element.FOLDER, false));
                        stackParentId.push(elementsSource.get(elementsSource.size() - 1).getId());
                        depth++;
                        fileName.delete(0, fileName.length());
                        break;
                    case ">"://add file & end folder
                        if (fileName.length() > 0) {
                            elementsSource.add(new DeviceFileEditActivity.Element(fileName.toString(),
                                    depth, elementsSource.size(), stackParentId.peek(), DeviceFileEditActivity.Element.TXT, false));
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
                elementsSource.add(new DeviceFileEditActivity.Element(fileName.toString(),
                        depth, elementsSource.size(), stackParentId.peek(), DeviceFileEditActivity.Element.TXT, false));
                fileName.delete(0, fileName.length());
            }

            updateTreeView();
        }

        protected void updateTreeView(){
            elementsAtView.clear();
            //add Tree View
            for (DeviceFileEditActivity.Element e : elementsSource) {
                if (e.getDepth() == DeviceFileEditActivity.Element.TOP_DEPTH) {
                    elementsAtView.add(e);
                }
            }
            notifyDataSetChanged();
        }

        public void updateDataFromLocal(File file) {
            elementsSource.clear();
            parseFileTreeUpdateToUI(file);
        }

        private void parseFileTreeUpdateToUI(File file) {
            int depth,parentId;
            if (elementsSource.size() == 0){
                depth = DeviceFileEditActivity.Element.TOP_DEPTH;
                parentId = DeviceFileEditActivity.Element.NO_PARENT;
            }else {
                DeviceFileEditActivity.Element last = elementsSource.get(elementsSource.size()-1);
                depth = last.getDepth() + 1;
                parentId = last.getId();
            }

            for (File f : file.listFiles()){
                if (f.isDirectory()){
                    elementsSource.add(new DeviceFileEditActivity.Element(
                            f.getName(), depth, elementsSource.size(), parentId, DeviceFileEditActivity.Element.FOLDER, false
                    ));
                    parseFileTreeUpdateToUI(f);
                }
                if (f.isFile()){
                    elementsSource.add(new DeviceFileEditActivity.Element(
                            f.getName(), depth, elementsSource.size(), parentId, DeviceFileEditActivity.Element.TXT, false
                    ));
                }
            }
            updateTreeView();
        }

        public String getAbsolutePath(int position){
            DeviceFileEditActivity.Element elementOfClick = getElementsAtView().get(position);
            DeviceFileEditActivity.Element temp;
            StringBuilder path = new StringBuilder().append(elementOfClick.getContentText());
            int depthForStartSearch = elementOfClick.getDepth();
            for(int index = position-1; index >= 0 && depthForStartSearch != DeviceFileEditActivity.Element.NO_PARENT; index--){
                temp = getElementsAtView().get(index);
                //check is folder && thisSearchDepth < lastSearchDepth
                if(depthForStartSearch > temp.getDepth()){
                    path.insert(0, temp.getContentText() + "/");
                    depthForStartSearch = temp.getDepth();
                }
            }

            return path.toString();
        }

        public String getFileFolderAbsolutePath(int position){
            DeviceFileEditActivity.Element clickElement = getElementsAtView().get(position);
            int clickDepth = clickElement.getDepth();
            String dirPath = "";
            if (clickElement.getExtension().equals(DeviceFileEditActivity.Element.FOLDER)){
                dirPath = getAbsolutePath(position);
            }
            if (clickDepth != DeviceFileEditActivity.Element.TOP_DEPTH
                    && clickElement.getExtension().equals(DeviceFileEditActivity.Element.TXT)){
                StringBuilder clickPath = new StringBuilder(getAbsolutePath(position));
                dirPath = clickPath.substring(0,
                        clickPath.length() - clickElement.getContentText().length() - 1);
            }
            return dirPath;
        }

        public List<String> getSubItems(int position) {
            List<String> subList = new ArrayList<>();
            DeviceFileEditActivity.Element e = elementsSource.get(position);
            int parentDepth = e.getDepth() + 1;
            if (e.getExtension().equals(DeviceFileEditActivity.Element.FOLDER)) {
                for (int i = 1; e.getDepth() == parentDepth; i++) {
                    e = elementsSource.get(position + i);
                    subList.add(e.getContentText());
                }
            }
            if (e.getExtension().equals(DeviceFileEditActivity.Element.TXT)) {
                for (int i = -1; i < 2; i += 2) {
                    for (int j = 1; position + i * j > 0 && position + i * j < elementsSource.size(); j++) {
                        e = elementsSource.get(position + i * j);
                        if (e.getDepth() == parentDepth) {
                            subList.add(e.getContentText());
                        } else {
                            break;
                        }
                    }
                }
                subList.add(elementsSource.get(position).getContentText());
            }
            return subList;
        }

        //create directory or file dialog sample
        public static class CreateDialog{
            private AlertDialog alertDialog;
            private EditText ed_fileNameInput;
            private Button button;
            private OnCreateListener onCreateListener;

            public CreateDialog(Context context) {
                ed_fileNameInput = new EditText(context);
                AlertDialog.Builder createDialogBuilder = new AlertDialog.Builder(ed_fileNameInput.getContext());
                createDialogBuilder.setTitle("Input create name:");
                createDialogBuilder.setView(ed_fileNameInput);
                createDialogBuilder.setPositiveButton("Create", null);
                alertDialog = createDialogBuilder.create();
            }

            public AlertDialog builde(final String dirPath, String defaultInput, OnCreateListener listener){
                this.onCreateListener = listener;
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String fileName = ed_fileNameInput.getText().toString().trim();
                                onCreateListener.onCreate(dirPath, fileName);
                            }
                        });

                    }
                });
                ed_fileNameInput.setText(defaultInput);
                alertDialog.setMessage("Path at:\n" + dirPath + "/");
                return alertDialog;
            }

            public interface OnCreateListener{
                void onCreate(String dirPath, String createName);
            }
        }
    }

    public static class ViewNodeHolder {
        public ImageView iv_fileNodeImg;
        public TextView tv_fileName;

        public ViewNodeHolder(View convertView) {
            this.iv_fileNodeImg = convertView.findViewById(R.id.iv_fileNodeImg);
            this.tv_fileName = convertView.findViewById(R.id.tv_fileName);
        }
    }

    public static abstract class FolderOpenCloseByItemClickListener implements AdapterView.OnItemClickListener{
        private AdapterRes adapterRes;

        public FolderOpenCloseByItemClickListener(AdapterRes adapterRes) {
            this.adapterRes = adapterRes;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DeviceFileEditActivity.Element elementOfClick =
                    (DeviceFileEditActivity.Element) adapterRes.getItem(position);
            List<DeviceFileEditActivity.Element> elementsAtView = adapterRes.getElementsAtView();
            List<DeviceFileEditActivity.Element> elementsSource = adapterRes.getElementsSource();

            if (!elementOfClick.getExtension().equals(DeviceFileEditActivity.Element.FOLDER)){
                onSeletedPath(adapterRes.getAbsolutePath(position));
                return;
            }

            //region open/close folder
            if (elementOfClick.isExpanded()) {
                //close sub item
                elementOfClick.setExpanded(false);
                List<DeviceFileEditActivity.Element> elementsToDel = new ArrayList<>();
                for (int i = position + 1; i < elementsAtView.size(); i++) {
                    if (elementOfClick.getDepth() >= elementsAtView.get(i).getDepth())
                        break;
                    elementsToDel.add(elementsAtView.get(i));
                }
                elementsAtView.removeAll(elementsToDel);
                adapterRes.notifyDataSetChanged();
            } else {
                //open sub item
                elementOfClick.setExpanded(true);

                int i = 1;
                for (DeviceFileEditActivity.Element e : elementsSource) {
                    if (e.getParentId() == elementOfClick.getId()) {
                        e.setExpanded(false);//if use memory open the item of isExpanded,
                        // How to solve it? use "Recursive"?
                        elementsAtView.add(position + i, e);
                        i++;
                    }
                }
                adapterRes.notifyDataSetChanged();
            }
            //endregion
        }

        public abstract void onSeletedPath(String seletedPath);
    }

    public static class ViewPathEdit{
        private TextView tv_path;
        private Button btn_browse;
        private ListView lv_resList;
        private AdapterRes adapterRes;

        public ViewPathEdit(Activity activity,String name,String lastPath,String searchFolder) {
            this.tv_path = activity.findViewById(R.id.tv_itemPathEdit_path);
            this.tv_path.setText(lastPath);

            this.lv_resList = activity.findViewById(R.id.lv_itemPathEdit_resList);
            this.adapterRes = new AdapterRes();
            this.lv_resList.setAdapter(adapterRes);
            this.lv_resList.setOnItemClickListener(new FolderOpenCloseByItemClickListener(adapterRes) {
                @Override
                public void onSeletedPath(String seletedPath) {
                    tv_path.setText(seletedPath);
                    adapterRes.updateDataFromDevice("");//clearListView
                }
            });

            this.btn_browse = activity.findViewById(R.id.btn_itemPathEdit_browse);
            this.btn_browse.setText(name + "\nBrowse");
            this.btn_browse.setOnClickListener(new BrowseClickListener(
                    activity,searchFolder,this.adapterRes)
            );
        }

        private class BrowseClickListener implements View.OnClickListener{
            private Activity activity;
            private String searchFolder;
            private AdapterRes adapterRes;

            public BrowseClickListener(Activity activity, String searchFolder, AdapterRes adapterRes) {
                this.activity = activity;
                this.searchFolder = searchFolder;
                this.adapterRes = adapterRes;
            }

            @Override
            public void onClick(View v) {
                String saveAt = AppSettingActivity.getAppSetting(activity,"dataSaveAt");

                if (saveAt.equals("local")){
                    String localPath = activity.getIntent().getStringExtra(
                            Data.ActivityConstant.INTENT_DEVICE_PATH);
                    File file = new File(activity.getExternalFilesDir(null),
                            localPath + "/" + searchFolder
                    );
                    adapterRes.updateDataFromLocal(file);
                }
                if (saveAt.equals("onLine")){
                    String getFileListFromDevice = FileProcess.readFile(
                            activity,FileProcess.PATH_APP_GLOBAL_TEMP);
                    String targetFolderItemsStr = Data.getItemInFolderPathByStr(
                            getFileListFromDevice,searchFolder,true);
                    adapterRes.updateDataFromDevice(targetFolderItemsStr);
                }
            }
        }

        public String getSeletedPath(){
            return tv_path.getText().toString();
        }
    }
}