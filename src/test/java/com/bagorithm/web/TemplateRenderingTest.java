package com.bagorithm.web;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRenderingTest {
    @Test
    void rendersAllBrowserPagesAndTravelToolVariants() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("");
        WebContext context = new WebContext(webApplication.buildExchange(request, new MockHttpServletResponse()), Locale.US);
        Trip trip = new Trip(1, 1, "Goa", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 4), 4, "Hot", "Vacation", 15);
        PackingItem packingItem = new PackingItem(1, 1, "T-shirts", "Clothing", 2, 150, false, true);
        context.setVariable("fullName", "Taylor");
        context.setVariable("trips", List.of(trip));
        context.setVariable("travelTools", List.of(
                Map.of("name", "Itinerary", "kind", "itinerary"),
                Map.of("name", "Shopping", "kind", "shopping"),
                Map.of("name", "Souvenirs", "kind", "souvenirs"),
                Map.of("name", "Home checklist", "kind", "checklist"),
                Map.of("name", "Travel journal", "kind", "memories")));
        context.setVariable("trip", trip);
        context.setVariable("items", List.of(packingItem));
        context.setVariable("recommendations", List.of(packingItem));
        context.setVariable("totalKg", 0.3);
        context.setVariable("remainingKg", 14.7);
        context.setVariable("withinCapacity", true);
        context.setVariable("packedCount", 0);
        context.setVariable("countdowns", List.of(Map.of("trip", trip, "message", "23 days remaining")));
        context.setVariable("completedCount", 0);
        context.setVariable("pendingCount", 1);
        context.setVariable("completionPercent", 0);
        context.setVariable("shoppingTotal", 250.0);
        context.setVariable("completedItems", List.of());
        context.setVariable("pendingItems", List.of());
        context.setVariable("souvenirs", List.of());
        context.setVariable("selectedTripId", null);
        context.setVariable("kind", "itinerary");

        for (String page : List.of("welcome", "login", "register", "dashboard", "trip-form", "trip", "previous", "countdown", "souvenirs", "fragments", "error")) {
            assertTrue(engine.process(page, context).contains("<"), page + " template should render");
        }

        for (String kind : List.of("itinerary", "shopping", "souvenirs", "checklist", "memories")) {
            context.setVariable("kind", kind);
            List<Map<String, Object>> rows = switch (kind) {
                case "itinerary" -> List.of(Map.of("id", 1L, "name", "Beach visit", "description", "A day at the beach.",
                        "planned_date", LocalDate.now(), "planned_time", LocalTime.NOON, "location", "Goa",
                        "priority", "High", "completed", false));
                case "shopping" -> List.of(Map.of("id", 1L, "name", "Sunscreen", "category", "Personal care",
                        "estimated_price", 250.0, "quantity", 1, "total", 250.0, "completed", false));
                case "souvenirs" -> List.of(Map.of("id", 1L, "name", "Shell", "description", "Collected on the beach.",
                        "collected_place", "Goa", "collection_date", LocalDate.now(), "photo_path", "123e4567-e89b-12d3-a456-426614174000.jpg"));
                case "checklist" -> List.of(Map.of("id", 1L, "name", "Lock all doors", "completed", false, "is_default", true));
                default -> List.of(Map.of("id", 1L, "name", "Great trip", "description", "Wonderful journey.",
                        "favourite_places", "Goa", "rating", 5, "photo_paths", "123e4567-e89b-12d3-a456-426614174000.jpg"));
            };
            context.setVariable("items", rows);
            context.setVariable("completedItems", List.of());
            context.setVariable("pendingItems", rows);
            String html = engine.process("tool", context);
            assertTrue(html.contains("Save changes"), kind + " form should render");
        }
    }
}
