package com.example.demofaceidapp;

import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.demofaceidapp.adapter.UserListAdapter;
import com.example.demofaceidapp.databinding.ActivityUserListBinding;
import com.example.demofaceidapp.detector.DetectorActivity;
import com.example.demofaceidapp.detector.utils.Constant;

public class UserListActivity extends BaseActivity {

    private ActivityUserListBinding binding;
    private UserListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new UserListAdapter(getApp().users, new UserListAdapter.Listener() {
            @Override
            public void onAdd(int userId) {
                Intent intent = new Intent(UserListActivity.this, DetectorActivity.class);
                intent.putExtra(DetectorActivity.KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
                intent.putExtra(DetectorActivity.KEY_CAMERA_MODE, Constant.MODE_MANUAL); // mode manual
                intent.putExtra(DetectorActivity.KEY_USER_ID, userId);
                intent.putExtra(DetectorActivity.KEY_ADDING_FACE, true);
                startActivity(intent);
            }

            @Override
            public void onDelete(int userId) {
                getApp().deleteUser(userId);
            }
        });
        binding.rv.setAdapter(adapter);
        binding.rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        binding.btn.setOnClickListener(v -> {
            AlertDialog.Builder builder
                    = new AlertDialog.Builder(this);
            builder.setTitle("Enter user name");

            final View customLayout = getLayoutInflater().inflate(R.layout.dialog_create_user, null);
            builder.setView(customLayout);

            builder.setPositiveButton(
                    "OK",
                    (dialog, which) -> {
                        EditText editText = customLayout.findViewById(R.id.edt);
                        getApp().addNewUser(editText.getText().toString());
                        adapter.notifyDataSetChanged();
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
