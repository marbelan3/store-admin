package com.g2u.admin.service.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file, UUID tenantId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        // Build tenant directory
        Path tenantDir = rootLocation.resolve(tenantId.toString()).normalize();
        Files.createDirectories(tenantDir);

        // Generate unique filename preserving extension
        String originalFilename = file.getOriginalFilename();
        String safeName = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_")
                : "file";
        String storedFilename = UUID.randomUUID() + "-" + safeName;

        Path destinationFile = tenantDir.resolve(storedFilename).normalize();

        // Security check: ensure the file is stored within the upload directory
        if (!destinationFile.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Cannot store file outside upload directory");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path: tenantId/filename
        return tenantId + "/" + storedFilename;
    }

    @Override
    public Resource load(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new IllegalArgumentException("Cannot access file outside upload directory");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new IllegalArgumentException("Cannot delete file outside upload directory");
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + path, e);
        }
    }
}
