package com.bagorithm.web.service;

import com.bagorithm.web.model.Trip;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripCountdownServiceTest {
    private final TripCountdownService service = new TripCountdownService();
    private final LocalDate today = LocalDate.of(2026, 11, 8);

    @Test
    void reportsMissingPastTodayAndFutureDepartureDates() {
        assertEquals("Departure date not set.", service.message(trip(null), today));
        assertEquals("Trip date has passed.", service.message(trip(today.minusDays(1)), today));
        assertEquals("Your trip starts today!", service.message(trip(today), today));
        assertEquals("3 days remaining", service.message(trip(today.plusDays(3)), today));
    }

    private Trip trip(LocalDate departure) {
        return new Trip(1, 1, "Goa", departure, null, 1, "Hot", "Vacation", 15);
    }
}
