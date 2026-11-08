package com.bagorithm.web.service;

import com.bagorithm.web.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.PreparedStatementCreator;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TripServiceTest {
    private JdbcTemplate jdbc;
    private TripService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new TripService(jdbc, new RecommendationService());
    }

    @Test
    void rejectsInvalidDatesAndUnsupportedTripFieldsBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1, "Goa",
                LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 3), "Hot", "Vacation", 15));
        assertThrows(IllegalArgumentException.class, () -> service.create(1, "Goa",
                null, null, "Tropical", "Vacation", 15));
        assertThrows(IllegalArgumentException.class, () -> service.create(1, "Goa",
                null, null, "Hot", "Vacation", Double.NaN));
        verifyNoInteractions(jdbc);
    }

    @Test
    void savesTripAndDestinationSpecificPackingRecommendationsAtomically() {
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder holder = invocation.getArgument(1);
            holder.getKeyList().add(Map.of("id", 42L));
            return 1;
        });

        Trip trip = service.create(7, "Goa", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 4), "Hot", "Vacation", 15);

        assertEquals(42, trip.id());
        assertEquals(4, trip.durationDays());
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO packing_items"),
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("Travel documents"),
                org.mockito.ArgumentMatchers.eq("Essentials"),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(100.0));
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO packing_items"),
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("Beachwear"),
                org.mockito.ArgumentMatchers.eq("Clothing"),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(600.0));
    }

    @Test
    void togglingSelectionPersistsPerPackingRow() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        service.toggleSelected(9, 15);

        verify(jdbc).update("UPDATE packing_items SET selected = NOT selected WHERE id = ? AND trip_id = ?", 15L, 9L);
    }
}
