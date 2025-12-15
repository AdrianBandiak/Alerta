package com.abandiak.alerta.util;

import com.abandiak.alerta.app.map.cluster.ClusterManagerInterface;
import com.abandiak.alerta.app.map.cluster.IncidentItem;

import java.util.ArrayList;
import java.util.List;

public class FakeClusterManager implements ClusterManagerInterface {

    private final List<IncidentItem> items = new ArrayList<>();

    @Override
    public void addItem(IncidentItem item) {
        items.add(item);
    }

    @Override
    public void clearItems() {
        items.clear();
    }

    @Override
    public void cluster() {
    }

    public List<IncidentItem> getItems() {
        return items;
    }
}
