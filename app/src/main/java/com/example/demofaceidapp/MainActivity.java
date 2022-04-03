package com.example.demofaceidapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;

import com.example.demofaceidapp.databinding.ActivityMainBinding;
import com.example.demofaceidapp.detector.DetectorActivity;
import com.example.demofaceidapp.detector.utils.Constant;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnUser.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserListActivity.class);
            startActivity(intent);
        });
        binding.btnVerify.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetectorActivity.class);
            intent.putExtra(DetectorActivity.KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
            intent.putExtra(DetectorActivity.KEY_ADDING_FACE, false);
            startActivity(intent);
        });
    }
}