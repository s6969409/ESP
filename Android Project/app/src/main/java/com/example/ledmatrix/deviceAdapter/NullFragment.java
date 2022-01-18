package com.example.ledmatrix.deviceAdapter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.ledmatrix.R;
import com.example.ledmatrix.Support.UserFragment;

public class NullFragment extends UserFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_null, container, false);
    }
}
