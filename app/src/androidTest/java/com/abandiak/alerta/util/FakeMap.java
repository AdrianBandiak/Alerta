package com.abandiak.alerta.util;

import com.abandiak.alerta.app.map.MapInterface;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class FakeMap implements MapInterface {

    private CameraPosition pos;

    public void setFakeCameraPosition(LatLng latLng) {
        pos = new CameraPosition.Builder()
                .target(latLng)
                .zoom(12)
                .build();
    }

    @Override
    public CameraPosition getCameraPosition() {
        return pos;
    }
}
