package com.bagorithm.web.service;

import com.bagorithm.web.model.Trip;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationServiceTest {
    private final RecommendationService service = new RecommendationService();

    @Test
    void combinesDestinationWeatherDurationAndPurposeRecommendationsWithoutDuplicates() {
        Trip trip = new Trip(1, 1, "Goa", LocalDate.now(), LocalDate.now().plusDays(4), 5,
                "Hot", "Vacation", 15);

        var recommendations = service.recommend(trip);
        Set<String> names = recommendations.stream().map(item -> item.name().toLowerCase()).collect(Collectors.toSet());

        assertTrue(names.containsAll(Set.of("beachwear", "sunscreen", "camera", "everyday outfits", "underwear")));
        assertTrue(names.size() == recommendations.size());
    }

    @Test
    void includesWeatherDurationAndPurposeSpecificItems() {
        var cold = service.recommend(trip("Unknown", 3, "Cold", "Business"));
        var rainy = service.recommend(trip("Unknown", 8, "Rainy", "Adventure"));
        var educational = service.recommend(trip("Unknown", 15, "Moderate", "Educational trip"));

        assertTrue(names(cold).containsAll(Set.of("jackets", "laptop", "formal clothes")));
        assertTrue(names(rainy).containsAll(Set.of("raincoat", "waterproof footwear", "trekking shoes")));
        assertTrue(names(educational).containsAll(Set.of("notebook", "stationery", "laundry bag and detergent")));
    }

    @Test
    void scalesOutfitsAndAddsDurationSpecificItems() {
        var shortTrip = service.recommend(trip("Unknown", 2, "Moderate", "Vacation"));
        var longTrip = service.recommend(trip("Unknown", 15, "Moderate", "Vacation"));

        assertEquals(2, item(shortTrip, "Everyday outfits").quantity());
        assertTrue(names(shortTrip).contains("basic travel essentials"));
        assertEquals(14, item(longTrip, "Everyday outfits").quantity());
        assertEquals(5, item(longTrip, "Additional outfits").quantity());
        assertTrue(names(longTrip).contains("laundry bag and detergent"));
    }

    @Test
    void includesEachSupportedDestinationRecommendation() {
        assertTrue(names(service.recommend(trip("Manali", 2, "Moderate", "Vacation")))
                .containsAll(Set.of("jackets", "woollen socks")));
        assertTrue(names(service.recommend(trip("Ooty", 2, "Moderate", "Vacation")))
                .contains("comfortable shoes"));
        assertTrue(names(service.recommend(trip("Kerala", 2, "Moderate", "Vacation")))
                .contains("quick-dry clothes"));
        assertTrue(names(service.recommend(trip("Bengaluru", 2, "Moderate", "Business")))
                .contains("comfortable footwear"));
    }

    private Set<String> names(java.util.List<com.bagorithm.web.model.PackingItem> items) {
        return items.stream().map(item -> item.name().toLowerCase()).collect(Collectors.toSet());
    }

    private com.bagorithm.web.model.PackingItem item(
            java.util.List<com.bagorithm.web.model.PackingItem> items, String name) {
        return items.stream().filter(candidate -> candidate.name().equals(name)).findFirst().orElseThrow();
    }

    private Trip trip(String destination, int days, String weather, String purpose) {
        return new Trip(1, 1, destination, LocalDate.now(), LocalDate.now().plusDays(days - 1), days,
                weather, purpose, 15);
    }
}
