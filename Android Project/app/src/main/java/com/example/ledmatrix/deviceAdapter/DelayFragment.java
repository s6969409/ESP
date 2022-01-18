package com.example.ledmatrix.deviceAdapter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.UserFragment;
import com.example.ledmatrix.local.FileProcess;
import com.google.gson.Gson;


public class DelayFragment extends UserFragment {
    private OnFragmentInteractionListener listener;
    private EditText ed_f_delaySec;
    private Gson gson = new Gson();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_delay, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = getOnFragmentInteractionListener();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initView();
    }

    private void initView() {
        String josnString = listener.getData();
        int timeMillis = josnString != null?
                gson.fromJson(josnString, ProgramActivity.ProgramItem.Delay.class).timeMillis:0;

        ed_f_delaySec = getActivity().findViewById(R.id.ed_f_delaySec);
        ed_f_delaySec.setText(String.valueOf(timeMillis));
    }

    @Override
    public String getData() {
        String checkMsg = FileProcess.CheckNumberStr(
                ed_f_delaySec.getText().toString(), 0,32767);
        if (!checkMsg.equals("")){
            Toast.makeText(getContext(), checkMsg, Toast.LENGTH_SHORT).show();
            return null;
        }

        int timeMillis = Integer.valueOf(ed_f_delaySec.getText().toString());
        ProgramActivity.ProgramItem.Delay delayObj = new ProgramActivity.ProgramItem.Delay(
                ProgramActivity.ProgramItem.Delay.KEY_delay,timeMillis);
        return gson.toJson(delayObj);
        //return super.getData();
    }
}
