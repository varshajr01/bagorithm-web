package com.bagorithm.web.service;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WeightCalculatorTest {
    private final WeightCalculator calculator = new WeightCalculator();

    @Test
    void calculatesItemAndTripWeightsFromQuantityAndIndividualWeight() {
        List<PackingItem> items = List.of(
                new PackingItem(1, 1, "T-shirts", "Clothing", 2, 150, false, true),
                new PackingItem(2, 1, "Jeans", "Clothing", 1, 600, false, true),
                new PackingItem(3, 1, "Jacket", "Clothing", 1, 800, false, true),
                new PackingItem(4, 1, "Sunglasses", "Accessories", 1, 30, false, true),
                new PackingItem(5, 1, "Book", "Personal", 1, 400, false, false));

        assertEquals(300, items.get(0).getTotalWeightGrams());
        assertEquals(1730, calculator.calculateGrams(items));
        assertEquals(1.73, calculator.calculateKg(items), 0.0001);
        assertEquals(1.7, calculator.calculateKg(items.subList(0, 3)), 0.0001);
    }

    @Test
    void checksCapacityFromCurrentPackingList() {
        Trip trip = new Trip(1, 1, "Goa", LocalDate.now(), LocalDate.now().plusDays(2), 3,
                "Hot", "Vacation", 1.5);
        List<PackingItem> items = List.of(new PackingItem(1, 1, "Bag", "Travel", 2, 800, false, true));

        assertEquals(-0.1, calculator.remainingCapacityKg(trip, items), 0.0001);
        assertFalse(calculator.isWithinCapacity(trip, items));
    }
}
