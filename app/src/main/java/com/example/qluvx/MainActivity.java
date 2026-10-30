package com.example.qluvx;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private SessionManager sessionManager;
    private BottomNavigationView bottomNav;
    private ObjectAnimator goLivePulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Session Gatekeeper: Redirect to LoginActivity if user explicitly logged out
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (sessionManager.isScreenshotShieldEnabled() && getWindow() != null) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(this);

        // Glowing Neon Hot Pink "🔴 GO LIVE" Floating Broadcast Button
        TextView btnFloatingGoLive = findViewById(R.id.btnFloatingGoLive);
        if (btnFloatingGoLive != null) {
            goLivePulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    btnFloatingGoLive,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f, 1.06f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f, 1.06f)
            );
            goLivePulseAnimator.setDuration(800L);
            goLivePulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            goLivePulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            goLivePulseAnimator.start();

            btnFloatingGoLive.setOnClickListener(v ->
                    WallaBroadcasterStudio.openBroadcasterStudio(this)
            );
        }

        // Load Default Tab (Tab 1: Walla Nearby Grid)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            if (sessionManager.isCalculatorCloakEnabled()) {
                PrivacySecurityActivity.showWorkingCalculatorCloakDialog(this, sessionManager);
            } else if (sessionManager.isBiometricPinLockEnabled()) {
                PrivacySecurityActivity.showBiometricOrPinPromptDialog(this, sessionManager, null);
            }
        }
    }

    public void switchToTab(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_nearby) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_live_pk) {
            fragment = new LivePkFragment();
        } else if (itemId == R.id.nav_voice) {
            fragment = new VoicePartyFragment();
        } else if (itemId == R.id.nav_messages) {
            fragment = new MessagesFragment();
        } else if (itemId == R.id.nav_profile) {
            fragment = new ProfileFragment();
        }

        return loadFragment(fragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null && !isFinishing()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitAllowingStateLoss();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current instanceof LivePkFragment) {
            ((LivePkFragment) current).refreshLiveCameraFeed();
        }
    }

    @Override
    protected void onDestroy() {
        if (goLivePulseAnimator != null) {
            goLivePulseAnimator.cancel();
        }
        super.onDestroy();
    }
}
