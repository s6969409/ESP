package com.example.ledmatrix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.Support.ViewTool;
import com.example.ledmatrix.deviceAdapter.Data;
import com.example.ledmatrix.local.AppSettingActivity;
import com.example.ledmatrix.tcpIp.Client;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceTempActivity extends AppCompatActivity implements Client.OnUpdateListener{
    private RecyclerView rv_onLineFiles;
    private Adapter adapter;

    private ViewTool.ProcessDialog processDialog;
    private Client client;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_temp);

        initView();
        initData();
    }

    private void initView() {
        rv_onLineFiles = findViewById(R.id.rv_onLineFiles);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_onLineFiles.setLayoutManager(manager);
        adapter = new Adapter(new ArrayList<FileNode>(),new ArrayList<FileNode>());
        rv_onLineFiles.setAdapter(adapter);
    }

    private void initData() {
//        String src;
//        if (debugOn) {
//            src = "tt.txt,123.txt,193.txt,423.txt,folder<tt1.txt,333.txt,tt2.txt>,folder2<tt21.txt,3332.txt,tt22.txt>,GFC我.txt";
//            adapter.updateData(src);
//        }else {
//            deviceRequest();
//        }
        deviceRequest();

    }

    private void deviceRequest(){
        Intent intent = getIntent();
        String name = intent.getStringExtra(OnLineListActivity.INTENT_DEVICE_NAME);
        String ip = intent.getStringExtra(OnLineListActivity.INTENT_DEVICE_IP);
        int port = intent.getIntExtra(OnLineListActivity.INTENT_DEVICE_PORT, -1);
        if (ip == null || port == -1){
            Toast.makeText(this,"null",Toast.LENGTH_SHORT).show();
//            Snackbar.make(rv_onLineFiles,"",Snackbar.LENGTH_LONG)
//                    .setAction("action",null).show();
            finish();
            return;
        }

        client = new Client(ip,port,this, new Handler());
        gson = new Gson();

        processDialog = new ViewTool.ProcessDialog(this);
        processDialog.setText("Connect to " + name + "...");
        processDialog.show();

        client.connectToServer();
    }

    @Override
    public void OnUpdateConnectionMsgInfo(String message) {

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
        if (serverFeedback.getCommandMode().equals(Data.Command.GET_FILE_LIST)){
            //getFileList feedback
            adapter.updateData(serverFeedback.getData());
        }
        //client.close("Have gotten feedback, then closed the connection");
    }

    private class FileNode{
        private String name;
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
        public static final String FUNCTION = "function";
        //endregion

        public FileNode(String name, int depth, int id, int parentId, String extension, boolean isExpanded) {
            this.name = name;
            this.depth = depth;
            this.id = id;
            this.parentId = parentId;
            this.extension = extension;
            this.isExpanded = isExpanded;
        }

        //region setter & getter method...
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private List<FileNode> listAtView;
        private List<FileNode> listFromData;
        private int itemViewContentOffset = 50;

        public Adapter(List<FileNode> listAtView, List<FileNode> listFromData) {
            this.listAtView = listAtView;
            this.listFromData = listFromData;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View childView = inflater.inflate(R.layout.item_file,null,false);
            return new ViewHolder(childView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FileNode fileNode = listAtView.get(position);
            holder.iv_fileNodeImg.setPadding(
                    itemViewContentOffset * (fileNode.getDepth() +1),
                    holder.iv_fileNodeImg.getPaddingTop(),
                    holder.iv_fileNodeImg.getPaddingRight(),
                    holder.iv_fileNodeImg.getPaddingBottom()
            );
            if (fileNode.getExtension().equals(FileNode.FOLDER)){
                if (fileNode.isExpanded()){
                    holder.iv_fileNodeImg.setImageResource(R.drawable.folder_open);
                }else {
                    holder.iv_fileNodeImg.setImageResource(R.drawable.folder_close);
                }
            }else if(fileNode.getExtension().equals(FileNode.TXT)){
                holder.iv_fileNodeImg.setImageResource(R.drawable.file);
            }
            holder.tv_fileName.setText(fileNode.getName());
            holder.setClickListener(position);
        }

        @Override
        public int getItemCount() {
            if (listAtView == null) {
                return 0;
            }
            return listAtView.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder{
            public ImageView iv_fileNodeImg;
            public TextView tv_fileName;
            private View view;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iv_fileNodeImg = itemView.findViewById(R.id.iv_fileNodeImg);
                tv_fileName = itemView.findViewById(R.id.tv_fileName);
                this.view = itemView;
            }

            public void setClickListener(int position){
                view.setOnClickListener(new OnClickListener(position));
            }

            private class OnClickListener implements View.OnClickListener {
                private int position;

                public OnClickListener(int position) {
                    this.position = position;
                }

                @Override
                public void onClick(View v) {
                    FileNode clickNode = listAtView.get(position);
                    if (!clickNode.getExtension().equals(FileNode.FOLDER)){
                        Toast.makeText(DeviceTempActivity.this,
                                clickNode.getName(), Toast.LENGTH_SHORT).show();
                        //start new Acrivity to show file content...

                        return;
                    }

                    if (clickNode.isExpanded()){
                        //close folder
                        clickNode.setExpanded(false);
                        List<FileNode> listToDel = new ArrayList<>();
                        for (int i = position + 1; i < listAtView.size();i++){
                            FileNode tempNode = listAtView.get(i);
                            if (tempNode.getDepth() <= clickNode.getDepth()){
                                break;
                            }
                            listToDel.add(tempNode);
                        }
                        listAtView.removeAll(listToDel);
                        notifyDataSetChanged();
                    }else {
                        //open folder
                        clickNode.setExpanded(true);
                        int i = 1;
                        for (FileNode node : listFromData){
                            if (node.getParentId() == clickNode.getId()){
                                node.setExpanded(false);//if use memory open the item of isExpanded,
                                // How to solve it? use "Recursive"?
                                listAtView.add(position + i, node);
                                i++;
                            }
                        }
                        notifyDataSetChanged();
                    }
                }
            }
        }

        public void updateData(String src) {
            listAtView.clear();
            listFromData.clear();

            StringBuilder fileName = new StringBuilder();
            int depth = FileNode.TOP_DEPTH;
            Stack<Integer> stackParentId = new Stack<>();
            stackParentId.push(FileNode.NO_PARENT);
            for (int id = 0; id < src.length(); id++) {
                String s = src.substring(id, id + 1);
                switch (s) {
                    case ","://add file
                        if (fileName.length() == 0) continue;
                        listFromData.add(new FileNode(fileName.toString(),
                                depth, id, stackParentId.peek(), FileNode.TXT, false));
                        fileName.delete(0, fileName.length());
                        break;
                    case "<"://add folder
                        listFromData.add(new FileNode(fileName.toString(),
                                depth, id, stackParentId.peek(), FileNode.FOLDER, false));
                        stackParentId.push(listFromData.get(listFromData.size() - 1).getId());
                        depth++;
                        fileName.delete(0, fileName.length());
                        break;
                    case ">"://add file & end folder
                        if (fileName.length() > 0) {
                            listFromData.add(new FileNode(fileName.toString(),
                                    depth, id, stackParentId.peek(), FileNode.TXT, false));
                        }
                        stackParentId.pop();
                        depth--;
                        fileName.delete(0, fileName.length());
                        break;
                    default:
                        fileName.append(s);
                }
            }

            //add Tree View
            for (FileNode e : listFromData) {
                if (e.getDepth() == FileNode.TOP_DEPTH) {
                    listAtView.add(e);
                }
            }
            notifyDataSetChanged();
        }
    }
}
