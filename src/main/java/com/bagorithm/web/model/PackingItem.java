package com.bagorithm.web.model;

public record PackingItem(long id, long tripId, String name, String category, int quantity,
                         double unitWeightGrams, boolean packed, boolean selected) {
    public double getTotalWeightGrams() {
        return quantity * unitWeightGrams;
    }
}
