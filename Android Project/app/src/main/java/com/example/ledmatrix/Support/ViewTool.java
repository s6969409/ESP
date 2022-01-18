package com.example.ledmatrix.Support;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.ledmatrix.R;
import com.example.ledmatrix.local.AppSettingActivity;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewTool {
    public static class ProcessDialog extends Dialog{
        private TextView tv_processMsg;

        public ProcessDialog(@NonNull Context context) {
            super(context,R.style.processing_dialog);

            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View viewProcess = layoutInflater.inflate(R.layout.dialog_progress, null);
            ImageView iv_process = viewProcess.findViewById(R.id.iv_process);
            this.tv_processMsg = viewProcess.findViewById(R.id.tv_processMsg);
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.processing);
            iv_process.startAnimation(animation);

            //this.setCancelable(false);
            this.setContentView(viewProcess, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));
        }

        public void setText(String message){
            tv_processMsg.setText(message);
        }
    }

    public static class YesNoSelectionBar{
        public static Snackbar initSnackBar(View parentView, View addView){
            Snackbar snackbar = Snackbar.make(parentView,"",Snackbar.LENGTH_INDEFINITE);
            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
            snackbarLayout.addView(addView);
            return snackbar;
        }

        public static class YesNoView{
            public static View init(Context context, String btnContentYes, String btnContentNo,
                                    View.OnClickListener btnClickListener){
                View view = LayoutInflater.from(context).inflate(R.layout.item_action_yes_no,null);
                Button btn_actionPositive = view.findViewById(R.id.btn_actionPositive);
                btn_actionPositive.setText(btnContentYes);
                btn_actionPositive.setOnClickListener(btnClickListener);
                Button btn_actionNegative = view.findViewById(R.id.btn_actionNegative);
                btn_actionNegative.setText(btnContentNo);
                btn_actionNegative.setOnClickListener(btnClickListener);
                return view;
            }

            public static abstract class ClickListener implements View.OnClickListener{
                public abstract void onClickYes(View v);
                public abstract void onClickNo(View v);

                @Override
                public final void onClick(View v) {
                    switch (v.getId()){
                        case R.id.btn_actionPositive:
                            onClickYes(v);
                            break;
                        case R.id.btn_actionNegative:
                            onClickNo(v);
                            break;
                    }
                }
            }
        }
    }

    public static TextView InitLogView(Activity activity){
        TextView tv_log = activity.findViewById(R.id.tv_log);
        if (AppSettingActivity.getAppSetting(activity,"LogView").equals("off")){
            tv_log.setVisibility(View.INVISIBLE);
            tv_log.setWidth(0);
            tv_log.setHeight(0);
        }
        return tv_log;
    }
}
