package com.bagorithm.web.controller;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;
import com.bagorithm.web.service.RegistrationService;
import com.bagorithm.web.service.RecommendationService;
import com.bagorithm.web.service.TripCountdownService;
import com.bagorithm.web.service.TravelToolsService;
import com.bagorithm.web.service.TripService;
import com.bagorithm.web.service.WeightCalculator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class BagorithmController {
    private static final Set<String> TOOL_KINDS = Set.of("itinerary", "shopping", "souvenirs", "checklist", "memories");
    private final JdbcTemplate jdbc;
    private final TripService trips;
    private final TravelToolsService tools;
    private final RegistrationService registration;
    private final RecommendationService recommendations;
    private final TripCountdownService countdownService;
    private final WeightCalculator weightCalculator = new WeightCalculator();

    public BagorithmController(JdbcTemplate jdbc, TripService trips, TravelToolsService tools,
                               RegistrationService registration, RecommendationService recommendations,
                               TripCountdownService countdownService) {
        this.jdbc = jdbc;
        this.trips = trips;
        this.tools = tools;
        this.registration = registration;
        this.recommendations = recommendations;
        this.countdownService = countdownService;
    }

    @GetMapping({"/", "/welcome"})
    public String welcome() {
        return "welcome";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                           RedirectAttributes flash) {
        try {
            registration.register(name, email, password);
            flash.addFlashAttribute("notice", "Your account is ready. Sign in to plan a trip.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        long userId = trips.userId(authentication.getName());
        model.addAttribute("fullName", fullName(authentication.getName()));
        model.addAttribute("trips", trips.findTrips(userId));
        model.addAttribute("travelTools", List.of(
                Map.of("name", "Itinerary", "kind", "itinerary"),
                Map.of("name", "Shopping", "kind", "shopping"),
                Map.of("name", "Souvenirs", "kind", "souvenirs"),
                Map.of("name", "Home checklist", "kind", "checklist"),
                Map.of("name", "Travel journal", "kind", "memories")));
        return "dashboard";
    }

    @GetMapping("/trips/new")
    public String newTrip() {
        return "trip-form";
    }

    @PostMapping("/trips")
    public String createTrip(Authentication authentication,
                             @RequestParam String destination,
                             @RequestParam(required = false) LocalDate departureDate,
                             @RequestParam(required = false) LocalDate returnDate,
                             @RequestParam String weather, @RequestParam String purpose,
                             @RequestParam double maxCapacityKg, RedirectAttributes flash) {
        try {
            Trip trip = trips.create(trips.userId(authentication.getName()), destination, departureDate,
                    returnDate, weather, purpose, maxCapacityKg);
            return "redirect:/trips/" + trip.id();
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/trips/new";
        }
    }

    @GetMapping("/trips/{tripId}")
    public String trip(@PathVariable long tripId, Authentication authentication, Model model) {
        Trip trip = ownedTrip(tripId, authentication);
        List<PackingItem> items = trips.packingItems(trip.id());
        double kg = weightCalculator.calculateKg(items);
        model.addAttribute("trip", trip);
        model.addAttribute("recommendations", recommendations.recommend(trip));
        model.addAttribute("items", items);
        model.addAttribute("totalKg", kg);
        model.addAttribute("remainingKg", weightCalculator.remainingCapacityKg(trip, items));
        model.addAttribute("withinCapacity", weightCalculator.isWithinCapacity(trip, items));
        model.addAttribute("packedCount", items.stream().filter(PackingItem::packed).count());
        return "trip";
    }

    @GetMapping("/trips/{tripId}/recommendations")
    public String recommendations(@PathVariable long tripId, Authentication authentication) {
        ownedTrip(tripId, authentication);
        return "redirect:/trips/" + tripId + "#packing";
    }

    @PostMapping("/trips/{tripId}/packing")
    public String addPacking(@PathVariable long tripId, Authentication authentication,
                             @RequestParam String name, @RequestParam String category,
                             @RequestParam int quantity, @RequestParam double unitWeightGrams,
                             RedirectAttributes flash) {
        ownedTrip(tripId, authentication);
        try {
            trips.addPackingItem(tripId, name, category, quantity, unitWeightGrams);
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/trips/" + tripId + "#packing";
    }

    @PostMapping("/trips/{tripId}/packing/{itemId}/toggle")
    public String togglePacking(@PathVariable long tripId, @PathVariable long itemId, Authentication authentication) {
        ownedTrip(tripId, authentication);
        trips.togglePacked(tripId, itemId);
        return "redirect:/trips/" + tripId + "#packing";
    }

    @PostMapping("/trips/{tripId}/packing/{itemId}/selection")
    public String togglePackingSelection(@PathVariable long tripId, @PathVariable long itemId, Authentication authentication) {
        ownedTrip(tripId, authentication);
        trips.toggleSelected(tripId, itemId);
        return "redirect:/trips/" + tripId + "#packing";
    }

    @PostMapping("/trips/{tripId}/packing/{itemId}/edit")
    public String editPacking(@PathVariable long tripId, @PathVariable long itemId, Authentication authentication,
                              @RequestParam int quantity, @RequestParam double unitWeightGrams,
                              RedirectAttributes flash) {
        ownedTrip(tripId, authentication);
        try {
            trips.updatePackingItem(tripId, itemId, quantity, unitWeightGrams);
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/trips/" + tripId + "#packing";
    }

    @PostMapping("/trips/{tripId}/packing/{itemId}/delete")
    public String deletePacking(@PathVariable long tripId, @PathVariable long itemId, Authentication authentication) {
        ownedTrip(tripId, authentication);
        trips.deletePackingItem(tripId, itemId);
        return "redirect:/trips/" + tripId + "#packing";
    }

    @GetMapping("/previous")
    public String previous(Authentication authentication, Model model) {
        model.addAttribute("trips", trips.findTrips(trips.userId(authentication.getName())));
        return "previous";
    }

    @GetMapping("/tools/countdown")
    public String countdown(Authentication authentication, Model model) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> countdowns = trips.findTrips(trips.userId(authentication.getName())).stream()
                .map(trip -> {
                    return Map.<String, Object>of("trip", trip, "message", countdownService.message(trip, today));
                }).toList();
        model.addAttribute("countdowns", countdowns);
        return "countdown";
    }

    @GetMapping("/souvenirs")
    public String allSouvenirs(@RequestParam(required = false) Long tripId,
                               Authentication authentication, Model model) {
        long userId = trips.userId(authentication.getName());
        if (tripId != null && trips.findTrip(tripId, userId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("trips", trips.findTrips(userId));
        model.addAttribute("souvenirs", tools.souvenirsForUser(userId, tripId));
        model.addAttribute("selectedTripId", tripId);
        return "souvenirs";
    }

    @GetMapping("/uploads/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> photo(@PathVariable String filename, Authentication authentication) {
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!tools.ownsPhoto(authentication.getName(), filename))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var path = tools.photoPath(filename);
        if (!java.nio.file.Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".gif") ? MediaType.IMAGE_GIF : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(path));
    }

    @GetMapping("/trips/{tripId}/tools/{kind}")
    public String tool(@PathVariable long tripId, @PathVariable String kind, Authentication authentication, Model model) {
        validateKind(kind);
        Trip trip = ownedTrip(tripId, authentication);
        if (kind.equals("checklist")) tools.ensureChecklist(tripId);
        List<Map<String, Object>> rows = tools.rows(kind, tripId);
        long completed = rows.stream().filter(row -> Boolean.TRUE.equals(row.get("completed"))).count();
        model.addAttribute("trip", trip);
        model.addAttribute("kind", kind);
        model.addAttribute("items", rows);
        model.addAttribute("completedItems", rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("completed"))).toList());
        model.addAttribute("pendingItems", rows.stream()
                .filter(row -> !Boolean.TRUE.equals(row.get("completed"))).toList());
        model.addAttribute("completedCount", completed);
        model.addAttribute("pendingCount", rows.size() - completed);
        model.addAttribute("completionPercent", rows.isEmpty() ? 0 : completed * 100 / rows.size());
        model.addAttribute("shoppingTotal", kind.equals("shopping") ? tools.shoppingTotal(tripId) : 0);
        return "tool";
    }

    @PostMapping("/trips/{tripId}/tools/{kind}/add")
    public String addToolItem(@PathVariable long tripId, @PathVariable String kind, Authentication authentication,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String location,
                              @RequestParam(required = false) String priority,
                              @RequestParam(required = false) LocalDate plannedDate,
                              @RequestParam(required = false) String plannedTime,
                              @RequestParam(required = false) LocalDate collectionDate,
                              @RequestParam(required = false) Double price,
                              @RequestParam(required = false) Integer quantity,
                              @RequestParam(required = false) String place,
                              @RequestParam(required = false) Integer rating,
                              @RequestParam(required = false) String favouritePlaces,
                              @RequestParam(required = false) MultipartFile photo,
                              @RequestParam(required = false) List<MultipartFile> photos,
                              RedirectAttributes flash) {
        validateKind(kind);
        ownedTrip(tripId, authentication);
        try {
            switch (kind) {
                case "itinerary" -> tools.addItinerary(tripId, name, description, plannedDate, plannedTime, location, priority);
                case "shopping" -> tools.addShopping(tripId, name, category, requiredPrice(price), requiredQuantity(quantity));
                case "souvenirs" -> tools.addSouvenir(tripId, name, description, place, collectionDate, photo);
                case "checklist" -> tools.addChecklistTask(tripId, name);
                case "memories" -> tools.addMemory(tripId, name, description, favouritePlaces,
                        rating == null ? 5 : rating, photos == null ? List.of() : photos);
                default -> throw new IllegalArgumentException("Unsupported travel tool.");
            }
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/trips/" + tripId + "/tools/" + kind;
    }

    @PostMapping("/trips/{tripId}/tools/{kind}/{itemId}/toggle")
    public String toggleToolItem(@PathVariable long tripId, @PathVariable String kind, @PathVariable long itemId,
                                 Authentication authentication) {
        validateKind(kind);
        ownedTrip(tripId, authentication);
        tools.toggle(kind, tripId, itemId);
        return "redirect:/trips/" + tripId + "/tools/" + kind;
    }

    @PostMapping("/trips/{tripId}/tools/{kind}/{itemId}/edit")
    public String editToolItem(@PathVariable long tripId, @PathVariable String kind, @PathVariable long itemId,
                               Authentication authentication, @RequestParam(required = false) String name,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String location,
                               @RequestParam(required = false) String priority,
                               @RequestParam(required = false) LocalDate plannedDate,
                               @RequestParam(required = false) String plannedTime,
                               @RequestParam(required = false) LocalDate collectionDate,
                               @RequestParam(required = false) Double price,
                               @RequestParam(required = false) Integer quantity,
                               @RequestParam(required = false) String place,
                               @RequestParam(required = false) Integer rating,
                               @RequestParam(required = false) String favouritePlaces,
                               @RequestParam(required = false) MultipartFile photo,
                               RedirectAttributes flash) {
        validateKind(kind);
        ownedTrip(tripId, authentication);
        try {
            switch (kind) {
                case "itinerary" -> tools.updateItinerary(tripId, itemId, name, description, plannedDate, plannedTime, location, priority);
                case "shopping" -> tools.updateShopping(tripId, itemId, name, category, requiredPrice(price), requiredQuantity(quantity));
                case "souvenirs" -> tools.updateSouvenir(tripId, itemId, name, description, place, collectionDate, photo);
                case "checklist" -> tools.updateChecklistTask(tripId, itemId, name);
                case "memories" -> tools.updateMemory(tripId, itemId, name, description, favouritePlaces, rating == null ? 5 : rating);
                default -> throw new IllegalArgumentException("Unsupported travel tool.");
            }
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/trips/" + tripId + "/tools/" + kind;
    }

    @PostMapping("/trips/{tripId}/tools/{kind}/{itemId}/delete")
    public String deleteToolItem(@PathVariable long tripId, @PathVariable String kind, @PathVariable long itemId,
                                 Authentication authentication, RedirectAttributes flash) {
        validateKind(kind);
        ownedTrip(tripId, authentication);
        try {
            tools.delete(kind, tripId, itemId);
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/trips/" + tripId + "/tools/" + kind;
    }

    @ModelAttribute("fullName")
    public String currentName(Authentication authentication) {
        return authentication == null ? "" : fullName(authentication.getName());
    }

    private String fullName(String email) {
        return jdbc.query("SELECT full_name FROM app_users WHERE lower(email) = lower(?)",
                        (row, number) -> row.getString(1), email).stream().findFirst().orElse("Traveler");
    }

    private Trip ownedTrip(long tripId, Authentication authentication) {
        return trips.findTrip(tripId, trips.userId(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateKind(String kind) {
        if (!TOOL_KINDS.contains(kind)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private double requiredPrice(Double value) {
        if (value == null) throw new IllegalArgumentException("Estimated price is required.");
        return value;
    }

    private int requiredQuantity(Integer value) {
        if (value == null) throw new IllegalArgumentException("Quantity is required.");
        return value;
    }
}
