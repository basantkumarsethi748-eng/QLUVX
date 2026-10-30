package com.example.qluvx;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvNearbyGrid;
    private NearbyUserAdapter adapter;
    private final List<NearbyUser> allUsers = new ArrayList<>();
    private final List<NearbyUser> filteredUsers = new ArrayList<>();
    private final List<ObjectAnimator> pulseAnimators = new ArrayList<>();

    private String activeRoleFilter = "All";
    private boolean onlineOnlyFilter = false;
    private String currentCity = "Bangalore";
    private String searchQuery = "";

    private TextView chipAll, chipOnline, chipTop, chipVersatile, chipBottom;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvNearbyGrid = view.findViewById(R.id.rvNearbyGrid);
        TextView tvLocationSelector = view.findViewById(R.id.tvLocationSelector);
        TextView btnFilter = view.findViewById(R.id.btnFilter);
        TextView btnSearchToggle = view.findViewById(R.id.btnSearchToggle);
        EditText etSearchNearby = view.findViewById(R.id.etSearchNearby);
        LinearLayout llLiveHostsContainer = view.findViewById(R.id.llLiveHostsContainer);
        TextView tvWatchAllLive = view.findViewById(R.id.tvWatchAllLive);

        chipAll = view.findViewById(R.id.chipAll);
        chipOnline = view.findViewById(R.id.chipOnline);
        chipTop = view.findViewById(R.id.chipTop);
        chipVersatile = view.findViewById(R.id.chipVersatile);
        chipBottom = view.findViewById(R.id.chipBottom);

        // 3-Column Exact Walla / Blued Discovery Grid
        rvNearbyGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadWallaNearbyUsers();
        filteredUsers.clear();
        filteredUsers.addAll(allUsers);

        adapter = new NearbyUserAdapter(getContext(), filteredUsers);
        rvNearbyGrid.setAdapter(adapter);

        // Populate Horizontal Live Hosts Banner with Pulsing Gradient Rings
        populateLiveHostsBanner(llLiveHostsContainer);

        if (tvWatchAllLive != null) {
            tvWatchAllLive.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_live_pk);
                }
            });
        }

        // Location Selector ("Bangalore ▾")
        if (tvLocationSelector != null) {
            tvLocationSelector.setOnClickListener(v -> showCityPickerDialog(tvLocationSelector));
        }

        // Search Icon Toggle
        if (btnSearchToggle != null && etSearchNearby != null) {
            btnSearchToggle.setOnClickListener(v -> {
                if (etSearchNearby.getVisibility() == View.VISIBLE) {
                    etSearchNearby.setText("");
                    etSearchNearby.setVisibility(View.GONE);
                } else {
                    etSearchNearby.setVisibility(View.VISIBLE);
                    etSearchNearby.requestFocus();
                }
            });

            etSearchNearby.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Role Filter Chips
        chipAll.setOnClickListener(v -> {
            activeRoleFilter = "All";
            onlineOnlyFilter = false;
            updateChipStyles();
            applyFilters();
        });

        chipOnline.setOnClickListener(v -> {
            onlineOnlyFilter = !onlineOnlyFilter;
            updateChipStyles();
            applyFilters();
        });

        chipTop.setOnClickListener(v -> {
            activeRoleFilter = "Top";
            updateChipStyles();
            applyFilters();
        });

        chipVersatile.setOnClickListener(v -> {
            activeRoleFilter = "Versatile";
            updateChipStyles();
            applyFilters();
        });

        chipBottom.setOnClickListener(v -> {
            activeRoleFilter = "Bottom";
            updateChipStyles();
            applyFilters();
        });

        // Filter Button Dialog
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog(btnFilter));
        }

        return view;
    }

    private void populateLiveHostsBanner(LinearLayout container) {
        Context ctx = getContext();
        if (ctx == null || container == null) return;
        container.removeAllViews();
        clearPulseAnimators();

        String[][] liveBroadcasters = new String[][]{
                {"Alex", "⚔️ PK", "1.4k", "0", "live"},
                {"Ryan", "⚔️ PK", "1.1k", "1", "live"},
                {"Julian", "🎙️ Party", "840", "6", "voice"},
                {"Kabir", "🔴 LIVE", "2.3k", "2", "live"},
                {"Zayn", "🔴 LIVE", "960", "3", "live"},
                {"Liam", "🎙️ Party", "620", "4", "voice"},
                {"Vikram", "🔴 LIVE", "1.8k", "9", "live"}
        };

        for (String[] host : liveBroadcasters) {
            String hostName = host[0];
            String badgeText = host[1];
            String viewers = host[2];
            int portraitIdx = Integer.parseInt(host[3]);
            boolean isVoiceRoom = "voice".equals(host[4]);

            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(dp(74), ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.setMarginEnd(dp(10));
            item.setLayoutParams(itemLp);

            FrameLayout ringFrame = new FrameLayout(ctx);
            ringFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));

            // Pulsing Neon Gradient Ring
            View pulseRing = new View(ctx);
            FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(dp(62), dp(62), Gravity.CENTER);
            pulseRing.setLayoutParams(ringLp);
            GradientDrawable ringDrawable = new GradientDrawable();
            ringDrawable.setShape(GradientDrawable.OVAL);
            ringDrawable.setColor(Color.TRANSPARENT);
            ringDrawable.setStroke(dp(2), isVoiceRoom ? Color.parseColor("#00E5FF") : Color.parseColor("#FF2A7A"));
            pulseRing.setBackground(ringDrawable);
            ringFrame.addView(pulseRing);

            // Animate Pulse
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                    pulseRing,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.94f, 1.07f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.94f, 1.07f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.45f)
            );
            pulse.setDuration(950 + (portraitIdx * 90L));
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.start();
            pulseAnimators.add(pulse);

            // Circular Portrait Card inside Ring
            CardView avatarCard = new CardView(ctx);
            FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER);
            avatarCard.setLayoutParams(cardLp);
            avatarCard.setRadius(dp(26));
            avatarCard.setCardElevation(0f);
            avatarCard.setCardBackgroundColor(Color.parseColor("#141424"));

            ImageView ivHost = new ImageView(ctx);
            ivHost.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivHost.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivHost.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, portraitIdx));
            avatarCard.addView(ivHost);
            ringFrame.addView(avatarCard);

            // Bottom Live / PK Pill Badge
            TextView tvBadge = new TextView(ctx);
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );
            tvBadge.setLayoutParams(badgeLp);
            tvBadge.setText(badgeText);
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setTextSize(8.5f);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setPadding(dp(6), dp(1), dp(6), dp(1));
            tvBadge.setBackgroundResource(isVoiceRoom ? R.drawable.bg_walla_cyan_pill : R.drawable.bg_walla_pink_pill);
            if (isVoiceRoom) {
                tvBadge.setTextColor(Color.parseColor("#0A0A14"));
            }
            ringFrame.addView(tvBadge);

            item.addView(ringFrame);

            TextView tvName = new TextView(ctx);
            tvName.setText(hostName);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(11f);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            tvName.setSingleLine(true);
            tvName.setPadding(0, dp(3), 0, 0);
            item.addView(tvName);

            TextView tvViewers = new TextView(ctx);
            tvViewers.setText("👁 " + viewers);
            tvViewers.setTextColor(Color.parseColor("#9E9EBC"));
            tvViewers.setTextSize(9.5f);
            item.addView(tvViewers);

            item.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    int targetTab = isVoiceRoom ? R.id.nav_voice : R.id.nav_live_pk;
                    ((MainActivity) getActivity()).switchToTab(targetTab);
                }
            });

            container.addView(item);
        }
    }

    private void showCityPickerDialog(TextView tvLocationSelector) {
        String[] cities = new String[]{
                "Bangalore",
                "Mumbai",
                "Delhi NCR",
                "Hyderabad",
                "Pune",
                "Chennai",
                "Goa",
                "Kolkata"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("📍 Select Discovery City")
                .setItems(cities, (dialog, which) -> {
                    currentCity = cities[which];
                    tvLocationSelector.setText("📍 " + currentCity + " ▾");
                    Toast.makeText(getContext(), "Showing nearby guys in " + currentCity, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showFilterDialog(TextView btnFilter) {
        String[] options = new String[]{
                "🔥 All Guys Nearby",
                "💖 Role: Top Only",
                "💎 Role: Versatile Only",
                "💜 Role: Bottom Only",
                "🟢 Online Right Now Only"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("⚡ Walla Discovery Filter")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        activeRoleFilter = "All";
                        onlineOnlyFilter = false;
                        btnFilter.setText("⚡ Filter");
                    } else if (which == 1) {
                        activeRoleFilter = "Top";
                        btnFilter.setText("⚡ Top");
                    } else if (which == 2) {
                        activeRoleFilter = "Versatile";
                        btnFilter.setText("⚡ Vers");
                    } else if (which == 3) {
                        activeRoleFilter = "Bottom";
                        btnFilter.setText("⚡ Bottom");
                    } else if (which == 4) {
                        onlineOnlyFilter = true;
                        btnFilter.setText("🟢 Online");
                    }
                    updateChipStyles();
                    applyFilters();
                })
                .show();
    }

    private void updateChipStyles() {
        if (chipAll == null) return;
        boolean isAll = "All".equalsIgnoreCase(activeRoleFilter) && !onlineOnlyFilter;
        chipAll.setBackgroundResource(isAll ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_glass_card);

        chipOnline.setBackgroundResource(onlineOnlyFilter ? R.drawable.bg_walla_cyan_pill : R.drawable.bg_walla_glass_card);
        chipOnline.setTextColor(onlineOnlyFilter ? Color.parseColor("#0A0A14") : Color.parseColor("#00E676"));

        boolean isTop = "Top".equalsIgnoreCase(activeRoleFilter);
        chipTop.setBackgroundResource(isTop ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_glass_card);
        chipTop.setTextColor(isTop ? Color.WHITE : Color.parseColor("#FF2A7A"));

        boolean isVers = "Versatile".equalsIgnoreCase(activeRoleFilter);
        chipVersatile.setBackgroundResource(isVers ? R.drawable.bg_walla_cyan_pill : R.drawable.bg_walla_glass_card);
        chipVersatile.setTextColor(isVers ? Color.parseColor("#0A0A14") : Color.parseColor("#00E5FF"));

        boolean isBottom = "Bottom".equalsIgnoreCase(activeRoleFilter);
        chipBottom.setBackgroundResource(isBottom ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_glass_card);
        chipBottom.setTextColor(isBottom ? Color.WHITE : Color.parseColor("#CE93D8"));
    }

    private void applyFilters() {
        filteredUsers.clear();
        for (NearbyUser u : allUsers) {
            if (onlineOnlyFilter && !u.isOnline) continue;
            if (!"All".equalsIgnoreCase(activeRoleFilter) && !u.role.equalsIgnoreCase(activeRoleFilter)) {
                continue;
            }
            if (!searchQuery.isEmpty()) {
                boolean matchName = u.name.toLowerCase().contains(searchQuery);
                boolean matchRole = u.role.toLowerCase().contains(searchQuery);
                boolean matchDist = u.distance.toLowerCase().contains(searchQuery);
                if (!matchName && !matchRole && !matchDist) continue;
            }
            filteredUsers.add(u);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadWallaNearbyUsers() {
        allUsers.clear();
        allUsers.add(new NearbyUser("Alex", 25, "120m", "Top", true, true, 0, "182 cm • 78 kg • Indiranagar Gym & Espresso"));
        allUsers.add(new NearbyUser("Ryan", 28, "450m", "Versatile", true, true, 1, "180 cm • 75 kg • Rooftop DJ & Late Night Drives"));
        allUsers.add(new NearbyUser("Kabir", 24, "680m", "Bottom", true, true, 2, "175 cm • 68 kg • Fashion Stylist & Coffee Lover"));
        allUsers.add(new NearbyUser("Zayn", 26, "1.2km", "Top", true, true, 3, "184 cm • 81 kg • Architect • Swimmer"));
        allUsers.add(new NearbyUser("Liam", 23, "1.5km", "Versatile", true, true, 4, "178 cm • 72 kg • Music Producer & Live Host"));
        allUsers.add(new NearbyUser("Noah", 27, "1.8km", "Bottom", false, true, 5, "176 cm • 70 kg • Contemporary Dancer"));
        allUsers.add(new NearbyUser("Aarav", 25, "2.1km", "Top", true, true, 6, "183 cm • 79 kg • Fitness Coach & Traveler"));
        allUsers.add(new NearbyUser("Ethan", 29, "2.4km", "Versatile", true, true, 7, "181 cm • 77 kg • Startup Founder • Wine & Jazz"));
        allUsers.add(new NearbyUser("Rohan", 22, "2.9km", "Bottom", true, false, 8, "174 cm • 66 kg • Art Director & Photographer"));
        allUsers.add(new NearbyUser("Vikram", 28, "3.2km", "Top", true, true, 9, "185 cm • 83 kg • Pilot • Weekend Rides"));
        allUsers.add(new NearbyUser("Lucas", 26, "3.6km", "Versatile", false, true, 10, "179 cm • 74 kg • CrossFit & Beach Trips"));
        allUsers.add(new NearbyUser("Dev", 24, "4.0km", "Bottom", true, true, 11, "176 cm • 69 kg • Midnight Voice Room Regular"));
    }

    private void clearPulseAnimators() {
        for (ObjectAnimator anim : pulseAnimators) {
            if (anim != null) {
                anim.cancel();
            }
        }
        pulseAnimators.clear();
    }

    @Override
    public void onDestroyView() {
        clearPulseAnimators();
        super.onDestroyView();
    }

    private int dp(int value) {
        if (getContext() == null) return value;
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    public static class NearbyUser {
        public String name;
        public int age;
        public String distance;
        public String role;
        public boolean isOnline;
        public boolean isVerified;
        public int portraitIndex;
        public String bioStats;
        public String avatarUrl;

        public NearbyUser(String name, int age, String distance, String role, boolean isOnline) {
            this(name, age, distance, role, isOnline, true, 0, "180 cm • 75 kg • Verified Walla Member");
        }

        public NearbyUser(String name, int age, String distance, String role, boolean isOnline, boolean isVerified, int portraitIndex, String bioStats) {
            this.name = name;
            this.age = age;
            this.distance = distance;
            this.role = role;
            this.isOnline = isOnline;
            this.isVerified = isVerified;
            this.portraitIndex = portraitIndex;
            this.bioStats = bioStats;
        }
    }
}
