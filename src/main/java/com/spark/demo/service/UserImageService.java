// src/main/java/com/spark/demo/service/UserImageService.java
package com.spark.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class UserImageService {

    @Value("${upload.base-dir}")
    private String baseDir;

    public void storeProfileImage(MultipartFile file, Long userId) throws Exception {
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") || contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Only PNG/JPEG allowed");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large (max 2MB)");
        }

        String ext = contentType.equals("image/png") ? ".png" : ".jpg";

        Path dir = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // Clean up the other format if it exists (e.g., replacing png with jpg)
        Path png = dir.resolve("user-" + userId + ".png");
        Path jpg = dir.resolve("user-" + userId + ".jpg");
        if (".png".equals(ext) && Files.exists(jpg)) {
            Files.deleteIfExists(jpg);
        }
        if (".jpg".equals(ext) && Files.exists(png)) {
            Files.deleteIfExists(png);
        }

        Path target = dir.resolve("user-" + userId + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Helper for controller to find the stored local image path if any.
     * Returns null if none exists.
     */
    public Path findLocalProfileImagePath(Long userId) {
        Path dir = Paths.get(baseDir).toAbsolutePath().normalize();
        Path png = dir.resolve("user-" + userId + ".png");
        if (Files.exists(png)) return png;
        Path jpg = dir.resolve("user-" + userId + ".jpg");
        if (Files.exists(jpg)) return jpg;
        return null;
    }
}
