package com.example.ledmatrix.deviceAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.UserFragment;
import com.example.ledmatrix.Support.ViewTool;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ProgramBlockEditActivity extends AppCompatActivity implements UserFragment.OnFragmentInteractionListener{
    private TextView tv_log;
    private Spinner sp_blockType;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private UserFragment nullFragment, readLedFileFragment, delayFragment;
    private Snackbar snackbar;

    private Gson gson;

    private static final int COMMAND_null = 0;
    private static final int COMMAND_readLedFile = 1;
    private static final int COMMAND_delay = 2;

    private ProgramActivity.ProgramItem.Command editProgramBlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_block_edit);

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
        gson = new Gson();
        tv_log = ViewTool.InitLogView(this);
        initEditProgramBlock();
        //程式單節設定View
        sp_blockType = findViewById(R.id.sp_blockType);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(//想一下如何處理到外面
                this,
                R.layout.support_simple_spinner_dropdown_item,
                new String[]{
                        ProgramActivity.ProgramItem.Command.COMMAND_NULL,
                        ProgramActivity.ProgramItem.ReadLedFile.KEY_readLedFile,
                        ProgramActivity.ProgramItem.Delay.KEY_delay
                }
        );
        sp_blockType.setAdapter(arrayAdapter);
        sp_blockType.setOnItemSelectedListener(spItemSelectedListener);
        sp_blockType.setSelection(getSelectionFromEditProgramBlock());

        //fragment init
        fragmentManager = getSupportFragmentManager();
        nullFragment = new NullFragment();
        readLedFileFragment = new ReadLedFileFragment();
        delayFragment = new DelayFragment();
        //default fragment setting?
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_programBlockEdit,nullFragment);
        fragmentTransaction.commit();

        //snackbar init
        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_log, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Save","Cancel",saveCheckListener)
        );
    }

    private void initEditProgramBlock(){
        Intent intent = getIntent();
        String blockJsonStr = intent.getStringExtra(Data.ActivityConstant.INTENT_PROGRAM_BLOCK);

        JsonElement jsonElement = new JsonParser().parse(blockJsonStr);
        editProgramBlock = (ProgramActivity.ProgramItem.Command)
                gson.fromJson(jsonElement, ProgramActivity.getProgramClass(jsonElement));
    }

    private int getSelectionFromEditProgramBlock(){
        if (editProgramBlock.type.equals(ProgramActivity.ProgramItem.Command.COMMAND_NULL)){
            return COMMAND_null;
        }
        if (editProgramBlock.type.equals(ProgramActivity.ProgramItem.ReadLedFile.KEY_readLedFile)){
            return COMMAND_readLedFile;
        }
        if (editProgramBlock.type.equals(ProgramActivity.ProgramItem.Delay.KEY_delay)){
            return COMMAND_delay;
        }
        return -1;
    }

    //spinnerItemSelectedListener
    private AdapterView.OnItemSelectedListener spItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            //update fragment view
            fragmentTransaction = fragmentManager.beginTransaction();
            switch (position){
                case COMMAND_null:
                    fragmentTransaction.replace(R.id.fragment_programBlockEdit,nullFragment);
                    break;
                case COMMAND_readLedFile:
                    fragmentTransaction.replace(R.id.fragment_programBlockEdit,readLedFileFragment);
                    break;
                case COMMAND_delay:
                    fragmentTransaction.replace(R.id.fragment_programBlockEdit,delayFragment);
                    break;
            }
            fragmentTransaction.commit();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    public String getData() {
        return gson.toJson(editProgramBlock);
    }

    private void saveEditProgramBlock() {
        UserFragment currentFragment = (UserFragment)
                fragmentManager.findFragmentById(R.id.fragment_programBlockEdit);
        String data = currentFragment.getData();

        if (data == null){
            return;
        }

        Intent intent = getIntent();
        intent.putExtra(Data.ActivityConstant.INTENT_PROGRAM_BLOCK_EDITED,data);
        setResult(Data.ActivityConstant.RESULT_CODE_programBlockEdited,intent);
        finish();
    }

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            saveEditProgramBlock();
        }

        @Override
        public void onClickNo(View v) {
            finish();
        }
    };
}
