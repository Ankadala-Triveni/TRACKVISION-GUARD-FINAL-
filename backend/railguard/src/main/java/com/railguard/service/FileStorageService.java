package com.railguard.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Service managing secure file uploads, magic-byte content validation, and relative path storage keys.
 */
@Service
public class FileStorageService {

    private final Path rootStorageLocation;
    private final Tika tika = new Tika();

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png");

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStorageLocation);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage directory: " + e.getMessage(), e);
        }
    }

    public String storeFile(Long inspectionId, MultipartFile file) {
        // 1. Null / Empty Check
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        // 2. Size Check
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 10MB.");
        }

        // 3. Extension Check
        String originalFilename = FilenameUtils.getName(file.getOriginalFilename());
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file extension. Allowed extensions: JPG, JPEG, PNG.");
        }

        // 4. Magic Byte Content Inspection via Apache Tika
        try (InputStream inputStream = file.getInputStream()) {
            String detectedMimeType = tika.detect(inputStream);
            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                throw new IllegalArgumentException("Security violation: Magic-byte inspection failed. Content type '" + detectedMimeType + "' is not a valid image.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed reading file content for security validation.");
        }

        // 5. Server-side UUID Filename Generation
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedFilename = "inst_" + inspectionId + "_" + uuid + "." + extension;

        // Relative path key to store in MySQL: "inspections/{id}/inst_{id}_{uuid}.ext"
        String relativeFolder = "inspections/" + inspectionId;
        String relativeKey = relativeFolder + "/" + storedFilename;

        // 6. Path Traversal Prevention & Absolute Local File Write
        Path inspectionSubDir = this.rootStorageLocation.resolve(relativeFolder).normalize();
        Path targetLocation = inspectionSubDir.resolve(storedFilename).normalize();

        if (!targetLocation.startsWith(this.rootStorageLocation)) {
            throw new SecurityException("Path traversal attack detected.");
        }

        try {
            Files.createDirectories(inspectionSubDir);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk: " + e.getMessage(), e);
        }

        return relativeKey;
    }

    public Path loadFileAsPath(String relativeKey) {
        Path filePath = this.rootStorageLocation.resolve(relativeKey).normalize();
        if (!filePath.startsWith(this.rootStorageLocation)) {
            throw new SecurityException("Access denied.");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + relativeKey);
        }
        return filePath;
    }
}
