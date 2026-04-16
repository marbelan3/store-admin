package com.g2u.admin.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductOptionDto(
        UUID id,
        String name,
        int displayOrder,
        List<OptionValueDto> values,
        Instant createdAt
) {
    public record OptionValueDto(
            UUID id,
            String value,
            int displayOrder
    ) {}
}
