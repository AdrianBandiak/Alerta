package com.abandiak.alerta.app.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

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
import com.google.android.material.search.SearchBar;
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

    private ClusterManager<IncidentItem> clusterManager;
    private IncidentRenderer renderer;

    private final IncidentRepository incidentRepo = new IncidentRepository();
    private ListenerRegistration registration;
    private List<IncidentItem> allItems = new ArrayList<>();

    private SearchBar searchBar;
    private String currentQuery = "";

    private final ActivityResultLauncher<String[]> permsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fused = LocationServices.getFusedLocationProviderClient(this);
        attachMap();

        setupSearchUI();
        setupBottomNavigation();
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
    }

    @SuppressWarnings("unused")
    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }
}
