package com.abandiak.alerta.app.map;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.abandiak.alerta.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class IncidentInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View view;

    public IncidentInfoWindowAdapter(LayoutInflater inflater) {
        view = inflater.inflate(R.layout.toast_custom, null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txt = view.findViewById(R.id.toast_text);
        txt.setText(marker.getTitle() + "\n" + (marker.getSnippet() == null ? "" : marker.getSnippet()));
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
