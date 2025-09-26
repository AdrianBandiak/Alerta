package com.abandiak.alerta.app.map;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.app.map.cluster.IncidentItem;
import com.abandiak.alerta.app.map.cluster.IncidentRenderer;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.repository.IncidentRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private BottomNavigationView bottomNav;

    private ClusterManager<IncidentItem> clusterManager;
    private IncidentRepository incidentRepo;
    private ListenerRegistration registration;

    private String currentRegion = "PL-MA";
    private String currentType = "ALL";

    private FusedLocationProviderClient fused;

    private final ActivityResultLauncher<String[]> locationPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fused = LocationServices.getFusedLocationProviderClient(this);
        incidentRepo = new IncidentRepository();

        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
            bottomNav.setOnItemSelectedListener(item -> {
                final int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_map) {
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

        SupportMapFragment frag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (frag != null) frag.getMapAsync(this);

        findViewById(R.id.filter_all).setOnClickListener(v -> setFilter("ALL"));
        findViewById(R.id.filter_info).setOnClickListener(v -> setFilter("INFO"));
        findViewById(R.id.filter_hazard).setOnClickListener(v -> setFilter("HAZARD"));
        findViewById(R.id.filter_critical).setOnClickListener(v -> setFilter("CRITICAL"));

        FloatingActionButton fabAdd = findViewById(R.id.btnAddMarkerFab);
        fabAdd.setOnClickListener(v -> openCreateIncidentSheet());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.0614, 19.9366), 11f));
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setRenderer(new IncidentRenderer(this, map, clusterManager));
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);


        ensureLocationPermission();
        subscribeIncidents();
    }

    private void setFilter(String type) {
        if (!type.equals(currentType)) {
            currentType = type;
            subscribeIncidents();
        }
    }

    private void subscribeIncidents() {
        if (registration != null) { registration.remove(); registration = null; }
        if (map == null || clusterManager == null) return;

        clusterManager.clearItems();
        clusterManager.cluster();

        registration = incidentRepo.listenIncidents(currentRegion, "ALL".equals(currentType) ? null : currentType,
                (QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        ToastUtils.show(this, "Error loading incidents: " + e.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    clusterManager.clearItems();
                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String id = d.getId();
                        String title = d.getString("title");
                        String desc = d.getString("description");
                        String type = d.getString("type");
                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        if (title == null || type == null || lat == null || lng == null) continue;
                        clusterManager.addItem(new IncidentItem(
                                id, title, desc == null ? "" : desc, lat, lng, type
                        ));
                    }
                    clusterManager.cluster();
                });
    }


    private void openCreateIncidentSheet() {
        if (map == null) return;

        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_incident_create, null, false);
        dialog.setContentView(sheet);

        final TextInputEditText inputTitle = sheet.findViewById(R.id.input_title);
        final TextInputEditText inputDescription = sheet.findViewById(R.id.input_description);
        final AutoCompleteTextView inputType = sheet.findViewById(R.id.input_type);

        String[] types = new String[]{"INFO", "HAZARD", "CRITICAL"};
        inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));

        sheet.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        sheet.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
            String desc  = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
            String type  = inputType.getText() != null ? inputType.getText().toString().trim() : "";

            if (title.isEmpty() || type.isEmpty()) {
                ToastUtils.show(this, getString(R.string.incident_invalid));
                return;
            }

            LatLng target = map.getCameraPosition().target;

            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

            com.abandiak.alerta.data.model.Incident inc =
                    new com.abandiak.alerta.data.model.Incident(
                            title, desc, type, target.latitude, target.longitude, currentRegion, uid
                    );

            incidentRepo.createIncident(inc.toMap())
                    .addOnSuccessListener(ref -> {
                        ToastUtils.show(this, "Incident created");
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        ToastUtils.show(this, "Create failed: " + e.getMessage());
                    });
        });

        dialog.show();
    }


    private void ensureLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            enableMyLocationLayer();
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 13f));
                }
            });
        } else {
            if (isPermanentlyDenied()) {
                ToastUtils.show(this, "Enable location permission in Settings.");
                openAppSettings();
            } else {
                locationPermsLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        }
    }

    private void onPermissionsResult(Map<String, Boolean> result) {
        boolean grantedFine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean grantedCoarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (grantedFine || grantedCoarse) {
            enableMyLocationLayer();
        }
    }

    private boolean isPermanentlyDenied() {
        boolean fineDenied = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean coarseDenied = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        boolean fineRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean coarseRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        return fineDenied && coarseDenied && !fineRationale && !coarseRationale;
    }

    private void openAppSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        } catch (ActivityNotFoundException ignored) { }
    }

    private void enableMyLocationLayer() {
        if (map == null) return;
        try { map.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_map);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
