package com.reservo.service.impl;

import com.reservo.modelo.property.ReservoImage;
import com.reservo.service.ImageService;
import com.reservo.service.ResetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private ResetService resetService;

    @AfterEach
    void tearDown() {
        resetService.resetAll();
    }

    @Test
    void shouldSaveAndDeleteImageSuccessfully() throws Exception {
        // Arrange
        InputStream is = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        if (is == null) throw new IllegalStateException("logo.jpg no encontrado en src/test/resources/");

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                is.readAllBytes()
        );

        // Act
        ReservoImage saved = imageService.saveImage(image);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getPublicId());
        assertNotNull(saved.getUrl());
        assertTrue(saved.getUrl().startsWith("https://"));
    }

    @Test
    void shouldSaveMultipleImagesSuccessfully() throws Exception {
        // Arrange
        InputStream is1 = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        InputStream is2 = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        if (is1 == null || is2 == null) throw new IllegalStateException("logo.jpg no encontrado en src/test/resources/");

        MockMultipartFile image1 = new MockMultipartFile(
                "file",
                "test-image-1.jpg",
                "image/jpeg",
                is1.readAllBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "file",
                "test-image-2.jpg",
                "image/jpeg",
                is2.readAllBytes()
        );

        // Act
        List<ReservoImage> saved = imageService.saveImages(List.of(image1, image2));

        // Assert
        assertNotNull(saved);
        assertEquals(2, saved.size());
        assertNotNull(saved.get(0).getPublicId());
        assertNotNull(saved.get(1).getPublicId());
        assertNotEquals(saved.get(0).getPublicId(), saved.get(1).getPublicId());
    }

    @Test
    void shouldHandleNullImageFile() {
        // Act
        ReservoImage result = imageService.saveImage(null);

        // Assert
        assertNull(result);
    }

    @Test
    void shouldHandleEmptyImagesList() {
        // Act
        List<ReservoImage> result = imageService.saveImages(List.of());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeleteImageSuccessfully() throws Exception {
        // Arrange
        InputStream is = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        if (is == null) throw new IllegalStateException("logo.jpg no encontrado en src/test/resources/");

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                is.readAllBytes()
        );

        ReservoImage saved = imageService.saveImage(image);

        // Act
        imageService.deleteImage(saved.getPublicId());

        // Assert - Si no lanza excepción, el delete fue exitoso
        assertNotNull(saved.getPublicId());
    }

    @Test
    void shouldDeleteMultipleImagesSuccessfully() throws Exception {
        // Arrange
        InputStream is1 = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        InputStream is2 = getClass().getClassLoader().getResourceAsStream("logo.jpg");
        if (is1 == null || is2 == null) throw new IllegalStateException("logo.jpg no encontrado en src/test/resources/");

        MockMultipartFile image1 = new MockMultipartFile("file", "test-1.jpg", "image/jpeg", is1.readAllBytes());
        MockMultipartFile image2 = new MockMultipartFile("file", "test-2.jpg", "image/jpeg", is2.readAllBytes());

        List<ReservoImage> saved = imageService.saveImages(List.of(image1, image2));

        // Act
        imageService.deleteImages(saved);

        // Assert - Si no lanza excepción, el delete fue exitoso
        assertEquals(2, saved.size());
    }
}
