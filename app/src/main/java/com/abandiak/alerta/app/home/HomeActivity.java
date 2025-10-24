package com.abandiak.alerta.app.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.abandiak.alerta.app.tasks.TaskAdapter;
import com.abandiak.alerta.data.model.Task;
import com.abandiak.alerta.data.repository.TaskRepository;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.map.cluster.IncidentItem;
import com.abandiak.alerta.app.map.cluster.IncidentRenderer;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.repository.IncidentRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String HOME_MAP_TAG = "home_map_full";
    private static final String REGION_SAME_AS_MAP = "PL-MA";

    private BottomNavigationView bottomNav;
    private GoogleMap map;
    private FusedLocationProviderClient fused;
    private Chip chipOnline;

    private ClusterManager<IncidentItem> clusterManager;
    private IncidentRenderer renderer;

    private final IncidentRepository incidentRepo = new IncidentRepository();
    private ListenerRegistration registration;
    private List<IncidentItem> allItems = new ArrayList<>();
    private TaskAdapter taskAdapter;
    private TaskRepository taskRepo = new TaskRepository();

    private ListenerRegistration taskListener;

    private String currentQuery = "";

    private ConnectivityManager.NetworkCallback networkCallback;

    private final ActivityResultLauncher<String[]> permsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        View statusBar = new View(this);
        statusBar.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(
                        getResources().getIdentifier("status_bar_height", "dimen", "android"))
        ));
        statusBar.setBackgroundColor(ContextCompat.getColor(this, R.color.status_bar_gray));

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        decorView.addView(statusBar);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
        }

        View root = findViewById(android.R.id.content);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        fused = LocationServices.getFusedLocationProviderClient(this);
        chipOnline = findViewById(R.id.chipOnline);
        TextView textTime = findViewById(R.id.textTime);
        updateCurrentDateTime(textTime);

        attachMap();
        setupSearchUI();
        setupBottomNavigation();
        observeNetworkStatus();

        RecyclerView recyclerTasks = findViewById(R.id.recyclerTasks);
        TextView btnViewAllTasks = findViewById(R.id.btnViewAllTasks);

        if (recyclerTasks != null) {
            recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
            taskAdapter = new TaskAdapter(new ArrayList<>());
            recyclerTasks.setAdapter(taskAdapter);
            recyclerTasks.setVisibility(View.GONE);
            subscribeToTodayTasks();
        }

        if (btnViewAllTasks != null && recyclerTasks != null) {
            btnViewAllTasks.setOnClickListener(v -> {
                if (recyclerTasks.getVisibility() == View.GONE) {
                    recyclerTasks.setVisibility(View.VISIBLE);
                    btnViewAllTasks.setText("Hide");
                } else {
                    recyclerTasks.setVisibility(View.GONE);
                    btnViewAllTasks.setText("View");
                }
            });
        }
    }




    private void observeNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        boolean initialOnline = cm.getActiveNetwork() != null;
        updateConnectionStatus(initialOnline);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> updateConnectionStatus(true));
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> updateConnectionStatus(false));
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    private void updateConnectionStatus(boolean isOnline) {
        if (chipOnline == null) return;

        if (isOnline) {
            chipOnline.setText("ONLINE");
            chipOnline.setChipBackgroundColorResource(R.color.status_online_bg);
            chipOnline.setChipIconResource(R.drawable.ic_status_dot);
            chipOnline.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chipOnline.setText("OFFLINE");
            chipOnline.setChipBackgroundColorResource(R.color.status_offline_bg);
            chipOnline.setChipIconResource(R.drawable.ic_status_dot);
            chipOnline.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }


    private void setupSearchUI() {
        EditText inputSearch = findViewById(R.id.inputSearch);

        if (inputSearch != null) {
            inputSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s == null ? "" : s.toString();
                    applySearchFilter();
                }

                @Override public void afterTextChanged(Editable s) { }
            });

            inputSearch.setOnEditorActionListener((v, actionId, event) -> {
                currentQuery = v.getText().toString();
                applySearchFilter();
                return true;
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, TasksActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_teams) {
                startActivity(new Intent(this, TeamsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_more) {
                startActivity(new Intent(this, MoreActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }


    private void attachMap() {
        SupportMapFragment existing =
                (SupportMapFragment) getSupportFragmentManager().findFragmentByTag(HOME_MAP_TAG);
        if (existing == null) {
            GoogleMapOptions opts = new GoogleMapOptions()
                    .mapToolbarEnabled(false)
                    .zoomControlsEnabled(false)
                    .compassEnabled(false);
            SupportMapFragment frag = SupportMapFragment.newInstance(opts);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, frag, HOME_MAP_TAG)
                    .commitNow();
            frag.getMapAsync(this);
        } else {
            existing.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.0614, 19.9366), 11f));

        clusterManager = new ClusterManager<>(this, map);
        renderer = new IncidentRenderer(this, map, clusterManager);
        clusterManager.setRenderer(renderer);

        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);

        ensureLocationPermissionAndCenter();
        subscribeIncidents(REGION_SAME_AS_MAP, null);
    }

    private void subscribeIncidents(String region, String type) {
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        if (clusterManager == null) return;
        clusterManager.clearItems();
        clusterManager.cluster();

        registration = incidentRepo.listenVisibleIncidentsForCurrentUser(
                type,
                null,
                region,
                (QuerySnapshot snapshots, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        ToastUtils.show(this, "Error loading incidents: " + e.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    allItems.clear();
                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String id = d.getId();
                        String title = d.getString("title");
                        String desc = d.getString("description");
                        String t = d.getString("type");
                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        String photo = d.getString("photoUrl");
                        String createdBy = d.getString("createdBy");
                        boolean verified = d.getBoolean("verified") != null && d.getBoolean("verified");

                        if (title == null || t == null || lat == null || lng == null) continue;

                        IncidentItem item = new IncidentItem(
                                id,
                                title,
                                desc == null ? "" : desc,
                                lat,
                                lng,
                                t,
                                photo,
                                verified,
                                createdBy
                        );
                        allItems.add(item);
                    }
                    applySearchFilter();
                });
    }

    private void applySearchFilter() {
        if (clusterManager == null) return;

        String q = currentQuery == null ? "" : currentQuery.trim();
        String qUpper = q.toUpperCase(Locale.ROOT);
        boolean queryIsType = qUpper.equals("INFO") || qUpper.equals("HAZARD") || qUpper.equals("CRITICAL");

        clusterManager.clearItems();
        for (IncidentItem it : allItems) {
            boolean matches;
            if (q.isEmpty()) {
                matches = true;
            } else if (queryIsType) {
                matches = qUpper.equals(String.valueOf(it.getType()).toUpperCase(Locale.ROOT));
            } else {
                matches = String.valueOf(it.getTitle())
                        .toLowerCase(Locale.ROOT)
                        .contains(q.toLowerCase(Locale.ROOT));
            }
            if (matches) clusterManager.addItem(it);
        }
        clusterManager.cluster();
    }


    private void ensureLocationPermissionAndCenter() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            enableMyLocationAndCenter();
        } else {
            permsLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void onPermissionsResult(Map<String, Boolean> result) {
        boolean grantedFine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean grantedCoarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

        if (grantedFine || grantedCoarse) {
            enableMyLocationAndCenter();
        } else {
            ToastUtils.show(this, "Location permission denied. Showing default region.");
        }
    }

    private void enableMyLocationAndCenter() {
        if (map == null) return;
        try {
            map.setMyLocationEnabled(true);
        } catch (SecurityException ignored) { }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fused.getLastLocation()
                .addOnSuccessListener(this, (Location loc) -> {
                    if (loc != null) {
                        LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 13f));
                    }
                });
    }

    private void subscribeToTodayTasks() {
        if (taskListener != null) taskListener.remove();

        taskListener = taskRepo.listenForTodayTasks(new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                runOnUiThread(() -> {
                    if (taskAdapter != null) taskAdapter.updateData(tasks);
                });
            }

            @Override
            public void onError(Exception e) {
                ToastUtils.show(HomeActivity.this, "Error loading tasks: " + e.getMessage());
            }
        });
    }

    private void updateCurrentDateTime(TextView textTime) {
        if (textTime == null) return;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault());
        String formatted = sdf.format(new java.util.Date());

        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);

        textTime.setText(formatted);

        android.os.Handler handler = new android.os.Handler();
        Runnable updater = new Runnable() {
            @Override
            public void run() {
                String newTime = sdf.format(new java.util.Date());
                newTime = newTime.substring(0, 1).toUpperCase() + newTime.substring(1);
                textTime.setText(newTime);
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(updater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    @SuppressWarnings("unused")
    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }
}
