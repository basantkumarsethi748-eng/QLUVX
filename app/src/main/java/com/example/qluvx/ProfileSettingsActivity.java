package com.example.qluvx;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileSettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private EditText etNickname, etBio, etServerUrl;
    private RadioGroup rgRole;
    private TextView tvHeightVal, tvWeightVal, btnBack;
    private SeekBar seekHeight, seekWeight;
    private Switch swHideDistance, swGhostMode;
    private Button btnSaveProfile, btnLogout, btnTestNgrok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        sessionManager = new SessionManager(this);

        btnBack = findViewById(R.id.btnBack);
        etNickname = findViewById(R.id.etNickname);
        etBio = findViewById(R.id.etBio);
        etServerUrl = findViewById(R.id.etServerUrl);
        rgRole = findViewById(R.id.rgRole);
        tvHeightVal = findViewById(R.id.tvHeightVal);
        tvWeightVal = findViewById(R.id.tvWeightVal);
        seekHeight = findViewById(R.id.seekHeight);
        seekWeight = findViewById(R.id.seekWeight);
        swHideDistance = findViewById(R.id.swHideDistance);
        swGhostMode = findViewById(R.id.swGhostMode);
        btnTestNgrok = findViewById(R.id.btnTestNgrok);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // Load saved profile & privacy values from SessionManager
        etNickname.setText(sessionManager.getUserName());
        etBio.setText(sessionManager.getUserBio());
        etServerUrl.setText(sessionManager.getServerUrl());

        String savedRole = sessionManager.getUserRole();
        if (savedRole.toLowerCase().contains("top")) {
            rgRole.check(R.id.rbTop);
        } else if (savedRole.toLowerCase().contains("bottom")) {
            rgRole.check(R.id.rbBottom);
        } else {
            rgRole.check(R.id.rbVersatile);
        }

        int savedHeight = Math.max(150, Math.min(210, sessionManager.getUserHeightCm()));
        seekHeight.setProgress(savedHeight - 150);
        tvHeightVal.setText("Height: " + savedHeight + " cm");

        int savedWeight = Math.max(45, Math.min(120, sessionManager.getUserWeightKg()));
        seekWeight.setProgress(savedWeight - 45);
        tvWeightVal.setText("Weight: " + savedWeight + " kg");

        swHideDistance.setChecked(sessionManager.isHideDistance());
        swGhostMode.setChecked(sessionManager.isGhostMode());

        btnBack.setOnClickListener(v -> finish());

        // Height Slider (150 - 210 cm)
        seekHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int heightCm = 150 + progress;
                tvHeightVal.setText("Height: " + heightCm + " cm");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Weight Slider (45 - 120 kg)
        seekWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int weightKg = 45 + progress;
                tvWeightVal.setText("Weight: " + weightKg + " kg");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        swHideDistance.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, isChecked ? "📍 Exact Distance Hidden" : "📍 Distance Visible", Toast.LENGTH_SHORT).show()
        );

        swGhostMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, isChecked ? "👻 Incognito Ghost Mode ON" : "👻 Incognito Ghost Mode OFF", Toast.LENGTH_SHORT).show()
        );

        btnTestNgrok.setOnClickListener(v -> {
            String rawUrl = etServerUrl.getText().toString().trim();
            if (rawUrl.isEmpty() || (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://"))) {
                Toast.makeText(this, "Enter a valid URL starting with https:// or http://", Toast.LENGTH_SHORT).show();
                return;
            }
            sessionManager.setServerUrl(rawUrl);
            String healthUrl = (rawUrl.endsWith("/") ? rawUrl : (rawUrl + "/")) + "api/health";
            Toast.makeText(this, "⚡ Pinging " + healthUrl + " ...", Toast.LENGTH_SHORT).show();

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(healthUrl)
                    .header("ngrok-skip-browser-warning", "true")
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(() -> Toast.makeText(
                            ProfileSettingsActivity.this,
                            "⚠️ Tunnel unreachable (" + e.getMessage() + "). Make sure `node backend/server.js` & `ngrok http 5000` are running.",
                            Toast.LENGTH_LONG
                    ).show());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    final int code = response.code();
                    response.close();
                    runOnUiThread(() -> {
                        if (code >= 200 && code < 300) {
                            Toast.makeText(ProfileSettingsActivity.this, "✅ Connected to Ngrok Tunnel (HTTP " + code + ")!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "⚠️ Server responded with HTTP " + code, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        });

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etNickname.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = "Julian Vance";
            }
            String newBio = etBio.getText().toString().trim();
            String serverUrl = etServerUrl.getText().toString().trim();
            if (!serverUrl.isEmpty()) {
                sessionManager.setServerUrl(serverUrl);
                SocketManager.getInstance().disconnect();
                SocketManager.getInstance().connect(sessionManager.getUserId(), sessionManager.getAuthToken());
            }

            int selectedRoleId = rgRole.getCheckedRadioButtonId();
            RadioButton rbSelected = findViewById(selectedRoleId);
            String role = rbSelected != null ? rbSelected.getText().toString() : "Versatile";

            int heightCm = 150 + seekHeight.getProgress();
            int weightKg = 45 + seekWeight.getProgress();

            sessionManager.updateProfileSettings(
                    newName,
                    newBio,
                    role,
                    heightCm,
                    weightKg,
                    swHideDistance.isChecked(),
                    swGhostMode.isChecked()
            );

            Toast.makeText(this, "✅ Profile & Server Settings Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // LOGOUT CONFIRMATION DIALOG
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Log Out of QLUVX?")
                    .setMessage("Are you sure you want to log out of your VIP account?")
                    .setPositiveButton("Yes, Log Out", (dialog, which) -> {
                        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        sessionManager.logoutUser();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
