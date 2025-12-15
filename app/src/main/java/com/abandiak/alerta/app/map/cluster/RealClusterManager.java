package com.abandiak.alerta.app.map.cluster;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.ClusterManager;

public class RealClusterManager implements ClusterManagerInterface {

    private final ClusterManager<IncidentItem> real;

    public RealClusterManager(Context context, GoogleMap map) {
        this.real = new ClusterManager<>(context, map);
    }

    public ClusterManager<IncidentItem> getReal() {
        return real;
    }

    @Override
    public void addItem(IncidentItem item) {
        real.addItem(item);
    }

    @Override
    public void clearItems() {
        real.clearItems();
    }

    @Override
    public void cluster() {
        real.cluster();
    }
}
