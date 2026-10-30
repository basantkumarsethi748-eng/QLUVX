package com.example.qluvx;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

    private SessionManager sessionManager;
    private TextView tvLocationSelector;
    private String activeRoleFilter = "All";
    private boolean onlineOnlyFilter = false;
    private String currentCity = "Bangalore";
    private String searchQuery = "";

    private TextView chipAll, chipOnline, chipTop, chipVersatile, chipBottom;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        currentCity = sessionManager.getTeleportCityName();

        rvNearbyGrid = view.findViewById(R.id.rvNearbyGrid);
        tvLocationSelector = view.findViewById(R.id.tvLocationSelector);
        TextView btnTeleportRadar = view.findViewById(R.id.btnTeleportRadar);
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

        adapter = new NearbyUserAdapter(getContext(), filteredUsers);
        rvNearbyGrid.setAdapter(adapter);

        refreshTeleportedCityRoster();

        // Populate Horizontal Live Hosts Banner with Pulsing Gradient Rings
        populateLiveHostsBanner(llLiveHostsContainer);

        if (tvWatchAllLive != null) {
            tvWatchAllLive.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_live_pk);
                }
            });
        }

        // Location Selector ("Bangalore ▾") & "🌍 Teleport" Button
        if (tvLocationSelector != null) {
            tvLocationSelector.setOnClickListener(v -> showCityPickerDialog(tvLocationSelector));
        }
        if (btnTeleportRadar != null) {
            btnTeleportRadar.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TeleportMapActivity.class);
                startActivity(intent);
            });
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

    @Override
    public void onResume() {
        super.onResume();
        refreshTeleportedCityRoster();
    }

    private void refreshTeleportedCityRoster() {
        if (sessionManager == null) return;
        currentCity = sessionManager.getTeleportCityName();
        String flag = sessionManager.getTeleportCityFlag();
        if (tvLocationSelector != null) {
            tvLocationSelector.setText(flag + " " + currentCity + " ▾");
        }
        loadWallaNearbyUsersForCity(currentCity);
        applyFilters();
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
            WallaPortraitStudio.loadGuyPhoto(ctx, ivHost, portraitIdx);
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
        String[] options = new String[]{
                "🌍 Open Global Radar Teleport Map (6 World Cities)",
                "🇹🇭 Bangkok (Silom • Sathorn)",
                "🇯🇵 Tokyo (Shinjuku Ni-chome)",
                "🇬🇧 London (Soho • Vauxhall)",
                "🇫🇷 Paris (Le Marais)",
                "🇮🇳 Mumbai (Bandra West)",
                "🇺🇸 New York (Hell's Kitchen • Chelsea)",
                "🇮🇳 Bangalore (Local GPS)"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("🌍 Global Roaming & City Discovery")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(getActivity(), TeleportMapActivity.class));
                        return;
                    }
                    String[][] cityMeta = {
                            {"Bangkok", "🇹🇭", "13.7563° N, 100.5018° E"},
                            {"Tokyo", "🇯🇵", "35.6895° N, 139.6917° E"},
                            {"London", "🇬🇧", "51.5074° N, 0.1278° W"},
                            {"Paris", "🇫🇷", "48.8566° N, 2.3522° E"},
                            {"Mumbai", "🇮🇳", "19.0760° N, 72.8777° E"},
                            {"New York", "🇺🇸", "40.7128° N, 74.0060° W"},
                            {"Bangalore", "🇮🇳", "12.9716° N, 77.5946° E"}
                    };
                    String[] chosen = cityMeta[which - 1];
                    sessionManager.setTeleportCity(chosen[0], chosen[1], chosen[2]);
                    refreshTeleportedCityRoster();
                    Toast.makeText(getContext(), "🌍 Teleported GPS to " + chosen[1] + " " + chosen[0] + "!", Toast.LENGTH_SHORT).show();
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

    private void loadWallaNearbyUsersForCity(String city) {
        allUsers.clear();
        if ("Bangkok".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Arthit", 25, "90m", "Top", true, true, 0, "181 cm • 76 kg • Silom Soi 4 Rooftop Pool"));
            allUsers.add(new NearbyUser("Tawan", 24, "210m", "Versatile", true, true, 2, "178 cm • 71 kg • Sathorn Fitness & Muay Thai"));
            allUsers.add(new NearbyUser("Korn", 27, "380m", "Bottom", true, true, 4, "175 cm • 66 kg • Thonglor DJ & Mixologist"));
            allUsers.add(new NearbyUser("Nanon", 23, "620m", "Versatile", true, true, 1, "179 cm • 72 kg • Sukhumvit Fashion Model"));
            allUsers.add(new NearbyUser("Bright", 26, "850m", "Top", true, true, 6, "183 cm • 79 kg • Bangkok Luxury Condo Host"));
            allUsers.add(new NearbyUser("Win", 24, "1.1km", "Bottom", true, true, 8, "176 cm • 68 kg • Siam Café & Photography"));
            allUsers.add(new NearbyUser("Ohm", 28, "1.4km", "Top", true, true, 3, "185 cm • 82 kg • Personal Trainer • Asok"));
            allUsers.add(new NearbyUser("Tay", 29, "1.8km", "Versatile", false, true, 7, "180 cm • 75 kg • Architect & Yacht Weekends"));
            allUsers.add(new NearbyUser("Mew", 27, "2.2km", "Top", true, true, 9, "184 cm • 80 kg • Rooftop Jazz & Wine"));
        } else if ("Tokyo".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Ren", 25, "110m", "Top", true, true, 1, "180 cm • 74 kg • Shinjuku Ni-chome Lounge"));
            allUsers.add(new NearbyUser("Haruto", 23, "290m", "Bottom", true, true, 3, "174 cm • 65 kg • Shibuya Streetwear Designer"));
            allUsers.add(new NearbyUser("Kaito", 27, "440m", "Versatile", true, true, 5, "182 cm • 77 kg • Roppongi Hills Tech Founder"));
            allUsers.add(new NearbyUser("Sora", 24, "710m", "Bottom", true, true, 8, "175 cm • 66 kg • Harajuku Art Director"));
            allUsers.add(new NearbyUser("Yuto", 28, "980m", "Top", true, true, 0, "184 cm • 81 kg • Ginza Fitness & Boxing"));
            allUsers.add(new NearbyUser("Riku", 26, "1.3km", "Versatile", true, true, 4, "178 cm • 72 kg • Tokyo Tower Night Drives"));
            allUsers.add(new NearbyUser("Daiki", 29, "1.7km", "Top", false, true, 9, "183 cm • 79 kg • Airline Pilot • Haneda"));
            allUsers.add(new NearbyUser("Takumi", 25, "2.1km", "Versatile", true, true, 6, "179 cm • 73 kg • Vinyl DJ & Espresso"));
            allUsers.add(new NearbyUser("Sho", 22, "2.5km", "Bottom", true, true, 11, "173 cm • 64 kg • Contemporary Dancer"));
        } else if ("London".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Oliver", 26, "80m", "Top", true, true, 6, "184 cm • 81 kg • Soho Old Compton St Bar"));
            allUsers.add(new NearbyUser("Jack", 28, "240m", "Versatile", true, true, 7, "182 cm • 78 kg • Vauxhall Club Promoter"));
            allUsers.add(new NearbyUser("Harry", 24, "510m", "Bottom", true, true, 8, "177 cm • 69 kg • Shoreditch Creative Director"));
            allUsers.add(new NearbyUser("Charlie", 27, "790m", "Top", true, true, 0, "186 cm • 84 kg • Canary Wharf Finance & Rugby"));
            allUsers.add(new NearbyUser("Alfie", 23, "1.1km", "Versatile", true, true, 1, "179 cm • 74 kg • Mayfair Cocktail Lounge"));
            allUsers.add(new NearbyUser("Theo", 25, "1.4km", "Bottom", false, true, 2, "176 cm • 68 kg • West End Theatre Actor"));
            allUsers.add(new NearbyUser("Archie", 29, "1.9km", "Top", true, true, 3, "183 cm • 80 kg • Kensington Gym & Sauna"));
            allUsers.add(new NearbyUser("Leo", 26, "2.3km", "Versatile", true, true, 4, "180 cm • 75 kg • Camden Music Producer"));
            allUsers.add(new NearbyUser("Jude", 24, "2.8km", "Bottom", true, true, 5, "175 cm • 67 kg • Notting Hill Photographer"));
        } else if ("Paris".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Lucas", 26, "100m", "Top", true, true, 9, "183 cm • 78 kg • Le Marais Rue des Archives"));
            allUsers.add(new NearbyUser("Gabriel", 24, "260m", "Versatile", true, true, 10, "180 cm • 74 kg • Saint-Germain Wine & Jazz"));
            allUsers.add(new NearbyUser("Louis", 27, "490m", "Bottom", true, true, 11, "176 cm • 68 kg • Haute Couture Stylist"));
            allUsers.add(new NearbyUser("Raphael", 28, "730m", "Top", true, true, 0, "185 cm • 81 kg • Champs-Élysées Fitness"));
            allUsers.add(new NearbyUser("Jules", 23, "1.0km", "Versatile", true, true, 2, "178 cm • 71 kg • Montmartre Artist & Film"));
            allUsers.add(new NearbyUser("Hugo", 29, "1.4km", "Top", true, true, 3, "184 cm • 80 kg • Rooftop Seine River Cruises"));
            allUsers.add(new NearbyUser("Arthur", 25, "1.8km", "Bottom", false, true, 5, "175 cm • 67 kg • Pastry Chef & Espresso"));
            allUsers.add(new NearbyUser("Maxime", 27, "2.2km", "Versatile", true, true, 6, "181 cm • 76 kg • Paris Nightlife Host"));
            allUsers.add(new NearbyUser("Antoine", 26, "2.7km", "Top", true, true, 7, "182 cm • 79 kg • Architect • Bastille"));
        } else if ("Mumbai".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Kabir", 24, "110m", "Bottom", true, true, 2, "176 cm • 68 kg • Bandra West Carter Road"));
            allUsers.add(new NearbyUser("Aarav", 26, "310m", "Top", true, true, 6, "183 cm • 80 kg • Juhu Beach Club & Gym"));
            allUsers.add(new NearbyUser("Vikram", 28, "540m", "Top", true, true, 9, "185 cm • 83 kg • Colaba Rooftop Yacht Host"));
            allUsers.add(new NearbyUser("Zayn", 25, "820m", "Versatile", true, true, 3, "181 cm • 76 kg • Worli Sea Face Penthouse"));
            allUsers.add(new NearbyUser("Rohan", 23, "1.1km", "Bottom", true, true, 8, "175 cm • 67 kg • Bollywood Fashion Stylist"));
            allUsers.add(new NearbyUser("Dev", 27, "1.5km", "Versatile", true, true, 11, "179 cm • 74 kg • Lower Parel Nightclub DJ"));
            allUsers.add(new NearbyUser("Aryan", 25, "1.9km", "Top", true, true, 0, "182 cm • 78 kg • Andheri Fitness Coach"));
            allUsers.add(new NearbyUser("Reyansh", 29, "2.3km", "Versatile", false, true, 7, "180 cm • 75 kg • Marine Drive Late Drives"));
            allUsers.add(new NearbyUser("Vihaan", 24, "2.8km", "Bottom", true, true, 4, "176 cm • 69 kg • Kala Ghoda Art & Coffee"));
        } else if ("New York".equalsIgnoreCase(city)) {
            allUsers.add(new NearbyUser("Mason", 26, "75m", "Top", true, true, 0, "184 cm • 82 kg • Hell's Kitchen 9th Ave Rooftop"));
            allUsers.add(new NearbyUser("Liam", 25, "220m", "Versatile", true, true, 4, "180 cm • 76 kg • Chelsea Piers Equinox Gym"));
            allUsers.add(new NearbyUser("Ethan", 28, "410m", "Versatile", true, true, 7, "182 cm • 78 kg • West Village Speakeasy Host"));
            allUsers.add(new NearbyUser("Noah", 24, "690m", "Bottom", true, true, 5, "176 cm • 69 kg • SoHo Fashion & Broadway"));
            allUsers.add(new NearbyUser("Logan", 29, "950m", "Top", true, true, 3, "186 cm • 85 kg • Tribeca Loft & Wall Street"));
            allUsers.add(new NearbyUser("Aiden", 23, "1.3km", "Bottom", true, true, 2, "175 cm • 67 kg • Brooklyn Warehouse DJ"));
            allUsers.add(new NearbyUser("Carter", 27, "1.6km", "Top", true, true, 9, "183 cm • 80 kg • Fire Island Pines Weekends"));
            allUsers.add(new NearbyUser("Jackson", 26, "2.0km", "Versatile", false, true, 1, "179 cm • 74 kg • Midtown Sky Lounge"));
            allUsers.add(new NearbyUser("Sebastian", 25, "2.4km", "Bottom", true, true, 8, "177 cm • 70 kg • East Village Photographer"));
        } else {
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
