package com.abandiak.alerta.app.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String HOME_MAP_TAG = "home_map_full";
    private static final String DEFAULT_REGION = "GLOBAL";

    private BottomNavigationView bottomNav;
    private GoogleMap map;
    private FusedLocationProviderClient fused;

    private ClusterManager<IncidentItem> clusterManager;
    private IncidentRenderer renderer;

    private final IncidentRepository incidentsRepo = new IncidentRepository();
    private ListenerRegistration incidentsReg;
    private String selectedType = null;

    private final ActivityResultLauncher<String[]> permsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fused = LocationServices.getFusedLocationProviderClient(this);
        attachMap();

        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                if (id == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_tasks) {
                    startActivity(new Intent(this, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_teams) {
                    startActivity(new Intent(this, TeamsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
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
        map.setOnInfoWindowClickListener(clusterManager);

        subscribeIncidents(DEFAULT_REGION, selectedType);

        ensureLocationPermissionAndCenter();
    }

    private void subscribeIncidents(String region, String type) {
        if (incidentsReg != null) {
            incidentsReg.remove();
            incidentsReg = null;
        }

        incidentsReg = incidentsRepo.listenIncidents(region, type, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                if (error != null) {
                    ToastUtils.show(HomeActivity.this, "Error loading incidents: " + error.getMessage());
                    return;
                }
                if (value == null) return;

                clusterManager.clearItems();
                for (DocumentSnapshot d : value.getDocuments()) {
                    Map<String, Object> m = d.getData();
                    if (m == null) continue;
                    Object t = m.get("title");
                    Object desc = m.get("description");
                    Object type = m.get("type");
                    Object lat = m.get("lat");
                    Object lng = m.get("lng");
                    if (t == null || type == null || lat == null || lng == null) continue;

                    double la, ln;
                    try {
                        la = (lat instanceof Number) ? ((Number) lat).doubleValue() : Double.parseDouble(String.valueOf(lat));
                        ln = (lng instanceof Number) ? ((Number) lng).doubleValue() : Double.parseDouble(String.valueOf(lng));
                    } catch (Exception ex) {
                        continue;
                    }

                    IncidentItem item = new IncidentItem(
                            d.getId(),
                            String.valueOf(t),
                            desc != null ? String.valueOf(desc) : "",
                            la, ln,
                            String.valueOf(type)
                    );
                    clusterManager.addItem(item);
                }
                clusterManager.cluster();
            }
        });
    }

    private void ensureLocationPermissionAndCenter() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

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
        if (incidentsReg != null) {
            incidentsReg.remove();
            incidentsReg = null;
        }
    }

    @SuppressWarnings("unused")
    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }
}
