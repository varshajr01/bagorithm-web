package com.bagorithm.web.service;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RecommendationService {
    public List<PackingItem> recommend(Trip trip) {
        Map<String, PackingItem> items = new LinkedHashMap<>();
        add(items, "Travel documents", "Essentials", 1, 100);
        add(items, "Phone and charger", "Electronics", 1, 250);
        add(items, "Toiletries", "Personal care", 1, 450);
        add(items, "Personal medication", "Health", 1, 100);

        String destination = trip.destination().toLowerCase(Locale.ROOT);
        if (destination.contains("goa")) {
            addAll(items, "Clothing", "Cotton clothes", "Beachwear");
            addAll(items, "Accessories", "Sunglasses", "Sunscreen", "Slippers");
        } else if (destination.contains("manali")) {
            addAll(items, "Clothing", "Jackets", "Sweaters", "Gloves", "Thermal wear", "Woollen socks");
        } else if (destination.contains("ooty")) {
            addAll(items, "Clothing", "Warm clothes", "Sweaters");
            add(items, "Comfortable shoes", "Footwear", 1, 700);
            add(items, "Umbrella", "Weather", 1, 350);
        } else if (destination.contains("kerala")) {
            addAll(items, "Weather", "Raincoat", "Umbrella");
            add(items, "Quick-dry clothes", "Clothing", 2, 200);
        } else if (destination.contains("bengaluru")) {
            add(items, "Casual clothes", "Clothing", 2, 300);
            add(items, "Comfortable footwear", "Footwear", 1, 600);
            if (trip.purpose().equalsIgnoreCase("Business")) add(items, "Formal clothes", "Clothing", 1, 600);
        } else {
            add(items, "Comfortable walking shoes", "Footwear", 1, 700);
            add(items, "Reusable water bottle", "Essentials", 1, 500);
            add(items, "Day bag", "Bags", 1, 350);
        }
        addWeather(items, trip.weather());
        addDuration(items, trip.durationDays());
        addPurpose(items, trip.purpose());
        return new ArrayList<>(items.values());
    }

    private void addWeather(Map<String, PackingItem> items, String weather) {
        switch (weather.toLowerCase(Locale.ROOT)) {
            case "hot" -> addAll(items, "Weather", "Cotton clothes", "Sunscreen", "Sunglasses", "Cap");
            case "cold" -> addAll(items, "Clothing", "Jackets", "Sweaters", "Gloves", "Thermal wear");
            case "rainy" -> addAll(items, "Weather", "Raincoat", "Umbrella", "Waterproof footwear");
            default -> addAll(items, "Essentials", "Comfortable clothes", "Regular travel essentials");
        }
    }

    private void addDuration(Map<String, PackingItem> items, int days) {
        int outfits = Math.max(1, Math.min(days, 14));
        add(items, "Everyday outfits", "Clothing", outfits, 350);
        add(items, "Underwear", "Clothing", outfits, 80);
        if (days <= 2) add(items, "Basic travel essentials", "Essentials", 1, 200);
        else if (days <= 5) {
            add(items, "Additional outfits", "Clothing", Math.max(1, days / 3), 350);
            add(items, "Toiletries refill", "Personal care", 1, 200);
        } else if (days <= 10) {
            add(items, "Additional outfits", "Clothing", Math.max(2, days / 3), 350);
            add(items, "Personal care essentials", "Personal care", 1, 300);
        } else {
            add(items, "Additional outfits", "Clothing", Math.max(3, days / 3), 350);
            add(items, "Laundry bag and detergent", "Laundry", 1, 350);
        }
    }

    private void addPurpose(Map<String, PackingItem> items, String purpose) {
        switch (purpose.toLowerCase(Locale.ROOT)) {
            case "business" -> addAll(items, "Work", "Formal clothes", "Laptop", "Laptop charger", "Business documents");
            case "vacation" -> addAll(items, "Leisure", "Casual clothes", "Camera", "Sunglasses");
            case "adventure" -> addAll(items, "Adventure", "Trekking shoes", "Water bottle", "First-aid kit");
            case "family trip" -> addAll(items, "Family", "Medicines", "Extra clothes", "Personal essentials");
            case "religious trip" -> addAll(items, "Religious", "Traditional clothes", "Comfortable footwear");
            case "educational trip" -> addAll(items, "Education", "Notebook", "Stationery", "Documents");
            default -> add(items, "Travel essentials", "Essentials", 1, 200);
        }
    }

    private void addAll(Map<String, PackingItem> items, String category, String... names) {
        for (String name : names) add(items, name, category, 1, defaultWeightGrams(name));
    }

    private double defaultWeightGrams(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "cotton clothes", "casual clothes", "extra clothes", "comfortable clothes", "warm clothes", "everyday outfits" -> 350;
            case "beachwear", "formal clothes", "traditional clothes" -> 600;
            case "sunglasses" -> 30;
            case "sunscreen" -> 120;
            case "slippers" -> 350;
            case "jackets" -> 800;
            case "sweaters" -> 450;
            case "gloves" -> 100;
            case "thermal wear" -> 350;
            case "woollen socks", "socks" -> 50;
            case "raincoat" -> 400;
            case "umbrella" -> 350;
            case "waterproof footwear", "comfortable footwear" -> 600;
            case "cap" -> 100;
            case "laptop" -> 1500;
            case "laptop charger" -> 200;
            case "business documents", "documents", "travel documents" -> 100;
            case "camera" -> 600;
            case "trekking shoes" -> 800;
            case "water bottle" -> 500;
            case "first-aid kit" -> 250;
            case "medicines", "personal medication" -> 100;
            case "personal essentials" -> 300;
            case "notebook" -> 300;
            case "stationery" -> 200;
            case "regular travel essentials", "travel essentials", "basic travel essentials" -> 200;
            case "toiletries refill" -> 200;
            case "personal care essentials", "toiletries" -> 450;
            case "additional outfits" -> 350;
            case "laundry bag and detergent" -> 350;
            case "underwear" -> 80;
            case "phone and charger" -> 250;
            case "comfortable shoes", "comfortable walking shoes" -> 700;
            case "reusable water bottle" -> 500;
            case "day bag" -> 350;
            case "quick-dry clothes" -> 200;
            default -> 250;
        };
    }

    private void add(Map<String, PackingItem> items, String name, String category, int quantity, double weight) {
        items.putIfAbsent(name.toLowerCase(Locale.ROOT),
                new PackingItem(0, 0, name, category, quantity, weight, false, true));
    }
}
