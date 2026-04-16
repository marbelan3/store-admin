package com.g2u.admin.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 255) String email,
        @Size(max = 50) String phone,
        String notes
) {
}
