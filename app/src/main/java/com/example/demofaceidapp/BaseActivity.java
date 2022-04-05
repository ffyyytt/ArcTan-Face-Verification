package com.example.demofaceidapp;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected MainApplication getApp() {
        return (MainApplication) getApplication();
    }
}
