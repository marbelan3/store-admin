package com.g2u.admin.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface FileStorageService {

    /**
     * Store a file and return its relative path.
     */
    String store(MultipartFile file, UUID tenantId) throws IOException;

    /**
     * Load a file as a Resource by its stored path (tenantId/filename).
     */
    Resource load(String path);

    /**
     * Delete a file by its stored path.
     */
    void delete(String path);
}
