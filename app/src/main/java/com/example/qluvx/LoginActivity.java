package com.example.qluvx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etOtp;
    private Button btnSendOtp, btnGoogleLogin, btnQuickGuest;
    private SessionManager sessionManager;
    private boolean isOtpSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnQuickGuest = findViewById(R.id.btnQuickGuest);

        btnSendOtp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.length() < 6) {
                Toast.makeText(this, "Enter a valid mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isOtpSent) {
                isOtpSent = true;
                etOtp.setVisibility(View.VISIBLE);
                etOtp.setText("1234");
                btnSendOtp.setText("Verify & Enter Walla");
                Toast.makeText(this, "OTP Sent! Code 1234 auto-filled", Toast.LENGTH_SHORT).show();
            } else {
                String otp = etOtp.getText().toString().trim();
                if (otp.length() == 4) {
                    sessionManager.createLoginSession("1002938", "Julian Vance", "Versatile");
                    navigateToHome();
                } else {
                    Toast.makeText(this, "Enter 4-digit OTP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGoogleLogin.setOnClickListener(v -> {
            sessionManager.createLoginSession("1002938", "Julian Vance", "Versatile");
            navigateToHome();
        });

        btnQuickGuest.setOnClickListener(v -> {
            sessionManager.createLoginSession("1002938", "Julian Vance", "Versatile");
            navigateToHome();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
