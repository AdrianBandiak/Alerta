package com.abandiak.alerta.app.map;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.cluster.IncidentItem;
import com.abandiak.alerta.app.map.cluster.IncidentRenderer;
import com.abandiak.alerta.app.messages.MessagesActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private BottomNavigationView bottomNav;
    private AppBarLayout appBar;

    private ClusterManager<IncidentItem> clusterManager;
    private IncidentRepository incidentRepo;
    private ListenerRegistration registration;

    private String currentRegion = "PL-MA";
    private String currentType = "ALL";

    private FusedLocationProviderClient fused;

    private Uri pickedPhotoUri = null;
    private Uri cameraOutputUri = null;
    private ImageView currentPhotoPreview = null;
    private boolean pendingOpenCreateIncident = false;

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

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pickedPhotoUri = uri;
                    updatePreview();
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success)) {
                    pickedPhotoUri = cameraOutputUri;
                    updatePreview();
                    scanIfNeeded(cameraOutputUri);
                } else {
                    pickedPhotoUri = null;
                    updatePreview();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemBars.apply(this);
        setContentView(R.layout.activity_map);

        fused = LocationServices.getFusedLocationProviderClient(this);
        incidentRepo = new IncidentRepository();

        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                }
                if (id == R.id.nav_map) return true;

                if (id == R.id.nav_tasks) {
                    startActivity(new Intent(this, TasksActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                }
                if (id == R.id.nav_teams) {
                    startActivity(new Intent(this, TeamsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                }
                if (id == R.id.nav_messages) {
                    startActivity(new Intent(this, MessagesActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                }
                if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    return true;
                }

                return false;
            });
        }

        SupportMapFragment frag =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (frag != null) frag.getMapAsync(this);

        ChipGroup chips = findViewById(R.id.chips_filters);
        if (chips != null) {

            chips.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            chips.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            adjustChipsToSingleLine(chips);
                        }
                    });

            chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                java.util.List<String> selectedTypes = new java.util.ArrayList<>();

                if (checkedIds.contains(R.id.chip_info)) selectedTypes.add("INFO");
                if (checkedIds.contains(R.id.chip_hazard)) selectedTypes.add("HAZARD");
                if (checkedIds.contains(R.id.chip_critical)) selectedTypes.add("CRITICAL");
                if (checkedIds.contains(R.id.chip_team)) selectedTypes.add("TEAM");

                if (selectedTypes.isEmpty()) {
                    currentType = "ALL";
                    subscribeIncidents();
                } else {
                    filterByTypes(selectedTypes);
                }
            });
        }

        FloatingActionButton fabAdd = findViewById(R.id.btnAddMarkerFab);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> openCreateIncidentSheet());
        }
    }


    private void filterByTypes(List<String> types) {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        if (map == null || clusterManager == null) return;

        clusterManager.clearItems();
        clusterManager.cluster();

        registration = incidentRepo.listenVisibleIncidentsForCurrentUser(
                null,
                null,
                currentRegion,
                (snapshots, e) -> {
                    if (e != null) {
                        Log.e("MAP_DEBUG", "Firestore error: " + e.getMessage(), e);
                        return;
                    }
                    if (snapshots == null) return;

                    clusterManager.clearItems();

                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        String type = d.getString("type");
                        if (type == null) continue;
                        if (!types.contains(type)) continue;

                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        if (lat == null || lng == null) continue;

                        String id = d.getId();
                        String title = d.getString("title");
                        String desc = d.getString("description");
                        String photo = d.getString("photoUrl");
                        String createdBy = d.getString("createdBy");
                        boolean verified = Boolean.TRUE.equals(d.getBoolean("verified"));

                        IncidentItem item = new IncidentItem(id, title, desc, lat, lng, type, photo, verified, createdBy);

                        if (d.contains("teamId")) item.setTeamId(d.getString("teamId"));
                        if (d.contains("teamColor")) {
                            Long colorLong = d.getLong("teamColor");
                            if (colorLong != null) item.setTeamColor(colorLong.intValue());
                        }

                        clusterManager.addItem(item);
                    }

                    clusterManager.cluster();
                });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
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

        if (pendingOpenCreateIncident) {
            openCreateIncidentSheet();
            pendingOpenCreateIncident = false;
        }
    }


    private void raiseFabAboveBottomNav(@NonNull View fab) {
        if (bottomNav == null) return;
        fab.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int navH = bottomNav.getHeight();
                int extra = dp(16);
                android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, navH + dp(24) + extra);
                fab.setLayoutParams(mlp);
                fab.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void openCreateIncidentSheet() {
        pickedPhotoUri = null;
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_incident_create, null, false);
        dialog.setContentView(sheet);

        final TextInputEditText inputTitle = sheet.findViewById(R.id.input_title);
        final TextInputEditText inputDescription = sheet.findViewById(R.id.input_description);
        final MaterialAutoCompleteTextView inputType = sheet.findViewById(R.id.input_type);
        final TextInputEditText inputLat = sheet.findViewById(R.id.input_lat);
        final TextInputEditText inputLng = sheet.findViewById(R.id.input_lng);
        final View btnAddPhoto = sheet.findViewById(R.id.btn_add_photo);
        final ImageView imgPreview = sheet.findViewById(R.id.img_preview);
        final View btnRemovePhoto = sheet.findViewById(R.id.btn_remove_photo);
        final View btnCancel = sheet.findViewById(R.id.btnCancel);
        final View btnSave = sheet.findViewById(R.id.btnSave);

        final TextInputLayout layoutTeam = sheet.findViewById(R.id.layout_team);
        final MaterialAutoCompleteTextView inputTeam = sheet.findViewById(R.id.input_team);

        String[] types = new String[]{"INFO", "HAZARD", "CRITICAL", "TEAM"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                types
        );
        inputType.setAdapter(adapter);

        inputType.setDropDownBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.bg_dropdown_alerta)
        );


        inputType.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();

            if ("TEAM".equals(selected)) {
                layoutTeam.setVisibility(View.VISIBLE);
                loadUserTeams(inputTeam);
                inputTeam.setDropDownBackgroundDrawable(
                        ContextCompat.getDrawable(this, R.drawable.bg_dropdown_alerta)
                );
            } else {
                layoutTeam.setVisibility(View.GONE);
                inputTeam.setText("");
            }
        });


        LatLng target = map.getCameraPosition().target;
        inputLat.setText(String.format(Locale.US, "%.6f", target.latitude));
        inputLng.setText(String.format(Locale.US, "%.6f", target.longitude));

        btnAddPhoto.setOnClickListener(v -> {
            currentPhotoPreview = imgPreview;
            showPhotoSourceChooser();
        });

        btnRemovePhoto.setOnClickListener(v -> {
            pickedPhotoUri = null;
            updatePreview();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
            String desc = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
            String type = inputType.getText() != null ? inputType.getText().toString().trim() : "";
            String slat = inputLat.getText() != null ? inputLat.getText().toString().trim() : "";
            String slng = inputLng.getText() != null ? inputLng.getText().toString().trim() : "";

            if (title.isEmpty() || type.isEmpty() || slat.isEmpty() || slng.isEmpty()) {
                ToastUtils.show(this, getString(R.string.incident_invalid));
                return;
            }

            double lat, lng;
            try {
                lat = Double.parseDouble(slat);
                lng = Double.parseDouble(slng);
            } catch (Exception ex) {
                ToastUtils.show(this, getString(R.string.incident_invalid));
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "anonymous";

            if ("TEAM".equals(type)) {
                String selectedTeam = inputTeam.getText() != null ? inputTeam.getText().toString().trim() : "";
                if (selectedTeam.isEmpty()) {
                    ToastUtils.show(this, "Select a team first");
                    return;
                }

                FirebaseFirestore.getInstance()
                        .collection("teams")
                        .whereEqualTo("name", selectedTeam)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(snap -> {
                            if (snap.isEmpty()) {
                                ToastUtils.show(this, "Team not found");
                                return;
                            }

                            DocumentSnapshot teamDoc = snap.getDocuments().get(0);
                            String teamId = teamDoc.getId();
                            Long colorLong = teamDoc.getLong("color");
                            int teamColor = colorLong != null ? colorLong.intValue() : ContextCompat.getColor(this, R.color.blue_700);

                            List<String> aud = (List<String>) teamDoc.get("membersIndex");

                            Incident inc = new Incident(title, desc, type, lat, lng, currentRegion, uid);
                            inc.setTeamId(teamId);
                            inc.setTeamColor(teamColor);
                            Map<String, Object> data = inc.toMap();
                            data.put("aud", aud);
                            data.put("createdBy", uid);
                            data.put("verified", false);
                            data.put("teamId", teamId);
                            data.put("teamColor", teamColor);
                            if (pickedPhotoUri != null) data.put("photoUrl", pickedPhotoUri.toString());

                            incidentRepo.createIncident(data)
                                    .addOnSuccessListener(ref -> {
                                        ToastUtils.show(this, "Team incident created for " + selectedTeam);
                                        dialog.dismiss();
                                        subscribeIncidents();
                                    })
                                    .addOnFailureListener(e -> ToastUtils.show(this, "Create failed: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> ToastUtils.show(this, "Failed to get team info"));
                return;
            }

            Incident inc = new Incident(title, desc, type, lat, lng, currentRegion, uid);
            Map<String, Object> data = inc.toMap();
            data.put("createdBy", uid);
            data.put("verified", false);
            if (pickedPhotoUri != null) data.put("photoUrl", pickedPhotoUri.toString());

            incidentRepo.createIncident(data)
                    .addOnSuccessListener(ref -> {
                        ToastUtils.show(this, "Incident created");
                        dialog.dismiss();
                        subscribeIncidents();
                    })
                    .addOnFailureListener(e ->
                            ToastUtils.show(this, "Create failed: " + e.getMessage()));
        });

        dialog.show();
    }


    private void setFilter(String type) {
        if (!type.equals(currentType)) {
            currentType = type;
            subscribeIncidents();
        }
    }


    private void subscribeIncidents() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        if (map == null || clusterManager == null) return;
        clusterManager.clearItems();
        clusterManager.cluster();

        Log.d("MAP_DEBUG", "Subscribing to incidents: type=" + currentType);

        registration = incidentRepo.listenVisibleIncidentsForCurrentUser(
                "ALL".equals(currentType) ? null : currentType,
                null,
                currentRegion,
                (QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("MAP_DEBUG", "Firestore error: " + e.getMessage(), e);
                        ToastUtils.show(this, "Error loading incidents: " + e.getMessage());
                        return;
                    }
                    if (snapshots == null) {
                        Log.w("MAP_DEBUG", "No snapshots returned (null)");
                        return;
                    }

                    Log.d("MAP_DEBUG", "Loaded " + snapshots.size() + " incidents from Firestore");
                    clusterManager.clearItems();

                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        Log.d("MAP_DEBUG", "Incident doc: " + d.getId() + " | " + d.getData());

                        String id = d.getId();
                        String title = d.getString("title");
                        String desc = d.getString("description");
                        String type = d.getString("type");
                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        String photo = d.getString("photoUrl");
                        String createdBy = d.getString("createdBy");
                        boolean verified = d.getBoolean("verified") != null && d.getBoolean("verified");

                        if (title == null || type == null || lat == null || lng == null) {
                            Log.w("MAP_DEBUG", "Skipping invalid incident: " + id);
                            continue;
                        }

                        IncidentItem item = new IncidentItem(
                                id,
                                title,
                                desc == null ? "" : desc,
                                lat,
                                lng,
                                type,
                                photo,
                                verified,
                                createdBy
                        );

                        if (d.contains("teamId")) item.setTeamId(d.getString("teamId"));
                        if (d.contains("teamColor")) {
                            Long colorLong = d.getLong("teamColor");
                            if (colorLong != null) item.setTeamColor(colorLong.intValue());
                            Log.d("MAP_DEBUG", "Team color for " + id + ": " + item.getTeamColor());
                        }

                        clusterManager.addItem(item);
                    }

                    Log.d("MAP_DEBUG", "Finished adding items → clustering...");
                    clusterManager.cluster();
                });
    }


    private void showPhotoSourceChooser() {
        String[] items = new String[]{"Take photo", "Pick from gallery"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.incident_attach_photo)
                .setItems(items, (d, which) -> {
                    if (which == 0) requestCameraThenLaunch();
                    else requestStorageThenPick();
                })
                .show();
    }

    private void requestStorageThenPick() {
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

    private void requestCameraThenLaunch() {
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
            String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Alerta");
                return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File dir = new File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                        "Alerta"
                );
                if (!dir.exists() && !dir.mkdirs()) return null;
                File img = new File(dir, fileName);
                return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", img);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void scanIfNeeded(Uri uri) {
        if (uri == null || Build.VERSION.SDK_INT >= 29) return;
        try {
            File pictures = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
            if (pictures != null) {
                android.media.MediaScannerConnection.scanFile(this, new String[]{pictures.getAbsolutePath()}, null, null);
            }
        } catch (Exception ignored) { }
    }

    private void updatePreview() {
        if (currentPhotoPreview == null) return;

        View parent = (View) currentPhotoPreview.getParent();
        View remove = parent.findViewById(R.id.btn_remove_photo);
        View addButton = ((View) parent.getParent()).findViewById(R.id.btn_add_photo);

        if (pickedPhotoUri != null) {
            currentPhotoPreview.setImageURI(pickedPhotoUri);
            currentPhotoPreview.setVisibility(View.VISIBLE);
            if (remove != null) remove.setVisibility(View.VISIBLE);
            if (addButton != null) addButton.setVisibility(View.GONE);
        } else {
            currentPhotoPreview.setImageDrawable(null);
            currentPhotoPreview.setVisibility(View.GONE);
            if (remove != null) remove.setVisibility(View.GONE);
            if (addButton != null) addButton.setVisibility(View.VISIBLE);
        }
    }


    private void showIncidentDetails(@NonNull IncidentItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.bottomsheet_incident_details, null, false);
        dialog.setContentView(content);

        View sheetParent = (View) content.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheetParent);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.setDraggable(true);

        ImageView img = content.findViewById(R.id.img);
        TextView title = content.findViewById(R.id.title);
        TextView desc = content.findViewById(R.id.desc);
        Chip chipType = content.findViewById(R.id.chipType);
        TextView coordsWgs = content.findViewById(R.id.coords_wgs);
        MaterialCheckBox checkboxVerified = content.findViewById(R.id.checkbox_verified);
        View btnDelete = content.findViewById(R.id.btn_delete_incident);
        LinearLayout logsContainer = content.findViewById(R.id.logs_container);
        TextView logsTitle = content.findViewById(R.id.logs_title);

        title.setText(item.getTitle());
        desc.setText(item.getSnippet() == null ? "" : item.getSnippet());
        chipType.setText(item.getType());

        int bgColor;
        if ("TEAM".equals(item.getType()) && item.getTeamColor() != 0) {
            bgColor = item.getTeamColor();
        } else {
            switch (String.valueOf(item.getType())) {
                case "CRITICAL":
                    bgColor = ContextCompat.getColor(this, R.color.red_700);
                    break;
                case "HAZARD":
                    bgColor = ContextCompat.getColor(this, R.color.amber_700);
                    break;
                case "INFO":
                default:
                    bgColor = ContextCompat.getColor(this, R.color.blue_700);
                    break;
            }
        }
        chipType.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
        chipType.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        double lat = item.getPosition().latitude;
        double lng = item.getPosition().longitude;
        String latDir = lat >= 0 ? "N" : "S";
        String lngDir = lng >= 0 ? "E" : "W";
        coordsWgs.setText(String.format(Locale.US, "%.2f° %s  %.2f° %s",
                Math.abs(lat), latDir, Math.abs(lng), lngDir));

        checkboxVerified.setChecked(item.isVerified());
        checkboxVerified.setEnabled(false);

        content.findViewById(R.id.btnNavigate).setOnClickListener(v -> {
            Uri gmm = Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + Uri.encode(item.getTitle()) + ")");
            startActivity(new Intent(Intent.ACTION_VIEW, gmm));
        });

        content.findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    item.getTitle() + "\n" +
                            String.format(Locale.US, "%.2f° %s, %.2f° %s",
                                    Math.abs(lat), latDir, Math.abs(lng), lngDir));
            startActivity(Intent.createChooser(share, "Share incident"));
        });

        if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
            img.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(item.getPhotoUrl())
                    .centerCrop()
                    .into(img);
        } else {
            img.setVisibility(View.GONE);
        }

        logsContainer.removeAllViews();
        logsTitle.setVisibility(View.GONE);

        if (item.getId() != null) {
            FirebaseFirestore.getInstance()
                    .collection("incidents")
                    .document(item.getId())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> logs =
                                    (List<Map<String, Object>>) snapshot.get("logs");

                            if (logs != null && !logs.isEmpty()) {
                                logsTitle.setVisibility(View.VISIBLE);
                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm: ", Locale.getDefault());

                                for (Map<String, Object> entry : logs) {
                                    try {
                                        Long ts = (Long) entry.get("timestamp");
                                        String action = String.valueOf(entry.get("action"));
                                        String time = (ts != null)
                                                ? sdf.format(new Date(ts))
                                                : "--:--";

                                        TextView logView = new TextView(this);
                                        logView.setText(String.format(Locale.US, "%s   %s", time, action));
                                        logView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                                        logView.setTextSize(13);
                                        logView.setTypeface(logView.getTypeface(), Typeface.ITALIC);
                                        logView.setPadding(0, dp(2), 0, dp(2));
                                        logsContainer.addView(logView);

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else {
                                logsTitle.setVisibility(View.VISIBLE);
                                TextView empty = new TextView(this);
                                empty.setText("No activity logs available.");
                                empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                                empty.setTextSize(13);
                                empty.setTypeface(empty.getTypeface(), Typeface.ITALIC);
                                logsContainer.addView(empty);
                            }
                        }
                    })
                    .addOnFailureListener(e -> ToastUtils.show(this, "Failed to load incident history: " + e.getMessage()));
        }

        String uid =
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;

        if (!item.isVerified() && uid != null && uid.equals(item.getCreatedBy())) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_incident, null);

                AlertDialog deleteDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();

                dialogView.findViewById(R.id.btnCancel).setOnClickListener(x -> deleteDialog.dismiss());

                dialogView.findViewById(R.id.btnConfirm).setOnClickListener(x -> new IncidentRepository()
                        .deleteIncident(item.getId())
                        .addOnSuccessListener(unused -> {
                            ToastUtils.show(this, "Incident deleted");
                            deleteDialog.dismiss();
                            dialog.dismiss(); // zamknij bottomsheet
                        })
                        .addOnFailureListener(e ->
                                ToastUtils.show(this, "Failed to delete: " + e.getMessage())));

                deleteDialog.show();
            });

        } else {
            btnDelete.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void adjustChipsToSingleLine(ChipGroup chipGroup) {
        int groupWidth = chipGroup.getWidth();
        int paddingH = chipGroup.getPaddingLeft() + chipGroup.getPaddingRight();
        int availableWidth = groupWidth - paddingH;

        if (availableWidth <= 0) return;

        int totalChipsWidth = 0;
        int childCount = chipGroup.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = chipGroup.getChildAt(i);
            child.measure(
                    View.MeasureSpec.UNSPECIFIED,
                    View.MeasureSpec.UNSPECIFIED
            );
            totalChipsWidth += child.getMeasuredWidth();
        }

        int chipSpacingPx = dp(4);
        int totalSpacing = chipSpacingPx * Math.max(0, childCount - 1);
        int totalWithSpacing = totalChipsWidth + totalSpacing;

        Log.e("CHIP_DEBUG", "------------------------------------------");
        Log.e("CHIP_DEBUG", "ChipGroup width: " + groupWidth);
        Log.e("CHIP_DEBUG", "ChipGroup horizontal padding: " + paddingH);
        Log.e("CHIP_DEBUG", "Chip spacing (H): " + chipSpacingPx);
        Log.e("CHIP_DEBUG", "TOTAL chips+spacing width: " + totalWithSpacing);
        Log.e("CHIP_DEBUG", "AVAILABLE width (group - padding): " + availableWidth);
        Log.e("CHIP_DEBUG", "------------------------------------------");
    }


    private void loadUserTeams(MaterialAutoCompleteTextView inputTeam) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("teams")
                .whereArrayContains("membersIndex", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        inputTeam.setText("No teams found", false);
                        inputTeam.setEnabled(false);
                        return;
                    }
                    inputTeam.setEnabled(true);

                    List<String> teamNames = new java.util.ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        String name = d.getString("name");
                        if (name != null) teamNames.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            R.layout.list_item_dropdown,
                            teamNames
                    );
                    inputTeam.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    inputTeam.setText("Error loading teams", false);
                    inputTeam.setEnabled(false);
                });
    }

    private void centerOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
                    } else {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(50.0614, 19.9366), 11f));
                    }
                });
    }

    private void ensureLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) enableMyLocationLayer();
        else locationPermsLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void enableMyLocationLayer() {
        if (map == null) return;

        try {
            map.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {}

        centerOnUserLocation();
    }


    private void onLocationPermissionsResult(Map<String, Boolean> result) {
        boolean grantedFine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean grantedCoarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (grantedFine || grantedCoarse) {
            enableMyLocationLayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getBooleanExtra("open_create_incident", false)) {
            getIntent().removeExtra("open_create_incident");
            pendingOpenCreateIncident = true;
        }

        if (pendingOpenCreateIncident && map != null) {
            openCreateIncidentSheet();
            pendingOpenCreateIncident = false;
        }

        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_map);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
