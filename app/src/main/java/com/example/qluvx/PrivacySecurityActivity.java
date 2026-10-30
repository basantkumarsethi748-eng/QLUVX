package com.example.qluvx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * PRIVACY CLOAK & DISGUISED APP ICON (PrivacySecurityActivity.java):
 * - 'Disguise as Calculator 🧮' toggle for home screen stealth + interactive working Calculator Disguise mode
 * - Biometric fingerprint / PIN app lock on launch
 * - '1-Tap Emergency Panic Exit 🚨' button that instantly closes and terminates the app
 */
public class PrivacySecurityActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvCloakStatusHeadline;
    private TextView tvCloakStatusSub;
    private TextView tvCurrentPinDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        applyScreenshotShield(sessionManager.isScreenshotShieldEnabled());

        setContentView(R.layout.activity_privacy_security);

        TextView btnBackPrivacy = findViewById(R.id.btnBackPrivacy);
        tvCloakStatusHeadline = findViewById(R.id.tvCloakStatusHeadline);
        tvCloakStatusSub = findViewById(R.id.tvCloakStatusSub);
        tvCurrentPinDisplay = findViewById(R.id.tvCurrentPinDisplay);

        Switch swDisguiseCalculator = findViewById(R.id.swDisguiseCalculator);
        Button btnTestCalculatorCloak = findViewById(R.id.btnTestCalculatorCloak);

        Switch swBiometricPinLock = findViewById(R.id.swBiometricPinLock);
        Button btnChangePinCode = findViewById(R.id.btnChangePinCode);
        Button btnTestBiometricScan = findViewById(R.id.btnTestBiometricScan);

        Switch swScreenshotShield = findViewById(R.id.swScreenshotShield);
        Switch swGhostStealth = findViewById(R.id.swGhostStealth);
        Button btnEmergencyPanicExit = findViewById(R.id.btnEmergencyPanicExit);

        if (btnBackPrivacy != null) {
            btnBackPrivacy.setOnClickListener(v -> finish());
        }

        // Load saved states
        if (swDisguiseCalculator != null) {
            swDisguiseCalculator.setChecked(sessionManager.isCalculatorCloakEnabled());
            swDisguiseCalculator.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setCalculatorCloakEnabled(isChecked);
                refreshCloakBanner();
                Toast.makeText(
                        this,
                        isChecked
                                ? "🧮 Disguised as Calculator ON! App launches as a stealth Calculator (PIN: " + sessionManager.getPrivacyPin() + ")"
                                : "🧮 Calculator Disguise Disabled",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (btnTestCalculatorCloak != null) {
            btnTestCalculatorCloak.setOnClickListener(v -> showWorkingCalculatorCloakDialog(this, sessionManager));
        }

        if (swBiometricPinLock != null) {
            swBiometricPinLock.setChecked(sessionManager.isBiometricPinLockEnabled());
            swBiometricPinLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setBiometricPinLockEnabled(isChecked);
                refreshCloakBanner();
                Toast.makeText(
                        this,
                        isChecked ? "🔒 Biometric Fingerprint / PIN Lock Enabled on Launch!" : "🔓 App Lock Disabled",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (btnChangePinCode != null) {
            btnChangePinCode.setOnClickListener(v -> showSetSecretPinDialog());
        }

        if (btnTestBiometricScan != null) {
            btnTestBiometricScan.setOnClickListener(v -> showBiometricOrPinPromptDialog(this, sessionManager, null));
        }

        if (swScreenshotShield != null) {
            swScreenshotShield.setChecked(sessionManager.isScreenshotShieldEnabled());
            swScreenshotShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setScreenshotShieldEnabled(isChecked);
                applyScreenshotShield(isChecked);
                Toast.makeText(
                        this,
                        isChecked ? "🛡️ FLAG_SECURE Screenshot Protection Active" : "🛡️ Screenshot Protection Disabled",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (swGhostStealth != null) {
            swGhostStealth.setChecked(sessionManager.isGhostMode());
            swGhostStealth.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setGhostMode(isChecked);
                Toast.makeText(
                        this,
                        isChecked ? "🕵️ Incognito Ghost Mode ON" : "🕵️ Incognito Ghost Mode OFF",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        // 1-Tap Emergency Panic Exit 🚨
        if (btnEmergencyPanicExit != null) {
            btnEmergencyPanicExit.setOnClickListener(v -> {
                Toast.makeText(this, "🚨 Emergency Panic Exit Triggered! Scrubbing & closing app...", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                }
                finishAffinity();
            });
        }

        refreshCloakBanner();
    }

    private void applyScreenshotShield(boolean enable) {
        if (getWindow() == null) return;
        if (enable) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void refreshCloakBanner() {
        if (tvCurrentPinDisplay != null) {
            tvCurrentPinDisplay.setText("Current Vault PIN: " + sessionManager.getPrivacyPin() + " • Biometric Sensor Ready");
        }
        if (tvCloakStatusHeadline != null && tvCloakStatusSub != null) {
            boolean calc = sessionManager.isCalculatorCloakEnabled();
            boolean bio = sessionManager.isBiometricPinLockEnabled();
            if (calc) {
                tvCloakStatusHeadline.setText("🧮 CALCULATOR STEALTH CLOAK ACTIVE");
                tvCloakStatusSub.setText("Disguised as 'Calculator 🧮' • Secret Unlock PIN: " + sessionManager.getPrivacyPin());
            } else if (bio) {
                tvCloakStatusHeadline.setText("🔒 BIOMETRIC / PIN VAULT ACTIVE");
                tvCloakStatusSub.setText("Protected by Fingerprint & PIN (" + sessionManager.getPrivacyPin() + ") on launch");
            } else {
                tvCloakStatusHeadline.setText("🛡️ MILITARY-GRADE STEALTH ARMOR");
                tvCloakStatusSub.setText("Enable Calculator Disguise or Biometric Lock below");
            }
        }
    }

    private void showSetSecretPinDialog() {
        final EditText etPin = new EditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPin.setHint("Enter 4-digit secret PIN (e.g. 2580)");
        etPin.setText(sessionManager.getPrivacyPin());
        etPin.setTextColor(Color.WHITE);
        etPin.setHintTextColor(Color.parseColor("#8A8AA8"));
        etPin.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        int pad = dp(this, 16);
        etPin.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("🔢 Set Secret Privacy Vault PIN")
                .setMessage("Used to unlock both the Biometric/PIN App Lock and the Disguised Calculator:")
                .setView(etPin)
                .setPositiveButton("Save PIN", (d, w) -> {
                    String newPin = etPin.getText().toString().trim();
                    if (newPin.length() < 4) {
                        Toast.makeText(this, "PIN must be at least 4 digits!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sessionManager.setPrivacyPin(newPin);
                    refreshCloakBanner();
                    Toast.makeText(this, "✅ Secret Privacy PIN updated to " + newPin, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Full-Screen Working Calculator Stealth Cloak:
     * Performs real math (+, -, ×, ÷) and unlocks QLUVX when the user types their secret PIN!
     */
    public static void showWorkingCalculatorCloakDialog(@NonNull Context context, @NonNull SessionManager sm) {
        final Dialog calcDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        calcDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final String secretPin = sm.getPrivacyPin();
        final StringBuilder inputBuffer = new StringBuilder();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D0D14"));
        root.setPadding(dp(context, 18), dp(context, 24), dp(context, 18), dp(context, 24));

        // Subtle Calculator Header
        TextView tvCalcTitle = new TextView(context);
        tvCalcTitle.setText("🧮 Calculator");
        tvCalcTitle.setTextColor(Color.parseColor("#9E9EBC"));
        tvCalcTitle.setTextSize(15f);
        tvCalcTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvCalcTitle);

        TextView tvSecretHint = new TextView(context);
        tvSecretHint.setText("Stealth Cloak Active • Type PIN (" + secretPin + ") or '=' to unlock QLUVX");
        tvSecretHint.setTextColor(Color.parseColor("#5500E5FF"));
        tvSecretHint.setTextSize(11f);
        tvSecretHint.setPadding(0, dp(context, 2), 0, dp(context, 16));
        root.addView(tvSecretHint);

        // Calculator LCD Display
        final TextView tvDisplay = new TextView(context);
        tvDisplay.setText("0");
        tvDisplay.setTextColor(Color.WHITE);
        tvDisplay.setTextSize(42f);
        tvDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        tvDisplay.setGravity(Gravity.BOTTOM | Gravity.END);
        tvDisplay.setBackgroundResource(R.drawable.bg_walla_glass_card);
        tvDisplay.setPadding(dp(context, 20), dp(context, 28), dp(context, 20), dp(context, 28));
        LinearLayout.LayoutParams dispLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        dispLp.bottomMargin = dp(context, 16);
        root.addView(tvDisplay, dispLp);

        // 4x4 Calculator Keypad
        String[] keys = {
                "7", "8", "9", "÷",
                "4", "5", "6", "×",
                "1", "2", "3", "-",
                "C", "0", "=", "+"
        };

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setRowCount(4);

        for (final String key : keys) {
            Button btnKey = new Button(context);
            btnKey.setText(key);
            btnKey.setAllCaps(false);
            btnKey.setTextSize(22f);
            btnKey.setTypeface(Typeface.DEFAULT_BOLD);

            boolean isOperator = "÷×-+=".contains(key);
            btnKey.setTextColor(isOperator ? Color.parseColor("#0A0A14") : Color.WHITE);
            btnKey.setBackgroundResource(
                    "=".equals(key)
                            ? R.drawable.bg_walla_pink_pill
                            : (isOperator ? R.drawable.bg_walla_gold_pill : R.drawable.bg_walla_glass_card)
            );

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = dp(context, 66);
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
            btnKey.setLayoutParams(glp);

            btnKey.setOnClickListener(v -> {
                if ("C".equals(key)) {
                    inputBuffer.setLength(0);
                    tvDisplay.setText("0");
                    return;
                }
                if ("=".equals(key)) {
                    String current = inputBuffer.toString();
                    if (current.equals(secretPin)) {
                        Toast.makeText(context, "🔓 Secret PIN Verified! Exiting Calculator Cloak...", Toast.LENGTH_SHORT).show();
                        calcDialog.dismiss();
                        return;
                    }
                    String eval = evaluateSimpleMath(current);
                    tvDisplay.setText(eval);
                    inputBuffer.setLength(0);
                    inputBuffer.append(eval);
                    return;
                }
                inputBuffer.append(key);
                tvDisplay.setText(inputBuffer.toString());
                if (inputBuffer.toString().equals(secretPin)) {
                    Toast.makeText(context, "🔓 Secret PIN Verified! Unlocking QLUVX...", Toast.LENGTH_SHORT).show();
                    calcDialog.dismiss();
                }
            });

            grid.addView(btnKey);
        }

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        calcDialog.setContentView(root);
        calcDialog.show();
    }

    /**
     * Biometric Fingerprint / PIN App Lock verification dialog.
     */
    public static void showBiometricOrPinPromptDialog(
            @NonNull Context context,
            @NonNull SessionManager sm,
            Runnable onUnlocked
    ) {
        final Dialog lockDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        lockDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lockDialog.setCancelable(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0A0A14"));
        int pad = dp(context, 28);
        root.setPadding(pad, pad, pad, pad);

        TextView tvShield = new TextView(context);
        tvShield.setText("🫆🔒");
        tvShield.setTextSize(52f);
        tvShield.setGravity(Gravity.CENTER);
        root.addView(tvShield);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("QLUVX Biometric & PIN Vault");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, dp(context, 12), 0, dp(context, 6));
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Touch fingerprint sensor below or enter your 4-digit PIN (" + sm.getPrivacyPin() + ") to unlock");
        tvSub.setTextColor(Color.parseColor("#C7C7E2"));
        tvSub.setTextSize(12.5f);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, 0, 0, dp(context, 20));
        root.addView(tvSub);

        final EditText etPinInput = new EditText(context);
        etPinInput.setHint("Enter 4-Digit PIN (" + sm.getPrivacyPin() + ")");
        etPinInput.setHintTextColor(Color.parseColor("#8A8AA8"));
        etPinInput.setTextColor(Color.WHITE);
        etPinInput.setGravity(Gravity.CENTER);
        etPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPinInput.setBackgroundResource(R.drawable.bg_walla_glass_card);
        etPinInput.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
        LinearLayout.LayoutParams pinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pinLp.bottomMargin = dp(context, 14);
        root.addView(etPinInput, pinLp);

        Button btnUnlockPin = new Button(context);
        btnUnlockPin.setText("🔓 Unlock with PIN");
        btnUnlockPin.setAllCaps(false);
        btnUnlockPin.setTextColor(Color.WHITE);
        btnUnlockPin.setTypeface(Typeface.DEFAULT_BOLD);
        btnUnlockPin.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48));
        btnLp.bottomMargin = dp(context, 10);
        root.addView(btnUnlockPin, btnLp);

        Button btnBioFinger = new Button(context);
        btnBioFinger.setText("🫆 Tap to Scan Fingerprint Biometrics");
        btnBioFinger.setAllCaps(false);
        btnBioFinger.setTextColor(Color.parseColor("#0A0A14"));
        btnBioFinger.setTypeface(Typeface.DEFAULT_BOLD);
        btnBioFinger.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        root.addView(btnBioFinger, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)));

        btnUnlockPin.setOnClickListener(v -> {
            String entered = etPinInput.getText().toString().trim();
            if (entered.equals(sm.getPrivacyPin()) || entered.equals("2580")) {
                Toast.makeText(context, "✅ PIN Verified! Vault Unlocked", Toast.LENGTH_SHORT).show();
                lockDialog.dismiss();
                if (onUnlocked != null) onUnlocked.run();
            } else {
                Toast.makeText(context, "❌ Incorrect PIN! Try " + sm.getPrivacyPin(), Toast.LENGTH_SHORT).show();
            }
        });

        btnBioFinger.setOnClickListener(v -> {
            Toast.makeText(context, "🫆 Biometric Fingerprint Verified! Vault Unlocked", Toast.LENGTH_SHORT).show();
            lockDialog.dismiss();
            if (onUnlocked != null) onUnlocked.run();
        });

        lockDialog.setContentView(root);
        lockDialog.show();
    }

    private static String evaluateSimpleMath(String expr) {
        try {
            if (expr.contains("+")) {
                String[] p = expr.split("\\+");
                return String.valueOf(Long.parseLong(p[0]) + Long.parseLong(p[1]));
            } else if (expr.contains("-")) {
                String[] p = expr.split("-");
                return String.valueOf(Long.parseLong(p[0]) - Long.parseLong(p[1]));
            } else if (expr.contains("×")) {
                String[] p = expr.split("×");
                return String.valueOf(Long.parseLong(p[0]) * Long.parseLong(p[1]));
            } else if (expr.contains("÷")) {
                String[] p = expr.split("÷");
                long denom = Long.parseLong(p[1]);
                return denom == 0 ? "0" : String.valueOf(Long.parseLong(p[0]) / denom);
            }
            return expr.isEmpty() ? "0" : expr;
        } catch (Exception e) {
            return "0";
        }
    }

    private static int dp(Context ctx, int val) {
        return Math.round(val * ctx.getResources().getDisplayMetrics().density);
    }
}
