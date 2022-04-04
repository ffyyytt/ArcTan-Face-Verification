package com.example.demofaceidapp.detector;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.demofaceidapp.BaseFragment;
import com.example.demofaceidapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CompatCameraConnectionFragment extends BaseFragment {

    public CameraListener cameraListener;

    protected FloatingActionButton fab;
    private TextView tvName;
    private ImageView ivLogo, ivPreview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        fab = view.findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (cameraListener != null) {
                    cameraListener.onSwitchCamera();
                }
            });
        }

        tvName = view.findViewById(R.id.tvName);
        ivLogo = view.findViewById(R.id.ivLogo);
        ivPreview = view.findViewById(R.id.ivPreview);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("onResume", "Camera fragment");
    }


    public interface CameraListener {
        void onSwitchCamera();

        void onRestart();

        void onLiveTracking();

        void onLivenessDetecting();
    }
}
