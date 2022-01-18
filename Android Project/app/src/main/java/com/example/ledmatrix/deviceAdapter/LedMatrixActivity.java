package com.example.ledmatrix.deviceAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.ValueAdjustListenerBind;
import com.example.ledmatrix.Support.ViewTool;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.ledmatrix.deviceAdapter.Data.ActivityConstant.RESULT_CODE_writeFileContetnt;

public class LedMatrixActivity extends AppCompatActivity {
    private List<Data.LedData> list;

    private int subViewDistance;
    private GridView gv_leds;
    private Adapter adapter;
    private ImageView iv_ledPreview;
    private ImageButton ib_colorR_up,ib_colorR_down,ib_colorG_up,ib_colorG_down,ib_colorB_up,ib_colorB_down;
    private TextView tv_log,tv_valueColorR,tv_valueColorG,tv_valueColorB;
    private Snackbar snackbar;
    private Handler UIHandler;

    private Gson gson;

    private static final int MENU_ITEM_ID_onlyPreview = 1;
    private static final int MENU_ITEM_ID_addLedColor_front = 2;
    private static final int MENU_ITEM_ID_addLedColor_back = 3;
    private static final int MENU_ITEM_ID_setLedColor = 4;
    private static final int MENU_ITEM_ID_getLedColor = 5;
    private static final int MENU_ITEM_ID_removeLedColor = 6;
    private int menuItemSelectedID = MENU_ITEM_ID_onlyPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_matrix);

        initData();
        //initView();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,MENU_ITEM_ID_onlyPreview,0,"onlyPreview");
        menu.add(0,MENU_ITEM_ID_addLedColor_front,0,"addLedColor(front)");
        menu.add(0,MENU_ITEM_ID_addLedColor_back,0,"addLedColor(back)");
        menu.add(0, MENU_ITEM_ID_setLedColor,0,"setColor");
        menu.add(0, MENU_ITEM_ID_getLedColor,0,"getColor");
        menu.add(0,MENU_ITEM_ID_removeLedColor,0,"removeLedColor");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this,item.getTitle(),Toast.LENGTH_SHORT).show();
        menuItemSelectedID = item.getItemId();
        setTitle(item.getTitle());
        //tv_log.append(item.getTitle() + "\n");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        initViewAtWindowFocusChanged();
        if (gv_leds.getWidth() < gv_leds.getHeight()){
            subViewDistance = gv_leds.getWidth()/8;
        } else {
            subViewDistance = gv_leds.getHeight()/8;
        }
        gv_leds.setColumnWidth(subViewDistance);
        gv_leds.setVisibility(View.VISIBLE);
    }

    private void initData(){
        tv_log = ViewTool.InitLogView(this);
        gson = new Gson();
        Intent intent = getIntent();

        String data = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_DATA);
        if (data == null || data.equals("[]")) {
            tv_log.setText("dataCreateError!"+"\n"+"createTestData");
            list = new ArrayList<>();
            list.add(new Data.LedData(0,200,0,0));
        }else {
            list = new ArrayList<>(Arrays.asList(gson.fromJson(data,Data.LedData[].class)));
        }

        String filePath = intent.getStringExtra(Data.ActivityConstant.INTENT_FILE_PATH);
        if (filePath != null) {
            tv_log.setText(filePath);
        } else {
            tv_log.setText("--"+data);
        }

        UIHandler = new Handler();

        initColorControlView();

        setTitle("onlyPreview");

        snackbar = ViewTool.YesNoSelectionBar.initSnackBar(
                tv_log, ViewTool.YesNoSelectionBar.YesNoView.init(
                        this,"Save","Cancel",saveCheckListener)
        );
    }

    private void initColorControlView() {
        ib_colorR_up = findViewById(R.id.ib_colorR_up);
        tv_valueColorR = findViewById(R.id.tv_valueColorR);
        tv_valueColorR.setBackgroundColor(Color.rgb(
                Integer.valueOf(tv_valueColorR.getText().toString()),0,0)
        );
        ib_colorR_down = findViewById(R.id.ib_colorR_down);
        ValueAdjustListenerBind valueAdjustListenerBind_R = new ValueAdjustListenerBind(
                UIHandler, 6, 255, 100);
        valueAdjustListenerBind_R.bindView(tv_valueColorR, ib_colorR_up, ib_colorR_down);
        valueAdjustListenerBind_R.setOnValueUpdateListener(new ValueAdjustListenerBind.OnValueUpdateListener() {
            @Override
            public void onValueUpadate(int value) {
                tv_valueColorR.setBackgroundColor(Color.rgb(value,0,0));
                updateRGB();
            }
        });

        ib_colorG_up = findViewById(R.id.ib_colorG_up);
        tv_valueColorG = findViewById(R.id.tv_valueColorG);
        tv_valueColorG.setBackgroundColor(Color.rgb(
                0,Integer.valueOf(tv_valueColorG.getText().toString()),0)
        );
        ib_colorG_down = findViewById(R.id.ib_colorG_down);
        ValueAdjustListenerBind valueAdjustListenerBind_G = new ValueAdjustListenerBind(
                UIHandler, 6, 255, 100);
        valueAdjustListenerBind_G.bindView(tv_valueColorG, ib_colorG_up, ib_colorG_down);
        valueAdjustListenerBind_G.setOnValueUpdateListener(new ValueAdjustListenerBind.OnValueUpdateListener() {
            @Override
            public void onValueUpadate(int value) {
                tv_valueColorG.setBackgroundColor(Color.rgb(0,value,0));
                updateRGB();
            }
        });

        ib_colorB_up = findViewById(R.id.ib_colorB_up);
        tv_valueColorB = findViewById(R.id.tv_valueColorB);
        tv_valueColorB.setBackgroundColor(Color.rgb(
                0,0,Integer.valueOf(tv_valueColorB.getText().toString()))
        );
        ib_colorB_down = findViewById(R.id.ib_colorB_down);
        ValueAdjustListenerBind valueAdjustListenerBind_B = new ValueAdjustListenerBind(
                UIHandler, 6, 255, 100);
        valueAdjustListenerBind_B.bindView(tv_valueColorB, ib_colorB_up, ib_colorB_down);
        valueAdjustListenerBind_B.setOnValueUpdateListener(new ValueAdjustListenerBind.OnValueUpdateListener() {
            @Override
            public void onValueUpadate(int value) {
                tv_valueColorB.setBackgroundColor(Color.rgb(0,0,value));
                updateRGB();
            }
        });
        iv_ledPreview = findViewById(R.id.iv_ledPreview);
        iv_ledPreview.setPadding(20,20,20,20);
        updateRGB();
    }

    private void initViewAtWindowFocusChanged() {
        gv_leds = findViewById(R.id.gv_leds);
        gv_leds.setVisibility(View.INVISIBLE);

        adapter = new Adapter(this, list);
        gv_leds.setAdapter(adapter);
    }

    private void updateRGB(){
        iv_ledPreview.setColorFilter(Color.rgb(
                Integer.valueOf(tv_valueColorR.getText().toString()),
                Integer.valueOf(tv_valueColorG.getText().toString()),
                Integer.valueOf(tv_valueColorB.getText().toString())
        ));
    }

    private class Adapter extends BaseAdapter{
        private List<Data.LedData> list;

        public Adapter(Context context, List<Data.LedData> list) {
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
            View viewHolder;
            if (convertView == null){
                viewHolder = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_led, null);
                ImageButton imageButton = viewHolder.findViewById(R.id.ib_led);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)
                        imageButton.getLayoutParams();
                params.width = subViewDistance;
                params.height = subViewDistance;
                imageButton.setPadding(20,20,20,20);

                int color = Color.rgb(
                        list.get(position).getColorR(),
                        list.get(position).getColorG(),
                        list.get(position).getColorB()
                );
                imageButton.setColorFilter(color);
                imageButton.setOnClickListener(new GridViewItemClickListener(position));
            }else {
                viewHolder = (View) convertView.getTag();
            }
            return viewHolder;
        }

        private class GridViewItemClickListener implements View.OnClickListener {
            int position;

            public GridViewItemClickListener(int position) {
                this.position = position;
            }

            @Override
            public void onClick(View v) {
                if (menuItemSelectedID == MENU_ITEM_ID_onlyPreview){
                    //onlyPreview
                }
                if (menuItemSelectedID == MENU_ITEM_ID_addLedColor_front){
                    //addLedColorr_front
                    Data.LedData addLedData = new Data.LedData(position,
                            Integer.valueOf(tv_valueColorR.getText().toString()),
                            Integer.valueOf(tv_valueColorG.getText().toString()),
                            Integer.valueOf(tv_valueColorB.getText().toString())
                    );
                    list.add(position,addLedData);
                    for (int i = position; i<list.size();i++){
                        list.get(i).setNum(i);
                    }
                    notifyDataSetChanged();
                }
                if (menuItemSelectedID == MENU_ITEM_ID_addLedColor_back){
                    //addLedColor_back
                    Data.LedData addLedData = new Data.LedData(position+1,
                            Integer.valueOf(tv_valueColorR.getText().toString()),
                            Integer.valueOf(tv_valueColorG.getText().toString()),
                            Integer.valueOf(tv_valueColorB.getText().toString())
                    );
                    list.add(position+1,addLedData);
                    notifyDataSetChanged();
                }
                if (menuItemSelectedID == MENU_ITEM_ID_setLedColor){
                    Data.LedData ledData = (Data.LedData) getItem(position);
                    ledData.setColorR(Integer.valueOf(tv_valueColorR.getText().toString()));
                    ledData.setColorG(Integer.valueOf(tv_valueColorG.getText().toString()));
                    ledData.setColorB(Integer.valueOf(tv_valueColorB.getText().toString()));
                    notifyDataSetChanged();
                }
                if (menuItemSelectedID == MENU_ITEM_ID_getLedColor){
                    Data.LedData ledData = (Data.LedData) getItem(position);
                    tv_valueColorR.setText(String.valueOf(ledData.getColorR()));
                    tv_valueColorR.setBackgroundColor(Color.rgb(
                            ledData.getColorR(),0,0)
                    );
                    tv_valueColorG.setText(String.valueOf(ledData.getColorG()));
                    tv_valueColorG.setBackgroundColor(Color.rgb(
                            0,ledData.getColorG(),0)
                    );
                    tv_valueColorB.setText(String.valueOf(ledData.getColorB()));
                    tv_valueColorB.setBackgroundColor(Color.rgb(
                            0,0,ledData.getColorB())
                    );
                    updateRGB();
                }
                if (menuItemSelectedID == MENU_ITEM_ID_removeLedColor){
                    //removeLedColor
                    list.remove(position);
                    notifyDataSetChanged();
                }
            }
        }
    }

    private ViewTool.YesNoSelectionBar.YesNoView.ClickListener saveCheckListener
            = new ViewTool.YesNoSelectionBar.YesNoView.ClickListener(){

        @Override
        public void onClickYes(View v) {
            //do saveData
            Intent intent = getIntent();
            intent.putExtra(Data.ActivityConstant.INTENT_FILE_DATA,gson.toJson(list));
            setResult(RESULT_CODE_writeFileContetnt,intent);
            finish();
        }

        @Override
        public void onClickNo(View v) {
            finish();
        }
    };
}
