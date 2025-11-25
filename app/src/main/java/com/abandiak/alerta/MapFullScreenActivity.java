package com.abandiak.alerta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

import com.google.android.material.appbar.MaterialToolbar;

public class MapFullScreenActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";
    public static final String EXTRA_ZOOM = "extra_zoom";

    private static final String TAG_FS_MAP = "fullscreen_map";

    private GoogleMap map;
    private FusedLocationProviderClient fusedClient;
    private CancellationTokenSource cts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_fullscreen);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle(R.string.app_name);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        setupMapFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cts != null) cts.cancel();
    }

    private void setupMapFragment() {
        SupportMapFragment frag = (SupportMapFragment) getSupportFragmentManager().findFragmentByTag(TAG_FS_MAP);
        if (frag == null) {
            GoogleMapOptions opts = new GoogleMapOptions()
                    .mapToolbarEnabled(true)
                    .zoomControlsEnabled(true);
            frag = SupportMapFragment.newInstance(opts);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainerFull, frag, TAG_FS_MAP)
                    .commitNow();
        }
        frag.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(true);
        enableMyLocationIfAllowed();

        double lat = getIntent().getDoubleExtra(EXTRA_LAT, Double.NaN);
        double lng = getIntent().getDoubleExtra(EXTRA_LNG, Double.NaN);
        float zoom = getIntent().getFloatExtra(EXTRA_ZOOM, 13f);

        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoom));
        } else {
            moveCameraToUser();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationIfAllowed() {
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if ((coarse || fine) && map != null) {
            map.setMyLocationEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void moveCameraToUser() {
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!(coarse || fine)) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.0, 19.0), 5.5f));
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 13f));
                    } else {
                        cts = new CancellationTokenSource();
                        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                                .addOnSuccessListener(loc -> {
                                    if (loc != null) {
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                                new LatLng(loc.getLatitude(), loc.getLongitude()), 13f));
                                    } else {
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.0, 19.0), 5.5f));
                                    }
                                })
                                .addOnFailureListener(e ->
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.0, 19.0), 5.5f)));
                    }
                })
                .addOnFailureListener(e ->
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.0, 19.0), 5.5f)));
    }
}
