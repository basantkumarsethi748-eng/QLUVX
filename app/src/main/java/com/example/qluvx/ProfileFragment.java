package com.example.qluvx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final double DIAMOND_TO_INR_RATE = 0.50; // 1 Diamond = ₹0.50 INR
    private static final int MIN_WITHDRAWAL_DIAMONDS = 5000; // Minimum 5,000 Diamonds = ₹2,500 INR

    private SessionManager sessionManager;
    private BillingManager billingManager;
    private TextView tvDisplayName, tvUserId, tvCoins, tvDiamonds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvCoins = view.findViewById(R.id.tvCoins);
        tvDiamonds = view.findViewById(R.id.tvDiamonds);
        ImageView ivSettings = view.findViewById(R.id.ivSettings);

        View cardBuyCoins = view.findViewById(R.id.cardBuyCoins);
        View cardDiamondPayout = view.findViewById(R.id.cardDiamondPayout);
        Button btnBuyCoins = view.findViewById(R.id.btnBuyCoins);
        Button btnWithdrawDiamonds = view.findViewById(R.id.btnWithdrawDiamonds);
        Button btnOpenAdminConsole = view.findViewById(R.id.btnOpenAdminConsole);
        Button btnTestPushAlerts = view.findViewById(R.id.btnTestPushAlerts);

        // Initialize Google Play Billing v7 Manager
        billingManager = new BillingManager(requireActivity(), new BillingManager.BillingCallback() {
            @Override
            public void onCoinsCredited(String productId, int creditedCoins, int newTotalCoins, boolean isVipActive, String purchaseToken) {
                if (tvCoins != null) {
                    tvCoins.setText(String.valueOf(newTotalCoins));
                }
            }

            @Override
            public void onBillingError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bind User Data from SessionManager
        refreshProfileHeader();

        // Copy User ID to Clipboard
        tvUserId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("QLUVX ID", sessionManager.getUserId());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "User ID Copied to Clipboard!", Toast.LENGTH_SHORT).show();
        });

        // Open Profile Settings & Logout Screen
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileSettingsActivity.class);
            startActivity(intent);
        });

        // 1. Google Play In-App Billing Store (₹89 / ₹399 / ₹1,499 / VIP ₹499)
        View.OnClickListener buyCoinsClick = v -> billingManager.showCoinStoreBottomSheet();
        if (cardBuyCoins != null) cardBuyCoins.setOnClickListener(buyCoinsClick);
        if (btnBuyCoins != null) btnBuyCoins.setOnClickListener(buyCoinsClick);

        // 2. Streamer Diamond Payout Engine (1 Diamond = ₹0.50 INR, Min 5,000 Diamonds = ₹2,500 INR)
        View.OnClickListener payoutClick = v -> showDiamondPayoutDialog();
        if (cardDiamondPayout != null) cardDiamondPayout.setOnClickListener(payoutClick);
        if (btnWithdrawDiamonds != null) btnWithdrawDiamonds.setOnClickListener(payoutClick);

        // 3. Web Admin & Moderation Console (admin_dashboard.html)
        if (btnOpenAdminConsole != null) {
            btnOpenAdminConsole.setOnClickListener(v -> showWebAdminDashboardDialog());
        }

        // 4. Firebase Cloud Messaging Push Alerts Tester
        if (btnTestPushAlerts != null) {
            btnTestPushAlerts.setOnClickListener(v -> showPushAlertSelectorDialog());
        }

        return view;
    }

    private void refreshProfileHeader() {
        if (sessionManager == null) return;
        if (tvDisplayName != null) tvDisplayName.setText(sessionManager.getUserName());
        if (tvUserId != null) tvUserId.setText("ID: " + sessionManager.getUserId() + " 📋");
        if (tvCoins != null) tvCoins.setText(String.valueOf(sessionManager.getUserCoins()));
        if (tvDiamonds != null) tvDiamonds.setText(String.valueOf(sessionManager.getUserDiamonds()));
    }

    /**
     * Streamer Diamond Payout Engine Dialog:
     * Conversion Rate: 1 Diamond = ₹0.50 INR (Min withdrawal: 5,000 Diamonds = ₹2,500 INR)
     * Supported withdrawal methods: UPI (username@upi), Direct Bank IMPS/NEFT, PayPal
     */
    private void showDiamondPayoutDialog() {
        Context ctx = requireContext();
        int currentDiamonds = sessionManager.getUserDiamonds();
        double currentInrValue = currentDiamonds * DIAMOND_TO_INR_RATE;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#111126"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(ctx);
        title.setText("💎 Streamer Diamond Payout Engine");
        title.setTextColor(Color.parseColor("#FF4081"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView rateInfo = new TextView(ctx);
        rateInfo.setText(String.format(
                Locale.US,
                "Available: %d Diamonds (≈ ₹%.2f INR)\nConversion Rate: 1 💎 = ₹0.50 INR\nMinimum Withdrawal: 5,000 💎 = ₹2,500 INR",
                currentDiamonds,
                currentInrValue
        ));
        rateInfo.setTextColor(Color.parseColor("#D0D0EC"));
        rateInfo.setTextSize(12f);
        rateInfo.setPadding(0, dp(8), 0, dp(12));
        root.addView(rateInfo);

        EditText etDiamondAmount = new EditText(ctx);
        etDiamondAmount.setHint("Enter Diamonds to withdraw (min 5000)");
        etDiamondAmount.setText(String.valueOf(Math.max(MIN_WITHDRAWAL_DIAMONDS, currentDiamonds)));
        etDiamondAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDiamondAmount.setTextColor(Color.WHITE);
        etDiamondAmount.setHintTextColor(Color.parseColor("#777799"));
        etDiamondAmount.setBackgroundColor(Color.parseColor("#1D1D3B"));
        etDiamondAmount.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(etDiamondAmount);

        TextView tvInrPreview = new TextView(ctx);
        tvInrPreview.setText(String.format(Locale.US, "Payout Amount: ₹%.2f INR", Math.max(MIN_WITHDRAWAL_DIAMONDS, currentDiamonds) * DIAMOND_TO_INR_RATE));
        tvInrPreview.setTextColor(Color.parseColor("#00E676"));
        tvInrPreview.setTextSize(14f);
        tvInrPreview.setTypeface(Typeface.DEFAULT_BOLD);
        tvInrPreview.setPadding(0, dp(8), 0, dp(10));
        root.addView(tvInrPreview);

        etDiamondAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int entered = 0;
                try {
                    entered = Integer.parseInt(s.toString().trim());
                } catch (NumberFormatException ignored) {}
                double inr = entered * DIAMOND_TO_INR_RATE;
                tvInrPreview.setText(String.format(Locale.US, "Payout Amount: ₹%.2f INR", inr));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        RadioGroup rgMethod = new RadioGroup(ctx);
        rgMethod.setOrientation(RadioGroup.VERTICAL);
        rgMethod.setPadding(0, dp(4), 0, dp(8));

        RadioButton rbUpi = new RadioButton(ctx);
        rbUpi.setId(2001);
        rbUpi.setText("⚡ UPI Instant Transfer (username@upi)");
        rbUpi.setTextColor(Color.WHITE);
        rbUpi.setChecked(true);
        rgMethod.addView(rbUpi);

        RadioButton rbImps = new RadioButton(ctx);
        rbImps.setId(2002);
        rbImps.setText("🏦 Direct Bank IMPS / NEFT (Account + IFSC)");
        rbImps.setTextColor(Color.WHITE);
        rgMethod.addView(rbImps);

        RadioButton rbPaypal = new RadioButton(ctx);
        rbPaypal.setId(2003);
        rbPaypal.setText("🌍 PayPal International Payout (email@domain.com)");
        rbPaypal.setTextColor(Color.WHITE);
        rgMethod.addView(rbPaypal);

        root.addView(rgMethod);

        EditText etAccountDetails = new EditText(ctx);
        etAccountDetails.setHint("Enter UPI ID (e.g. julianvance@okaxis)");
        etAccountDetails.setText("julianvance@upi");
        etAccountDetails.setTextColor(Color.WHITE);
        etAccountDetails.setHintTextColor(Color.parseColor("#777799"));
        etAccountDetails.setBackgroundColor(Color.parseColor("#1D1D3B"));
        etAccountDetails.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(etAccountDetails);

        rgMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == 2001) {
                etAccountDetails.setHint("Enter UPI ID (username@upi)");
                etAccountDetails.setText("julianvance@upi");
            } else if (checkedId == 2002) {
                etAccountDetails.setHint("Enter Bank A/C & IFSC (e.g. 50100482910 / HDFC0001234)");
                etAccountDetails.setText("50100482910 / HDFC0001234");
            } else {
                etAccountDetails.setHint("Enter PayPal Email");
                etAccountDetails.setText("julian.vance@paypal.com");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .setPositiveButton("Submit Withdrawal", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setOnClickListener(v -> {
                int diamondsToWithdraw;
                try {
                    diamondsToWithdraw = Integer.parseInt(etDiamondAmount.getText().toString().trim());
                } catch (NumberFormatException e) {
                    Toast.makeText(ctx, "Please enter a valid diamond amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (diamondsToWithdraw < MIN_WITHDRAWAL_DIAMONDS) {
                    Toast.makeText(ctx, "Minimum withdrawal is 5,000 Diamonds (₹2,500 INR)", Toast.LENGTH_LONG).show();
                    return;
                }

                if (diamondsToWithdraw > sessionManager.getUserDiamonds()) {
                    Toast.makeText(ctx, "Insufficient diamonds! Need at least " + diamondsToWithdraw + " 💎", Toast.LENGTH_LONG).show();
                    return;
                }

                String account = etAccountDetails.getText().toString().trim();
                if (account.isEmpty()) {
                    Toast.makeText(ctx, "Please enter your payout account details", Toast.LENGTH_SHORT).show();
                    return;
                }

                String method = "UPI";
                int checkedId = rgMethod.getCheckedRadioButtonId();
                if (checkedId == 2002) method = "IMPS";
                else if (checkedId == 2003) method = "PAYPAL";

                double payoutInr = diamondsToWithdraw * DIAMOND_TO_INR_RATE;
                submitDiamondPayoutRequest(diamondsToWithdraw, payoutInr, method, account, dialog);
            });
        });

        dialog.show();
    }

    private void submitDiamondPayoutRequest(int diamondsAmount,
                                            double payoutInr,
                                            String method,
                                            String accountDetails,
                                            AlertDialog dialog) {
        ApiService.PayoutRequestDto dto = new ApiService.PayoutRequestDto(
                sessionManager.getUserId(),
                diamondsAmount,
                payoutInr,
                method,
                accountDetails
        );

        ApiClient.getApiService().requestDiamondPayout(dto).enqueue(new Callback<ApiService.PayoutResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.PayoutResponseDto> call,
                                   @NonNull Response<ApiService.PayoutResponseDto> response) {
                int remaining = sessionManager.getUserDiamonds() - diamondsAmount;
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    remaining = response.body().remainingDiamonds;
                }
                sessionManager.setUserDiamonds(remaining);
                refreshProfileHeader();
                dialog.dismiss();
                Toast.makeText(
                        requireContext(),
                        String.format(Locale.US, "✅ Payout Requested: ₹%.2f INR via %s (%s)", payoutInr, method, accountDetails),
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.PayoutResponseDto> call, @NonNull Throwable t) {
                int remaining = Math.max(0, sessionManager.getUserDiamonds() - diamondsAmount);
                sessionManager.setUserDiamonds(remaining);
                refreshProfileHeader();
                dialog.dismiss();
                Toast.makeText(
                        requireContext(),
                        String.format(Locale.US, "✅ Payout Queued: ₹%.2f INR via %s (%s)", payoutInr, method, accountDetails),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Opens the Dark Neon Web Admin & Moderation Console (admin_dashboard.html)
     */
    private void showWebAdminDashboardDialog() {
        Dialog webDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        webDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#080814"));

        LinearLayout topBar = new LinearLayout(requireContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#14142B"));
        topBar.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView tvHeader = new TextView(requireContext());
        tvHeader.setText("🛡️ QLUVX Web Admin & Moderation Console");
        tvHeader.setTextColor(Color.parseColor("#FFD700"));
        tvHeader.setTextSize(15f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(tvHeader);

        Button btnClose = new Button(requireContext());
        btnClose.setText("Close ✕");
        btnClose.setAllCaps(false);
        btnClose.setTextColor(Color.WHITE);
        btnClose.setBackgroundColor(Color.parseColor("#FF1744"));
        btnClose.setOnClickListener(v -> webDialog.dismiss());
        topBar.addView(btnClose);

        container.addView(topBar);

        WebView webView = new WebView(requireContext());
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/admin_dashboard.html");

        container.addView(webView);
        webDialog.setContentView(container);
        webDialog.show();
    }

    /**
     * Triggers any of the 4 official FCM Push Notification templates via MyFirebaseMessagingService.
     */
    private void showPushAlertSelectorDialog() {
        String[] options = new String[]{
                "1. Alex sent you a 5-second disappearing photo ⏱️",
                "2. 🔥 Julian Vance just went live! Tap to watch",
                "3. ⚡ You have been challenged to a 5-minute PK battle!",
                "4. 🎉 You received a Super Sports Car from Leo!"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("🔔 Dispatch FCM Push Notification")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            MyFirebaseMessagingService.triggerPresetPushAlert(
                                    requireContext(),
                                    MyFirebaseMessagingService.ALERT_TYPE_DISAPPEARING_PHOTO
                            );
                            break;
                        case 1:
                            MyFirebaseMessagingService.triggerPresetPushAlert(
                                    requireContext(),
                                    MyFirebaseMessagingService.ALERT_TYPE_LIVE_STREAM
                            );
                            break;
                        case 2:
                            MyFirebaseMessagingService.triggerPresetPushAlert(
                                    requireContext(),
                                    MyFirebaseMessagingService.ALERT_TYPE_PK_CHALLENGE
                            );
                            break;
                        case 3:
                        default:
                            MyFirebaseMessagingService.triggerPresetPushAlert(
                                    requireContext(),
                                    MyFirebaseMessagingService.ALERT_TYPE_LUXURY_GIFT
                            );
                            break;
                    }
                    Toast.makeText(requireContext(), "Push Alert Dispatched! Check Status Bar 🔔", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileHeader();
    }

    @Override
    public void onDestroyView() {
        if (billingManager != null) {
            billingManager.destroy();
        }
        super.onDestroyView();
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
