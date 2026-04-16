package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String slug
) {
}
