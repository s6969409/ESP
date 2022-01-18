package com.example.ledmatrix.deviceAdapter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.UserFragment;
import com.example.ledmatrix.local.FileProcess;
import com.google.gson.Gson;

public class ReadLedFileFragment extends UserFragment {
    private OnFragmentInteractionListener listener;
    private EditText ed_readLedFile_offset;
    private MainProgramActivity.ViewPathEdit viewPathEdit;
    private Gson gson = new Gson();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read_led_file, container, false);
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
        String filePath = josnString != null?
                gson.fromJson(josnString, ProgramActivity.ProgramItem.ReadLedFile.class).filePath:"null";
        int ledOffset = josnString != null?
                gson.fromJson(josnString, ProgramActivity.ProgramItem.ReadLedFile.class).ledOffset:0;

        viewPathEdit = new MainProgramActivity.ViewPathEdit(
                getActivity(),"filePath",filePath,Data.FolderPath.RES_LEDS);

        ed_readLedFile_offset = getActivity().findViewById(R.id.ed_readLedFile_offset);
        ed_readLedFile_offset.setText(String.valueOf(ledOffset));
    }

    @Override
    public String getData() {
        Gson gson = new Gson();
        String filePath = viewPathEdit.getSeletedPath();

        String checkMsg = FileProcess.CheckNumberStr(
                ed_readLedFile_offset.getText().toString(), 0,32767);
        if (!checkMsg.equals("")){
            Toast.makeText(getContext(), checkMsg, Toast.LENGTH_SHORT).show();
            return null;
        }

        int ledOffset = Integer.valueOf(ed_readLedFile_offset.getText().toString());

        ProgramActivity.ProgramItem.ReadLedFile readLedFileObj
                = new ProgramActivity.ProgramItem.ReadLedFile(
                ProgramActivity.ProgramItem.ReadLedFile.KEY_readLedFile,
                filePath,
                ledOffset
        );
        return gson.toJson(readLedFileObj);
    }
}
