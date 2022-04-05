package com.example.demofaceidapp;

import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {

    public BaseFragment() {
    }

    public MainApplication getApp() {
        if (getActivity() == null) return null;
        return (MainApplication) getActivity().getApplicationContext();
    }
}
