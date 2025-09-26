package com.example.sailspots.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sailspots.R;
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MaterialButton backToLoginButton = findViewById(R.id.btn_go_to_login);
        backToLoginButton.setOnClickListener(view -> finish());
    }
}