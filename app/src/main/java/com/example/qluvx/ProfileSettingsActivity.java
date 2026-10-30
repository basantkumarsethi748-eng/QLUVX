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
    private EditText etNickname, etBio;
    private RadioGroup rgRole;
    private TextView tvHeightVal, tvWeightVal, btnBack;
    private SeekBar seekHeight, seekWeight;
    private Switch swHideDistance, swGhostMode;
    private Button btnSaveProfile, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        sessionManager = new SessionManager(this);

        btnBack = findViewById(R.id.btnBack);
        etNickname = findViewById(R.id.etNickname);
        etBio = findViewById(R.id.etBio);
        rgRole = findViewById(R.id.rgRole);
        tvHeightVal = findViewById(R.id.tvHeightVal);
        tvWeightVal = findViewById(R.id.tvWeightVal);
        seekHeight = findViewById(R.id.seekHeight);
        seekWeight = findViewById(R.id.seekWeight);
        swHideDistance = findViewById(R.id.swHideDistance);
        swGhostMode = findViewById(R.id.swGhostMode);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // Load saved profile & privacy values from SessionManager
        etNickname.setText(sessionManager.getUserName());
        etBio.setText(sessionManager.getUserBio());

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

        swGhostMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, isChecked ? "🕵️ Stealth Mode Activated" : "🕵️ Stealth Mode Disabled", Toast.LENGTH_SHORT).show()
        );

        swHideDistance.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, isChecked ? "📍 Exact Distance Hidden" : "📍 Distance Visible", Toast.LENGTH_SHORT).show()
        );

        Button btnOpenPrivacyCloakFromSettings = findViewById(R.id.btnOpenPrivacyCloakFromSettings);
        if (btnOpenPrivacyCloakFromSettings != null) {
            btnOpenPrivacyCloakFromSettings.setOnClickListener(v ->
                    startActivity(new android.content.Intent(this, PrivacySecurityActivity.class))
            );
        }

        Button btnOpenTeleportFromSettings = findViewById(R.id.btnOpenTeleportFromSettings);
        if (btnOpenTeleportFromSettings != null) {
            btnOpenTeleportFromSettings.setOnClickListener(v ->
                    startActivity(new android.content.Intent(this, TeleportMapActivity.class))
            );
        }

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etNickname.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = "Julian Vance";
            }
            String newBio = etBio.getText().toString().trim();

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

            Toast.makeText(this, "✅ Profile & Privacy Settings Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // LOGOUT CONFIRMATION DIALOG
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Log Out?")
                    .setMessage("Are you sure you want to log out of your Walla Deluxe VIP account?")
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
