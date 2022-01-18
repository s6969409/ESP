package com.example.ledmatrix;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.ledmatrix.Support.ViewTool;
import com.example.ledmatrix.deviceAdapter.Data;
import com.google.android.material.snackbar.Snackbar;

import static com.example.ledmatrix.deviceAdapter.Data.ActivityConstant.RESULT_CODE_writeFileContetnt;

public class FileEditActivity extends AppCompatActivity implements View.OnLongClickListener {
    private TextView tv_filePath,tv_fileContent;
    private EditText ed_fileContent;
    private Snackbar snackbar;

    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_edit);

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
        tv_filePath = findViewById(R.id.tv_filePath);
        tv_fileContent = findViewById(R.id.tv_fileContent);
        ed_fileContent = findViewById(R.id.ed_fileContent);
        ed_fileContent.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        filePath = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
        String fileData = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);

        tv_filePath.setText("path: " + filePath);
        tv_fileContent.setText(fileData);
        ed_fileContent.setText(fileData);

        tv_fileContent.setOnLongClickListener(this);

        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_fileContent, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Save","Cancel",saveCheckListener)
        );
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.tv_fileContent:
                tv_fileContent.setVisibility(View.INVISIBLE);
                ed_fileContent.setVisibility(View.VISIBLE);
                break;
        }
        return true;
    }

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            if(ed_fileContent.getVisibility() == View.VISIBLE){
                tv_fileContent.setText(ed_fileContent.getText());
                ed_fileContent.setVisibility(View.INVISIBLE);
                tv_fileContent.setVisibility(View.VISIBLE);
            }
            if (tv_fileContent.getVisibility() == View.VISIBLE){
                Intent intent = getIntent();
                intent.putExtra(Data.ActivityConstant.INTENT_FILE_DATA,
                        tv_fileContent.getText().toString());
                setResult(RESULT_CODE_writeFileContetnt,intent);
            }
            finish();
        }

        @Override
        public void onClickNo(View v) {
            finish();
        }
    };
}
