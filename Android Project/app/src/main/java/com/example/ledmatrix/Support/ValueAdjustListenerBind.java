package com.example.ledmatrix.Support;

import android.graphics.Color;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class ValueAdjustListenerBind
        implements View.OnClickListener,View.OnLongClickListener,View.OnTouchListener{
    private Handler UIhandler;
    private TextView tv_value;
    private View btn_valueAdjustUp, btn_valueAdjustDown;
    private int valueUpdateRate, startChangeValue;
    private final int maxRate, maxValue, gapBaseTime_millis;

    private OnValueUpdateListener onValueUpdateListener;

    public ValueAdjustListenerBind(Handler UIhandler, int maxRate, int maxValue, int gapBaseTime_millis) {
        this.UIhandler = UIhandler;
        this.valueUpdateRate = 1;
        this.startChangeValue = 0;

        this.maxRate = maxRate;
        this.maxValue = maxValue;
        this.gapBaseTime_millis = gapBaseTime_millis;
    }

    public void bindView(TextView tv_color, View btn_colorUp, View btn_colorDown){
        this.tv_value = tv_color;
        this.btn_valueAdjustUp = btn_colorUp;
        this.btn_valueAdjustDown = btn_colorDown;
        btn_colorUp.setOnClickListener(this);
        btn_colorUp.setOnLongClickListener(this);
        btn_colorUp.setOnTouchListener(this);
        btn_colorDown.setOnClickListener(this);
        btn_colorDown.setOnLongClickListener(this);
        btn_colorDown.setOnTouchListener(this);
    }

    @Override// +1 & -1
    public void onClick(View v) {
        int value = Integer.valueOf(tv_value.getText().toString());

        if (v.getId() == btn_valueAdjustUp.getId() && value < maxValue){
            value++;
        }
        if (v.getId() == btn_valueAdjustDown.getId() && value > 0){
            value--;
        }

        tv_value.setText(String.valueOf(value));
        onValueUpdateListenerCheck(value);
    }

    @Override//start +1/-1 thread
    public boolean onLongClick(View v) {
        if (v.getId() == btn_valueAdjustUp.getId()){
            startChangeValue = Integer.valueOf(tv_value.getText().toString());
            UIhandler.post(valueUpThread);
        }
        if (v.getId() == btn_valueAdjustDown.getId()){
            startChangeValue = Integer.valueOf(tv_value.getText().toString());
            UIhandler.post(valueDownThread);
        }
        valueUpdateRate = 1;
        return true;
    }

    @Override//cancel +1/-1 thread
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP){
            if (v.getId() == btn_valueAdjustUp.getId()){
                UIhandler.removeCallbacks(valueUpThread);
            }
            if (v.getId() == btn_valueAdjustDown.getId()){
                UIhandler.removeCallbacks(valueDownThread);
            }
        }
        return false;
    }

    private Runnable valueUpThread = new Runnable() {
        @Override
        public void run() {
            int value = Integer.valueOf(tv_value.getText().toString());
            if (value < maxValue) {
                tv_value.setText(String.valueOf(value + 1));
                onValueUpdateListenerCheck(value+1);
                updateValueUpdateRate(value);
                UIhandler.postDelayed(valueUpThread, gapBaseTime_millis/valueUpdateRate);
            }
        }
    };

    private Runnable valueDownThread = new Runnable() {
        @Override
        public void run() {
            int value = Integer.valueOf(tv_value.getText().toString());
            if (value > 0) {
                tv_value.setText(String.valueOf(value - 1));
                onValueUpdateListenerCheck(value-1);
                updateValueUpdateRate(value);
                UIhandler.postDelayed(valueDownThread, gapBaseTime_millis/valueUpdateRate);
            }
        }
    };

    private void updateValueUpdateRate(int value){
        if(valueUpdateRate < maxRate && Math.abs(value - startChangeValue) == 10) {
            valueUpdateRate++;
            startChangeValue = value;
        }
    }

    private void onValueUpdateListenerCheck(int value){
        if (onValueUpdateListener != null){
            onValueUpdateListener.onValueUpadate(value);
        }
    }

    public void setOnValueUpdateListener(OnValueUpdateListener onValueUpdateListener) {
        this.onValueUpdateListener = onValueUpdateListener;
    }

    public interface OnValueUpdateListener{
        void onValueUpadate(int value);
    }
}
