package com.bagorithm.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.env.MockEnvironment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TravelToolsServiceTest {
    @TempDir
    Path temporaryDirectory;

    private JdbcTemplate jdbc;
    private Environment environment;
    private TravelToolsService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        environment = new MockEnvironment().withProperty("bagorithm.upload-dir", temporaryDirectory.toString());
        service = new TravelToolsService(jdbc, environment);
    }

    @Test
    void validatesRequiredActivityFieldsBeforeDatabaseWrite() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addItinerary(1, "Visit", "", null, null, "", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.addItinerary(1, "Visit", "", LocalDate.now(), "not-a-time", "", "High"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void validatesShoppingPriceAndQuantityBeforeDatabaseWrite() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addShopping(1, "Adapter", "", Double.NaN, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.addShopping(1, "Adapter", "", 25, 0));
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsSpoofedOrCorruptPhotoBeforeDatabaseWrite() throws Exception {
        var corrupt = new MockMultipartFile("photo", "photo.jpg", "image/jpeg", "not an image".getBytes());
        var corruptPng = new MockMultipartFile("photo", "photo.png", "image/png",
                java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZQmcAAAAASUVORK5CYII="));

        assertThrows(IllegalArgumentException.class,
                () -> service.addSouvenir(1, "Shell", "", "Goa", LocalDate.now(), corrupt));
        assertThrows(IllegalArgumentException.class,
                () -> service.addSouvenir(1, "Shell", "", "Goa", LocalDate.now(), corruptPng));

        verifyNoInteractions(jdbc);
        assertEquals(0, filesIn(temporaryDirectory));
    }

    @Test
    void storesValidPhotosUnderStableConfiguredDirectory() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        var photo = new MockMultipartFile("photo", "souvenir.png", "image/png", output.toByteArray());

        service.addSouvenir(1, "Shell", "Beach find", "Goa", LocalDate.now(), photo);

        assertEquals(1, filesIn(temporaryDirectory));
        Path storedPhoto;
        try (var files = Files.list(temporaryDirectory)) {
            storedPhoto = files.findFirst().orElseThrow();
        }
        assertTrue(Files.size(storedPhoto) > 0);
        TravelToolsService reopened = new TravelToolsService(jdbc, environment);
        assertEquals(1, filesIn(temporaryDirectory));
        assertTrue(reopened.photoPath(storedPhoto.getFileName().toString()).startsWith(temporaryDirectory));
    }

    private long filesIn(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }
}
