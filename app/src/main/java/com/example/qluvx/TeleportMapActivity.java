package com.example.qluvx;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * RADAR GLOBAL CITY TELEPORT MODE (TeleportMapActivity.java):
 * - Interactive Satellite Radar Scanner with sweeping neon beam & pulsing blips
 * - World City Roaming Grid: Bangkok, Tokyo, London, Paris, Mumbai, New York
 * - Tapping any city teleports user GPS in SessionManager and loads that city's nearby guys immediately!
 */
public class TeleportMapActivity extends AppCompatActivity {

    public static final String EXTRA_TELEPORTED_CITY = "extra_teleported_city";

    private SessionManager sessionManager;
    private TextView tvActiveTeleportStatus;
    private TextView tvRadarSubtext;
    private LinearLayout llWorldCitiesGrid;
    private RadarScannerView radarScannerView;

    public static class WorldCitySpec {
        public final String cityName;
        public final String flag;
        public final String country;
        public final String gpsCoords;
        public final String districtVibe;
        public final int onlineCount;
        public final int[] guyPortraitIndices;

        public WorldCitySpec(String cityName, String flag, String country, String gpsCoords,
                             String districtVibe, int onlineCount, int[] guyPortraitIndices) {
            this.cityName = cityName;
            this.flag = flag;
            this.country = country;
            this.gpsCoords = gpsCoords;
            this.districtVibe = districtVibe;
            this.onlineCount = onlineCount;
            this.guyPortraitIndices = guyPortraitIndices;
        }
    }

    public static final WorldCitySpec[] WORLD_CITIES = new WorldCitySpec[]{
            new WorldCitySpec("Bangkok", "🇹🇭", "Thailand", "13.7563° N, 100.5018° E",
                    "Silom Soi 4 • Sathorn Rooftop Pools", 3420, new int[]{0, 2, 4}),
            new WorldCitySpec("Tokyo", "🇯🇵", "Japan", "35.6895° N, 139.6917° E",
                    "Shinjuku Ni-chome • Shibuya Sky Lounge", 2890, new int[]{1, 3, 5}),
            new WorldCitySpec("London", "🇬🇧", "United Kingdom", "51.5074° N, 0.1278° W",
                    "Soho Old Compton St • Vauxhall Clubs", 4150, new int[]{6, 7, 8}),
            new WorldCitySpec("Paris", "🇫🇷", "France", "48.8566° N, 2.3522° E",
                    "Le Marais • Rue des Archives Cafés", 2640, new int[]{9, 10, 11}),
            new WorldCitySpec("Mumbai", "🇮🇳", "India", "19.0760° N, 72.8777° E",
                    "Bandra West • Colaba Sea Lounge", 3980, new int[]{2, 6, 9}),
            new WorldCitySpec("New York", "🇺🇸", "United States", "40.7128° N, 74.0060° W",
                    "Hell's Kitchen • Chelsea & West Village", 5280, new int[]{0, 4, 7})
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teleport_map);

        sessionManager = new SessionManager(this);

        TextView btnBackTeleport = findViewById(R.id.btnBackTeleport);
        TextView btnResetLocalGps = findViewById(R.id.btnResetLocalGps);
        FrameLayout radarCanvasFrame = findViewById(R.id.radarCanvasFrame);
        tvActiveTeleportStatus = findViewById(R.id.tvActiveTeleportStatus);
        tvRadarSubtext = findViewById(R.id.tvRadarSubtext);
        llWorldCitiesGrid = findViewById(R.id.llWorldCitiesGrid);

        if (radarCanvasFrame != null) {
            radarScannerView = new RadarScannerView(this);
            radarCanvasFrame.addView(radarScannerView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        if (btnBackTeleport != null) {
            btnBackTeleport.setOnClickListener(v -> finish());
        }

        if (btnResetLocalGps != null) {
            btnResetLocalGps.setOnClickListener(v -> {
                teleportToCity("Bangalore", "🇮🇳", "12.9716° N, 77.5946° E", "Indiranagar • Koramangala");
            });
        }

        refreshStatusBanner();
        populateWorldCitiesGrid();
    }

    private void refreshStatusBanner() {
        String city = sessionManager.getTeleportCityName();
        String flag = sessionManager.getTeleportCityFlag();
        String coords = sessionManager.getTeleportCityCoords();
        if (tvActiveTeleportStatus != null) {
            tvActiveTeleportStatus.setText("📡 ACTIVE GPS: " + flag + " " + city + " • " + coords);
        }
    }

    private void populateWorldCitiesGrid() {
        if (llWorldCitiesGrid == null) return;
        llWorldCitiesGrid.removeAllViews();

        String activeCity = sessionManager.getTeleportCityName();

        for (int i = 0; i < WORLD_CITIES.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowLp.bottomMargin = dp(10);
            row.setLayoutParams(rowLp);

            View cardLeft = createWorldCityCard(WORLD_CITIES[i], activeCity.equalsIgnoreCase(WORLD_CITIES[i].cityName));
            LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            leftLp.rightMargin = dp(5);
            row.addView(cardLeft, leftLp);

            if (i + 1 < WORLD_CITIES.length) {
                View cardRight = createWorldCityCard(WORLD_CITIES[i + 1], activeCity.equalsIgnoreCase(WORLD_CITIES[i + 1].cityName));
                LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                rightLp.leftMargin = dp(5);
                row.addView(cardRight, rightLp);
            }

            llWorldCitiesGrid.addView(row);
        }
    }

    private View createWorldCityCard(final WorldCitySpec spec, boolean isCurrentlyTeleported) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(isCurrentlyTeleported ? R.drawable.bg_seat_neon_gold : R.drawable.bg_walla_glass_card);

        // Top Row: Flag + City Name + Online Dot Count
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvCityTitle = new TextView(this);
        tvCityTitle.setText(spec.flag + " " + spec.cityName);
        tvCityTitle.setTextColor(Color.WHITE);
        tvCityTitle.setTextSize(15.5f);
        tvCityTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(tvCityTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvOnlinePill = new TextView(this);
        tvOnlinePill.setText("🟢 " + spec.onlineCount);
        tvOnlinePill.setTextColor(Color.parseColor("#00E676"));
        tvOnlinePill.setTextSize(10f);
        tvOnlinePill.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(tvOnlinePill);

        card.addView(headerRow);

        // District Vibe & GPS Coords
        TextView tvDistrict = new TextView(this);
        tvDistrict.setText(spec.districtVibe);
        tvDistrict.setTextColor(Color.parseColor("#00E5FF"));
        tvDistrict.setTextSize(10.5f);
        tvDistrict.setTypeface(Typeface.DEFAULT_BOLD);
        tvDistrict.setPadding(0, dp(3), 0, dp(2));
        card.addView(tvDistrict);

        TextView tvCoords = new TextView(this);
        tvCoords.setText("🛰️ " + spec.gpsCoords);
        tvCoords.setTextColor(Color.parseColor("#9E9EBC"));
        tvCoords.setTextSize(9.5f);
        card.addView(tvCoords);

        // 3 Real HD Portrait Previews of Guys in that City
        LinearLayout avatarsRow = new LinearLayout(this);
        avatarsRow.setOrientation(LinearLayout.HORIZONTAL);
        avatarsRow.setGravity(Gravity.CENTER_VERTICAL);
        avatarsRow.setPadding(0, dp(8), 0, dp(8));

        for (int idx = 0; idx < spec.guyPortraitIndices.length; idx++) {
            CardView cv = new CardView(this);
            LinearLayout.LayoutParams cvLp = new LinearLayout.LayoutParams(dp(34), dp(34));
            if (idx > 0) cvLp.leftMargin = dp(6);
            cv.setLayoutParams(cvLp);
            cv.setRadius(dp(17));
            cv.setCardElevation(0f);
            cv.setCardBackgroundColor(Color.parseColor("#1E1E36"));

            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            WallaPortraitStudio.loadGuyPhoto(this, iv, spec.guyPortraitIndices[idx]);
            cv.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            avatarsRow.addView(cv);
        }

        TextView tvMoreGuys = new TextView(this);
        tvMoreGuys.setText("  + Hot Guys");
        tvMoreGuys.setTextColor(Color.parseColor("#F1C40F"));
        tvMoreGuys.setTextSize(10f);
        tvMoreGuys.setTypeface(Typeface.DEFAULT_BOLD);
        avatarsRow.addView(tvMoreGuys);

        card.addView(avatarsRow);

        // Teleport Action Button
        Button btnTeleportHere = new Button(this);
        btnTeleportHere.setText(isCurrentlyTeleported ? "✅ Teleported Here" : "🚀 Teleport to " + spec.cityName);
        btnTeleportHere.setAllCaps(false);
        btnTeleportHere.setTextSize(11.5f);
        btnTeleportHere.setTypeface(Typeface.DEFAULT_BOLD);
        btnTeleportHere.setTextColor(isCurrentlyTeleported ? Color.parseColor("#0A0A14") : Color.WHITE);
        btnTeleportHere.setBackgroundResource(isCurrentlyTeleported ? R.drawable.bg_walla_cyan_pill : R.drawable.bg_walla_pink_pill);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36));
        btnTeleportHere.setLayoutParams(btnLp);

        View.OnClickListener teleportClick = v ->
                teleportToCity(spec.cityName, spec.flag, spec.gpsCoords, spec.districtVibe);

        card.setOnClickListener(teleportClick);
        btnTeleportHere.setOnClickListener(teleportClick);
        card.addView(btnTeleportHere);

        return card;
    }

    private void teleportToCity(String cityName, String flag, String gpsCoords, String districtVibe) {
        sessionManager.setTeleportCity(cityName, flag, gpsCoords);
        refreshStatusBanner();
        if (tvRadarSubtext != null) {
            tvRadarSubtext.setText("⚡ GPS locked onto " + flag + " " + cityName + " (" + districtVibe + ")! Loading nearby guys...");
        }
        Toast.makeText(
                this,
                "🌍 Teleported GPS to " + flag + " " + cityName + "! Loaded " + cityName + " nearby guys!",
                Toast.LENGTH_SHORT
        ).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_TELEPORTED_CITY, cityName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (radarScannerView != null) {
            radarScannerView.stopSweep();
        }
        super.onDestroy();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    /**
     * Custom Animated Satellite Radar View with rotating cyan/pink sweep beam and glowing city blips.
     */
    private static class RadarScannerView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint blipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator sweepAnimator;
        private float sweepAngle = 0f;

        public RadarScannerView(Context context) {
            super(context);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(3f);
            ringPaint.setColor(Color.parseColor("#4400E5FF"));

            crossPaint.setStyle(Paint.Style.STROKE);
            crossPaint.setStrokeWidth(2f);
            crossPaint.setColor(Color.parseColor("#2800E5FF"));

            blipPaint.setStyle(Paint.Style.FILL);

            sweepAnimator = ValueAnimator.ofFloat(0f, 360f);
            sweepAnimator.setDuration(2600L);
            sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
            sweepAnimator.setInterpolator(new LinearInterpolator());
            sweepAnimator.addUpdateListener(a -> {
                sweepAngle = (float) a.getAnimatedValue();
                invalidate();
            });
            sweepAnimator.start();
        }

        public void stopSweep() {
            if (sweepAnimator != null) {
                sweepAnimator.cancel();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float maxR = Math.min(cx, cy) - 8f;
            if (maxR <= 0) return;

            // Concentric Radar Rings
            canvas.drawCircle(cx, cy, maxR, ringPaint);
            canvas.drawCircle(cx, cy, maxR * 0.68f, ringPaint);
            canvas.drawCircle(cx, cy, maxR * 0.36f, ringPaint);

            // Crosshair Axes
            canvas.drawLine(cx - maxR, cy, cx + maxR, cy, crossPaint);
            canvas.drawLine(cx, cy - maxR, cx, cy + maxR, crossPaint);

            // Rotating Radar Sweep Sector
            canvas.save();
            canvas.rotate(sweepAngle, cx, cy);
            SweepGradient gradient = new SweepGradient(
                    cx, cy,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#1000E5FF"), Color.parseColor("#8800E5FF")},
                    new float[]{0f, 0.72f, 1f}
            );
            sweepPaint.setShader(gradient);
            canvas.drawCircle(cx, cy, maxR, sweepPaint);
            canvas.restore();

            // Glowing Target Blips representing Global Roaming Hotspots
            float[][] blips = {
                    {-0.45f, -0.30f}, {0.52f, -0.25f}, {-0.28f, 0.48f},
                    {0.38f, 0.42f}, {0.15f, -0.58f}, {-0.55f, 0.15f}
            };
            for (int i = 0; i < blips.length; i++) {
                float bx = cx + blips[i][0] * maxR;
                float by = cy + blips[i][1] * maxR;
                blipPaint.setColor(i % 2 == 0 ? Color.parseColor("#FF2A7A") : Color.parseColor("#00E676"));
                canvas.drawCircle(bx, by, 7f, blipPaint);
            }

            // Center GPS Beacon Dot
            blipPaint.setColor(Color.parseColor("#F1C40F"));
            canvas.drawCircle(cx, cy, 9f, blipPaint);
        }
    }
}
