package com.example.ledmatrix.Support;

import android.content.Context;

import androidx.fragment.app.Fragment;

public class UserFragment extends Fragment {
    private OnFragmentInteractionListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        String getData();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        //need use listener
    }

    public OnFragmentInteractionListener getOnFragmentInteractionListener(){
        return listener;
    }

    public String getData() {
        return "";
    }
}
