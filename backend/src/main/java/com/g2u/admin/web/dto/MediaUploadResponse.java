package com.g2u.admin.web.dto;

public record MediaUploadResponse(
        String url,
        String filename,
        String contentType,
        long size
) {
}
