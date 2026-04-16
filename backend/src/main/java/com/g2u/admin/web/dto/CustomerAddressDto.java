package com.g2u.admin.web.dto;

import java.util.UUID;

public record CustomerAddressDto(
        UUID id,
        String type,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault
) {
}
