package com.abandiak.alerta.app.map;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.cluster.IncidentItem;
import com.abandiak.alerta.app.map.cluster.IncidentRenderer;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.model.Incident;
import com.abandiak.alerta.data.repository.IncidentRepository;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onLocationPermissionsResult);

    private final ActivityResultLauncher<String> storagePermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) ToastUtils.show(this, "Storage permission denied");
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else ToastUtils.show(this, "Camera permission denied");
            });

    private Uri pickedPhotoUri = null;
    private Uri cameraOutputUri = null;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) pickedPhotoUri = uri;
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success)) {
                    pickedPhotoUri = cameraOutputUri;
                } else {
                    pickedPhotoUri = null;
                }
            });

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

        ChipGroup chips = findViewById(R.id.chips_filters);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if (id == R.id.chip_all) {
                setFilter("ALL");
            } else if (id == R.id.chip_info) {
                setFilter("INFO");
            } else if (id == R.id.chip_hazard) {
                setFilter("HAZARD");
            } else if (id == R.id.chip_critical) {
                setFilter("CRITICAL");
            }
        });

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

        clusterManager.setOnClusterItemClickListener(item -> {
            showIncidentDetails(item);
            return true;
        });

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

        registration = incidentRepo.listenVisibleIncidentsForCurrentUser(
                "ALL".equals(currentType) ? null : currentType,
                /*regionBucket*/ null,
                /*region*/ currentRegion,
                (QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        ToastUtils.show(this, "Error loading incidents: " + e.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    clusterManager.clearItems();
                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String id     = d.getId();
                        String title  = d.getString("title");
                        String desc   = d.getString("description");
                        String type   = d.getString("type");
                        Double lat    = d.getDouble("lat");
                        Double lng    = d.getDouble("lng");
                        String photo  = d.getString("photoUrl");
                        if (title == null || type == null || lat == null || lng == null) continue;

                        IncidentItem item = new IncidentItem(
                                id,
                                title,
                                desc == null ? "" : desc,
                                lat,
                                lng,
                                type,
                                photo
                        );
                        clusterManager.addItem(item);
                    }
                    clusterManager.cluster();
                });
    }

    private void openCreateIncidentSheet() {
        if (map == null) return;

        pickedPhotoUri = null;

        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_incident_create, null, false);
        dialog.setContentView(sheet);

        final TextInputEditText inputTitle = sheet.findViewById(R.id.input_title);
        final TextInputEditText inputDescription = sheet.findViewById(R.id.input_description);
        final AutoCompleteTextView inputType = sheet.findViewById(R.id.input_type);
        final TextView coordsValue = sheet.findViewById(R.id.coords_value);
        final View btnAddPhoto = sheet.findViewById(R.id.btn_add_photo);
        final ImageView imgPreview = sheet.findViewById(R.id.img_preview);

        String[] types = new String[]{"INFO", "HAZARD", "CRITICAL"};
        inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));

        LatLng target = map.getCameraPosition().target;
        if (coordsValue != null) {
            coordsValue.setText(String.format(Locale.US, "lat: %.6f, lng: %.6f", target.latitude, target.longitude));
        }

        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> showPhotoSourceChooser(imgPreview));
        }

        sheet.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        sheet.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
            String desc  = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
            String type  = inputType.getText() != null ? inputType.getText().toString().trim() : "";

            if (title.isEmpty() || type.isEmpty()) {
                ToastUtils.show(this, getString(R.string.incident_invalid));
                return;
            }

            LatLng t = map.getCameraPosition().target;
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

            Incident inc = new Incident(title, desc, type, t.latitude, t.longitude, currentRegion, uid);
            Map<String, Object> data = inc.toMap();
            if (pickedPhotoUri != null) {
                data.put("photoUrl", pickedPhotoUri.toString());
            }

            incidentRepo.createIncident(data)
                    .addOnSuccessListener(ref -> {
                        ToastUtils.show(this, "Incident created");
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        ToastUtils.show(this, "Create failed: " + e.getMessage());
                    });
        });

        if (imgPreview != null && pickedPhotoUri != null) {
            imgPreview.setImageURI(pickedPhotoUri);
            imgPreview.setVisibility(View.VISIBLE);
        }

        dialog.show();
    }

    private void showPhotoSourceChooser(ImageView preview) {
        String[] items = new String[] { "Take photo", "Pick from gallery" };
        new AlertDialog.Builder(this)
                .setTitle(R.string.incident_attach_photo)
                .setItems(items, (d, which) -> {
                    if (which == 0) requestCameraThenLaunch(preview);
                    else requestStorageThenPick(preview);
                })
                .show();
    }

    private void requestStorageThenPick(ImageView preview) {
        if (Build.VERSION.SDK_INT >= 33) {
            pickImage.launch("image/*");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            pickImage.launch("image/*");
        } else {
            storagePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void requestCameraThenLaunch(ImageView preview) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        cameraOutputUri = createImageUriForCamera();
        if (cameraOutputUri == null) {
            ToastUtils.show(this, "Cannot create image file");
            return;
        }
        takePicture.launch(cameraOutputUri);
    }

    private Uri createImageUriForCamera() {
        try {
            File dir = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Alerta");
            if (!dir.exists() && !dir.mkdirs()) return null;

            String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File img = new File(dir, "IMG_" + time + ".jpg");
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", img);
        } catch (Exception e) {
            return null;
        }
    }

    private void showIncidentDetails(@NonNull IncidentItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.bottomsheet_incident_details, null, false);
        dialog.setContentView(content);

        ImageView img = content.findViewById(R.id.img);
        TextView title = content.findViewById(R.id.title);
        TextView desc = content.findViewById(R.id.desc);
        com.google.android.material.chip.Chip chipType = content.findViewById(R.id.chipType);
        TextView coordsWgs = content.findViewById(R.id.coords_wgs);

        title.setText(item.getTitle());
        desc.setText(item.getSnippet() == null ? "" : item.getSnippet());
        chipType.setText(item.getType());

        int color;
        switch (String.valueOf(item.getType())) {
            case "CRITICAL": color = ContextCompat.getColor(this, R.color.red_700); break;
            case "HAZARD":   color = ContextCompat.getColor(this, R.color.amber_700); break;
            default:         color = ContextCompat.getColor(this, R.color.blue_700); break;
        }
        chipType.setTextColor(color);

        double lat = item.getPosition().latitude;
        double lng = item.getPosition().longitude;
        coordsWgs.setText(String.format(Locale.US, "WGS-84:  lat %.6f,  lng %.6f", lat, lng));

        if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
            img.setVisibility(View.VISIBLE);
            Glide.with(this).load(item.getPhotoUrl()).into(img);
        } else {
            img.setVisibility(View.GONE);
        }

        content.findViewById(R.id.btnNavigate).setOnClickListener(v -> {
            Uri gmm = Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + Uri.encode(item.getTitle()) + ")");
            startActivity(new Intent(Intent.ACTION_VIEW, gmm));
        });

        content.findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    item.getTitle() + "\n" +
                            String.format(Locale.US, "WGS-84: %.6f, %.6f", lat, lng));
            startActivity(Intent.createChooser(share, "Share incident"));
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

    private void onLocationPermissionsResult(Map<String, Boolean> result) {
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
