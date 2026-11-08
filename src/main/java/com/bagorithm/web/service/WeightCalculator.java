package com.bagorithm.web.service;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;

import java.util.List;

public class WeightCalculator {
    public double calculateGrams(List<PackingItem> items) {
        return items.stream().filter(PackingItem::selected)
                .mapToDouble(PackingItem::getTotalWeightGrams).sum();
    }

    public double calculateKg(List<PackingItem> items) {
        return calculateGrams(items) / 1000.0;
    }

    public double remainingCapacityKg(Trip trip, List<PackingItem> items) {
        return trip.maxCapacityKg() - calculateKg(items);
    }

    public boolean isWithinCapacity(Trip trip, List<PackingItem> items) {
        return calculateKg(items) <= trip.maxCapacityKg();
    }
}
