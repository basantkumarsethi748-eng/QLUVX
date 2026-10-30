package com.example.qluvx;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvNearbyGrid;
    private NearbyUserAdapter adapter;
    private List<NearbyUser> userList;
    private String activeRoleFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvNearbyGrid = view.findViewById(R.id.rvNearbyGrid);
        TextView btnFilter = view.findViewById(R.id.btnFilter);

        // 3-Column Blued / Walla Grid Layout
        rvNearbyGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadNearbyUsers();
        adapter = new NearbyUserAdapter(getContext(), userList);
        rvNearbyGrid.setAdapter(adapter);

        // Fetch real nearby users from backend via GPS coordinates
        fetchNearbyUsersViaGps();

        btnFilter.setOnClickListener(v -> {
            if ("All".equals(activeRoleFilter)) {
                activeRoleFilter = "Top";
            } else if ("Top".equals(activeRoleFilter)) {
                activeRoleFilter = "Vers";
            } else if ("Vers".equals(activeRoleFilter)) {
                activeRoleFilter = "Bottom";
            } else {
                activeRoleFilter = "All";
            }
            btnFilter.setText("⚡ Online • " + activeRoleFilter + " ▾");
            Toast.makeText(getContext(), "Filtered by: Online Now & Role (" + activeRoleFilter + ")", Toast.LENGTH_SHORT).show();
            fetchNearbyUsersViaGps();
        });

        return view;
    }

    private void fetchNearbyUsersViaGps() {
        Context context = getContext();
        if (context == null) return;

        double lat = 19.0760; // Default Mumbai coordinates if GPS not yet granted
        double lng = 72.8777;

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (loc != null) {
                        lat = loc.getLatitude();
                        lng = loc.getLongitude();
                    }
                }
            }
        } catch (SecurityException ignored) {
        }

        ApiClient.getApiService()
                .getNearbyUsers(lat, lng, 50.0, activeRoleFilter)
                .enqueue(new Callback<ApiService.NearbyUsersResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiService.NearbyUsersResponse> call,
                            @NonNull Response<ApiService.NearbyUsersResponse> response
                    ) {
                        if (!isAdded() || response.body() == null || response.body().users == null || response.body().users.isEmpty()) {
                            return;
                        }
                        userList.clear();
                        for (ApiService.NearbyUserDto dto : response.body().users) {
                            String dist = String.format(Locale.US, "%.1f km", dto.distanceKm);
                            NearbyUser item = new NearbyUser(
                                    dto.name != null ? dto.name : "VIP User",
                                    dto.age > 0 ? dto.age : 24,
                                    dist,
                                    dto.role != null ? dto.role : "Vers",
                                    dto.isOnline
                            );
                            item.avatarUrl = dto.avatarUrl;
                            userList.add(item);
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.NearbyUsersResponse> call, @NonNull Throwable t) {
                        // Retains immediate local 3-column grid items on offline/preview
                    }
                });
    }

    private void loadNearbyUsers() {
        userList = new ArrayList<>();
        userList.add(new NearbyUser("Aarav", 23, "0.4 km", "Top", true));
        userList.add(new NearbyUser("Kabir", 25, "0.7 km", "Vers", true));
        userList.add(new NearbyUser("Rohan", 21, "1.1 km", "Bottom", true));
        userList.add(new NearbyUser("Zayn", 26, "1.4 km", "Top", false));
        userList.add(new NearbyUser("Dev", 24, "1.9 km", "Vers", true));
        userList.add(new NearbyUser("Vikram", 28, "2.2 km", "Top", true));
        userList.add(new NearbyUser("Aryan", 22, "2.5 km", "Bottom", true));
        userList.add(new NearbyUser("Neel", 27, "3.0 km", "Vers", false));
        userList.add(new NearbyUser("Karan", 24, "3.4 km", "Top", true));
        userList.add(new NearbyUser("Sameer", 25, "3.8 km", "Bottom", true));
        userList.add(new NearbyUser("Reyansh", 23, "4.1 km", "Vers", true));
        userList.add(new NearbyUser("Aditya", 29, "4.6 km", "Top", true));
    }

    public static class NearbyUser {
        public String name;
        public int age;
        public String distance;
        public String role;
        public boolean isOnline;
        public String avatarUrl;

        public NearbyUser(String name, int age, String distance, String role, boolean isOnline) {
            this.name = name;
            this.age = age;
            this.distance = distance;
            this.role = role;
            this.isOnline = isOnline;
        }
    }
}
