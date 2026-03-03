package com.imperiordevelopment.dtc.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StatsStore {
    void addDamage(UUID uuid, String name, double amount);

    double getDamage(String name);

    List<Map.Entry<String, Double>> getTop(int size);

    void flushSave();
}


