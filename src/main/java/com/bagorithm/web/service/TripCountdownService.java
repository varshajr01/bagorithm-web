package com.bagorithm.web.service;

import com.bagorithm.web.model.Trip;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class TripCountdownService {
    public String message(Trip trip, LocalDate today) {
        if (trip.departureDate() == null) return "Departure date not set.";
        if (trip.departureDate().isBefore(today)) return "Trip date has passed.";
        if (trip.departureDate().isEqual(today)) return "Your trip starts today!";
        return ChronoUnit.DAYS.between(today, trip.departureDate()) + " days remaining";
    }
}
