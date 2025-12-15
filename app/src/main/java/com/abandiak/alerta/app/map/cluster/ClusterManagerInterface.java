package com.abandiak.alerta.app.map.cluster;

public interface ClusterManagerInterface {
    void addItem(IncidentItem item);
    void clearItems();
    void cluster();
}
