package com.bagorithm.web.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Trip(long id, long userId, String destination, LocalDate departureDate, LocalDate returnDate,
                   int durationDays, String weather, String purpose, double maxCapacityKg) {
    public static int duration(LocalDate departure, LocalDate returning) {
        return departure == null || returning == null ? 1
                : Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(departure, returning)) + 1);
    }
}
