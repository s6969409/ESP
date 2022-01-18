package com.example.ledmatrix.deviceAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.ViewTool;
import com.example.ledmatrix.local.AppSettingActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class ProgramActivity extends AppCompatActivity {
    private ListView lv_program;
    private List<ProgramItem.Command> list;
    private Adapter adapter;
    private TextView tv_log;
    private Snackbar snackbar;

    private Gson gson;

    private static final int MENU_ITEM_GROUP_programEdit = 1;
    private static final int MENU_ITEM_ID_onlyPreview = 1;
    private static final int MENU_ITEM_ID_addSingleBlock = 2;
    private static final int MENU_ITEM_ID_setSingleBlock = 3;
    private static final int MENU_ITEM_ID_getSingleBlock = 4;
    private static final int MENU_ITEM_ID_removeSingleBlock = 5;
    private int programEditId = MENU_ITEM_ID_onlyPreview;
    private String setSingleBlockCommand = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program);

        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Data.ActivityConstant.REQUEST_CODE_programBlockEdit
                && resultCode == Data.ActivityConstant.RESULT_CODE_programBlockEdited
        ){
            setSingleBlockCommand = data.getStringExtra(
                    Data.ActivityConstant.INTENT_PROGRAM_BLOCK_EDITED);
            tv_log.setText(getSingleBlockJsonShowStr(
                    (ProgramItem.Command) gson.fromJson(setSingleBlockCommand,getProgramClass(
                            new JsonParser().parse(setSingleBlockCommand)
                    ))
            ));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(MENU_ITEM_GROUP_programEdit,MENU_ITEM_ID_onlyPreview,0,"onlyPreview");
        menu.add(MENU_ITEM_GROUP_programEdit,MENU_ITEM_ID_addSingleBlock,0,"addSingleBlock");
        menu.add(MENU_ITEM_GROUP_programEdit,MENU_ITEM_ID_setSingleBlock,0,"setSingleBlock");
        menu.add(MENU_ITEM_GROUP_programEdit,MENU_ITEM_ID_getSingleBlock,0,"getSingleBlock");
        menu.add(MENU_ITEM_GROUP_programEdit,MENU_ITEM_ID_removeSingleBlock,0,"removeSingleBlock");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this,item.getTitle(),Toast.LENGTH_SHORT).show();
        setTitle(item.getTitle());
        if (item.getGroupId() == MENU_ITEM_GROUP_programEdit) {
            programEditId = item.getItemId();
            if (item.getItemId() == MENU_ITEM_ID_addSingleBlock
                    ||item.getItemId() == MENU_ITEM_ID_setSingleBlock
                    ||item.getItemId() == MENU_ITEM_ID_getSingleBlock ){
                tv_log.setText(getSingleBlockJsonShowStr(
                        (ProgramItem.Command) gson.fromJson(setSingleBlockCommand,getProgramClass(
                                new JsonParser().parse(setSingleBlockCommand)
                        ))
                ));
                tv_log.setOnLongClickListener(clickListener);
            }else {
                tv_log.setOnLongClickListener(null);
            }
        }
        return super.onOptionsItemSelected(item);
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
        gson = new Gson();
        lv_program = findViewById(R.id.lv_program);
        tv_log = findViewById(R.id.tv_log);
        tv_log.setBackgroundColor(Color.YELLOW);
        setSingleBlockCommand = gson.toJson(ProgramItem.nullCommand);
        list = new ArrayList<>();
        initListData2();
        adapter = new Adapter(this,list);
        lv_program.setAdapter(adapter);
        lv_program.setOnItemClickListener(new ListItemClickLitener(list));
        TextView tv_EndItem = new TextView(this);
        tv_EndItem.setTextSize(18);
        tv_EndItem.setText("End");
        tv_EndItem.setTextColor(Color.RED);
        tv_EndItem.setBackgroundColor(Color.DKGRAY);
        lv_program.addFooterView(tv_EndItem);
        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_log, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Save","Cancel",saveCheckListener)
        );
    }

    private void initListData2(){
        Intent intent = getIntent();
        String path = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
        String data = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);
        if (data==null){
            data = getTestDataString();
            if (AppSettingActivity.getAppSetting(this, "LogView").equals("on")) {
                tv_log.append("dataNull\n");
            }
        }
        JsonArray jsonArray = new JsonParser().parse(data).getAsJsonArray();

        for (JsonElement jsonElement:jsonArray){
            if (AppSettingActivity.getAppSetting(this, "LogView").equals("on")) {
                tv_log.append(jsonElement.toString() + "\n");
            }
            list.add((ProgramItem.Command) gson.fromJson(jsonElement, getProgramClass(jsonElement)));
        }
    }

    public static Class<?> getProgramClass(JsonElement jsonElement){
        String type = jsonElement.getAsJsonObject().get(ProgramItem.Command.KEY_type).getAsString();
        if (type.equals(ProgramItem.ReadResFile.KEY_readResFile)){
            return ProgramItem.ReadResFile.class;
        }
        if (type.equals(ProgramItem.Delay.KEY_delay)){
            return ProgramItem.Delay.class;
        }
        if (type.equals(ProgramItem.ReadLedFile.KEY_readLedFile)){
            return ProgramItem.ReadLedFile.class;
        }
        return ProgramItem.Command.class;
    }

    private String getSingleBlockJsonShowStr(ProgramItem.Command programBlock){
        String singleBlockJsonShowStr = programBlock.type;
        singleBlockJsonShowStr += "(";
        if (programBlock.type.equals(ProgramItem.ReadLedFile.KEY_readLedFile)){
            singleBlockJsonShowStr += "ledOffset = " + String.valueOf(programBlock.toReadLedFile().ledOffset) + ",";
            singleBlockJsonShowStr += "path = " + programBlock.toReadLedFile().filePath;
        }
        if (programBlock.type.equals(ProgramItem.Delay.KEY_delay)){
            singleBlockJsonShowStr += String.valueOf(programBlock.toDelay().timeMillis);
        }
        singleBlockJsonShowStr += ")";
        return singleBlockJsonShowStr;
    }

    private String getTestDataString(){
        return "["
                +"{\"type\":\"readResFile\",\"filePath\":\"resource/program\"}"+","
                +"{\"type\":\"readLedFile\",\"filePath\":\"resource/LEDs/example/HellowWord/example/HellowWord/example/HellowWord.txt\",\"ledOffset\":2}"+","
                +"{\"type\":\"delay\",\"timeMillis\":2000}"

                +"]";
    }

    private class Adapter extends BaseAdapter{
        private List<ProgramItem.Command> list;
        private Context context;

        public Adapter(Context context, List<ProgramItem.Command> list) {
            this.list = list;
            this.context = context;
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
            View viewHolder;
            if (convertView == null){
                TextView textView = new TextView(context);
                textView.setTextSize(18);//default unit:sp
                ProgramItem.Command p = list.get(position);
                String tvContetnt = getSingleBlockJsonShowStr(p);
                textView.setText(tvContetnt);
                if (position % 2 == 1){
                    textView.setBackgroundColor(Color.LTGRAY);
                }
                viewHolder = textView;
            }else {
                viewHolder = (View) convertView.getTag();
            }
            return viewHolder;
        }
    }

    private class ListItemClickLitener implements AdapterView.OnItemClickListener{
        private List<ProgramItem.Command> list;

        public ListItemClickLitener(List<ProgramItem.Command> list) {
            this.list = list;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (programEditId == MENU_ITEM_ID_onlyPreview){

            }
            if (programEditId == MENU_ITEM_ID_addSingleBlock){
                JsonElement jsonElement = new JsonParser().parse(setSingleBlockCommand);
                list.add(position, (ProgramItem.Command) gson.fromJson(setSingleBlockCommand, getProgramClass(jsonElement)));
                adapter.notifyDataSetChanged();
            }
            if (programEditId == MENU_ITEM_ID_setSingleBlock){
                list.remove(position);
                JsonElement jsonElement = new JsonParser().parse(setSingleBlockCommand);
                list.add(position, (ProgramItem.Command) gson.fromJson(setSingleBlockCommand, getProgramClass(jsonElement)));
                adapter.notifyDataSetChanged();
            }
            if (programEditId == MENU_ITEM_ID_getSingleBlock){
                setSingleBlockCommand = gson.toJson(list.get(position));
                String showProgram = getSingleBlockJsonShowStr(list.get(position));
                tv_log.setText(showProgram);
                Toast.makeText(ProgramActivity.this,showProgram,Toast.LENGTH_SHORT).show();
            }
            if (programEditId == MENU_ITEM_ID_removeSingleBlock){
                list.remove(position);
                adapter.notifyDataSetChanged();
            }
        }
    }

    public static class ProgramItem {
        //parent class
        public static class Command{
            public String type;
            public static final String COMMAND_NULL = "null";
            public static final String KEY_type = "type";

            public Command(String type) {
                this.type = type;
            }

            public ReadResFile toReadResFile(){
                return (ReadResFile)this;
            }
            public Delay toDelay(){
                return (Delay) this;
            }
            public ReadLedFile toReadLedFile(){
                return (ReadLedFile)this;
            }
        }

        public static class ReadResFile extends Command{
            public String filePath;
            public static final String KEY_readResFile = "readResFile";
            public static final String KEY_filePath = "filePath";

            public ReadResFile(String type, String filePath) {
                super(type);
                this.filePath = filePath;
            }
        }

        public static class Delay extends Command{
            public int timeMillis;
            public static final String KEY_delay = "delay";
            public static final String KEY_timeMillis = "timeMillis";

            public Delay(String type, int timeMillis) {
                super(type);
                this.timeMillis = timeMillis;
            }
        }

        public static class ReadLedFile extends ReadResFile{
            public int ledOffset;
            public static final String KEY_readLedFile = "readLedFile";
            public static final String KEY_ledOffset = "ledOffset";

            public ReadLedFile(String type, String filePath, int ledOffset) {
                super(type, filePath);
                this.ledOffset = ledOffset;
            }
        }

        private static final Command nullCommand = new Command(Command.COMMAND_NULL);
    }

    private View.OnLongClickListener clickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Intent intent = getIntent();
            intent.putExtra(Data.ActivityConstant.INTENT_PROGRAM_BLOCK,setSingleBlockCommand);
            intent.setClass(ProgramActivity.this,ProgramBlockEditActivity.class);
            startActivityForResult(intent,Data.ActivityConstant.REQUEST_CODE_programBlockEdit);
            return true;
        }
    };

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            //save data to device
            Intent intent = getIntent();
            intent.putExtra(Data.ActivityConstant.INTENT_FILE_DATA,gson.toJson(list));
            setResult(Data.ActivityConstant.RESULT_CODE_writeFileContetnt,intent);
            finish();
        }

        @Override
        public void onClickNo(View v) {
            finish();
        }
    };
}
