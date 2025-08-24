package com.abandiak.alerta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.graphics.Insets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQ_LOCATION = 1001;
    private static final int REQ_LOCATION_UPGRADE = 1002;
    private static final String TAG_HOME_MAP = "home_map";

    private TextView textLocation;
    private TextView textTime;

    private FusedLocationProviderClient fusedClient;
    private CancellationTokenSource cts;
    private ExecutorService executor;

    private GoogleMap homeMap;
    private CameraPosition lastCamera;

    private final BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                updateTimeNow();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        View root = findViewById(R.id.home_root);
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), root);
        wic.setAppearanceLightStatusBars(true);
        wic.setAppearanceLightNavigationBars(true);

        View appBar = findViewById(R.id.appbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        View scroll = findViewById(R.id.scroll);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            appBar.setPadding(
                    appBar.getPaddingLeft(),
                    appBar.getPaddingTop() + bars.top,
                    appBar.getPaddingRight(),
                    appBar.getPaddingBottom()
            );

            bottomNav.setPadding(
                    bottomNav.getPaddingLeft(),
                    bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(),
                    bottomNav.getPaddingBottom() + bars.bottom
            );

            if (scroll != null) {
                scroll.setPadding(
                        scroll.getPaddingLeft(),
                        scroll.getPaddingTop(),
                        scroll.getPaddingRight(),
                        scroll.getPaddingBottom() + bars.bottom
                );
            }
            return insets;
        });

        textLocation = findViewById(R.id.textLocation);
        textTime = findViewById(R.id.textTime);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        executor = Executors.newSingleThreadExecutor();

        updateTimeNow();

        if (ensurePlayServicesOrShow()) {
            setupEmbeddedMap();
            setupMapCardClick();
        } else {
            View cardMap = findViewById(R.id.cardMap);
            if (cardMap != null) {
                cardMap.setOnClickListener(v ->
                        Toast.makeText(this, R.string.maps_unavailable, Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        ensureLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(timeTickReceiver);
        if (cts != null) { cts.cancel(); cts = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }

    private void updateTimeNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
        textTime.setText(sdf.format(System.currentTimeMillis()));
    }

    private boolean ensurePlayServicesOrShow() {
        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int code = gaa.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) return true;

        if (gaa.isUserResolvableError(code)) {
            gaa.getErrorDialog(this, code, 9001, dialog ->
                    Toast.makeText(this, R.string.maps_unavailable, Toast.LENGTH_SHORT).show()
            ).show();
        } else {
            Toast.makeText(this, R.string.maps_unavailable, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void setupMapCardClick() {
        View cardMap = findViewById(R.id.cardMap);
        if (cardMap != null) {
            cardMap.setOnClickListener(v -> {
                Intent i = new Intent(this, MapFullScreenActivity.class);
                if (homeMap != null) {
                    CameraPosition cp = homeMap.getCameraPosition();
                    i.putExtra(MapFullScreenActivity.EXTRA_LAT, cp.target.latitude);
                    i.putExtra(MapFullScreenActivity.EXTRA_LNG, cp.target.longitude);
                    i.putExtra(MapFullScreenActivity.EXTRA_ZOOM, cp.zoom);
                }
                startActivity(i);
            });
        }
    }

    private void setupEmbeddedMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME_MAP);
        if (mapFragment == null) {
            GoogleMapOptions opts = new GoogleMapOptions()
                    .mapToolbarEnabled(false)
                    .liteMode(false)
                    .zoomControlsEnabled(false)
                    .rotateGesturesEnabled(false)
                    .tiltGesturesEnabled(false);
            mapFragment = SupportMapFragment.newInstance(opts);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment, TAG_HOME_MAP)
                    .commitNow();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        homeMap = googleMap;
        homeMap.getUiSettings().setMyLocationButtonEnabled(false);
        homeMap.getUiSettings().setMapToolbarEnabled(false);
        homeMap.setOnCameraIdleListener(() -> lastCamera = homeMap.getCameraPosition());

        enableMyLocationIfAllowed(homeMap);
        moveCameraToUser(homeMap, true);
    }


    private boolean hasCoarse() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFine() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureLocation() {
        if (!(hasFine() || hasCoarse())) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQ_LOCATION
            );
        } else {
            maybeAskForPreciseUpgrade();
            getLocationOnce();
        }
    }

    private void maybeAskForPreciseUpgrade() {
        if (hasCoarse() && !hasFine() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_LOCATION_UPGRADE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION || requestCode == REQ_LOCATION_UPGRADE) {
            if (hasCoarse() || hasFine()) {
                enableMyLocationIfAllowed(homeMap);
                moveCameraToUser(homeMap, true);
            } else {
                textLocation.setText(getString(R.string.location_permission_denied));
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationIfAllowed(GoogleMap map) {
        if (map != null && (hasCoarse() || hasFine()) && ensurePlayServicesOrShow()) {
            map.setMyLocationEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void moveCameraToUser(GoogleMap map, boolean animate) {
        if (map == null) return;

        if (hasCoarse() || hasFine()) {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                            if (animate) map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 13f));
                            else map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 13f));
                            reverseGeocode(location);
                        } else {
                            cts = new CancellationTokenSource();
                            int priority = hasFine()
                                    ? Priority.PRIORITY_HIGH_ACCURACY
                                    : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
                            fusedClient.getCurrentLocation(priority, cts.getToken())
                                    .addOnSuccessListener(loc -> {
                                        if (loc != null) {
                                            LatLng me2 = new LatLng(loc.getLatitude(), loc.getLongitude());
                                            if (animate) map.animateCamera(CameraUpdateFactory.newLatLngZoom(me2, 13f));
                                            else map.moveCamera(CameraUpdateFactory.newLatLngZoom(me2, 13f));
                                            reverseGeocode(loc);
                                        } else {
                                            moveCameraFallback(map, animate);
                                        }
                                    })
                                    .addOnFailureListener(e -> moveCameraFallback(map, animate));
                        }
                    })
                    .addOnFailureListener(e -> moveCameraFallback(map, animate));
        } else {
            moveCameraFallback(map, animate);
        }
    }

    private void moveCameraFallback(GoogleMap map, boolean animate) {
        LatLng plCenter = new LatLng(52.0, 19.0);
        if (animate) map.animateCamera(CameraUpdateFactory.newLatLngZoom(plCenter, 5.5f));
        else map.moveCamera(CameraUpdateFactory.newLatLngZoom(plCenter, 5.5f));
        textLocation.setText(getString(R.string.location_unknown));
    }

    private void reverseGeocode(@NonNull Location location) {
        if (!Geocoder.isPresent()) {
            textLocation.setText(getString(R.string.location_unknown));
            return;
        }

        final double lat = location.getLatitude();
        final double lon = location.getLongitude();

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                @SuppressWarnings("deprecation")
                List<Address> addrs = geocoder.getFromLocation(lat, lon, 1);
                runOnUiThread(() -> setLocationTextFromAddresses(addrs));
            } catch (Exception e) {
                runOnUiThread(() -> textLocation.setText(getString(R.string.location_unknown)));
            }
        });
    }

    private void setLocationTextFromAddresses(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            textLocation.setText(getString(R.string.location_unknown));
            return;
        }
        Address a = addresses.get(0);
        String city = nullSafe(a.getLocality(), a.getSubAdminArea(), a.getAdminArea());
        String countryCode = a.getCountryCode();
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = Locale.getDefault().getCountry();
        }
        if (city == null || city.isEmpty()) {
            textLocation.setText(getString(R.string.location_unknown));
        } else {
            textLocation.setText(String.format(Locale.getDefault(), "%s (%s)", city, countryCode));
        }
    }

    private String nullSafe(String... options) {
        for (String s : options) if (s != null && !s.trim().isEmpty()) return s;
        return null;
    }

    @SuppressLint("MissingPermission")
    private void getLocationOnce() {
        if (!(hasCoarse() || hasFine())) return;
        try {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) reverseGeocode(location);
                    });
        } catch (SecurityException ignore) { }
    }
}
